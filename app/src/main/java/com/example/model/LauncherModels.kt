package com.example.model

import android.view.KeyEvent

enum class LauncherTab {
    HOME,
    GRAPHICS,
    CONTROLLER,
    RECOMPILE
}

enum class GraphicsPreset(val label: String, val subtitle: String) {
    POWER_SAVER("Hemat Daya", "720p • 30 FPS • Stabil"),
    BALANCED("Seimbang", "900p • 60 FPS • Direkomendasikan"),
    MAX_PERFORMANCE("Performa Maks", "1080p+ • 60 FPS • Detail Penuh"),
    CUSTOM("Kustom", "Konfigurasi manual")
}

enum class RenderBackend(val label: String, val detail: String) {
    VULKAN_DXVK("Vulkan (DXVK)", "Tercepat di Adreno/Mali modern, shader async"),
    OPENGL_ES_WINED3D("OpenGL ES 3.2", "Kompatibilitas fallback untuk GPU lawas")
}

enum class ResolutionScale(val factor: Float, val width: Int, val height: Int, val label: String) {
    SCALE_050(0.50f, 960, 540, "0.5x (540p)"),
    SCALE_075(0.75f, 1280, 720, "0.75x (720p HD)"),
    SCALE_100(1.00f, 1920, 1080, "1.0x (1080p FHD)"),
    SCALE_125(1.25f, 2240, 1260, "1.25x (1260p)"),
    SCALE_150(1.50f, 2560, 1440, "1.5x (1440p QHD)")
}

enum class FpsTarget(val fps: Int, val label: String) {
    FPS_30(30, "30 FPS (Timing Asli)"),
    FPS_60(60, "60 FPS (Patch Halus)"),
    UNCAPPED(0, "Tanpa Batas")
}

enum class ShadowLevel(val iniValue: Int, val label: String) {
    OFF(0, "Mati"),
    LOW(1, "Rendah"),
    MEDIUM(2, "Sedang"),
    HIGH(3, "Tinggi")
}

enum class AspectRatioMode(val aspectX: Int, val aspectY: Int, val label: String) {
    RATIO_16_9(16, 9, "16:9 Standar"),
    RATIO_16_10(16, 10, "16:10"),
    RATIO_20_9(20, 9, "20:9 Layar Penuh"),
    RATIO_4_3(4, 3, "4:3 Klasik")
}

enum class CpuAffinityMode(val label: String, val maskDescription: String) {
    BIG_CORES("Inti Performa (Big Cores)", "Fokus core kencang untuk thread utama"),
    ALL_CORES("Semua Inti CPU", "Distribusi merata untuk CPK & shader")
}

data class GraphicsConfig(
    val preset: GraphicsPreset = GraphicsPreset.BALANCED,
    val renderBackend: RenderBackend = RenderBackend.VULKAN_DXVK,
    val resolutionScale: ResolutionScale = ResolutionScale.SCALE_075,
    val fpsTarget: FpsTarget = FpsTarget.FPS_60,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_16_9,
    val fxaaEnabled: Boolean = true,
    val shadowLevel: ShadowLevel = ShadowLevel.MEDIUM,
    val glareEnabled: Boolean = true,
    val ssaoEnabled: Boolean = false,
    val celOutlineBoost: Boolean = true,
    val asyncShaderCompile: Boolean = true,
    val cpuAffinity: CpuAffinityMode = CpuAffinityMode.BIG_CORES,
    val textureMemoryMb: Int = 1024
) {
    fun estimatedGpuLoadPercent(): Int {
        var load = when (resolutionScale) {
            ResolutionScale.SCALE_050 -> 26
            ResolutionScale.SCALE_075 -> 42
            ResolutionScale.SCALE_100 -> 64
            ResolutionScale.SCALE_125 -> 78
            ResolutionScale.SCALE_150 -> 92
        }
        if (fpsTarget == FpsTarget.FPS_60) load += 14
        if (fpsTarget == FpsTarget.UNCAPPED) load += 20
        if (ssaoEnabled) load += 12
        if (fxaaEnabled) load += 5
        if (glareEnabled) load += 4
        load += shadowLevel.iniValue * 4
        if (renderBackend == RenderBackend.OPENGL_ES_WINED3D) load += 10
        return load.coerceIn(15, 100)
    }

    fun estimatedVramUsageMb(): Int {
        val base = 360
        val resAdd = (resolutionScale.factor * 240).toInt()
        val shadowAdd = shadowLevel.iniValue * 75
        val ssaoAdd = if (ssaoEnabled) 90 else 0
        return (base + resAdd + shadowAdd + ssaoAdd).coerceAtMost(textureMemoryMb + 256)
    }

    fun toConfigIniString(): String {
        val scaledWidth = when (aspectRatio) {
            AspectRatioMode.RATIO_20_9 -> (resolutionScale.height * 20) / 9
            AspectRatioMode.RATIO_16_10 -> (resolutionScale.height * 16) / 10
            AspectRatioMode.RATIO_4_3 -> (resolutionScale.height * 4) / 3
            AspectRatioMode.RATIO_16_9 -> resolutionScale.width
        }
        val fpsIni = if (fpsTarget == FpsTarget.FPS_30) 0 else 1
        return buildString {
            appendLine("; Dihasilkan oleh StormRev Launcher untuk Naruto Shippuden: UNS Revolution")
            appendLine("[setting]")
            appendLine("width=$scaledWidth")
            appendLine("height=${resolutionScale.height}")
            appendLine("aspectx=${aspectRatio.aspectX}")
            appendLine("aspecty=${aspectRatio.aspectY}")
            appendLine("windowed=0")
            appendLine("fxaa=${if (fxaaEnabled) 1 else 0}")
            appendLine("shadow=${shadowLevel.iniValue}")
            appendLine("ssao=${if (ssaoEnabled) 1 else 0}")
            appendLine("glare=${if (glareEnabled) 1 else 0}")
            appendLine("fps=$fpsIni")
            appendLine("controller=1")
            appendLine("[controlSetting]")
            appendLine("kb_type=1")
            appendLine("pad_enabled=1")
        }
    }

    fun toDxvkConfString(): String {
        val maxFps = if (fpsTarget == FpsTarget.UNCAPPED) 0 else fpsTarget.fps
        return buildString {
            appendLine("# StormRev Translation Profile (${renderBackend.label})")
            appendLine("dxvk.enableAsync = ${if (asyncShaderCompile) "True" else "False"}")
            appendLine("dxvk.numCompilerThreads = ${if (cpuAffinity == CpuAffinityMode.ALL_CORES) 4 else 2}")
            appendLine("dxgi.maxFrameRate = $maxFps")
            appendLine("d3d9.maxFrameRate = $maxFps")
            appendLine("d3d9.maxAvailableMemory = $textureMemoryMb")
            appendLine("d3d9.samplerAnisotropy = ${if (celOutlineBoost) 4 else 1}")
        }
    }

    fun toLaunchBatString(): String {
        val maxFps = if (fpsTarget == FpsTarget.UNCAPPED) 0 else fpsTarget.fps
        return buildString {
            appendLine("@echo off")
            appendLine("title Naruto Shippuden: UNS Revolution - StormRev Bridge")
            appendLine("set DXVK_CONFIG_FILE=dxvk.conf")
            appendLine("set DXVK_ASYNC=${if (asyncShaderCompile) "1" else "0"}")
            appendLine("set DXVK_FRAME_RATE=$maxFps")
            appendLine("set WINEESYNC=1")
            appendLine("set WINEFSYNC=1")
            appendLine("start \"\" \"NSUNSR.exe\"")
        }
    }

    companion object {
        fun fromPreset(preset: GraphicsPreset): GraphicsConfig = when (preset) {
            GraphicsPreset.POWER_SAVER -> GraphicsConfig(
                preset = GraphicsPreset.POWER_SAVER,
                renderBackend = RenderBackend.VULKAN_DXVK,
                resolutionScale = ResolutionScale.SCALE_050,
                fpsTarget = FpsTarget.FPS_30,
                fxaaEnabled = false,
                shadowLevel = ShadowLevel.LOW,
                glareEnabled = true,
                ssaoEnabled = false,
                celOutlineBoost = true,
                asyncShaderCompile = true,
                cpuAffinity = CpuAffinityMode.BIG_CORES,
                textureMemoryMb = 512
            )
            GraphicsPreset.BALANCED -> GraphicsConfig(
                preset = GraphicsPreset.BALANCED,
                renderBackend = RenderBackend.VULKAN_DXVK,
                resolutionScale = ResolutionScale.SCALE_075,
                fpsTarget = FpsTarget.FPS_60,
                fxaaEnabled = true,
                shadowLevel = ShadowLevel.MEDIUM,
                glareEnabled = true,
                ssaoEnabled = false,
                celOutlineBoost = true,
                asyncShaderCompile = true,
                cpuAffinity = CpuAffinityMode.BIG_CORES,
                textureMemoryMb = 1024
            )
            GraphicsPreset.MAX_PERFORMANCE -> GraphicsConfig(
                preset = GraphicsPreset.MAX_PERFORMANCE,
                renderBackend = RenderBackend.VULKAN_DXVK,
                resolutionScale = ResolutionScale.SCALE_100,
                fpsTarget = FpsTarget.FPS_60,
                fxaaEnabled = true,
                shadowLevel = ShadowLevel.HIGH,
                glareEnabled = true,
                ssaoEnabled = true,
                celOutlineBoost = true,
                asyncShaderCompile = true,
                cpuAffinity = CpuAffinityMode.ALL_CORES,
                textureMemoryMb = 2048
            )
            GraphicsPreset.CUSTOM -> GraphicsConfig(preset = GraphicsPreset.CUSTOM)
        }
    }
}

enum class StormAction(
    val actionName: String,
    val combatRole: String,
    val defaultKeyCode: Int
) {
    CHAKRA_LOAD("Isi Chakra", "Chakra Load / Ultimate Jutsu", KeyEvent.KEYCODE_BUTTON_Y),
    MELEE_ATTACK("Serangan Jutsu", "Serangan Jarak Dekat / Kombo", KeyEvent.KEYCODE_BUTTON_B),
    SHURIKEN("Shuriken / Kunai", "Serangan Jarak Jauh", KeyEvent.KEYCODE_BUTTON_X),
    JUMP_DASH("Lompat / Chakra Dash", "Hindar & Dash Cepat", KeyEvent.KEYCODE_BUTTON_A),
    SUBSTITUTION("Kawarimi (Substitusi)", "Menghindar dari Kombo Lawan", KeyEvent.KEYCODE_BUTTON_L2),
    GUARD("Bertahan (Guard)", "Tangkis Serangan & Counter", KeyEvent.KEYCODE_BUTTON_R2),
    SUPPORT_LEFT("Support Kiri", "Panggil Rekan Tim 1", KeyEvent.KEYCODE_BUTTON_L1),
    SUPPORT_RIGHT("Support Kanan", "Panggil Rekan Tim 2", KeyEvent.KEYCODE_BUTTON_R1),
    PAUSE("Menu Jeda", "Pause & Daftar Kombo", KeyEvent.KEYCODE_BUTTON_START),
    TARGET_SWITCH("Kunci / Reset Kamera", "Fokus Target Pertarungan", KeyEvent.KEYCODE_BUTTON_SELECT)
}

enum class ControllerLayoutPreset(val label: String) {
    XBOX("Xbox (A/B/X/Y)"),
    PLAYSTATION("PlayStation (✕/○/□/△)"),
    SWITCH_PRO("Switch Pro")
}

data class ControllerProfile(
    val layoutPreset: ControllerLayoutPreset = ControllerLayoutPreset.XBOX,
    val deadzonePercent: Int = 12,
    val triggerThresholdPercent: Int = 15,
    val vibrationEnabled: Boolean = true,
    val swapConfirmCancel: Boolean = false,
    val bindings: Map<StormAction, Int> = StormAction.entries.associateWith { it.defaultKeyCode }
) {
    fun toJsonSummary(): String = buildString {
        appendLine("{")
        appendLine("  \"profile\": \"NSUNSR_Android_XInput\",")
        appendLine("  \"layoutPreset\": \"${layoutPreset.name}\",")
        appendLine("  \"deadzone\": ${deadzonePercent / 100f},")
        appendLine("  \"triggerThreshold\": ${triggerThresholdPercent / 100f},")
        appendLine("  \"vibration\": $vibrationEnabled,")
        appendLine("  \"swapConfirmCancel\": $swapConfirmCancel,")
        appendLine("  \"bindings\": {")
        val entries = bindings.entries.toList()
        entries.forEachIndexed { idx, (action, keyCode) ->
            val comma = if (idx < entries.lastIndex) "," else ""
            appendLine("    \"${action.name}\": $keyCode$comma")
        }
        appendLine("  }")
        appendLine("}")
    }
}

data class ConnectedGamepad(
    val deviceId: Int,
    val name: String,
    val descriptor: String,
    val vendorId: Int,
    val productId: Int,
    val hasVibrator: Boolean,
    val sourcesLabel: String,
    val isExternal: Boolean
)

data class LiveGamepadState(
    val pressedKeyCodes: Set<Int> = emptySet(),
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val hatX: Float = 0f,
    val hatY: Float = 0f,
    val lastKeyCode: Int? = null,
    val lastKeyLabel: String = "Menunggu input...",
    val lastDeviceName: String = "-"
)

data class DeviceHardwareProfile(
    val deviceModel: String = "",
    val socLabel: String = "",
    val primaryAbi: String = "arm64-v8a",
    val cpuCores: Int = 8,
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val supportsVulkan: Boolean = false,
    val vulkanVersionLabel: String = "Tidak terdeteksi",
    val displayRefreshHz: Int = 60,
    val installedRuntimePackage: String? = null
)

enum class GameFileCategory {
    EXECUTABLE,
    ARCHIVE_CPK,
    LIBRARY_DLL,
    CONFIG_INI,
    OTHER
}

data class GameFileEntry(
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val category: GameFileCategory,
    val peArchLabel: String? = null
)

sealed interface GameDirectoryStatus {
    data object NotSelected : GameDirectoryStatus
    data object Scanning : GameDirectoryStatus
    data class Ready(
        val treeUriString: String,
        val folderName: String,
        val exeDetected: Boolean,
        val exeArchLabel: String?,
        val cpkCount: Int,
        val totalScannedSizeBytes: Long,
        val files: List<GameFileEntry>,
        val existingConfigIniSnippet: String?,
        val isCompleteStormRevolutionBuild: Boolean
    ) : GameDirectoryStatus

    data class Error(val message: String) : GameDirectoryStatus
}

enum class RecompileStageStatus {
    PENDING,
    RUNNING,
    VERIFIED,
    WARNING
}

data class RecompileStepResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val status: RecompileStageStatus = RecompileStageStatus.PENDING
)

data class RecompilePipelineState(
    val isRunning: Boolean = false,
    val hasRunOnce: Boolean = false,
    val overallProgress: Float = 0f,
    val steps: List<RecompileStepResult> = defaultSteps(),
    val logLines: List<String> = emptyList(),
    val lastCompletedLabel: String? = null,
    val localBundlePath: String? = null,
    val syncedToGameFolder: Boolean = false
) {
    val canLaunchGame: Boolean get() = hasRunOnce && !isRunning
    companion object {
        fun defaultSteps(): List<RecompileStepResult> = listOf(
            RecompileStepResult(
                id = "pe_verify",
                title = "1. Analisis Biner PE32 (NSUNSR.exe)",
                subtitle = "Deteksi header MZ/PE & instruksi x86 -> ARM64",
                detail = "Menunggu pemindaian"
            ),
            RecompileStepResult(
                id = "cpk_index",
                title = "2. Indeks Arsip CPK (data_win32)",
                subtitle = "Verifikasi paket data karakter, arena, & audio",
                detail = "Menunggu pemindaian"
            ),
            RecompileStepResult(
                id = "shader_bridge",
                title = "3. Translasi Shader DX9 -> Vulkan/GLES",
                subtitle = "Pembuatan profil cache shader & cel-shading Storm",
                detail = "Menunggu kompilasi profil"
            ),
            RecompileStepResult(
                id = "xinput_bridge",
                title = "4. Jembatan Pengontrol XInput <-> Android HID",
                subtitle = "Sinkronisasi deadzone, trigger analog, & remap tombol",
                detail = "Menunggu pemetaan"
            ),
            RecompileStepResult(
                id = "ini_inject",
                title = "5. Sintesis config.ini & dxvk.conf",
                subtitle = "Menyiapkan berkas konfigurasi peluncuran siap ekspor",
                detail = "Belum dijalankan"
            )
        )
    }
}
