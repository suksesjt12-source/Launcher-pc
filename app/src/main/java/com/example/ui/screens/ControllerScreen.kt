package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.HardwareAndControllerMonitor
import com.example.model.ConnectedGamepad
import com.example.model.ControllerLayoutPreset
import com.example.model.ControllerProfile
import com.example.model.LiveGamepadState
import com.example.model.StormAction
import com.example.ui.components.LivePadVisualizer
import com.example.ui.components.StatusPill
import com.example.ui.components.StormCard
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianOutlineVariant
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RasenganCyan
import com.example.ui.theme.ShinobiAmber
import com.example.ui.theme.ShinobiEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControllerScreen(
    connectedGamepads: List<ConnectedGamepad>,
    profile: ControllerProfile,
    liveState: LiveGamepadState,
    awaitingRemapAction: StormAction?,
    onSelectLayoutPreset: (ControllerLayoutPreset) -> Unit,
    onSetDeadzone: (Int) -> Unit,
    onSetTriggerThreshold: (Int) -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onSetSwapConfirm: (Boolean) -> Unit,
    onStartRemap: (StormAction?) -> Unit,
    onBindKeyCode: (StormAction, Int) -> Unit,
    onResetDefaults: () -> Unit,
    onTestRumble: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (awaitingRemapAction != null) {
        RemapKeyDialog(
            action = awaitingRemapAction,
            preset = profile.layoutPreset,
            onBindKeyCode = { code -> onBindKeyCode(awaitingRemapAction, code) },
            onDismiss = { onStartRemap(null) }
        )
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
                // Controller Banner & Detected Devices
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ObsidianSurface,
                    border = BorderStroke(
                        1.dp,
                        if (connectedGamepads.isNotEmpty()) RasenganCyan.copy(alpha = 0.65f) else ObsidianOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_controller_deck),
                                contentDescription = "Ilustrasi pengontrol eksternal",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                ObsidianBg.copy(alpha = 0.25f),
                                                ObsidianSurface
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dukungan Pengontrol Eksternal",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "XInput Bridge • Bluetooth & USB-C OTG HID",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RasenganCyan
                                    )
                                }
                                StatusPill(
                                    label = if (connectedGamepads.isNotEmpty()) {
                                        "${connectedGamepads.size} Aktif"
                                    } else {
                                        "Menunggu"
                                    },
                                    color = if (connectedGamepads.isNotEmpty()) ShinobiEmerald else ShinobiAmber
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (connectedGamepads.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.controller_none_connected),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            } else {
                                connectedGamepads.forEach { pad ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = ObsidianSurfaceVariant,
                                        border = BorderStroke(1.dp, RasenganCyan.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.BluetoothConnected,
                                                    contentDescription = null,
                                                    tint = ShinobiEmerald
                                                )
                                                Column {
                                                    Text(
                                                        text = pad.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "VID:${pad.vendorId.toString(16)} PID:${pad.productId.toString(16)} • ${pad.sourcesLabel}",
                                                        fontFamily = JetBrainsMonoFamily,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                            }
                                            StatusPill(
                                                label = if (pad.hasVibrator) "Rumble" else "HID",
                                                color = if (pad.hasVibrator) ChakraOrange else RasenganCyan
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onTestRumble,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("test_rumble_button"),
                                border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChakraOrange)
                            ) {
                                Icon(Icons.Filled.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.controller_test_rumble))
                            }
                        }
                    }
                }

                // Live Gamepad Input Visualizer
                StormCard(accentColor = RasenganCyan.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Gamepad, contentDescription = null, tint = RasenganCyan)
                            Text(
                                text = "Uji Input Langsung",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = liveState.lastKeyLabel,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = ChakraOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LivePadVisualizer(
                        liveState = liveState,
                        deadzonePercent = profile.deadzonePercent,
                        layoutPreset = profile.layoutPreset
                    )
                }

                // Calibration & Preset Card
                StormCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = ChakraOrange)
                        Text("Kalibrasi Stik & Tata Letak", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Gaya Ikon Tombol", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControllerLayoutPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = profile.layoutPreset == preset,
                                onClick = { onSelectLayoutPreset(preset) },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("ctrl_preset_${preset.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RasenganCyan,
                                    selectedLabelColor = ObsidianBg,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deadzone Analog", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "${profile.deadzonePercent}%",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelLarge,
                            color = RasenganCyan
                        )
                    }
                    Slider(
                        value = profile.deadzonePercent.toFloat(),
                        onValueChange = { onSetDeadzone(it.toInt()) },
                        valueRange = 0f..35f,
                        modifier = Modifier.testTag("deadzone_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = RasenganCyan,
                            activeTrackColor = RasenganCyan,
                            inactiveTrackColor = ObsidianSurfaceHigh
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensitivitas Trigger L2/R2", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "${profile.triggerThresholdPercent}%",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelLarge,
                            color = ChakraOrange
                        )
                    }
                    Slider(
                        value = profile.triggerThresholdPercent.toFloat(),
                        onValueChange = { onSetTriggerThreshold(it.toInt()) },
                        valueRange = 5f..50f,
                        modifier = Modifier.testTag("trigger_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ChakraOrange,
                            activeTrackColor = ChakraOrange,
                            inactiveTrackColor = ObsidianSurfaceHigh
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Umpan Balik Getar (Rumble)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Aktifkan getaran saat jutsu & guard break", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(
                            checked = profile.vibrationEnabled,
                            onCheckedChange = onSetVibration,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = ChakraOrange
                            )
                        )
                    }

                    HorizontalDivider(color = ObsidianOutlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tukar Konfirmasi / Batal", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Gunakan tata letak menu Jepang (B/○ konfirmasi)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(
                            checked = profile.swapConfirmCancel,
                            onCheckedChange = onSetSwapConfirm,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = RasenganCyan
                            )
                        )
                    }
                }

                // Storm Revolution Combat Action Remapping
                StormCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pemetaan Tombol Pertarungan",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = onResetDefaults,
                            modifier = Modifier.testTag("reset_controller_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = "Reset pemetaan",
                                tint = ChakraOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", color = ChakraOrange)
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.controller_remap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    StormAction.entries.forEachIndexed { idx, action ->
                        val boundCode = profile.bindings[action] ?: action.defaultKeyCode
                        val keyLabel = HardwareAndControllerMonitor.formatKeyLabel(boundCode, profile.layoutPreset)
                        Surface(
                            onClick = { onStartRemap(action) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("remap_row_${action.name.lowercase()}"),
                            shape = MaterialTheme.shapes.small,
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianOutline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = action.actionName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = action.combatRole,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ChakraOrange.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = keyLabel,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ChakraOrange,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemapKeyDialog(
    action: StormAction,
    preset: ControllerLayoutPreset,
    onBindKeyCode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val quickKeys = listOf(
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_SELECT
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Petakan: ${action.actionName}",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tekan tombol pada pengontrol eksternal Anda sekarang, atau pilih tombol di bawah secara manual:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickKeys.forEach { code ->
                        OutlinedButton(
                            onClick = { onBindKeyCode(code) },
                            border = BorderStroke(1.dp, RasenganCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RasenganCyan)
                        ) {
                            Text(
                                text = HardwareAndControllerMonitor.formatKeyLabel(code, preset),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianSurfaceVariant,
                    contentColor = TextPrimary
                )
            ) {
                Text("Batal")
            }
        }
    )
}
