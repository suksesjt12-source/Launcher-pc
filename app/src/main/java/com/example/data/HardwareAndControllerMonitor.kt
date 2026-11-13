package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import com.example.model.ConnectedGamepad
import com.example.model.ControllerLayoutPreset
import com.example.model.DeviceHardwareProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HardwareAndControllerMonitor(private val context: Context) {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

    fun probeDeviceHardware(): DeviceHardwareProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val pm = context.packageManager
        val hasVulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val vulkanVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && hasVulkan) {
            val featureInfo = pm.systemAvailableFeatures.firstOrNull {
                it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
            }
            val rawVer = featureInfo?.version ?: 0
            if (rawVer > 0) {
                val major = (rawVer ushr 22) and 0x3FF
                val minor = (rawVer ushr 12) and 0x3FF
                "Vulkan $major.$minor"
            } else {
                "Vulkan Tersedia"
            }
        } else {
            "OpenGL ES Fallback"
        }

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val refreshHz = defaultDisplay?.refreshRate?.toInt() ?: 60

        val socLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != "unknown" } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }

        val knownRunners = listOf(
            "com.winlator",
            "com.winlator.cmod",
            "com.winlator.frost",
            "com.termux.x11",
            "com.termux",
            "org.libsdl.app",
            "com.xiaoji.egggame"
        )
        val installedRunner = knownRunners.firstOrNull { pkg ->
            runCatching {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }.isSuccess
        }

        return DeviceHardwareProfile(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            socLabel = socLabel,
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = memInfo.totalMem / (1024L * 1024L),
            availableRamMb = memInfo.availMem / (1024L * 1024L),
            supportsVulkan = hasVulkan,
            vulkanVersionLabel = vulkanVersion,
            displayRefreshHz = refreshHz,
            installedRuntimePackage = installedRunner
        )
    }

    fun observeConnectedGamepads(): Flow<List<ConnectedGamepad>> = callbackFlow {
        fun snapshot(): List<ConnectedGamepad> {
            return InputDevice.getDeviceIds().toList().mapNotNull { id ->
                val device = InputDevice.getDevice(id) ?: return@mapNotNull null
                val sources = device.sources
                val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                if (!isGamepad && !isJoystick) return@mapNotNull null

                val hasVibe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    device.vibratorManager.vibratorIds.isNotEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    device.vibrator.hasVibrator()
                }

                val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    device.isExternal
                } else {
                    true
                }

                val sourceParts = buildList {
                    if (isGamepad) add("Gamepad")
                    if (isJoystick) add("Joystick")
                }

                ConnectedGamepad(
                    deviceId = device.id,
                    name = device.name.ifBlank { "Pengontrol HID #${device.id}" },
                    descriptor = device.descriptor,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    hasVibrator = hasVibe,
                    sourcesLabel = sourceParts.joinToString(" + "),
                    isExternal = isExternal
                )
            }
        }

        trySend(snapshot())

        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                trySend(snapshot())
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                trySend(snapshot())
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                trySend(snapshot())
            }
        }

        inputManager.registerInputDeviceListener(listener, null)
        awaitClose { inputManager.unregisterInputDeviceListener(listener) }
    }

    fun triggerRumbleTest(targetDeviceId: Int?): String {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 90, 60, 140), intArrayOf(0, 180, 0, 255), -1)
        } else {
            null
        }

        if (targetDeviceId != null) {
            val device = InputDevice.getDevice(targetDeviceId)
            if (device != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = device.vibratorManager
                    if (vm.vibratorIds.isNotEmpty()) {
                        val vib = vm.defaultVibrator
                        if (effect != null) vib.vibrate(effect)
                        return "Getar dikirim ke pengontrol: ${device.name}"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val vib = device.vibrator
                    if (vib.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
                            vib.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(180L)
                        }
                        return "Getar dikirim ke pengontrol: ${device.name}"
                    }
                }
            }
        }

        // Fallback ke motor haptik perangkat Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vib = vm.defaultVibrator
            if (vib.hasVibrator() && effect != null) {
                vib.vibrate(effect)
                return "Pengontrol tanpa motor getar; uji haptik dijalankan di perangkat Android."
            }
        } else {
            @Suppress("DEPRECATION")
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(180L)
                }
                return "Uji haptik dijalankan di perangkat Android."
            }
        }
        return "Perangkat dan pengontrol tidak melaporkan motor getar."
    }

    companion object {
        fun formatKeyLabel(keyCode: Int, preset: ControllerLayoutPreset = ControllerLayoutPreset.XBOX): String {
            return when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> when (preset) {
                    ControllerLayoutPreset.PLAYSTATION -> "✕ (Cross)"
                    ControllerLayoutPreset.SWITCH_PRO -> "B (Bawah)"
                    ControllerLayoutPreset.XBOX -> "A"
                }
                KeyEvent.KEYCODE_BUTTON_B -> when (preset) {
                    ControllerLayoutPreset.PLAYSTATION -> "○ (Circle)"
                    ControllerLayoutPreset.SWITCH_PRO -> "A (Kanan)"
                    ControllerLayoutPreset.XBOX -> "B"
                }
                KeyEvent.KEYCODE_BUTTON_X -> when (preset) {
                    ControllerLayoutPreset.PLAYSTATION -> "□ (Square)"
                    ControllerLayoutPreset.SWITCH_PRO -> "Y (Kiri)"
                    ControllerLayoutPreset.XBOX -> "X"
                }
                KeyEvent.KEYCODE_BUTTON_Y -> when (preset) {
                    ControllerLayoutPreset.PLAYSTATION -> "△ (Triangle)"
                    ControllerLayoutPreset.SWITCH_PRO -> "X (Atas)"
                    ControllerLayoutPreset.XBOX -> "Y"
                }
                KeyEvent.KEYCODE_BUTTON_L1 -> if (preset == ControllerLayoutPreset.PLAYSTATION) "L1" else "LB / L1"
                KeyEvent.KEYCODE_BUTTON_R1 -> if (preset == ControllerLayoutPreset.PLAYSTATION) "R1" else "RB / R1"
                KeyEvent.KEYCODE_BUTTON_L2 -> if (preset == ControllerLayoutPreset.PLAYSTATION) "L2" else "LT / L2"
                KeyEvent.KEYCODE_BUTTON_R2 -> if (preset == ControllerLayoutPreset.PLAYSTATION) "R2" else "RT / R2"
                KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3 (Tekan Stik)"
                KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3 (Tekan Stik)"
                KeyEvent.KEYCODE_BUTTON_START -> if (preset == ControllerLayoutPreset.PLAYSTATION) "Options" else "Start"
                KeyEvent.KEYCODE_BUTTON_SELECT -> if (preset == ControllerLayoutPreset.PLAYSTATION) "Share / Touch" else "Back / View"
                KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Atas"
                KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Bawah"
                KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Kiri"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Kanan"
                else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            }
        }

        fun isGamepadKeyCode(keyCode: Int): Boolean {
            return KeyEvent.isGamepadButton(keyCode) ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        }
    }
}
