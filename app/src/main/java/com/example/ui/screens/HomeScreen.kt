package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameFolderScanner
import com.example.model.ConnectedGamepad
import com.example.model.ControllerProfile
import com.example.model.DeviceHardwareProfile
import com.example.model.GameDirectoryStatus
import com.example.model.GraphicsConfig
import com.example.model.LauncherTab
import com.example.model.RecompilePipelineState
import com.example.ui.components.StatusPill
import com.example.ui.components.StormCard
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RasenganCyan
import com.example.ui.theme.SharinganCrimson
import com.example.ui.theme.ShinobiAmber
import com.example.ui.theme.ShinobiEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    isWideScreen: Boolean,
    gameDirectoryStatus: GameDirectoryStatus,
    graphicsConfig: GraphicsConfig,
    controllerProfile: ControllerProfile,
    connectedGamepads: List<ConnectedGamepad>,
    hardwareProfile: DeviceHardwareProfile,
    recompileState: RecompilePipelineState,
    onPickGameFolder: () -> Unit,
    onRescanFolder: () -> Unit,
    onNavigateTab: (LauncherTab) -> Unit,
    onRunRecompile: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onLaunchExternalContainer: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                // Hero Banner Card
                HeroGameCard(
                    gameDirectoryStatus = gameDirectoryStatus,
                    recompileState = recompileState,
                    installedContainer = hardwareProfile.installedRuntimePackage,
                    onPickGameFolder = onPickGameFolder,
                    onRunRecompile = {
                        onRunRecompile()
                        onNavigateTab(LauncherTab.RECOMPILE)
                    },
                    onOpenDiagnostic = onOpenDiagnostic,
                    onLaunchExternalContainer = onLaunchExternalContainer
                )

                // PC Game Folder Status Card
                GameFolderCard(
                    status = gameDirectoryStatus,
                    onPickGameFolder = onPickGameFolder,
                    onRescanFolder = onRescanFolder
                )

                // Summary Cards (Adaptive 2-column on wide screens, stacked on phones)
                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GraphicsQuickCard(
                            config = graphicsConfig,
                            onClick = { onNavigateTab(LauncherTab.GRAPHICS) },
                            modifier = Modifier.weight(1f)
                        )
                        ControllerQuickCard(
                            gamepads = connectedGamepads,
                            profile = controllerProfile,
                            onClick = { onNavigateTab(LauncherTab.CONTROLLER) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    GraphicsQuickCard(
                        config = graphicsConfig,
                        onClick = { onNavigateTab(LauncherTab.GRAPHICS) }
                    )
                    ControllerQuickCard(
                        gamepads = connectedGamepads,
                        profile = controllerProfile,
                        onClick = { onNavigateTab(LauncherTab.CONTROLLER) }
                    )
                }

                // Device Hardware Probe Card
                DeviceHardwareCard(hardwareProfile = hardwareProfile)

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroGameCard(
    gameDirectoryStatus: GameDirectoryStatus,
    recompileState: RecompilePipelineState,
    installedContainer: String?,
    onPickGameFolder: () -> Unit,
    onRunRecompile: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onLaunchExternalContainer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.55f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_storm_hero_banner),
                    contentDescription = "Ilustrasi arena Naruto Storm Revolution",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.25f),
                                    ObsidianBg.copy(alpha = 0.65f),
                                    ObsidianSurface
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (pillText, pillColor) = when {
                        gameDirectoryStatus is GameDirectoryStatus.Ready &&
                            gameDirectoryStatus.isCompleteStormRevolutionBuild ->
                            "BUILD PC TERVERIFIKASI" to ShinobiEmerald
                        gameDirectoryStatus is GameDirectoryStatus.Ready ->
                            "FOLDER TERHUBUNG" to ShinobiAmber
                        recompileState.hasRunOnce ->
                            "PROFIL RECOMPILE SIAP" to RasenganCyan
                        else ->
                            "MENUNGGU BERKAS GAME PC" to ChakraOrange
                    }
                    StatusPill(label = pillText, color = pillColor)

                    Text(
                        text = stringResource(id = R.string.game_full_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(id = R.string.game_short_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = RasenganCyan
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = if (gameDirectoryStatus is GameDirectoryStatus.Ready) {
                            onRunRecompile
                        } else {
                            onPickGameFolder
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_primary_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChakraOrange,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Icon(
                            imageVector = if (gameDirectoryStatus is GameDirectoryStatus.Ready) {
                                Icons.Filled.Build
                            } else {
                                Icons.Filled.FolderOpen
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (gameDirectoryStatus is GameDirectoryStatus.Ready) {
                                "Recompile & Verifikasi"
                            } else {
                                stringResource(id = R.string.action_select_folder)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenDiagnostic,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_diagnostic_button"),
                        border = BorderStroke(1.dp, RasenganCyan.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RasenganCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Uji frame dan kontroler",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uji Frame", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (installedContainer != null) {
                    OutlinedButton(
                        onClick = onLaunchExternalContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_external_container_button"),
                        border = BorderStroke(1.dp, ShinobiEmerald.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShinobiEmerald)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Runtime Container ($installedContainer)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameFolderCard(
    status: GameDirectoryStatus,
    onPickGameFolder: () -> Unit,
    onRescanFolder: () -> Unit
) {
    StormCard(accentColor = ObsidianOutline) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = "Folder Game PC",
                    tint = ChakraOrange
                )
                Text(
                    text = "Sumber Berkas Game PC",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            if (status is GameDirectoryStatus.Ready) {
                OutlinedButton(
                    onClick = onRescanFolder,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("rescan_folder_button"),
                    border = BorderStroke(1.dp, ObsidianOutline)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Pindai ulang",
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pindai", color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (status) {
            GameDirectoryStatus.NotSelected -> {
                Text(
                    text = "Pilih direktori instalasi PC Naruto Ultimate Ninja Storm Revolution (yang memuat NSUNSR.exe dan folder data_win32) melalui Storage Access Framework tanpa izin penyimpanan luas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPickGameFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("select_game_folder_button"),
                    border = BorderStroke(1.dp, ChakraOrange.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChakraOrange)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.action_select_folder))
                }
            }

            GameDirectoryStatus.Scanning -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = ChakraOrange,
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Memindai NSUNSR.exe, header PE32, dan arsip CPK...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            is GameDirectoryStatus.Ready -> {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = ObsidianSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (status.exeDetected) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (status.exeDetected) ShinobiEmerald else ShinobiAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = status.folderName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = buildString {
                                append("NSUNSR.exe: ")
                                append(if (status.exeDetected) status.exeArchLabel ?: "Terdeteksi" else "Tidak ditemukan")
                                append(" • Arsip CPK: ${status.cpkCount}")
                                append(" • Total: ${GameFolderScanner.formatBytes(status.totalScannedSizeBytes)}")
                            },
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPickGameFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("change_game_folder_button"),
                    border = BorderStroke(1.dp, ObsidianOutline)
                ) {
                    Text("Ganti Folder Game PC", color = TextPrimary)
                }
            }

            is GameDirectoryStatus.Error -> {
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SharinganCrimson
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onPickGameFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Pilih Ulang Folder", color = ChakraOrange)
                }
            }
        }
    }
}

@Composable
private fun GraphicsQuickCard(
    config: GraphicsConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_graphics_card"),
        shape = MaterialTheme.shapes.medium,
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(Icons.Filled.Tune, contentDescription = "Grafis", tint = ChakraOrange)
                    Text("Performa Grafis", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                StatusPill(label = config.preset.label, color = ChakraOrange)
            }
            Text(
                text = "${config.renderBackend.label} • ${config.resolutionScale.label} • ${config.fpsTarget.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "Estimasi beban GPU: ${config.estimatedGpuLoadPercent()}% • VRAM ${config.estimatedVramUsageMb()} MB",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = RasenganCyan
            )
        }
    }
}

@Composable
private fun ControllerQuickCard(
    gamepads: List<ConnectedGamepad>,
    profile: ControllerProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePad = gamepads.firstOrNull()
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_controller_card"),
        shape = MaterialTheme.shapes.medium,
        color = ObsidianSurface,
        border = BorderStroke(
            1.dp,
            if (activePad != null) RasenganCyan.copy(alpha = 0.65f) else ObsidianOutline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(Icons.Filled.Gamepad, contentDescription = "Pengontrol", tint = RasenganCyan)
                    Text("Pengontrol Eksternal", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                StatusPill(
                    label = if (activePad != null) "${gamepads.size} Terhubung" else "Siaga HID",
                    color = if (activePad != null) ShinobiEmerald else TextMuted
                )
            }
            Text(
                text = activePad?.name ?: "Belum ada gamepad Bluetooth/USB OTG terdeteksi",
                style = MaterialTheme.typography.bodyMedium,
                color = if (activePad != null) TextPrimary else TextSecondary
            )
            Text(
                text = "Tata letak: ${profile.layoutPreset.label} • Deadzone: ${profile.deadzonePercent}%",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun DeviceHardwareCard(hardwareProfile: DeviceHardwareProfile) {
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
                Icon(Icons.Filled.Memory, contentDescription = "Perangkat Keras", tint = RasenganCyan)
                Text(
                    text = "Telemetri Perangkat Target",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            if (hardwareProfile.isMediaTekOrMali) {
                StatusPill(label = "MediaTek/Mali", color = ChakraOrange)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        val rows = listOf(
            "Perangkat" to hardwareProfile.deviceModel,
            "SoC / ABI" to "${hardwareProfile.socLabel} (${hardwareProfile.primaryAbi})",
            "GPU Renderer" to hardwareProfile.gpuRendererLabel,
            "CPU & Layar" to "${hardwareProfile.cpuCores} Core • ${hardwareProfile.displayRefreshHz} Hz",
            "Memori RAM" to "${hardwareProfile.availableRamMb} MB bebas / ${hardwareProfile.totalRamMb} MB total",
            "API & Rekomendasi" to "${hardwareProfile.vulkanVersionLabel} • ${hardwareProfile.recommendedDriver.label}"
        )
        rows.forEach { (k, v) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = k, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(
                    text = v,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
        }
    }
}
