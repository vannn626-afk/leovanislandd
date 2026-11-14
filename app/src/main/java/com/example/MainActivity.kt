package com.example

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.island.manager.IslandStateManager
import com.example.ui.customization.CustomizationStudioScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.IslandViewModel
import com.example.ui.permissions.PermissionsAndTroubleshootingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.LeovnIslandTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SapphireAccent
import com.example.ui.theme.TitaniumMuted

enum class LeovnDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    STUDIO("Studio", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_studio"),
    PERMISSIONS("System", Icons.Filled.Security, Icons.Outlined.Security, "nav_permissions"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: IslandViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        detectDisplayCutoutAndOrientation()

        setContent {
            LeovnIslandTheme {
                LeovnIslandMainApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionsSnapshot()
        detectDisplayCutoutAndOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        detectDisplayCutoutAndOrientation()
    }

    private fun detectDisplayCutoutAndOrientation() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        val cutoutInsetDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = window?.decorView?.rootWindowInsets?.displayCutout
            ((cutout?.safeInsetTop ?: 0) / density)
        } else {
            0f
        }
        IslandStateManager.updateCutoutAndOrientation(
            safeTopInsetDp = cutoutInsetDp,
            isLandscape = isLandscape
        )
    }
}

@Composable
fun LeovnIslandMainApp(viewModel: IslandViewModel) {
    val runtimeState by viewModel.runtimeState.collectAsState()
    val permissions by viewModel.permissionsSnapshot.collectAsState()
    var currentTab by rememberSaveable { mutableStateOf(LeovnDestination.DASHBOARD) }

    if (currentTab != LeovnDestination.DASHBOARD) {
        BackHandler {
            currentTab = LeovnDestination.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = CarbonSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                LeovnDestination.entries.forEach { dest ->
                    val selected = currentTab == dest
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = dest },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label
                            )
                        },
                        label = { Text(dest.label) },
                        modifier = Modifier.testTag(dest.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SapphireAccent,
                            selectedTextColor = Color.White,
                            indicatorColor = SapphireAccent.copy(alpha = 0.18f),
                            unselectedIconColor = TitaniumMuted,
                            unselectedTextColor = TitaniumMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                LeovnDestination.DASHBOARD -> DashboardScreen(
                    runtimeState = runtimeState,
                    permissions = permissions,
                    viewModel = viewModel,
                    onNavigateToPermissions = { currentTab = LeovnDestination.PERMISSIONS },
                    onNavigateToStudio = { currentTab = LeovnDestination.STUDIO }
                )

                LeovnDestination.STUDIO -> CustomizationStudioScreen(
                    runtimeState = runtimeState,
                    viewModel = viewModel
                )

                LeovnDestination.PERMISSIONS -> PermissionsAndTroubleshootingScreen(
                    permissions = permissions,
                    viewModel = viewModel
                )

                LeovnDestination.SETTINGS -> SettingsScreen(
                    runtimeState = runtimeState,
                    viewModel = viewModel
                )
            }
        }
    }
}
