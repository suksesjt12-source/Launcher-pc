package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AspectRatioMode
import com.example.model.CpuAffinityMode
import com.example.model.DeviceHardwareProfile
import com.example.model.FpsTarget
import com.example.model.GraphicsConfig
import com.example.model.GraphicsPreset
import com.example.model.RenderBackend
import com.example.model.ResolutionScale
import com.example.model.ShadowLevel
import com.example.ui.components.StatusPill
import com.example.ui.components.StormCard
import com.example.ui.components.TelemetryGaugeBar
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianOutlineVariant
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RasenganCyan
import com.example.ui.theme.SharinganCrimson
import com.example.ui.theme.ShinobiAmber
import com.example.ui.theme.ShinobiEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraphicsScreen(
    config: GraphicsConfig,
    hardwareProfile: DeviceHardwareProfile,
    onApplyPreset: (GraphicsPreset) -> Unit,
    onSetBackend: (RenderBackend) -> Unit,
    onSetResolution: (ResolutionScale) -> Unit,
    onSetFpsTarget: (FpsTarget) -> Unit,
    onSetAspectRatio: (AspectRatioMode) -> Unit,
    onSetShadowLevel: (ShadowLevel) -> Unit,
    onSetFxaa: (Boolean) -> Unit,
    onSetGlare: (Boolean) -> Unit,
    onSetSsao: (Boolean) -> Unit,
    onSetCelOutline: (Boolean) -> Unit,
    onSetAsyncShader: (Boolean) -> Unit,
    onSetCpuAffinity: (CpuAffinityMode) -> Unit,
    onSetTextureMemory: (Int) -> Unit,
    onExportIni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gpuLoad = config.estimatedGpuLoadPercent()
    val vramMb = config.estimatedVramUsageMb()
    val gpuColor = when {
        gpuLoad >= 82 -> SharinganCrimson
        gpuLoad >= 60 -> ShinobiAmber
        else -> ShinobiEmerald
    }
    val ramRatio = if (hardwareProfile.totalRamMb > 0) {
        (vramMb.toFloat() / (hardwareProfile.totalRamMb.toFloat() * 0.45f)).coerceIn(0.08f, 1f)
    } else {
        0.4f
    }

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
                // Live Thermal & GPU Load Telemetry Card
                StormCard(accentColor = gpuColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = gpuColor)
                            Text("Estimasi Beban Performa", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        }
                        StatusPill(
                            label = when {
                                gpuLoad >= 82 -> "Beban Berat"
                                gpuLoad >= 60 -> "Optimal"
                                else -> "Sangat Ringan"
                            },
                            color = gpuColor
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    TelemetryGaugeBar(
                        title = "Beban Shader & GPU (${config.renderBackend.label})",
                        valueText = "$gpuLoad%",
                        progress = gpuLoad / 100f,
                        barColor = gpuColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TelemetryGaugeBar(
                        title = "Anggaran Tekstur VRAM",
                        valueText = "$vramMb MB / ${config.textureMemoryMb} MB",
                        progress = ramRatio,
                        barColor = RasenganCyan
                    )
                }

                // Performance Presets
                StormCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = ChakraOrange)
                        Text(
                            text = stringResource(id = R.string.graphics_preset_label),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphicsPreset.entries.forEach { preset ->
                            val selected = config.preset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onApplyPreset(preset) },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("preset_chip_${preset.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChakraOrange,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = config.preset.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // Render Backend & Resolution
                StormCard {
                    Text(
                        text = "Pipeline Render & Resolusi",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(id = R.string.graphics_backend_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RenderBackend.entries.forEach { backend ->
                            val selected = config.renderBackend == backend
                            FilterChip(
                                selected = selected,
                                onClick = { onSetBackend(backend) },
                                label = { Text(backend.label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backend_chip_${backend.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RasenganCyan,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Text(
                        text = if (hardwareProfile.supportsVulkan) {
                            "Perangkat mendukung ${hardwareProfile.vulkanVersionLabel} secara native."
                        } else {
                            "Vulkan tidak dilaporkan sistem; gunakan OpenGL ES 3.2 jika terjadi crash."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hardwareProfile.supportsVulkan) ShinobiEmerald else ShinobiAmber
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Skala Resolusi Internal",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResolutionScale.entries.forEach { scale ->
                            FilterChip(
                                selected = config.resolutionScale == scale,
                                onClick = { onSetResolution(scale) },
                                label = { Text(scale.label) },
                                modifier = Modifier.testTag("resolution_chip_${scale.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChakraOrange,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Batas Frame Rate (FPS)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FpsTarget.entries.forEach { fps ->
                            FilterChip(
                                selected = config.fpsTarget == fps,
                                onClick = { onSetFpsTarget(fps) },
                                label = { Text(fps.label) },
                                modifier = Modifier.testTag("fps_chip_${fps.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChakraOrange,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Rasio Aspek Layar",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatioMode.entries.forEach { ratio ->
                            FilterChip(
                                selected = config.aspectRatio == ratio,
                                onClick = { onSetAspectRatio(ratio) },
                                label = { Text(ratio.label) },
                                modifier = Modifier.testTag("aspect_chip_${ratio.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RasenganCyan,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Engine Visual Effects & CPU Threading
                StormCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = RasenganCyan)
                        Text(
                            text = "Efek Storm Engine & CPU",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Kualitas Bayangan (Shadow Map)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShadowLevel.entries.forEach { level ->
                            FilterChip(
                                selected = config.shadowLevel == level,
                                onClick = { onSetShadowLevel(level) },
                                label = { Text(level.label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shadow_chip_${level.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChakraOrange,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    GraphicsToggleRow(
                        title = "FXAA Anti-Aliasing",
                        subtitle = "Menghaluskan garis tepi model anime cel-shaded",
                        checked = config.fxaaEnabled,
                        onCheckedChange = onSetFxaa,
                        tag = "toggle_fxaa"
                    )
                    HorizontalDivider(color = ObsidianOutlineVariant)
                    GraphicsToggleRow(
                        title = "Glare & Aura Bloom",
                        subtitle = "Efek cahaya jutsu Rasengan/Chidori khas Storm",
                        checked = config.glareEnabled,
                        onCheckedChange = onSetGlare,
                        tag = "toggle_glare"
                    )
                    HorizontalDivider(color = ObsidianOutlineVariant)
                    GraphicsToggleRow(
                        title = "SSAO Ambient Occlusion",
                        subtitle = "Bayangan kedalaman arena (menguras GPU lebih tinggi)",
                        checked = config.ssaoEnabled,
                        onCheckedChange = onSetSsao,
                        tag = "toggle_ssao"
                    )
                    HorizontalDivider(color = ObsidianOutlineVariant)
                    GraphicsToggleRow(
                        title = "Peningkatan Outline Cel-Shader",
                        subtitle = "Anisotropik 4x agar tekstur karakter tetap tajam",
                        checked = config.celOutlineBoost,
                        onCheckedChange = onSetCelOutline,
                        tag = "toggle_cel"
                    )
                    HorizontalDivider(color = ObsidianOutlineVariant)
                    GraphicsToggleRow(
                        title = "Kompilasi Shader Asinkron",
                        subtitle = "Mengurangi patah-patah (stutter) saat Ultimate Jutsu",
                        checked = config.asyncShaderCompile,
                        onCheckedChange = onSetAsyncShader,
                        tag = "toggle_async_shader"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Penjadwalan CPU & Batas VRAM",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CpuAffinityMode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.cpuAffinity == mode,
                                onClick = { onSetCpuAffinity(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RasenganCyan,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(512, 1024, 2048).forEach { mb ->
                            FilterChip(
                                selected = config.textureMemoryMb == mb,
                                onClick = { onSetTextureMemory(mb) },
                                label = { Text("$mb MB VRAM") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChakraOrange,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Live INI & DXVK Preview + Export
                StormCard(accentColor = ObsidianOutline) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = ChakraOrange)
                            Text(
                                text = stringResource(id = R.string.graphics_ini_preview_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = ObsidianBg,
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${config.toConfigIniString()}\n${config.toDxvkConfString()}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = RasenganCyan,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onExportIni,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_config_ini_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChakraOrange,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.action_export_config), style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun GraphicsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = ChakraOrange,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ObsidianSurfaceHigh
            )
        )
    }
}
