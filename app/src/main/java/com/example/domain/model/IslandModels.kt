package com.example.domain.model

import android.app.PendingIntent
import android.graphics.Bitmap

/**
 * Priority Hierarchy defined in Specification Section 7:
 * 1. Incoming Call
 * 2. Ongoing Call
 * 3. Active Music
 * 4. Important Notification
 * 5. Timer
 * 6. Battery
 * 7. Connectivity
 * 8. Idle
 */
enum class IslandActivityPriority(val rank: Int, val label: String) {
    INCOMING_CALL(1, "Incoming Call"),
    ONGOING_CALL(2, "Ongoing Call"),
    ACTIVE_MUSIC(3, "Active Music"),
    IMPORTANT_NOTIFICATION(4, "Smart Notification"),
    TIMER(5, "Live Timer"),
    BATTERY(6, "Battery Status"),
    CONNECTIVITY(7, "Connectivity & System"),
    IDLE(8, "Idle")
}

enum class IslandDisplayMode {
    MINIMAL,
    COMPACT,
    EXPANDED,
    CONTROL_PANEL
}

enum class IslandThemeStyle(val title: String, val subtitle: String) {
    LIQUID_GLASS("Liquid Glass", "Frosted obsidian glass with subtle specular border"),
    PREMIUM_DARK("Premium Dark", "Deep carbon surface with titanium accents"),
    CLASSIC_IOS("Classic iOS", "Pure pitch black capsule with dynamic spring glow"),
    MINIMAL_BLACK("Minimal Black", "Ultra-low contrast stealth OLED black"),
    CUSTOM("Custom Studio", "User-configured colors, border, and glass intensity")
}

enum class AnimationPreset(val label: String, val stiffness: Float, val dampingRatio: Float) {
    FLUID_SPRING("Fluid iOS Spring", 380f, 0.72f),
    SNAPPY("Snappy Elastic", 600f, 0.65f),
    SMOOTH_SILK("Smooth Silk", 260f, 0.88f),
    BATTERY_SAVER("Battery Friendly", 900f, 1.0f)
}

enum class DoubleTapAction(val label: String) {
    PLAY_PAUSE_MEDIA("Play / Pause Active Media"),
    DISMISS_CURRENT("Dismiss Current Notification / Collapse"),
    TOGGLE_TIMER("Start / Pause Quick Timer"),
    OPEN_APP("Open LEOVNISLAND Studio"),
    TOGGLE_FLASHLIGHT("Toggle Device Flashlight")
}

enum class CutoutAdaptationMode(val label: String) {
    AUTO_DETECT("Auto-Detect Display Cutout"),
    CENTER_PUNCH("Center Punch-Hole / Notch"),
    LEFT_PUNCH("Left Corner Punch-Hole"),
    WATERDROP("Waterdrop Notch"),
    NO_NOTCH("Full Screen / No Notch")
}

data class MediaSessionState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val packageName: String = "",
    val appName: String = "Media Player",
    val title: String = "Not Playing",
    val artist: String = "Unknown Artist",
    val album: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val artwork: Bitmap? = null,
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class SupportedAppCategory(
    val packageName: String,
    val displayName: String,
    val accentColorHex: Long
) {
    WHATSAPP("com.whatsapp", "WhatsApp", 0xFF25D366),
    TELEGRAM("org.telegram.messenger", "Telegram", 0xFF2AABEE),
    INSTAGRAM("com.instagram.android", "Instagram", 0xFFE1306C),
    MESSENGER("com.facebook.orca", "Messenger", 0xFF0084FF),
    GMAIL("com.google.android.gm", "Gmail", 0xFFEA4335),
    DISCORD("com.discord", "Discord", 0xFF5865F2),
    SMS("com.google.android.apps.messaging", "Messages", 0xFF4285F4),
    OTHER("other", "System & Other Apps", 0xFF94A3B8);

    companion object {
        fun fromPackage(pkg: String): SupportedAppCategory {
            return entries.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) }
                ?: when {
                    pkg.contains("whatsapp", ignoreCase = true) -> WHATSAPP
                    pkg.contains("telegram", ignoreCase = true) -> TELEGRAM
                    pkg.contains("instagram", ignoreCase = true) -> INSTAGRAM
                    pkg.contains("orca", ignoreCase = true) || pkg.contains("messenger", ignoreCase = true) -> MESSENGER
                    pkg.contains("gm", ignoreCase = true) || pkg.contains("mail", ignoreCase = true) -> GMAIL
                    pkg.contains("discord", ignoreCase = true) -> DISCORD
                    pkg.contains("mms", ignoreCase = true) || pkg.contains("messaging", ignoreCase = true) || pkg.contains("sms", ignoreCase = true) -> SMS
                    else -> OTHER
                }
        }
    }
}

data class IslandNotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val senderName: String,
    val title: String,
    val messagePreview: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: SupportedAppCategory = SupportedAppCategory.fromPackage(packageName),
    val isPrivateRedacted: Boolean = false,
    val groupKey: String? = null,
    val groupCount: Int = 1,
    val priorityLevel: Int = 0,
    val contentIntent: PendingIntent? = null,
    val avatarBitmap: Bitmap? = null
)

enum class CallStatus {
    NONE,
    INCOMING,
    ONGOING,
    ENDED
}

data class CallActivityState(
    val status: CallStatus = CallStatus.NONE,
    val callerName: String = "Phone Call",
    val callerNumber: String = "",
    val callStartTimestampMs: Long = 0L,
    val durationSeconds: Long = 0L
)

data class BatteryActivityState(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val isLow: Boolean = false,
    val powerSource: String = "Battery",
    val temperatureCelsius: Float = 28.0f,
    val lastAlertTimestamp: Long = 0L
)

enum class TimerMode {
    COUNTDOWN,
    STOPWATCH
}

data class TimerActivityState(
    val isActive: Boolean = false,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val mode: TimerMode = TimerMode.COUNTDOWN,
    val totalDurationMs: Long = 300_000L, // Default 5 mins
    val remainingOrElapsedMs: Long = 300_000L,
    val label: String = "Focus Timer"
) {
    val progressFraction: Float
        get() = when (mode) {
            TimerMode.COUNTDOWN -> if (totalDurationMs > 0L) (remainingOrElapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
            TimerMode.STOPWATCH -> ((remainingOrElapsedMs % 60_000L).toFloat() / 60_000f).coerceIn(0f, 1f)
        }
}

data class ConnectivityActivityState(
    val isWifiConnected: Boolean = true,
    val wifiSsidOrStatus: String = "Wi-Fi Connected",
    val isCellularConnected: Boolean = false,
    val networkLabel: String = "Online",
    val isBluetoothConnected: Boolean = false,
    val bluetoothDeviceName: String = "Bluetooth",
    val isAirplaneMode: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val isFlashlightAvailable: Boolean = true,
    val nextAlarmFormatted: String? = null,
    val currentVolumePercent: Int = 70,
    val isMutedOrDnd: Boolean = false,
    val activeDownloadTitle: String? = null,
    val activeDownloadProgress: Int? = null,
    val transientBannerMessage: String? = null,
    val transientBannerTimestamp: Long = 0L
)

sealed class IslandActivityPayload(
    val id: String,
    val priority: IslandActivityPriority,
    val createdAt: Long = System.currentTimeMillis()
) {
    data class CallPayload(
        val callState: CallActivityState
    ) : IslandActivityPayload(
        id = "call_${callState.status.name}",
        priority = if (callState.status == CallStatus.INCOMING) {
            IslandActivityPriority.INCOMING_CALL
        } else {
            IslandActivityPriority.ONGOING_CALL
        }
    )

    data class MusicPayload(
        val mediaState: MediaSessionState
    ) : IslandActivityPayload(
        id = "music_active",
        priority = IslandActivityPriority.ACTIVE_MUSIC
    )

    data class NotificationPayload(
        val notification: IslandNotificationItem
    ) : IslandActivityPayload(
        id = "notif_${notification.id}",
        priority = IslandActivityPriority.IMPORTANT_NOTIFICATION
    )

    data class TimerPayload(
        val timerState: TimerActivityState
    ) : IslandActivityPayload(
        id = "timer_active",
        priority = IslandActivityPriority.TIMER
    )

    data class BatteryPayload(
        val batteryState: BatteryActivityState
    ) : IslandActivityPayload(
        id = "battery_alert",
        priority = IslandActivityPriority.BATTERY
    )

    data class ConnectivityPayload(
        val connectivityState: ConnectivityActivityState,
        val headline: String
    ) : IslandActivityPayload(
        id = "connectivity_event",
        priority = IslandActivityPriority.CONNECTIVITY
    )

    data object IdlePayload : IslandActivityPayload(
        id = "idle",
        priority = IslandActivityPriority.IDLE
    )
}
