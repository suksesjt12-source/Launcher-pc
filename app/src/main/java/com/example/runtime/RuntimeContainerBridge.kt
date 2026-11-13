package com.example.runtime

import android.content.Context
import android.content.Intent
import com.example.model.ControllerProfile
import com.example.model.DeviceHardwareProfile
import com.example.model.GameDirectoryStatus
import com.example.model.GraphicsConfig

sealed interface RuntimeLaunchResult {
    data class Launched(val targetLabel: String) : RuntimeLaunchResult
    data class NotAvailable(val reason: String) : RuntimeLaunchResult
}

/**
 * Kontrak jembatan peluncuran container Windows/Box64/Wine.
 * Di build standalone, implementasi bawaan membuka aplikasi container eksternal (seperti Winlator).
 * Saat digabungkan ke repositori fork Winlator NDK, ganti implementasi ini agar memanggil
 * ContainerManager dan XServerDisplayActivity internal Winlator secara langsung.
 */
interface RuntimeContainerBridge {
    fun detectInstalledRuntime(): String?
    fun launchContainerWithPreset(
        graphicsConfig: GraphicsConfig,
        controllerProfile: ControllerProfile,
        hardware: DeviceHardwareProfile,
        gameDir: GameDirectoryStatus.Ready?
    ): RuntimeLaunchResult
}

class ExternalPackageRuntimeBridge(private val context: Context) : RuntimeContainerBridge {

    private val knownPackages = listOf(
        "com.winlator",
        "com.winlator.cmod",
        "com.winlator.frost",
        "com.termux.x11",
        "com.termux",
        "org.libsdl.app",
        "com.xiaoji.egggame"
    )

    override fun detectInstalledRuntime(): String? {
        val pm = context.packageManager
        return knownPackages.firstOrNull { pkg ->
            runCatching {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }.isSuccess
        }
    }

    override fun launchContainerWithPreset(
        graphicsConfig: GraphicsConfig,
        controllerProfile: ControllerProfile,
        hardware: DeviceHardwareProfile,
        gameDir: GameDirectoryStatus.Ready?
    ): RuntimeLaunchResult {
        val pkg = detectInstalledRuntime()
            ?: return RuntimeLaunchResult.NotAvailable(
                "Container Windows/Wine (seperti Winlator) belum terpasang di perangkat."
            )

        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return RuntimeLaunchResult.NotAvailable(
                "Tidak dapat membuka Activity utama untuk paket $pkg."
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return RuntimeLaunchResult.Launched(pkg)
    }
}
