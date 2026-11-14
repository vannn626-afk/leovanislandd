package com.example.utils

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.IslandNotificationListener

data class SystemPermissionsSnapshot(
    val hasOverlayPermission: Boolean = false,
    val hasNotificationListenerAccess: Boolean = false,
    val hasPostNotificationsPermission: Boolean = true,
    val hasPhoneStatePermission: Boolean = false,
    val hasBluetoothConnectPermission: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isDoNotDisturbActive: Boolean = false
) {
    val coreReadyCount: Int
        get() = listOf(
            hasOverlayPermission,
            hasNotificationListenerAccess,
            hasPostNotificationsPermission
        ).count { it }

    val totalGrantedCount: Int
        get() = listOf(
            hasOverlayPermission,
            hasNotificationListenerAccess,
            hasPostNotificationsPermission,
            hasPhoneStatePermission,
            hasBluetoothConnectPermission,
            isIgnoringBatteryOptimizations
        ).count { it }
}

object SystemPermissionHelper {

    // Test override hook for deterministic Robolectric testing of overlay permission
    var testOverlayPermissionOverride: Boolean? = null
    var testNotificationListenerOverride: Boolean? = null

    fun canDrawOverlays(context: Context): Boolean {
        testOverlayPermissionOverride?.let { return it }
        return Settings.canDrawOverlays(context)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        testNotificationListenerOverride?.let { return it }
        val expectedComponent = ComponentName(context, IslandNotificationListener::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any {
            ComponentName.unflattenFromString(it) == expectedComponent
        }
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isDoNotDisturbActive(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        val filter = nm.currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    fun captureSnapshot(context: Context): SystemPermissionsSnapshot {
        return SystemPermissionsSnapshot(
            hasOverlayPermission = canDrawOverlays(context),
            hasNotificationListenerAccess = isNotificationListenerEnabled(context),
            hasPostNotificationsPermission = hasPostNotificationsPermission(context),
            hasPhoneStatePermission = hasPhoneStatePermission(context),
            hasBluetoothConnectPermission = hasBluetoothConnectPermission(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            isDoNotDisturbActive = isDoNotDisturbActive(context)
        )
    }

    fun createOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createBatteryOptimizationIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
