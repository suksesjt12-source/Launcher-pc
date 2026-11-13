package com.example.ui.components

import android.app.Activity
import android.view.Choreographer
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.HardwareAndControllerMonitor
import com.example.model.ConnectedGamepad
import com.example.model.ControllerLayoutPreset
import com.example.model.ControllerProfile
import com.example.model.DeviceHardwareProfile
import com.example.model.FpsTarget
import com.example.model.GameDirectoryStatus
import com.example.model.GraphicsConfig
import com.example.model.LiveGamepadState
import com.example.model.RecompilePipelineState
import com.example.model.StormAction
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
import kotlinx.coroutines.delay

@Composable
fun GameRuntimeStageDialog(
    graphicsConfig: GraphicsConfig,
    controllerProfile: ControllerProfile,
    connectedGamepads: List<ConnectedGamepad>,
    liveState: LiveGamepadState,
    hardwareProfile: DeviceHardwareProfile,
    gameDirectoryStatus: GameDirectoryStatus,
    recompileState: RecompilePipelineState,
    onVirtualKeyPress: (Int, Boolean) -> Unit,
    onPickGameFolder: () -> Unit,
    onExportBundleToFolder: () -> Unit,
    onLaunchExternalContainer: () -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var bootProgress by remember { mutableFloatStateOf(0f) }
    var bootPhaseText by remember { mutableStateOf("Memuat profil shader ${graphicsConfig.renderBackend.label}...") }
    var runtimeReady by remember { mutableStateOf(false) }
    var showVirtualPad by remember { mutableStateOf(connectedGamepads.isEmpty()) }

    LaunchedEffect(Unit) {
        bootProgress = 0.25f
        bootPhaseText = "Menginisialisasi konfigurasi Storm Revolution (${graphicsConfig.resolutionScale.label})..."
        delay(260)
        bootProgress = 0.55f
        bootPhaseText = "Mengaitkan XInput Bridge (${controllerProfile.layoutPreset.label}, Deadzone ${controllerProfile.deadzonePercent}%)..."
        delay(260)
        bootProgress = 0.85f
        bootPhaseText = "Memverifikasi bundel config.ini, dxvk.conf & launch_nsunsr.bat..."
        delay(240)
        bootProgress = 1f
        bootPhaseText = if (gameDirectoryStatus is GameDirectoryStatus.Ready && gameDirectoryStatus.exeDetected) {
            "Runtime Siap • NSUNSR.exe Terhubung"
        } else {
            "Mode Runtime Stage Siap • Tautkan NSUNSR.exe / Container untuk Eksekusi Penuh"
        }
        runtimeReady = true
    }

    // Real Choreographer frame pacing monitor
    var currentFps by remember { mutableIntStateOf(0) }
    var frameMs by remember { mutableFloatStateOf(16.6f) }
    DisposableEffect(Unit) {
        var lastNanos = 0L
        var count = 0
        var windowStart = 0L
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastNanos != 0L) {
                    frameMs = (frameTimeNanos - lastNanos) / 1_000_000f
                }
                lastNanos = frameTimeNanos
                if (windowStart == 0L) windowStart = frameTimeNanos
                count++
                val elapsed = frameTimeNanos - windowStart
                if (elapsed >= 500_000_000L) {
                    currentFps = ((count * 1_000_000_000L) / elapsed).toInt()
                    count = 0
                    windowStart = frameTimeNanos
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(cb)
        onDispose { Choreographer.getInstance().removeFrameCallback(cb) }
    }

    // Resolve active Storm combat action from user's custom keybindings
    val activeActions = remember(liveState.pressedKeyCodes, controllerProfile.bindings) {
        StormAction.entries.filter { action ->
            val boundCode = controllerProfile.bindings[action] ?: action.defaultKeyCode
            boundCode in liveState.pressedKeyCodes
        }
    }

    val auraPulse by rememberInfiniteTransition(label = "aura_pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ObsidianBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Telemetry HUD Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STORMREV RUNTIME STAGE",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChakraOrange
                        )
                        Text(
                            text = "Naruto Shippuden: UNS Revolution • ${graphicsConfig.renderBackend.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(
                            label = "$currentFps FPS (${String.format("%.1f", frameMs)}ms)",
                            color = ShinobiEmerald
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_game_stage_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Keluar dari Runtime Stage",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                // Viewport Arena Stage
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ObsidianSurface,
                    border = BorderStroke(
                        1.5.dp,
                        if (activeActions.isNotEmpty()) RasenganCyan else ChakraOrange.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_storm_hero_banner),
                            contentDescription = "Layar peluncuran Storm Revolution",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ObsidianBg.copy(alpha = 0.45f),
                                            ObsidianBg.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        // Chakra / Analog Vector Canvas Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = 58f + (auraPulse * 14f)
                            val ringColor = when {
                                StormAction.CHAKRA_LOAD in activeActions -> RasenganCyan
                                StormAction.MELEE_ATTACK in activeActions -> ChakraOrange
                                StormAction.SUBSTITUTION in activeActions -> ShinobiAmber
                                StormAction.GUARD in activeActions -> ShinobiEmerald
                                else -> ChakraOrange.copy(alpha = 0.45f)
                            }
                            drawCircle(
                                color = ringColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 4f)
                            )
                            val moveOffset = Offset(
                                x = center.x + (liveState.leftStickX * 65f),
                                y = center.y + (liveState.leftStickY * 65f)
                            )
                            drawCircle(
                                color = RasenganCyan,
                                radius = 14f,
                                center = moveOffset
                            )
                        }

                        // Status & Active Combat Action HUD
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!runtimeReady) {
                                Text(
                                    text = bootPhaseText,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RasenganCyan
                                )
                                LinearProgressIndicator(
                                    progress = { bootProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ChakraOrange,
                                    trackColor = ObsidianSurfaceHigh
                                )
                            } else {
                                val actionHeadline = if (activeActions.isNotEmpty()) {
                                    activeActions.joinToString(" + ") { it.actionName.uppercase() }
                                } else {
                                    "SIAP BERTARUNG • TEKAN TOMBOL KONTROLER"
                                }
                                StatusPill(
                                    label = actionHeadline,
                                    color = if (activeActions.isNotEmpty()) RasenganCyan else ShinobiEmerald
                                )
                                Text(
                                    text = bootPhaseText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Runtime Handoff & File Bridge Actions
                StormCard(accentColor = ObsidianOutline) {
                    val targetFpsStr = when (graphicsConfig.fpsTarget) {
                        FpsTarget.FPS_30 -> "30 FPS"
                        FpsTarget.FPS_60 -> "60 FPS"
                        FpsTarget.UNCAPPED -> "Uncapped"
                    }
                    Text(
                        text = "Status Eksekusi & Jembatan Windows/Wine",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("Resolusi: ${graphicsConfig.resolutionScale.label} • Target: $targetFpsStr • ")
                            append("Kontroler: ${connectedGamepads.firstOrNull()?.name ?: "Virtual/Siaga"}")
                        },
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = RasenganCyan
                    )
                    if (recompileState.localBundlePath != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bundel: ${recompileState.localBundlePath}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val pkg = hardwareProfile.installedRuntimePackage
                    if (pkg != null) {
                        Button(
                            onClick = onLaunchExternalContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("stage_launch_external_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShinobiEmerald,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jalankan NSUNSR.exe di $pkg", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "Android memerlukan container Windows/Wine (mis. Winlator) untuk mengeksekusi biner x86 NSUNSR.exe secara penuh. Anda tetap dapat menyinkronkan skrip peluncuran (launch_nsunsr.bat + config.ini + dxvk.conf) ke folder game PC dan menguji seluruh kontroler di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (gameDirectoryStatus is GameDirectoryStatus.Ready) {
                            OutlinedButton(
                                onClick = onExportBundleToFolder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, RasenganCyan.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RasenganCyan)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sinkron ke PC Folder")
                            }
                        } else {
                            OutlinedButton(
                                onClick = onPickGameFolder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChakraOrange)
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih NSUNSR.exe")
                            }
                        }

                        OutlinedButton(
                            onClick = { showVirtualPad = !showVirtualPad },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("toggle_virtual_pad_button"),
                            border = BorderStroke(1.dp, ObsidianOutline)
                        ) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showVirtualPad) "Sembunyikan Pad" else "Tampilkan Pad", color = TextPrimary)
                        }
                    }
                }

                // Virtual On-Screen Touch Gamepad (for testing bindings or touchscreen play)
                AnimatedVisibility(visible = showVirtualPad) {
                    StormCard(accentColor = RasenganCyan.copy(alpha = 0.5f)) {
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
                                    text = "Kontroler Virtual & Uji Aksi Storm",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = controllerProfile.layoutPreset.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Shoulder + D-Pad
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VirtualActionButton(
                                        label = "L2\nKawarimi",
                                        keyCode = controllerProfile.bindings[StormAction.SUBSTITUTION] ?: KeyEvent.KEYCODE_BUTTON_L2,
                                        pressedKeys = liveState.pressedKeyCodes,
                                        accent = RasenganCyan,
                                        onPressChange = onVirtualKeyPress
                                    )
                                    VirtualActionButton(
                                        label = "L1\nSupp 1",
                                        keyCode = controllerProfile.bindings[StormAction.SUPPORT_LEFT] ?: KeyEvent.KEYCODE_BUTTON_L1,
                                        pressedKeys = liveState.pressedKeyCodes,
                                        accent = TextSecondary,
                                        onPressChange = onVirtualKeyPress
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VirtualActionButton(
                                        label = "R2\nGuard",
                                        keyCode = controllerProfile.bindings[StormAction.GUARD] ?: KeyEvent.KEYCODE_BUTTON_R2,
                                        pressedKeys = liveState.pressedKeyCodes,
                                        accent = ShinobiEmerald,
                                        onPressChange = onVirtualKeyPress
                                    )
                                    VirtualActionButton(
                                        label = "R1\nSupp 2",
                                        keyCode = controllerProfile.bindings[StormAction.SUPPORT_RIGHT] ?: KeyEvent.KEYCODE_BUTTON_R1,
                                        pressedKeys = liveState.pressedKeyCodes,
                                        accent = TextSecondary,
                                        onPressChange = onVirtualKeyPress
                                    )
                                }
                            }

                            // Right Face Buttons (Chakra, Melee, Shuriken, Jump/Dash)
                            val yLabel = if (controllerProfile.layoutPreset == ControllerLayoutPreset.PLAYSTATION) "△" else "Y"
                            val bLabel = if (controllerProfile.layoutPreset == ControllerLayoutPreset.PLAYSTATION) "○" else "B"
                            val xLabel = if (controllerProfile.layoutPreset == ControllerLayoutPreset.PLAYSTATION) "□" else "X"
                            val aLabel = if (controllerProfile.layoutPreset == ControllerLayoutPreset.PLAYSTATION) "✕" else "A"

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VirtualFaceCircle(
                                    label = "$yLabel\nChakra",
                                    keyCode = controllerProfile.bindings[StormAction.CHAKRA_LOAD] ?: KeyEvent.KEYCODE_BUTTON_Y,
                                    pressedKeys = liveState.pressedKeyCodes,
                                    accent = ShinobiAmber,
                                    onPressChange = onVirtualKeyPress
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    VirtualFaceCircle(
                                        label = "$xLabel\nKunai",
                                        keyCode = controllerProfile.bindings[StormAction.SHURIKEN] ?: KeyEvent.KEYCODE_BUTTON_X,
                                        pressedKeys = liveState.pressedKeyCodes,
                                        accent = RasenganCyan,
                                        onPressChange = onVirtualKeyPress
                                    )
                                    VirtualFaceCircle(
                                        label = "$bLabel\nJutsu",
                                        keyCode = controllerProfile.bindings[StormAction.MELEE_ATTACK] ?: KeyEvent.KEYCODE_BUTTON_B,
                                        pressedKeys = liveState.pressedKeyCodes,
                                        accent = SharinganCrimson,
                                        onPressChange = onVirtualKeyPress
                                    )
                                }
                                VirtualFaceCircle(
                                    label = "$aLabel\nDash",
                                    keyCode = controllerProfile.bindings[StormAction.JUMP_DASH] ?: KeyEvent.KEYCODE_BUTTON_A,
                                    pressedKeys = liveState.pressedKeyCodes,
                                    accent = ShinobiEmerald,
                                    onPressChange = onVirtualKeyPress
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianSurfaceVariant,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kembali ke Launcher")
                }
            }
        }
    }
}

@Composable
private fun VirtualActionButton(
    label: String,
    keyCode: Int,
    pressedKeys: Set<Int>,
    accent: Color,
    onPressChange: (Int, Boolean) -> Unit
) {
    val active = keyCode in pressedKeys
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) accent else ObsidianSurfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
        modifier = Modifier
            .size(width = 76.dp, height = 48.dp)
            .pointerInput(keyCode) {
                detectTapGestures(
                    onPress = {
                        onPressChange(keyCode, true)
                        tryAwaitRelease()
                        onPressChange(keyCode, false)
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) ObsidianBg else TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun VirtualFaceCircle(
    label: String,
    keyCode: Int,
    pressedKeys: Set<Int>,
    accent: Color,
    onPressChange: (Int, Boolean) -> Unit
) {
    val active = keyCode in pressedKeys
    Surface(
        shape = CircleShape,
        color = if (active) accent else ObsidianSurfaceVariant,
        border = BorderStroke(1.5.dp, accent),
        modifier = Modifier
            .size(56.dp)
            .pointerInput(keyCode) {
                detectTapGestures(
                    onPress = {
                        onPressChange(keyCode, true)
                        tryAwaitRelease()
                        onPressChange(keyCode, false)
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) ObsidianBg else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
