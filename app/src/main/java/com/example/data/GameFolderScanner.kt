package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.model.GameDirectoryStatus
import com.example.model.GameFileCategory
import com.example.model.GameFileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class GameFolderScanner(private val context: Context) {

    suspend fun scanTree(treeUri: Uri): GameDirectoryStatus = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val folderName = queryDisplayName(rootDocUri) ?: "Folder Game PC"

            val collected = mutableListOf<GameFileEntry>()
            var existingConfigSnippet: String? = null
            var exeArchLabel: String? = null
            var exeDetected = false

            fun walk(parentDocId: String, prefix: String, depth: Int) {
                if (depth > 2 || collected.size >= 120) return
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
                resolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idIdx) ?: continue
                        val name = cursor.getString(nameIdx) ?: continue
                        val mime = cursor.getString(mimeIdx).orEmpty()
                        val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                        val relPath = if (prefix.isEmpty()) name else "$prefix/$name"

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (name.equals("data_win32", ignoreCase = true) ||
                                name.equals("data", ignoreCase = true) ||
                                name.equals("dlc", ignoreCase = true)
                            ) {
                                walk(docId, relPath, depth + 1)
                            }
                        } else {
                            val lower = name.lowercase()
                            val category = when {
                                lower.endsWith(".exe") -> GameFileCategory.EXECUTABLE
                                lower.endsWith(".cpk") -> GameFileCategory.ARCHIVE_CPK
                                lower.endsWith(".dll") -> GameFileCategory.LIBRARY_DLL
                                lower == "config.ini" || lower.endsWith(".ini") -> GameFileCategory.CONFIG_INI
                                else -> GameFileCategory.OTHER
                            }

                            var peArch: String? = null
                            if (lower == "nsunsr.exe" || (category == GameFileCategory.EXECUTABLE && !exeDetected)) {
                                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                peArch = inspectPeHeader(fileUri)
                                if (lower == "nsunsr.exe") {
                                    exeDetected = true
                                    exeArchLabel = peArch ?: "PE32 Windows"
                                }
                            }

                            if (lower == "config.ini" && existingConfigSnippet == null) {
                                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                existingConfigSnippet = readTextPreview(fileUri)
                            }

                            if (category != GameFileCategory.OTHER || lower.contains("nsunsr")) {
                                collected.add(
                                    GameFileEntry(
                                        name = name,
                                        relativePath = relPath,
                                        sizeBytes = size,
                                        category = category,
                                        peArchLabel = peArch
                                    )
                                )
                            }
                        }
                    }
                }
            }

            walk(rootDocId, "", 0)

            val sortedFiles = collected.sortedWith(
                compareBy<GameFileEntry> { it.category.ordinal }.thenBy { it.relativePath }
            )
            val cpkCount = sortedFiles.count { it.category == GameFileCategory.ARCHIVE_CPK }
            val totalBytes = sortedFiles.sumOf { it.sizeBytes }

            GameDirectoryStatus.Ready(
                treeUriString = treeUri.toString(),
                folderName = folderName,
                exeDetected = exeDetected,
                exeArchLabel = exeArchLabel,
                cpkCount = cpkCount,
                totalScannedSizeBytes = totalBytes,
                files = sortedFiles,
                existingConfigIniSnippet = existingConfigSnippet,
                isCompleteStormRevolutionBuild = exeDetected && cpkCount > 0
            )
        }.getOrElse { err ->
            GameDirectoryStatus.Error(
                err.localizedMessage ?: "Gagal membaca folder game PC melalui SAF."
            )
        }
    }

    suspend fun writeConfigFilesToFolder(
        treeUri: Uri,
        configIniContent: String,
        dxvkConfContent: String,
        controllerJsonContent: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val written = mutableListOf<String>()

            writeOrReplaceChildDocument(treeUri, rootDocId, rootDocUri, "config.ini", "text/plain", configIniContent)
            written.add("config.ini")

            writeOrReplaceChildDocument(treeUri, rootDocId, rootDocUri, "dxvk.conf", "text/plain", dxvkConfContent)
            written.add("dxvk.conf")

            writeOrReplaceChildDocument(
                treeUri,
                rootDocId,
                rootDocUri,
                "stormrev_controller.json",
                "application/json",
                controllerJsonContent
            )
            written.add("stormrev_controller.json")

            written
        }
    }

    suspend fun writeSingleDocument(uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Tidak dapat membuka aliran tulis dokumen.")
            Unit
        }
    }

    private fun writeOrReplaceChildDocument(
        treeUri: Uri,
        parentDocId: String,
        parentDocUri: Uri,
        fileName: String,
        mimeType: String,
        content: String
    ) {
        val resolver = context.contentResolver
        val existingUri = findChildUri(treeUri, parentDocId, fileName)
        val targetUri = existingUri ?: DocumentsContract.createDocument(resolver, parentDocUri, mimeType, fileName)
        ?: error("Gagal membuat berkas $fileName di folder tujuan.")

        resolver.openOutputStream(targetUri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Gagal menulis isi berkas $fileName.")
    }

    private fun findChildUri(treeUri: Uri, parentDocId: String, targetName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                if (name.equals(targetName, ignoreCase = true)) {
                    val docId = cursor.getString(idIdx)
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                }
            }
        }
        return null
    }

    private fun queryDisplayName(docUri: Uri): String? {
        context.contentResolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return null
    }

    private fun inspectPeHeader(fileUri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(fileUri)?.use { stream ->
                val buf = ByteArray(512)
                val read = stream.read(buf)
                if (read < 64) return@use null
                // Check MZ signature
                if (buf[0] != 0x4D.toByte() || buf[1] != 0x5A.toByte()) return@use "Bukan PE valid"
                val peOffset = (buf[0x3C].toInt() and 0xFF) or
                    ((buf[0x3D].toInt() and 0xFF) shl 8) or
                    ((buf[0x3E].toInt() and 0xFF) shl 16) or
                    ((buf[0x3F].toInt() and 0xFF) shl 24)
                if (peOffset < 0 || peOffset + 6 >= read) return@use "PE32 Windows (MZ)"
                if (buf[peOffset] == 0x50.toByte() && buf[peOffset + 1] == 0x45.toByte()) {
                    val machine = (buf[peOffset + 4].toInt() and 0xFF) or
                        ((buf[peOffset + 5].toInt() and 0xFF) shl 8)
                    when (machine) {
                        0x014C -> "PE32 x86 (32-bit)"
                        0x8664 -> "PE32+ x64 (64-bit)"
                        else -> "PE (0x${machine.toString(16)})"
                    }
                } else {
                    "DOS/MZ Header"
                }
            }
        }.getOrNull()
    }

    private fun readTextPreview(fileUri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(fileUri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    lines.take(18).joinToString("\n")
                }
            }
        }.getOrNull()
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
