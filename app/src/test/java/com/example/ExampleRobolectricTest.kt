package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.model.AspectRatioMode
import com.example.model.ControllerLayoutPreset
import com.example.model.ControllerProfile
import com.example.model.DeviceHardwareProfile
import com.example.model.DxWrapperMode
import com.example.model.FpsTarget
import com.example.model.GraphicsConfig
import com.example.model.GraphicsPreset
import com.example.model.ResolutionScale
import com.example.model.StormAction
import com.example.model.WinlatorGraphicsDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches StormRev Launcher`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StormRev Launcher", appName)
    }

    @Test
    fun `graphics config generates valid Storm Revolution config ini`() {
        val config = GraphicsConfig.fromPreset(GraphicsPreset.BALANCED).copy(
            resolutionScale = ResolutionScale.SCALE_100,
            fpsTarget = FpsTarget.FPS_60,
            aspectRatio = AspectRatioMode.RATIO_20_9
        )
        val ini = config.toConfigIniString()
        assertTrue(ini.contains("[setting]"))
        assertTrue(ini.contains("width=2400"))
        assertTrue(ini.contains("height=1080"))
        assertTrue(ini.contains("fps=1"))
        assertTrue(ini.contains("controller=1"))
    }

    @Test
    fun `controller profile serializes bindings and deadzone`() {
        val profile = ControllerProfile(
            layoutPreset = ControllerLayoutPreset.PLAYSTATION,
            deadzonePercent = 15,
            bindings = StormAction.entries.associateWith { it.defaultKeyCode }.toMutableMap().apply {
                put(StormAction.SUBSTITUTION, KeyEvent.KEYCODE_BUTTON_L1)
            }
        )
        val json = profile.toJsonSummary()
        assertTrue(json.contains("\"layoutPreset\": \"PLAYSTATION\""))
        assertTrue(json.contains("\"deadzone\": 0.15"))
        assertTrue(json.contains("\"SUBSTITUTION\": ${KeyEvent.KEYCODE_BUTTON_L1}"))
    }

    @Test
    fun `graphics config generates launch_nsunsr bat script`() {
        val config = GraphicsConfig.fromPreset(GraphicsPreset.MAX_PERFORMANCE)
        val bat = config.toLaunchBatString()
        assertTrue(bat.contains("set DXVK_ASYNC=1"))
        assertTrue(bat.contains("set DXVK_FRAME_RATE=60"))
        assertTrue(bat.contains("NSUNSR.exe"))
    }

    @Test
    fun `winlator mali profile generates vortek box64 env and container json`() {
        val hw = DeviceHardwareProfile(
            deviceModel = "Xiaomi Redmi",
            socLabel = "MediaTek Dimensity 8300",
            cpuCores = 8,
            supportsVulkan = true,
            gpuVendorLabel = "ARM",
            gpuRendererLabel = "Mali-G615 MC6",
            isMediaTekSoc = true,
            isMaliGpu = true
        )
        val config = GraphicsConfig.fromPreset(GraphicsPreset.BALANCED).copy(
            winlatorDriver = WinlatorGraphicsDriver.VORTEK_MALI,
            dxWrapper = DxWrapperMode.DXVK_SAREK,
            box64StrongMem = 1
        )
        val env = config.toWinlatorMaliEnvString(hw)
        val presetJson = config.toWinlatorContainerPresetJson(hw)

        assertTrue(hw.isMediaTekOrMali)
        assertEquals(WinlatorGraphicsDriver.VORTEK_MALI, hw.recommendedDriver)
        assertTrue(env.contains("WINLATOR_GRAPHICS_DRIVER=vortek"))
        assertTrue(env.contains("WINLATOR_DX_WRAPPER=dxvk-sarek"))
        assertTrue(env.contains("BOX64_DYNAREC_STRONGMEM=1"))
        assertTrue(presetJson.contains("\"graphicsDriver\": \"vortek\""))
        assertTrue(presetJson.contains("\"isMediaTekOrMali\": true"))
    }
}
