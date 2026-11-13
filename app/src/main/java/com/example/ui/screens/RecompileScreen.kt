package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameFolderScanner
import com.example.model.DeviceHardwareProfile
import com.example.model.GameDirectoryStatus
import com.example.model.GameFileCategory
import com.example.model.RecompilePipelineState
import com.example.model.RecompileStageStatus
import com.example.ui.components.StatusPill
import com.example.ui.components.StormCard
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RasenganCyan
import com.example.ui.theme.ShinobiAmber
import com.example.ui.theme.ShinobiEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RecompileScreen(
    recompileState: RecompilePipelineState,
    gameDirectoryStatus: GameDirectoryStatus,
    hardwareProfile: DeviceHardwareProfile,
    onRunRecompile: () -> Unit,
    onRunRecompileAndLaunch: () -> Unit,
    onLaunchGame: () -> Unit,
    onPickGameFolder: () -> Unit,
    onExportAllToFolder: () -> Unit,
    onExportSingleConfigIni: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onLaunchExternalContainer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = recompileState.overallProgress,
        animationSpec = spring(),
        label = "recompile_progress"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 880.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pipeline Control Header
                StormCard(accentColor = ChakraOrange) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = null, tint = ChakraOrange)
                            Text(
                                text = "Pipeline Recompile PC -> Android",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        StatusPill(
                            label = when {
                                recompileState.isRunning -> "Berjalan"
                                recompileState.hasRunOnce -> "Terverifikasi"
                                else -> "Siap"
                            },
                            color = when {
                                recompileState.isRunning -> RasenganCyan
                                recompileState.hasRunOnce -> ShinobiEmerald
                                else -> ChakraOrange
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recompileState.lastCompletedLabel
                            ?: "Memverifikasi NSUNSR.exe, arsip CPK, profil shader Vulkan/GLES, dan jembatan XInput untuk perangkat ${hardwareProfile.primaryAbi}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ChakraOrange,
                        trackColor = ObsidianSurfaceHigh
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRunRecompile,
                        enabled = !recompileState.isRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_recompile_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChakraOrange,
                            contentColor = ObsidianBg
                        )
                    ) {
                        if (recompileState.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ObsidianBg,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memproses Recompile & Verifikasi...")
                        } else {
                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.action_run_recompile))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (recompileState.canLaunchGame) {
                        Button(
                            onClick = onLaunchGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("launch_game_after_recompile_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShinobiEmerald,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jalankan Game Sekarang", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onRunRecompileAndLaunch,
                            enabled = !recompileState.isRunning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("recompile_and_launch_button"),
                            border = BorderStroke(1.dp, ShinobiEmerald.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShinobiEmerald)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recompile & Langsung Jalankan")
                        }
                    }
                }

                // 5-Stage Pipeline Steps
                StormCard {
                    Text(
                        text = "Tahapan Verifikasi & Translasi",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    recompileState.steps.forEach { step ->
                        val (icon, tint) = when (step.status) {
                            RecompileStageStatus.PENDING -> Icons.Filled.HourglassTop to TextMuted
                            RecompileStageStatus.RUNNING -> Icons.Filled.Build to RasenganCyan
                            RecompileStageStatus.VERIFIED -> Icons.Filled.CheckCircle to ShinobiEmerald
                            RecompileStageStatus.WARNING -> Icons.Filled.Warning to ShinobiAmber
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (step.status == RecompileStageStatus.RUNNING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(20.dp),
                                        color = RasenganCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = step.status.name,
                                        tint = tint,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(20.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = step.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = step.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = step.detail,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tint
                                    )
                                }
                            }
                        }
                    }
                }

                // Export & Runtime Actions
                StormCard {
                    Text(
                        text = "Ekspor Berkas & Peluncuran",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (gameDirectoryStatus is GameDirectoryStatus.Ready) {
                        Button(
                            onClick = onExportAllToFolder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_bundle_to_folder_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RasenganCyan,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan config.ini, dxvk.conf & Map ke Folder PC")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        OutlinedButton(
                            onClick = onPickGameFolder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChakraOrange)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.action_select_folder))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportSingleConfigIni,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_single_ini_button"),
                            border = BorderStroke(1.dp, ObsidianOutline)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor INI", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onOpenDiagnostic,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("recompile_diagnostic_button"),
                            border = BorderStroke(1.dp, RasenganCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RasenganCyan)
                        ) {
                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uji Frame")
                        }
                    }

                    if (hardwareProfile.installedRuntimePackage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onLaunchExternalContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShinobiEmerald,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Luncurkan di ${hardwareProfile.installedRuntimePackage}")
                        }
                    }
                }

                // Diagnostic Console Log
                if (recompileState.logLines.isNotEmpty()) {
                    StormCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Terminal, contentDescription = null, tint = RasenganCyan)
                            Text("Log Konsol Recompile", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ObsidianBg,
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                recompileState.logLines.forEach { line ->
                                    Text(
                                        text = line,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Scanned PC Game Files Inspector
                if (gameDirectoryStatus is GameDirectoryStatus.Ready && gameDirectoryStatus.files.isNotEmpty()) {
                    StormCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = ChakraOrange)
                            Text(
                                text = "Berkas Terdeteksi (${gameDirectoryStatus.files.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        gameDirectoryStatus.files.take(25).forEach { file ->
                            val badgeColor = when (file.category) {
                                GameFileCategory.EXECUTABLE -> ChakraOrange
                                GameFileCategory.ARCHIVE_CPK -> RasenganCyan
                                GameFileCategory.LIBRARY_DLL -> ShinobiAmber
                                GameFileCategory.CONFIG_INI -> ShinobiEmerald
                                GameFileCategory.OTHER -> TextMuted
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = file.relativePath,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                    if (file.peArchLabel != null) {
                                        Text(
                                            text = file.peArchLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ChakraOrange
                                        )
                                    }
                                }
                                Text(
                                    text = GameFolderScanner.formatBytes(file.sizeBytes),
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
