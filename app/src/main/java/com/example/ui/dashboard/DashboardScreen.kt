package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CallStatus
import com.example.domain.model.IslandDisplayMode
import com.example.domain.model.SupportedAppCategory
import com.example.island.components.DynamicIslandCapsule
import com.example.island.components.formatDurationMs
import com.example.island.manager.IslandStateManager
import com.example.island.state.IslandRuntimeState
import com.example.ui.theme.AmberTimer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.SapphireAccent
import com.example.ui.theme.TitaniumMuted
import com.example.utils.SystemPermissionsSnapshot

@Composable
fun DashboardScreen(
    runtimeState: IslandRuntimeState,
    permissions: SystemPermissionsSnapshot,
    viewModel: IslandViewModel,
    onNavigateToPermissions: () -> Unit,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeader(
                isServiceRunning = runtimeState.isOverlayServiceRunning,
                activePriorityLabel = runtimeState.primaryActivity.priority.label,
                onNavigateToStudio = onNavigateToStudio
            )
        }

        item {
            LiveIslandInteractiveStage(
                runtimeState = runtimeState
            )
        }

        if (!permissions.hasOverlayPermission || !permissions.hasNotificationListenerAccess) {
            item {
                PermissionWarningBanner(
                    permissions = permissions,
                    onClickSetup = onNavigateToPermissions
                )
            }
        }

        item {
            MasterOverlayControlCard(
                runtimeState = runtimeState,
                viewModel = viewModel
            )
        }

        item {
            PriorityQueueStatusCard(
                runtimeState = runtimeState
            )
        }

        item {
            MusicPlayerControlStudioCard(
                runtimeState = runtimeState,
                viewModel = viewModel
            )
        }

        item {
            SmartNotificationTestBenchCard(
                runtimeState = runtimeState,
                viewModel = viewModel
            )
        }

        item {
            LiveActivitiesHubCard(
                runtimeState = runtimeState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    isServiceRunning: Boolean,
    activePriorityLabel: String,
    onNavigateToStudio: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LEOVNISLAND",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SapphireAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, SapphireAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "BY LEOVAN",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SapphireAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "Priority: $activePriorityLabel • Liquid Glass Engine",
                style = MaterialTheme.typography.bodyMedium,
                color = TitaniumMuted
            )
        }

        Surface(
            modifier = Modifier.clickable { onNavigateToStudio() },
            shape = RoundedCornerShape(20.dp),
            color = if (isServiceRunning) EmeraldLive.copy(alpha = 0.16f) else LiquidGlassSurface,
            border = BorderStroke(
                1.dp,
                if (isServiceRunning) EmeraldLive.copy(alpha = 0.5f) else GlassBorderSubtle
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) EmeraldLive else TitaniumMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isServiceRunning) "OVERLAY ACTIVE" else "STANDBY",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceRunning) EmeraldLive else TitaniumMuted
                )
            }
        }
    }
}

@Composable
private fun LiveIslandInteractiveStage(
    runtimeState: IslandRuntimeState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_island_stage_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SapphireAccent.copy(alpha = 0.12f),
                            CarbonSurface
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERACTIVE ISLAND STAGE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SapphireAccent
                )
                Text(
                    text = "Tap: Expand • Long-Press: Hub • Swipe: Queue",
                    fontSize = 11.sp,
                    color = TitaniumMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                DynamicIslandCapsule(runtimeState = runtimeState)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Switcher Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IslandDisplayMode.entries.forEach { mode ->
                    val selected = runtimeState.displayMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = {
                            when (mode) {
                                IslandDisplayMode.MINIMAL -> IslandStateManager.setMinimalMode()
                                IslandDisplayMode.COMPACT -> IslandStateManager.collapseIsland()
                                IslandDisplayMode.EXPANDED -> IslandStateManager.expandIsland()
                                IslandDisplayMode.CONTROL_PANEL -> IslandStateManager.openControlPanel()
                            }
                        },
                        label = {
                            Text(
                                text = mode.name.replace("_", " "),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SapphireAccent.copy(alpha = 0.22f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(
    permissions: SystemPermissionsSnapshot,
    onClickSetup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_warning_banner")
            .clickable { onClickSetup() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F161B)),
        border = BorderStroke(1.dp, AmberTimer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Permissions Needed",
                tint = AmberTimer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Integration Permissions Required",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                val missing = buildList {
                    if (!permissions.hasOverlayPermission) add("Draw Over Other Apps")
                    if (!permissions.hasNotificationListenerAccess) add("Notification & Media Access")
                }.joinToString(" • ")
                Text(
                    text = "Missing: $missing. Tap to review & grant.",
                    color = Color(0xFFFDE68A),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MasterOverlayControlCard(
    runtimeState: IslandRuntimeState,
    viewModel: IslandViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Overlay Service",
                        tint = SapphireAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Floating WindowManager Overlay",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "TYPE_APPLICATION_OVERLAY • Keeps Island above all apps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TitaniumMuted
                        )
                    }
                }

                Switch(
                    checked = runtimeState.isOverlayServiceRunning,
                    onCheckedChange = { enabled ->
                        viewModel.toggleOverlayService(enabled)
                    },
                    modifier = Modifier.testTag("master_overlay_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SapphireAccent
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Visibility",
                        tint = TitaniumMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Island Capsule Visibility",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = runtimeState.configuration.isIslandVisible,
                    onCheckedChange = { visible ->
                        viewModel.updateConfig { it.copy(isIslandVisible = visible) }
                    },
                    modifier = Modifier.testTag("island_visibility_switch")
                )
            }
        }
    }
}

@Composable
private fun PriorityQueueStatusCard(
    runtimeState: IslandRuntimeState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Priority Queue",
                        tint = SapphireAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Priority Queue Engine (${runtimeState.activeQueue.size} Active)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                if (runtimeState.activeQueue.size > 1) {
                    Text(
                        text = "Cycle Next →",
                        color = SapphireAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag("cycle_queue_button")
                            .clickable { IslandStateManager.cycleNextActivity() }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                runtimeState.activeQueue.forEachIndexed { idx, payload ->
                    val isSelected = idx == runtimeState.selectedQueueIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SapphireAccent.copy(alpha = 0.22f) else CarbonSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SapphireAccent else GlassBorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "P${payload.priority.rank}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SapphireAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = payload.priority.label,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicPlayerControlStudioCard(
    runtimeState: IslandRuntimeState,
    viewModel: IslandViewModel
) {
    val media = runtimeState.mediaState
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "MediaSession",
                        tint = SapphireAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MediaSession Mini Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (media.isActive) "${media.appName} • ${media.title} — ${media.artist}"
                            else "Syncs automatically with Spotify, YouTube Music & active MediaSessions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TitaniumMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.activateOrCycleMediaSession() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_launch_music_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireAccent)
                ) {
                    Text(
                        text = if (media.isActive) "Switch Track" else "Test Active Music Session",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (media.isActive) {
                    IconButton(
                        onClick = { IslandStateManager.triggerPreviousTrack() },
                        modifier = Modifier.testTag("dashboard_music_prev")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                    }
                    IconButton(
                        onClick = { IslandStateManager.triggerPlayPause() },
                        modifier = Modifier.testTag("dashboard_music_play_pause")
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = SapphireAccent
                        )
                    }
                    IconButton(
                        onClick = { IslandStateManager.triggerNextTrack() },
                        modifier = Modifier.testTag("dashboard_music_next")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                    IconButton(
                        onClick = { viewModel.stopMediaSession() },
                        modifier = Modifier.testTag("dashboard_music_stop")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop Session", tint = CrimsonAlert)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartNotificationTestBenchCard(
    runtimeState: IslandRuntimeState,
    viewModel: IslandViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Smart Notifications",
                        tint = SapphireAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Smart Notification System (${runtimeState.notifications.size} Queued)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Tap any app chip to dispatch a live notification into the Island queue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TitaniumMuted
                        )
                    }
                }
                if (runtimeState.notifications.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        color = CrimsonAlert,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag("clear_notifications_button")
                            .clickable { IslandStateManager.clearAllNotifications() }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupportedAppCategory.entries.forEach { category ->
                    Surface(
                        modifier = Modifier
                            .testTag("dispatch_notif_${category.name.lowercase()}")
                            .clickable { viewModel.dispatchSmartNotification(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(category.accentColorHex).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(category.accentColorHex).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+ ${category.displayName}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveActivitiesHubCard(
    runtimeState: IslandRuntimeState,
    viewModel: IslandViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Live Activities Control Center",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            // 1. Live Timer & Stopwatch Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarbonSurface,
                border = BorderStroke(1.dp, GlassBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = AmberTimer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (runtimeState.timerState.isActive) {
                                    "${runtimeState.timerState.label}: ${formatDurationMs(runtimeState.timerState.remainingOrElapsedMs)}"
                                } else {
                                    "Timer & Stopwatch Module"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { IslandStateManager.startCountdownTimer(60_000L, "1m Sprint") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_1m_timer_button")
                        ) {
                            Text("1m Timer", fontSize = 11.sp, color = AmberTimer)
                        }
                        OutlinedButton(
                            onClick = { IslandStateManager.startStopwatch("Stopwatch") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_stopwatch_button")
                        ) {
                            Text("Stopwatch", fontSize = 11.sp, color = Color.White)
                        }
                        if (runtimeState.timerState.isActive) {
                            OutlinedButton(
                                onClick = { IslandStateManager.stopAndResetTimer() },
                                modifier = Modifier.testTag("reset_timer_button")
                            ) {
                                Text("Reset", fontSize = 11.sp, color = CrimsonAlert)
                            }
                        }
                    }
                }
            }

            // 2. Phone Calls & Battery & Connectivity Triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_call_button")
                        .clickable {
                            when (runtimeState.callState.status) {
                                CallStatus.NONE, CallStatus.ENDED -> viewModel.triggerIncomingCall()
                                CallStatus.INCOMING -> viewModel.triggerOngoingCall()
                                CallStatus.ONGOING -> viewModel.endCallActivity()
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (runtimeState.callState.status == CallStatus.NONE) Icons.Default.Call else Icons.Default.CallEnd,
                            contentDescription = "Call State",
                            tint = EmeraldLive,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (runtimeState.callState.status) {
                                    CallStatus.NONE, CallStatus.ENDED -> "Simulate Call"
                                    CallStatus.INCOMING -> "Answer Call"
                                    CallStatus.ONGOING -> "End Call"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Priority 1 & 2",
                                color = TitaniumMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_battery_button")
                        .clickable {
                            val nextCharging = !runtimeState.batteryState.isCharging
                            viewModel.triggerBatteryAlert(
                                charging = nextCharging,
                                percentage = if (nextCharging) 84 else 18
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery",
                            tint = EmeraldLive,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Battery (${runtimeState.batteryState.percentage}%)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (runtimeState.batteryState.isCharging) "Charging" else "Tap Alert",
                                color = TitaniumMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 3. Connectivity & Flashlight Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_flashlight_button")
                        .clickable {
                            IslandStateManager.flashlightToggleAction?.invoke() ?: run {
                                val next = !runtimeState.connectivityState.isFlashlightOn
                                IslandStateManager.updateConnectivityState(
                                    transform = { it.copy(isFlashlightOn = next) },
                                    transientHeadline = if (next) "Flashlight On" else "Flashlight Off"
                                )
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = if (runtimeState.connectivityState.isFlashlightOn) AmberTimer else TitaniumMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (runtimeState.connectivityState.isFlashlightOn) "Torch Active" else "Toggle Torch",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "CameraManager API",
                                color = TitaniumMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_bluetooth_button")
                        .clickable {
                            val nextBt = !runtimeState.connectivityState.isBluetoothConnected
                            IslandStateManager.updateConnectivityState(
                                transform = {
                                    it.copy(
                                        isBluetoothConnected = nextBt,
                                        bluetoothDeviceName = "AirPods Pro / Buds"
                                    )
                                },
                                transientHeadline = if (nextBt) "Connected: AirPods Pro" else "Bluetooth Disconnected"
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = "Bluetooth",
                            tint = SapphireAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Connectivity",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = runtimeState.connectivityState.networkLabel,
                                color = TitaniumMuted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
