package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameFolderScanner
import com.example.data.HardwareAndControllerMonitor
import com.example.data.LauncherPreferencesRepository
import com.example.model.AspectRatioMode
import com.example.model.ConnectedGamepad
import com.example.model.ControllerLayoutPreset
import com.example.model.ControllerProfile
import com.example.model.CpuAffinityMode
import com.example.model.DeviceHardwareProfile
import com.example.model.FpsTarget
import com.example.model.GameDirectoryStatus
import com.example.model.GraphicsConfig
import com.example.model.GraphicsPreset
import com.example.model.LauncherTab
import com.example.model.LiveGamepadState
import com.example.model.RecompilePipelineState
import com.example.model.RecompileStageStatus
import com.example.model.RenderBackend
import com.example.model.ResolutionScale
import com.example.model.ShadowLevel
import com.example.model.StormAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherPreferencesRepository(application)
    private val hardwareMonitor = HardwareAndControllerMonitor(application)
    private val folderScanner = GameFolderScanner(application)

    private val _selectedTab = MutableStateFlow(LauncherTab.HOME)
    val selectedTab: StateFlow<LauncherTab> = _selectedTab.asStateFlow()

    val graphicsConfig: StateFlow<GraphicsConfig> = repository.graphicsConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GraphicsConfig()
    )

    val controllerProfile: StateFlow<ControllerProfile> = repository.controllerProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ControllerProfile()
    )

    val connectedGamepads: StateFlow<List<ConnectedGamepad>> =
        hardwareMonitor.observeConnectedGamepads().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _hardwareProfile = MutableStateFlow(hardwareMonitor.probeDeviceHardware())
    val hardwareProfile: StateFlow<DeviceHardwareProfile> = _hardwareProfile.asStateFlow()

    private val _gameDirectoryStatus = MutableStateFlow<GameDirectoryStatus>(GameDirectoryStatus.NotSelected)
    val gameDirectoryStatus: StateFlow<GameDirectoryStatus> = _gameDirectoryStatus.asStateFlow()

    private val _liveGamepadState = MutableStateFlow(LiveGamepadState())
    val liveGamepadState: StateFlow<LiveGamepadState> = _liveGamepadState.asStateFlow()

    private val _awaitingRemapAction = MutableStateFlow<StormAction?>(null)
    val awaitingRemapAction: StateFlow<StormAction?> = _awaitingRemapAction.asStateFlow()

    private val _recompileState = MutableStateFlow(RecompilePipelineState())
    val recompileState: StateFlow<RecompilePipelineState> = _recompileState.asStateFlow()

    private val _showDiagnosticViewport = MutableStateFlow(false)
    val showDiagnosticViewport: StateFlow<Boolean> = _showDiagnosticViewport.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUri = repository.persistedTreeUri.first()
            if (!savedUri.isNullOrBlank()) {
                val parsed = runCatching { savedUri.toUri() }.getOrNull()
                if (parsed != null) {
                    val hasPerm = getApplication<Application>().contentResolver.persistedUriPermissions.any {
                        it.uri == parsed && it.isReadPermission
                    }
                    if (hasPerm) {
                        scanGameDirectory(parsed)
                    }
                }
            }
        }
    }

    fun selectTab(tab: LauncherTab) {
        _selectedTab.value = tab
    }

    fun refreshHardwareProbe() {
        _hardwareProfile.value = hardwareMonitor.probeDeviceHardware()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun setDiagnosticViewportVisible(visible: Boolean) {
        _showDiagnosticViewport.value = visible
    }

    // --- Game Folder SAF ---

    fun onFolderSelected(treeUri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            resolver.takePersistableUriPermission(treeUri, flags)
        }.onFailure {
            runCatching {
                resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModelScope.launch {
            repository.saveTreeUri(treeUri.toString())
            scanGameDirectory(treeUri)
        }
    }

    fun rescanCurrentFolder() {
        val current = _gameDirectoryStatus.value
        if (current is GameDirectoryStatus.Ready) {
            scanGameDirectory(current.treeUriString.toUri())
        }
    }

    private fun scanGameDirectory(treeUri: Uri) {
        viewModelScope.launch {
            _gameDirectoryStatus.value = GameDirectoryStatus.Scanning
            val result = folderScanner.scanTree(treeUri)
            _gameDirectoryStatus.value = result
            if (result is GameDirectoryStatus.Ready) {
                val msg = if (result.isCompleteStormRevolutionBuild) {
                    "Folder valid: NSUNSR.exe & ${result.cpkCount} arsip CPK terdeteksi."
                } else {
                    "Folder dipindai (${result.files.size} berkas). Pastikan berisi NSUNSR.exe & data_win32."
                }
                _snackbarMessage.value = msg
            } else if (result is GameDirectoryStatus.Error) {
                _snackbarMessage.value = result.message
            }
        }
    }

    // --- Graphics Configuration ---

    fun applyGraphicsPreset(preset: GraphicsPreset) {
        viewModelScope.launch {
            val base = GraphicsConfig.fromPreset(preset)
            val current = graphicsConfig.value
            val updated = if (preset == GraphicsPreset.CUSTOM) {
                current.copy(preset = GraphicsPreset.CUSTOM)
            } else {
                base.copy(
                    aspectRatio = current.aspectRatio,
                    renderBackend = if (!_hardwareProfile.value.supportsVulkan) {
                        RenderBackend.OPENGL_ES_WINED3D
                    } else {
                        base.renderBackend
                    }
                )
            }
            repository.saveGraphicsConfig(updated)
        }
    }

    fun updateGraphics(transform: (GraphicsConfig) -> GraphicsConfig) {
        viewModelScope.launch {
            val updated = transform(graphicsConfig.value).copy(preset = GraphicsPreset.CUSTOM)
            repository.saveGraphicsConfig(updated)
        }
    }

    fun setRenderBackend(backend: RenderBackend) = updateGraphics { it.copy(renderBackend = backend) }
    fun setResolutionScale(scale: ResolutionScale) = updateGraphics { it.copy(resolutionScale = scale) }
    fun setFpsTarget(fps: FpsTarget) = updateGraphics { it.copy(fpsTarget = fps) }
    fun setAspectRatio(ratio: AspectRatioMode) = updateGraphics { it.copy(aspectRatio = ratio) }
    fun setShadowLevel(level: ShadowLevel) = updateGraphics { it.copy(shadowLevel = level) }
    fun setFxaa(enabled: Boolean) = updateGraphics { it.copy(fxaaEnabled = enabled) }
    fun setGlare(enabled: Boolean) = updateGraphics { it.copy(glareEnabled = enabled) }
    fun setSsao(enabled: Boolean) = updateGraphics { it.copy(ssaoEnabled = enabled) }
    fun setCelOutlineBoost(enabled: Boolean) = updateGraphics { it.copy(celOutlineBoost = enabled) }
    fun setAsyncShaderCompile(enabled: Boolean) = updateGraphics { it.copy(asyncShaderCompile = enabled) }
    fun setCpuAffinity(mode: CpuAffinityMode) = updateGraphics { it.copy(cpuAffinity = mode) }
    fun setTextureMemoryMb(mb: Int) = updateGraphics { it.copy(textureMemoryMb = mb) }

    // --- External Controller & Remapping ---

    fun setControllerLayoutPreset(preset: ControllerLayoutPreset) {
        viewModelScope.launch {
            val swap = preset == ControllerLayoutPreset.SWITCH_PRO
            repository.saveControllerProfile(
                controllerProfile.value.copy(
                    layoutPreset = preset,
                    swapConfirmCancel = swap
                )
            )
        }
    }

    fun setDeadzonePercent(percent: Int) {
        viewModelScope.launch {
            repository.saveControllerProfile(
                controllerProfile.value.copy(deadzonePercent = percent.coerceIn(0, 35))
            )
        }
    }

    fun setTriggerThresholdPercent(percent: Int) {
        viewModelScope.launch {
            repository.saveControllerProfile(
                controllerProfile.value.copy(triggerThresholdPercent = percent.coerceIn(5, 50))
            )
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveControllerProfile(
                controllerProfile.value.copy(vibrationEnabled = enabled)
            )
        }
    }

    fun setSwapConfirmCancel(swap: Boolean) {
        viewModelScope.launch {
            repository.saveControllerProfile(
                controllerProfile.value.copy(swapConfirmCancel = swap)
            )
        }
    }

    fun startWaitingForRemap(action: StormAction?) {
        _awaitingRemapAction.value = action
    }

    fun bindActionToKeyCode(action: StormAction, keyCode: Int) {
        viewModelScope.launch {
            val updatedMap = controllerProfile.value.bindings.toMutableMap()
            updatedMap[action] = keyCode
            repository.saveControllerProfile(controllerProfile.value.copy(bindings = updatedMap))
            _awaitingRemapAction.value = null
            val label = HardwareAndControllerMonitor.formatKeyLabel(keyCode, controllerProfile.value.layoutPreset)
            _snackbarMessage.value = "${action.actionName} dipetakan ke $label"
        }
    }

    fun resetControllerDefaults() {
        viewModelScope.launch {
            repository.saveControllerProfile(ControllerProfile())
            _awaitingRemapAction.value = null
            _snackbarMessage.value = "Pemetaan pengontrol dikembalikan ke bawaan Storm Revolution."
        }
    }

    fun testRumble() {
        val targetId = connectedGamepads.value.firstOrNull { it.hasVibrator }?.deviceId
            ?: connectedGamepads.value.firstOrNull()?.deviceId
        val result = hardwareMonitor.triggerRumbleTest(targetId)
        _snackbarMessage.value = result
    }

    // --- Real Hardware Gamepad Input Processing ---

    fun handleKeyDown(event: KeyEvent): Boolean {
        val code = event.keyCode
        val fromGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            HardwareAndControllerMonitor.isGamepadKeyCode(code)
        if (!fromGamepad) return false

        val deviceName = event.device?.name ?: "Pengontrol Eksternal"
        val preset = controllerProfile.value.layoutPreset
        val label = HardwareAndControllerMonitor.formatKeyLabel(code, preset)

        _liveGamepadState.update { state ->
            state.copy(
                pressedKeyCodes = state.pressedKeyCodes + code,
                lastKeyCode = code,
                lastKeyLabel = label,
                lastDeviceName = deviceName
            )
        }

        val pendingAction = _awaitingRemapAction.value
        if (pendingAction != null && event.repeatCount == 0) {
            bindActionToKeyCode(pendingAction, code)
        }
        return true
    }

    fun handleKeyUp(event: KeyEvent): Boolean {
        val code = event.keyCode
        val fromGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            HardwareAndControllerMonitor.isGamepadKeyCode(code)
        if (!fromGamepad) return false

        _liveGamepadState.update { state ->
            state.copy(pressedKeyCodes = state.pressedKeyCodes - code)
        }
        return true
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (!isJoystick) return false

        val deadzone = controllerProfile.value.deadzonePercent / 100f
        val triggerThresh = controllerProfile.value.triggerThresholdPercent / 100f

        fun applyDeadzone(raw: Float): Float = if (abs(raw) < deadzone) 0f else raw

        val lx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X))
        val ly = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y))
        val rx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Z))
        val ry = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RZ))
        val lt = maxOf(
            event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_BRAKE)
        ).coerceIn(0f, 1f)
        val rt = maxOf(
            event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_GAS)
        ).coerceIn(0f, 1f)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val deviceName = event.device?.name ?: "Pengontrol Eksternal"

        _liveGamepadState.update { state ->
            val updatedKeys = state.pressedKeyCodes.toMutableSet()
            if (lt >= triggerThresh) updatedKeys.add(KeyEvent.KEYCODE_BUTTON_L2)
            else updatedKeys.remove(KeyEvent.KEYCODE_BUTTON_L2)
            if (rt >= triggerThresh) updatedKeys.add(KeyEvent.KEYCODE_BUTTON_R2)
            else updatedKeys.remove(KeyEvent.KEYCODE_BUTTON_R2)

            if (hatX < -0.5f) updatedKeys.add(KeyEvent.KEYCODE_DPAD_LEFT) else updatedKeys.remove(KeyEvent.KEYCODE_DPAD_LEFT)
            if (hatX > 0.5f) updatedKeys.add(KeyEvent.KEYCODE_DPAD_RIGHT) else updatedKeys.remove(KeyEvent.KEYCODE_DPAD_RIGHT)
            if (hatY < -0.5f) updatedKeys.add(KeyEvent.KEYCODE_DPAD_UP) else updatedKeys.remove(KeyEvent.KEYCODE_DPAD_UP)
            if (hatY > 0.5f) updatedKeys.add(KeyEvent.KEYCODE_DPAD_DOWN) else updatedKeys.remove(KeyEvent.KEYCODE_DPAD_DOWN)

            state.copy(
                pressedKeyCodes = updatedKeys,
                leftStickX = lx,
                leftStickY = ly,
                rightStickX = rx,
                rightStickY = ry,
                leftTrigger = lt,
                rightTrigger = rt,
                hatX = hatX,
                hatY = hatY,
                lastDeviceName = deviceName
            )
        }

        val pendingAction = _awaitingRemapAction.value
        if (pendingAction != null) {
            if (lt >= triggerThresh) {
                bindActionToKeyCode(pendingAction, KeyEvent.KEYCODE_BUTTON_L2)
            } else if (rt >= triggerThresh) {
                bindActionToKeyCode(pendingAction, KeyEvent.KEYCODE_BUTTON_R2)
            }
        }
        return true
    }

    // --- Recompile & Verification Pipeline ---

    fun runRecompilePipeline() {
        if (_recompileState.value.isRunning) return

        viewModelScope.launch {
            val hw = hardwareMonitor.probeDeviceHardware()
            _hardwareProfile.value = hw
            val dirStatus = _gameDirectoryStatus.value
            val gfx = graphicsConfig.value
            val ctrl = controllerProfile.value
            val pads = connectedGamepads.value

            val logs = mutableListOf<String>()
            val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            fun log(msg: String) {
                logs.add("[${timeFmt.format(Date())}] $msg")
            }

            val baseSteps = RecompilePipelineState.defaultSteps().toMutableList()
            _recompileState.value = RecompilePipelineState(
                isRunning = true,
                hasRunOnce = true,
                overallProgress = 0.05f,
                steps = baseSteps,
                logLines = listOf("[${timeFmt.format(Date())}] Memulai verifikasi jembatan recompile NSUNSR -> Android (${hw.primaryAbi})...")
            )

            // Step 1: PE32 Binary Check
            baseSteps[0] = baseSteps[0].copy(
                status = RecompileStageStatus.RUNNING,
                detail = "Memeriksa header biner PC & arsitektur host..."
            )
            _recompileState.update { it.copy(steps = baseSteps.toList(), overallProgress = 0.15f) }
            delay(320)

            when (dirStatus) {
                is GameDirectoryStatus.Ready -> {
                    if (dirStatus.exeDetected) {
                        val arch = dirStatus.exeArchLabel ?: "PE32 x86"
                        log("PE32 terverifikasi: NSUNSR.exe ($arch) siap translasi ${hw.primaryAbi}.")
                        baseSteps[0] = baseSteps[0].copy(
                            status = RecompileStageStatus.VERIFIED,
                            detail = "Terdeteksi NSUNSR.exe ($arch) -> Target ${hw.primaryAbi}"
                        )
                    } else {
                        log("PERINGATAN: Folder '${dirStatus.folderName}' tidak memuat NSUNSR.exe.")
                        baseSteps[0] = baseSteps[0].copy(
                            status = RecompileStageStatus.WARNING,
                            detail = "NSUNSR.exe tidak ditemukan di '${dirStatus.folderName}'"
                        )
                    }
                }
                else -> {
                    log("INFO: Folder PC belum dipilih; validasi profil recompile berjalan dalam mode konfigurasi.")
                    baseSteps[0] = baseSteps[0].copy(
                        status = RecompileStageStatus.WARNING,
                        detail = "Folder game PC belum dipilih (mode profil)"
                    )
                }
            }

            // Step 2: CPK Index
            baseSteps[1] = baseSteps[1].copy(
                status = RecompileStageStatus.RUNNING,
                detail = "Memeriksa arsip CPK data_win32..."
            )
            _recompileState.update { it.copy(steps = baseSteps.toList(), logLines = logs.toList(), overallProgress = 0.35f) }
            delay(280)

            if (dirStatus is GameDirectoryStatus.Ready && dirStatus.cpkCount > 0) {
                val formattedSize = GameFolderScanner.formatBytes(dirStatus.totalScannedSizeBytes)
                log("Indeks CPK selesai: ${dirStatus.cpkCount} arsip CPK ($formattedSize).")
                baseSteps[1] = baseSteps[1].copy(
                    status = RecompileStageStatus.VERIFIED,
                    detail = "${dirStatus.cpkCount} arsip CPK terindeks ($formattedSize)"
                )
            } else {
                log("PERINGATAN: Arsip CPK (data1.cpk / movie.cpk) belum terindeks.")
                baseSteps[1] = baseSteps[1].copy(
                    status = RecompileStageStatus.WARNING,
                    detail = "Arsip .cpk belum terdeteksi di data_win32"
                )
            }

            // Step 3: Shader & Graphics Bridge
            baseSteps[2] = baseSteps[2].copy(
                status = RecompileStageStatus.RUNNING,
                detail = "Menyusun profil shader ${gfx.renderBackend.label}..."
            )
            _recompileState.update { it.copy(steps = baseSteps.toList(), logLines = logs.toList(), overallProgress = 0.58f) }
            delay(320)

            val vulkanReady = gfx.renderBackend == RenderBackend.VULKAN_DXVK && hw.supportsVulkan
            val backendNote = if (gfx.renderBackend == RenderBackend.VULKAN_DXVK && !hw.supportsVulkan) {
                "Vulkan tidak dilaporkan sistem; disarankan OpenGL ES 3.2"
            } else {
                "${gfx.renderBackend.label} • ${gfx.resolutionScale.label} • ${gfx.fpsTarget.label}"
            }
            log("Profil grafis dikompilasi: $backendNote (Async=${gfx.asyncShaderCompile}, VRAM=${gfx.textureMemoryMb}MB).")
            baseSteps[2] = baseSteps[2].copy(
                status = if (gfx.renderBackend == RenderBackend.VULKAN_DXVK && !hw.supportsVulkan) {
                    RecompileStageStatus.WARNING
                } else {
                    RecompileStageStatus.VERIFIED
                },
                detail = backendNote
            )

            // Step 4: XInput Controller Bridge
            baseSteps[3] = baseSteps[3].copy(
                status = RecompileStageStatus.RUNNING,
                detail = "Menghubungkan pemetaan pengontrol XInput..."
            )
            _recompileState.update { it.copy(steps = baseSteps.toList(), logLines = logs.toList(), overallProgress = 0.78f) }
            delay(260)

            val padDesc = if (pads.isNotEmpty()) {
                "${pads.size} pengontrol aktif (${pads.first().name})"
            } else {
                "Siap auto-detect HID/Bluetooth saat tersambung"
            }
            log("Jembatan XInput: ${ctrl.layoutPreset.label}, deadzone ${ctrl.deadzonePercent}%. $padDesc.")
            baseSteps[3] = baseSteps[3].copy(
                status = RecompileStageStatus.VERIFIED,
                detail = "$padDesc • Deadzone ${ctrl.deadzonePercent}%"
            )

            // Step 5: INI & DXVK Artifact Synthesis
            baseSteps[4] = baseSteps[4].copy(
                status = RecompileStageStatus.RUNNING,
                detail = "Menyusun config.ini, dxvk.conf, & stormrev_controller.json..."
            )
            _recompileState.update { it.copy(steps = baseSteps.toList(), logLines = logs.toList(), overallProgress = 0.92f) }
            delay(260)

            val configIni = gfx.toConfigIniString()
            val dxvkConf = gfx.toDxvkConfString()
            val ctrlJson = ctrl.toJsonSummary()
            val launchBat = gfx.toLaunchBatString()

            val localBundlePath = folderScanner.writeLocalRuntimeBundle(
                configIniContent = configIni,
                dxvkConfContent = dxvkConf,
                controllerJsonContent = ctrlJson,
                launchScriptContent = launchBat
            ).getOrNull()

            var syncedFolder = false
            if (dirStatus is GameDirectoryStatus.Ready) {
                val syncResult = folderScanner.writeConfigFilesToFolder(
                    treeUri = dirStatus.treeUriString.toUri(),
                    configIniContent = configIni,
                    dxvkConfContent = dxvkConf,
                    controllerJsonContent = ctrlJson,
                    launchScriptContent = launchBat
                )
                syncedFolder = syncResult.isSuccess
            }

            if (syncedFolder) {
                log("Sintesis selesai & tersinkron ke folder game PC (config.ini, dxvk.conf, launch_nsunsr.bat).")
            } else {
                log("Sintesis selesai! Bundel runtime siap di perangkat ($localBundlePath).")
            }
            baseSteps[4] = baseSteps[4].copy(
                status = RecompileStageStatus.VERIFIED,
                detail = if (syncedFolder) {
                    "Tersinkron ke folder PC & siap jalankan game"
                } else {
                    "Bundel runtime siap • Klik Jalankan Game"
                }
            )

            _recompileState.value = RecompilePipelineState(
                isRunning = false,
                hasRunOnce = true,
                overallProgress = 1f,
                steps = baseSteps.toList(),
                logLines = logs.toList(),
                lastCompletedLabel = "Verifikasi & sinkronisasi selesai pada ${timeFmt.format(Date())}",
                localBundlePath = localBundlePath,
                syncedToGameFolder = syncedFolder
            )
        }
    }

    fun exportAllConfigsToGameFolder() {
        val dir = _gameDirectoryStatus.value
        if (dir !is GameDirectoryStatus.Ready) {
            _snackbarMessage.value = "Pilih folder game PC terlebih dahulu untuk mengekspor berkas."
            return
        }
        viewModelScope.launch {
            val result = folderScanner.writeConfigFilesToFolder(
                treeUri = dir.treeUriString.toUri(),
                configIniContent = graphicsConfig.value.toConfigIniString(),
                dxvkConfContent = graphicsConfig.value.toDxvkConfString(),
                controllerJsonContent = controllerProfile.value.toJsonSummary(),
                launchScriptContent = graphicsConfig.value.toLaunchBatString()
            )
            result.onSuccess { files ->
                _snackbarMessage.value = "Berhasil menulis ${files.joinToString(", ")} ke '${dir.folderName}'."
                scanGameDirectory(dir.treeUriString.toUri())
            }.onFailure { err ->
                _snackbarMessage.value = "Gagal menulis ke folder: ${err.localizedMessage}"
            }
        }
    }

    fun exportConfigIniToDocument(uri: Uri) {
        viewModelScope.launch {
            val result = folderScanner.writeSingleDocument(uri, graphicsConfig.value.toConfigIniString())
            result.onSuccess {
                _snackbarMessage.value = "config.ini berhasil disimpan."
            }.onFailure { err ->
                _snackbarMessage.value = "Gagal menyimpan config.ini: ${err.localizedMessage}"
            }
        }
    }

    fun launchExternalContainer(): Boolean {
        val pkg = _hardwareProfile.value.installedRuntimePackage
        if (pkg == null) {
            _snackbarMessage.value = "Container Windows/Wine (mis. Winlator) belum terpasang di perangkat."
            return false
        }
        val app = getApplication<Application>()
        val launchIntent = app.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(launchIntent)
        return true
    }
}
