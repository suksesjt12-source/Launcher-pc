package com.example.ui.components

import android.view.Choreographer
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ControllerLayoutPreset
import com.example.model.FpsTarget
import com.example.model.GraphicsConfig
import com.example.model.LiveGamepadState
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RasenganCyan
import com.example.ui.theme.SharinganCrimson
import com.example.ui.theme.ShinobiAmber
import com.example.ui.theme.ShinobiEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StormCard(
    modifier: Modifier = Modifier,
    accentColor: Color = ObsidianOutline,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ObsidianSurface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TelemetryGaugeBar(
    title: String,
    valueText: String,
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "gauge_progress"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = ObsidianSurfaceHigh
        )
    }
}

@Composable
fun LivePadVisualizer(
    liveState: LiveGamepadState,
    deadzonePercent: Int,
    layoutPreset: ControllerLayoutPreset,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Trigger bars (LT & RT)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TriggerBar(
                label = if (layoutPreset == ControllerLayoutPreset.PLAYSTATION) "L2 (Kawarimi)" else "LT / L2 (Kawarimi)",
                value = liveState.leftTrigger,
                isPressed = KeyEvent.KEYCODE_BUTTON_L2 in liveState.pressedKeyCodes,
                accent = RasenganCyan,
                modifier = Modifier.weight(1f)
            )
            TriggerBar(
                label = if (layoutPreset == ControllerLayoutPreset.PLAYSTATION) "R2 (Guard)" else "RT / R2 (Guard)",
                value = liveState.rightTrigger,
                isPressed = KeyEvent.KEYCODE_BUTTON_R2 in liveState.pressedKeyCodes,
                accent = ChakraOrange,
                modifier = Modifier.weight(1f)
            )
        }

        // Sticks & Face Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StickCanvas(
                title = "Stik Kiri",
                x = liveState.leftStickX,
                y = liveState.leftStickY,
                deadzoneRatio = deadzonePercent / 100f,
                accent = RasenganCyan
            )

            FaceButtonsCluster(
                pressed = liveState.pressedKeyCodes,
                preset = layoutPreset
            )

            StickCanvas(
                title = "Stik Kanan",
                x = liveState.rightStickX,
                y = liveState.rightStickY,
                deadzoneRatio = deadzonePercent / 100f,
                accent = ChakraOrange
            )
        }

        // Shoulder & D-Pad quick indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shoulderButtons = listOf(
                KeyEvent.KEYCODE_BUTTON_L1 to "L1/LB",
                KeyEvent.KEYCODE_BUTTON_SELECT to "Back",
                KeyEvent.KEYCODE_BUTTON_START to "Start",
                KeyEvent.KEYCODE_BUTTON_R1 to "R1/RB"
            )
            shoulderButtons.forEach { (code, label) ->
                val active = code in liveState.pressedKeyCodes
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) ChakraOrange else ObsidianSurfaceVariant,
                    border = BorderStroke(1.dp, if (active) ChakraOrange else ObsidianOutline)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) ObsidianBg else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TriggerBar(
    label: String,
    value: Float,
    isPressed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = if (isPressed) accent else ObsidianOutline,
        label = "trigger_color"
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPressed) accent else TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            color = accent,
            trackColor = ObsidianSurfaceHigh
        )
    }
}

@Composable
private fun StickCanvas(
    title: String,
    x: Float,
    y: Float,
    deadzoneRatio: Float,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 6f

            drawCircle(
                color = ObsidianSurfaceVariant,
                radius = radius,
                center = center
            )
            drawCircle(
                color = ObsidianOutline,
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )
            // Deadzone ring
            if (deadzoneRatio > 0f) {
                drawCircle(
                    color = SharinganCrimson.copy(alpha = 0.45f),
                    radius = radius * deadzoneRatio.coerceIn(0.05f, 0.45f),
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }
            // Crosshair
            drawLine(
                color = ObsidianOutline,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = ObsidianOutline,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1f
            )

            val dotCenter = Offset(
                x = center.x + (x.coerceIn(-1f, 1f) * (radius - 8f)),
                y = center.y + (y.coerceIn(-1f, 1f) * (radius - 8f))
            )
            drawLine(
                color = accent.copy(alpha = 0.7f),
                start = center,
                end = dotCenter,
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = accent,
                radius = 9f,
                center = dotCenter
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun FaceButtonsCluster(
    pressed: Set<Int>,
    preset: ControllerLayoutPreset
) {
    val topLabel = when (preset) {
        ControllerLayoutPreset.PLAYSTATION -> "△"
        ControllerLayoutPreset.SWITCH_PRO -> "X"
        ControllerLayoutPreset.XBOX -> "Y"
    }
    val rightLabel = when (preset) {
        ControllerLayoutPreset.PLAYSTATION -> "○"
        ControllerLayoutPreset.SWITCH_PRO -> "A"
        ControllerLayoutPreset.XBOX -> "B"
    }
    val leftLabel = when (preset) {
        ControllerLayoutPreset.PLAYSTATION -> "□"
        ControllerLayoutPreset.SWITCH_PRO -> "Y"
        ControllerLayoutPreset.XBOX -> "X"
    }
    val bottomLabel = when (preset) {
        ControllerLayoutPreset.PLAYSTATION -> "✕"
        ControllerLayoutPreset.SWITCH_PRO -> "B"
        ControllerLayoutPreset.XBOX -> "A"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FaceGlyph(topLabel, KeyEvent.KEYCODE_BUTTON_Y in pressed, ShinobiAmber)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            FaceGlyph(leftLabel, KeyEvent.KEYCODE_BUTTON_X in pressed, RasenganCyan)
            FaceGlyph(rightLabel, KeyEvent.KEYCODE_BUTTON_B in pressed, SharinganCrimson)
        }
        FaceGlyph(bottomLabel, KeyEvent.KEYCODE_BUTTON_A in pressed, ShinobiEmerald)
    }
}

@Composable
private fun FaceGlyph(label: String, isActive: Boolean, accent: Color) {
    Surface(
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = if (isActive) accent else ObsidianSurfaceVariant,
        border = BorderStroke(1.5.dp, if (isActive) accent else accent.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) ObsidianBg else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DiagnosticViewportDialog(
    graphicsConfig: GraphicsConfig,
    liveState: LiveGamepadState,
    layoutPreset: ControllerLayoutPreset,
    deadzonePercent: Int,
    onDismiss: () -> Unit
) {
    var currentFps by remember { mutableIntStateOf(0) }
    var lastFrameMs by remember { mutableFloatStateOf(16.6f) }
    val frameTimes = remember { mutableStateListOf<Float>() }

    DisposableEffect(Unit) {
        var lastNanos = 0L
        var framesInWindow = 0
        var windowStartNanos = 0L

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastNanos != 0L) {
                    val dtMs = (frameTimeNanos - lastNanos) / 1_000_000f
                    lastFrameMs = dtMs
                    frameTimes.add(dtMs)
                    if (frameTimes.size > 48) frameTimes.removeAt(0)
                }
                lastNanos = frameTimeNanos
                if (windowStartNanos == 0L) windowStartNanos = frameTimeNanos
                framesInWindow++
                val elapsed = frameTimeNanos - windowStartNanos
                if (elapsed >= 500_000_000L) {
                    currentFps = ((framesInWindow * 1_000_000_000L) / elapsed).toInt()
                    framesInWindow = 0
                    windowStartNanos = frameTimeNanos
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
        onDispose {
            Choreographer.getInstance().removeFrameCallback(callback)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = ObsidianBg,
            border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Diagnostik Frame",
                            tint = ChakraOrange
                        )
                        Text(
                            text = "Diagnostik Frame & Kontroler",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_diagnostic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup diagnostik",
                            tint = TextSecondary
                        )
                    }
                }

                // Real Choreographer Frame-Time Graph
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val targetLabel = when (graphicsConfig.fpsTarget) {
                            FpsTarget.FPS_30 -> "Target: 30 FPS (33.3 ms)"
                            FpsTarget.FPS_60 -> "Target: 60 FPS (16.6 ms)"
                            FpsTarget.UNCAPPED -> "Target: Tanpa Batas"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Laju Layar Aktual: $currentFps FPS",
                                style = MaterialTheme.typography.titleMedium,
                                color = ShinobiEmerald
                            )
                            Text(
                                text = String.format("%.1f ms", lastFrameMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = RasenganCyan
                            )
                        }
                        Text(
                            text = "$targetLabel • ${graphicsConfig.renderBackend.label} • ${graphicsConfig.resolutionScale.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBg)
                        ) {
                            val samples = frameTimes.toList()
                            if (samples.size >= 2) {
                                val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                                val path = Path()
                                samples.forEachIndexed { idx, ms ->
                                    val normalized = (ms / 40f).coerceIn(0.05f, 1f)
                                    val px = idx * stepX
                                    val py = size.height * (1f - normalized)
                                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.horizontalGradient(listOf(RasenganCyan, ChakraOrange)),
                                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                                )
                            }
                            // 16.6ms baseline
                            val y16 = size.height * (1f - (16.6f / 40f))
                            drawLine(
                                color = ShinobiEmerald.copy(alpha = 0.45f),
                                start = Offset(0f, y16),
                                end = Offset(size.width, y16),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                }

                // Live Gamepad Input Pad inside Diagnostic Viewport
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gamepad,
                                contentDescription = "Input Pengontrol",
                                tint = RasenganCyan
                            )
                            Text(
                                text = "Input Terakhir: ${liveState.lastKeyLabel}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Perangkat: ${liveState.lastDeviceName}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        LivePadVisualizer(
                            liveState = liveState,
                            deadzonePercent = deadzonePercent,
                            layoutPreset = layoutPreset
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChakraOrange,
                        contentColor = ObsidianBg
                    )
                ) {
                    Text("Selesai Uji Diagnostik", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
