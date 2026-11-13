package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.LauncherTab
import com.example.ui.LauncherViewModel
import com.example.ui.components.DiagnosticViewportDialog
import com.example.ui.components.StatusPill
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.GraphicsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecompileScreen
import com.example.ui.theme.ChakraOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RasenganCyan
import com.example.ui.theme.ShinobiEmerald
import com.example.ui.theme.StormRevTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StormRevTheme {
                StormRevApp(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && viewModel.handleKeyDown(event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && viewModel.handleKeyUp(event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null && viewModel.handleMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}

private data class NavDestination(
    val tab: LauncherTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StormRevApp(viewModel: LauncherViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val graphicsConfig by viewModel.graphicsConfig.collectAsStateWithLifecycle()
    val controllerProfile by viewModel.controllerProfile.collectAsStateWithLifecycle()
    val connectedGamepads by viewModel.connectedGamepads.collectAsStateWithLifecycle()
    val hardwareProfile by viewModel.hardwareProfile.collectAsStateWithLifecycle()
    val gameDirectoryStatus by viewModel.gameDirectoryStatus.collectAsStateWithLifecycle()
    val liveGamepadState by viewModel.liveGamepadState.collectAsStateWithLifecycle()
    val awaitingRemapAction by viewModel.awaitingRemapAction.collectAsStateWithLifecycle()
    val recompileState by viewModel.recompileState.collectAsStateWithLifecycle()
    val showDiagnostic by viewModel.showDiagnosticViewport.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (selectedTab != LauncherTab.HOME) {
        BackHandler {
            viewModel.selectTab(LauncherTab.HOME)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    val createConfigIniLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConfigIniToDocument(uri)
        }
    }

    if (showDiagnostic) {
        DiagnosticViewportDialog(
            graphicsConfig = graphicsConfig,
            liveState = liveGamepadState,
            layoutPreset = controllerProfile.layoutPreset,
            deadzonePercent = controllerProfile.deadzonePercent,
            onDismiss = { viewModel.setDiagnosticViewportVisible(false) }
        )
    }

    val destinations = remember {
        listOf(
            NavDestination(LauncherTab.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            NavDestination(LauncherTab.GRAPHICS, R.string.nav_graphics, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_graphics"),
            NavDestination(LauncherTab.CONTROLLER, R.string.nav_controller, Icons.Filled.Gamepad, Icons.Outlined.Gamepad, "nav_controller"),
            NavDestination(LauncherTab.RECOMPILE, R.string.nav_recompile, Icons.Filled.Build, Icons.Outlined.Build, "nav_recompile")
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    },
                    actions = {
                        if (connectedGamepads.isNotEmpty()) {
                            StatusPill(
                                label = "PAD",
                                color = ShinobiEmerald,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        } else {
                            StatusPill(
                                label = if (hardwareProfile.supportsVulkan) "VK" else "GL",
                                color = RasenganCyan,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ObsidianSurface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 4.dp
                    ) {
                        destinations.forEach { dest ->
                            val selected = selectedTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(id = dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = dest.labelRes)) },
                                modifier = Modifier.testTag(dest.tag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ChakraOrange,
                                    selectedTextColor = ChakraOrange,
                                    indicatorColor = ObsidianSurfaceVariant,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEach { dest ->
                            val selected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(id = dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(id = dest.labelRes)) },
                                modifier = Modifier.testTag(dest.tag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ChakraOrange,
                                    selectedTextColor = ChakraOrange,
                                    indicatorColor = ObsidianSurfaceVariant,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
                        when (tab) {
                            LauncherTab.HOME -> HomeScreen(
                                isWideScreen = isWideScreen,
                                gameDirectoryStatus = gameDirectoryStatus,
                                graphicsConfig = graphicsConfig,
                                controllerProfile = controllerProfile,
                                connectedGamepads = connectedGamepads,
                                hardwareProfile = hardwareProfile,
                                recompileState = recompileState,
                                onPickGameFolder = { folderPickerLauncher.launch(null) },
                                onRescanFolder = { viewModel.rescanCurrentFolder() },
                                onNavigateTab = { viewModel.selectTab(it) },
                                onRunRecompile = { viewModel.runRecompilePipeline() },
                                onOpenDiagnostic = { viewModel.setDiagnosticViewportVisible(true) },
                                onLaunchExternalContainer = { viewModel.launchExternalContainer() }
                            )

                            LauncherTab.GRAPHICS -> GraphicsScreen(
                                config = graphicsConfig,
                                hardwareProfile = hardwareProfile,
                                onApplyPreset = { viewModel.applyGraphicsPreset(it) },
                                onSetBackend = { viewModel.setRenderBackend(it) },
                                onSetWinlatorDriver = { viewModel.setWinlatorDriver(it) },
                                onSetDxWrapper = { viewModel.setDxWrapper(it) },
                                onSetResolution = { viewModel.setResolutionScale(it) },
                                onSetFpsTarget = { viewModel.setFpsTarget(it) },
                                onSetAspectRatio = { viewModel.setAspectRatio(it) },
                                onSetShadowLevel = { viewModel.setShadowLevel(it) },
                                onSetFxaa = { viewModel.setFxaa(it) },
                                onSetGlare = { viewModel.setGlare(it) },
                                onSetSsao = { viewModel.setSsao(it) },
                                onSetCelOutline = { viewModel.setCelOutlineBoost(it) },
                                onSetAsyncShader = { viewModel.setAsyncShaderCompile(it) },
                                onSetCpuAffinity = { viewModel.setCpuAffinity(it) },
                                onSetTextureMemory = { viewModel.setTextureMemoryMb(it) },
                                onSetBox64StrongMem = { viewModel.setBox64StrongMem(it) },
                                onExportIni = { createConfigIniLauncher.launch("config.ini") }
                            )

                            LauncherTab.CONTROLLER -> ControllerScreen(
                                connectedGamepads = connectedGamepads,
                                profile = controllerProfile,
                                liveState = liveGamepadState,
                                awaitingRemapAction = awaitingRemapAction,
                                onSelectLayoutPreset = { viewModel.setControllerLayoutPreset(it) },
                                onSetDeadzone = { viewModel.setDeadzonePercent(it) },
                                onSetTriggerThreshold = { viewModel.setTriggerThresholdPercent(it) },
                                onSetVibration = { viewModel.setVibrationEnabled(it) },
                                onSetSwapConfirm = { viewModel.setSwapConfirmCancel(it) },
                                onStartRemap = { viewModel.startWaitingForRemap(it) },
                                onBindKeyCode = { action, code -> viewModel.bindActionToKeyCode(action, code) },
                                onResetDefaults = { viewModel.resetControllerDefaults() },
                                onTestRumble = { viewModel.testRumble() }
                            )

                            LauncherTab.RECOMPILE -> RecompileScreen(
                                recompileState = recompileState,
                                gameDirectoryStatus = gameDirectoryStatus,
                                hardwareProfile = hardwareProfile,
                                onRunRecompile = { viewModel.runRecompilePipeline() },
                                onPickGameFolder = { folderPickerLauncher.launch(null) },
                                onExportAllToFolder = { viewModel.exportAllConfigsToGameFolder() },
                                onExportSingleConfigIni = { createConfigIniLauncher.launch("config.ini") },
                                onOpenDiagnostic = { viewModel.setDiagnosticViewportVisible(true) },
                                onLaunchExternalContainer = { viewModel.launchExternalContainer() }
                            )
                        }
                    }
                }
            }
        }
    }
}
