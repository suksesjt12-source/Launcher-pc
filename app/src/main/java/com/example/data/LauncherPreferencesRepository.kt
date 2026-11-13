package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AspectRatioMode
import com.example.model.ControllerLayoutPreset
import com.example.model.ControllerProfile
import com.example.model.CpuAffinityMode
import com.example.model.FpsTarget
import com.example.model.GraphicsConfig
import com.example.model.GraphicsPreset
import com.example.model.RenderBackend
import com.example.model.ResolutionScale
import com.example.model.ShadowLevel
import com.example.model.StormAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "stormrev_launcher_prefs"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val TREE_URI = stringPreferencesKey("game_tree_uri")

        val GFX_PRESET = stringPreferencesKey("gfx_preset")
        val GFX_BACKEND = stringPreferencesKey("gfx_backend")
        val GFX_RES_SCALE = stringPreferencesKey("gfx_res_scale")
        val GFX_FPS_TARGET = stringPreferencesKey("gfx_fps_target")
        val GFX_ASPECT = stringPreferencesKey("gfx_aspect")
        val GFX_FXAA = booleanPreferencesKey("gfx_fxaa")
        val GFX_SHADOW = stringPreferencesKey("gfx_shadow")
        val GFX_GLARE = booleanPreferencesKey("gfx_glare")
        val GFX_SSAO = booleanPreferencesKey("gfx_ssao")
        val GFX_CEL_OUTLINE = booleanPreferencesKey("gfx_cel_outline")
        val GFX_ASYNC_SHADER = booleanPreferencesKey("gfx_async_shader")
        val GFX_CPU_AFFINITY = stringPreferencesKey("gfx_cpu_affinity")
        val GFX_TEXTURE_MB = intPreferencesKey("gfx_texture_mb")

        val CTRL_LAYOUT = stringPreferencesKey("ctrl_layout")
        val CTRL_DEADZONE = intPreferencesKey("ctrl_deadzone")
        val CTRL_TRIGGER = intPreferencesKey("ctrl_trigger")
        val CTRL_VIBRATION = booleanPreferencesKey("ctrl_vibration")
        val CTRL_SWAP_CONFIRM = booleanPreferencesKey("ctrl_swap_confirm")
        val CTRL_BINDINGS_PACKED = stringPreferencesKey("ctrl_bindings_packed")
    }

    val persistedTreeUri: Flow<String?> = context.launcherDataStore.data.map { prefs ->
        prefs[Keys.TREE_URI]
    }

    val graphicsConfig: Flow<GraphicsConfig> = context.launcherDataStore.data.map { prefs ->
        GraphicsConfig(
            preset = enumOrDefault(prefs[Keys.GFX_PRESET], GraphicsPreset.BALANCED),
            renderBackend = enumOrDefault(prefs[Keys.GFX_BACKEND], RenderBackend.VULKAN_DXVK),
            resolutionScale = enumOrDefault(prefs[Keys.GFX_RES_SCALE], ResolutionScale.SCALE_075),
            fpsTarget = enumOrDefault(prefs[Keys.GFX_FPS_TARGET], FpsTarget.FPS_60),
            aspectRatio = enumOrDefault(prefs[Keys.GFX_ASPECT], AspectRatioMode.RATIO_16_9),
            fxaaEnabled = prefs[Keys.GFX_FXAA] ?: true,
            shadowLevel = enumOrDefault(prefs[Keys.GFX_SHADOW], ShadowLevel.MEDIUM),
            glareEnabled = prefs[Keys.GFX_GLARE] ?: true,
            ssaoEnabled = prefs[Keys.GFX_SSAO] ?: false,
            celOutlineBoost = prefs[Keys.GFX_CEL_OUTLINE] ?: true,
            asyncShaderCompile = prefs[Keys.GFX_ASYNC_SHADER] ?: true,
            cpuAffinity = enumOrDefault(prefs[Keys.GFX_CPU_AFFINITY], CpuAffinityMode.BIG_CORES),
            textureMemoryMb = prefs[Keys.GFX_TEXTURE_MB] ?: 1024
        )
    }

    val controllerProfile: Flow<ControllerProfile> = context.launcherDataStore.data.map { prefs ->
        val defaultBindings = StormAction.entries.associateWith { it.defaultKeyCode }.toMutableMap()
        val packed = prefs[Keys.CTRL_BINDINGS_PACKED].orEmpty()
        if (packed.isNotBlank()) {
            packed.split(";").forEach { pair ->
                val parts = pair.split("=")
                if (parts.size == 2) {
                    val action = runCatching { StormAction.valueOf(parts[0]) }.getOrNull()
                    val code = parts[1].toIntOrNull()
                    if (action != null && code != null) {
                        defaultBindings[action] = code
                    }
                }
            }
        }
        ControllerProfile(
            layoutPreset = enumOrDefault(prefs[Keys.CTRL_LAYOUT], ControllerLayoutPreset.XBOX),
            deadzonePercent = prefs[Keys.CTRL_DEADZONE] ?: 12,
            triggerThresholdPercent = prefs[Keys.CTRL_TRIGGER] ?: 15,
            vibrationEnabled = prefs[Keys.CTRL_VIBRATION] ?: true,
            swapConfirmCancel = prefs[Keys.CTRL_SWAP_CONFIRM] ?: false,
            bindings = defaultBindings
        )
    }

    suspend fun saveTreeUri(uriString: String?) {
        context.launcherDataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(Keys.TREE_URI)
            } else {
                prefs[Keys.TREE_URI] = uriString
            }
        }
    }

    suspend fun saveGraphicsConfig(config: GraphicsConfig) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.GFX_PRESET] = config.preset.name
            prefs[Keys.GFX_BACKEND] = config.renderBackend.name
            prefs[Keys.GFX_RES_SCALE] = config.resolutionScale.name
            prefs[Keys.GFX_FPS_TARGET] = config.fpsTarget.name
            prefs[Keys.GFX_ASPECT] = config.aspectRatio.name
            prefs[Keys.GFX_FXAA] = config.fxaaEnabled
            prefs[Keys.GFX_SHADOW] = config.shadowLevel.name
            prefs[Keys.GFX_GLARE] = config.glareEnabled
            prefs[Keys.GFX_SSAO] = config.ssaoEnabled
            prefs[Keys.GFX_CEL_OUTLINE] = config.celOutlineBoost
            prefs[Keys.GFX_ASYNC_SHADER] = config.asyncShaderCompile
            prefs[Keys.GFX_CPU_AFFINITY] = config.cpuAffinity.name
            prefs[Keys.GFX_TEXTURE_MB] = config.textureMemoryMb
        }
    }

    suspend fun saveControllerProfile(profile: ControllerProfile) {
        val packed = profile.bindings.entries.joinToString(";") { "${it.key.name}=${it.value}" }
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.CTRL_LAYOUT] = profile.layoutPreset.name
            prefs[Keys.CTRL_DEADZONE] = profile.deadzonePercent
            prefs[Keys.CTRL_TRIGGER] = profile.triggerThresholdPercent
            prefs[Keys.CTRL_VIBRATION] = profile.vibrationEnabled
            prefs[Keys.CTRL_SWAP_CONFIRM] = profile.swapConfirmCancel
            prefs[Keys.CTRL_BINDINGS_PACKED] = packed
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw == null) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
