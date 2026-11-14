package com.example.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.dashboard.IslandViewModel
import com.example.ui.theme.AmberTimer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.SapphireAccent
import com.example.ui.theme.TitaniumMuted
import com.example.utils.SystemPermissionHelper
import com.example.utils.SystemPermissionsSnapshot

private data class PendingPermissionExplanation(
    val title: String,
    val rationale: String,
    val onConfirm: () -> Unit
)

@Composable
fun PermissionsAndTroubleshootingScreen(
    permissions: SystemPermissionsSnapshot,
    viewModel: IslandViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingDialog by remember { mutableStateOf<PendingPermissionExplanation?>(null) }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsSnapshot()
    }

    pendingDialog?.let { dialogData ->
        AlertDialog(
            onDismissRequest = { pendingDialog = null },
            containerColor = LiquidGlassSurface,
            titleContentColor = Color.White,
            textContentColor = TitaniumMuted,
            title = {
                Text(text = dialogData.title, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = dialogData.rationale)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = dialogData.onConfirm
                        pendingDialog = null
                        action.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireAccent)
                ) {
                    Text("Continue to Android Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDialog = null }) {
                    Text("Cancel", color = TitaniumMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                border = BorderStroke(1.dp, GlassBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SYSTEM PERMISSIONS DASHBOARD",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SapphireAccent
                        )
                        Text(
                            text = "${permissions.totalGrantedCount} / 6 System Integrations Ready",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) Compatibility Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TitaniumMuted
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.refreshPermissionsSnapshot() },
                        modifier = Modifier.testTag("refresh_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SapphireAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // 1. Draw Over Other Apps
        item {
            val title = stringResource(R.string.permission_overlay_title)
            val desc = stringResource(R.string.permission_overlay_desc)
            PermissionStatusItemCard(
                icon = Icons.Default.Layers,
                title = title,
                description = desc,
                isGranted = permissions.hasOverlayPermission,
                badgeText = if (permissions.hasOverlayPermission) "GRANTED" else "REQUIRED",
                testTag = "perm_card_overlay",
                onActionClick = {
                    pendingDialog = PendingPermissionExplanation(
                        title = title,
                        rationale = "$desc\n\nLEOVNISLAND never reads your screen content or captures keystrokes.",
                        onConfirm = {
                            runCatching {
                                context.startActivity(SystemPermissionHelper.createOverlayPermissionIntent(context))
                            }
                        }
                    )
                }
            )
        }

        // 2. Notification & MediaSession Listener
        item {
            val title = stringResource(R.string.permission_notification_listener_title)
            val desc = stringResource(R.string.permission_notification_listener_desc)
            PermissionStatusItemCard(
                icon = Icons.Default.Notifications,
                title = title,
                description = desc,
                isGranted = permissions.hasNotificationListenerAccess,
                badgeText = if (permissions.hasNotificationListenerAccess) "CONNECTED" else "REQUIRED",
                testTag = "perm_card_notification_listener",
                onActionClick = {
                    pendingDialog = PendingPermissionExplanation(
                        title = title,
                        rationale = "$desc\n\nAll notification data stays strictly in volatile memory and respects redacted private notifications.",
                        onConfirm = {
                            runCatching {
                                context.startActivity(SystemPermissionHelper.createNotificationListenerSettingsIntent())
                            }
                        }
                    )
                }
            )
        }

        // 3. Foreground Service Post Notifications (Android 13+)
        item {
            val title = stringResource(R.string.permission_post_notifications_title)
            val desc = stringResource(R.string.permission_post_notifications_desc)
            PermissionStatusItemCard(
                icon = Icons.Default.Security,
                title = title,
                description = desc,
                isGranted = permissions.hasPostNotificationsPermission,
                badgeText = if (permissions.hasPostNotificationsPermission) "GRANTED" else "ANDROID 13+",
                testTag = "perm_card_post_notifications",
                onActionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runtimePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        // 4. Phone State Indicator
        item {
            val title = stringResource(R.string.permission_phone_state_title)
            val desc = stringResource(R.string.permission_phone_state_desc)
            PermissionStatusItemCard(
                icon = Icons.Default.Phone,
                title = title,
                description = desc,
                isGranted = permissions.hasPhoneStatePermission,
                badgeText = if (permissions.hasPhoneStatePermission) "GRANTED" else "OPTIONAL",
                testTag = "perm_card_phone_state",
                onActionClick = {
                    runtimePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                }
            )
        }

        // 5. Bluetooth Connect (Android 12+)
        item {
            val title = stringResource(R.string.permission_bluetooth_title)
            val desc = stringResource(R.string.permission_bluetooth_desc)
            PermissionStatusItemCard(
                icon = Icons.Default.Bluetooth,
                title = title,
                description = desc,
                isGranted = permissions.hasBluetoothConnectPermission,
                badgeText = if (permissions.hasBluetoothConnectPermission) "GRANTED" else "OPTIONAL",
                testTag = "perm_card_bluetooth",
                onActionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runtimePermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
            )
        }

        // 6. Battery Optimization Exemption
        item {
            val title = stringResource(R.string.permission_battery_opt_title)
            val desc = stringResource(R.string.permission_battery_opt_desc)
            PermissionStatusItemCard(
                icon = Icons.Default.BatterySaver,
                title = title,
                description = desc,
                isGranted = permissions.isIgnoringBatteryOptimizations,
                badgeText = if (permissions.isIgnoringBatteryOptimizations) "UNRESTRICTED" else "RECOMMENDED",
                testTag = "perm_card_battery_opt",
                onActionClick = {
                    pendingDialog = PendingPermissionExplanation(
                        title = title,
                        rationale = desc,
                        onConfirm = {
                            runCatching {
                                context.startActivity(SystemPermissionHelper.createBatteryOptimizationIntent(context))
                            }
                        }
                    )
                }
            )
        }

        // Troubleshooting & OEM Guide
        item {
            TroubleshootingGuideCard()
        }
    }
}

@Composable
private fun PermissionStatusItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    badgeText: String,
    testTag: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(
            1.dp,
            if (isGranted) EmeraldLive.copy(alpha = 0.35f) else GlassBorderSubtle
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) EmeraldLive else SapphireAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGranted) EmeraldLive.copy(alpha = 0.16f) else AmberTimer.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGranted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Granted",
                                tint = EmeraldLive,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = badgeText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) EmeraldLive else AmberTimer
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TitaniumMuted
            )

            if (!isGranted) {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireAccent)
                ) {
                    Text(
                        text = "Configure Permission",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TroubleshootingGuideCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("troubleshooting_guide_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Troubleshooting",
                    tint = SapphireAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Troubleshooting & OEM Compatibility Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            TroubleshootingFaqItem(
                title = "1. Android 13 / 14 / 15 'Restricted Setting' on Notification Access",
                solution = "If Android blocks Notification Listener access when installing via APK, open Android Settings → Apps → LEOVNISLAND → tap the 3-dot menu in the top-right corner → select 'Allow restricted settings', then enable Notification Access."
            )

            TroubleshootingFaqItem(
                title = "2. Xiaomi (HyperOS / MIUI), OPPO (ColorOS), Vivo & Samsung Background Limits",
                solution = "Enable 'Autostart' and set Battery Saver to 'No restrictions' (Unrestricted). Lock LEOVNISLAND in Recent Apps so the OEM task killer does not stop the WindowManager overlay service."
            )

            TroubleshootingFaqItem(
                title = "3. Spotify / YouTube Music Session Not Appearing",
                solution = "Ensure Notification Listener Access is connected above. Play a song in Spotify or YouTube Music; LEOVNISLAND automatically binds to MediaSessionManager without requiring AccessibilityService."
            )

            TroubleshootingFaqItem(
                title = "4. Android System Limitations & Privacy Guarantee",
                solution = "LEOVNISLAND uses official WindowManager TYPE_APPLICATION_OVERLAY and NotificationListenerService APIs. It never uses AccessibilityService to scrape screen text, and it never overrides your default Phone Dialer."
            )

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(SystemPermissionHelper.createAppDetailsSettingsIntent(context))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Android App Info Settings", color = SapphireAccent)
            }
        }
    }
}

@Composable
private fun TroubleshootingFaqItem(
    title: String,
    solution: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CarbonSurface,
        border = BorderStroke(1.dp, GlassBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = solution,
                color = TitaniumMuted,
                fontSize = 12.sp
            )
        }
    }
}
