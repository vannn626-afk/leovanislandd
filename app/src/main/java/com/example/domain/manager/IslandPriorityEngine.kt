package com.example.domain.manager

import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.CallStatus
import com.example.domain.model.ConnectivityActivityState
import com.example.domain.model.IslandActivityPayload
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.MediaSessionState
import com.example.domain.model.TimerActivityState

/**
 * Pure domain priority queue solver for LEOVNISLAND.
 * Enforces priority ranking:
 * 1. Incoming Call
 * 2. Ongoing Call
 * 3. Active Music
 * 4. Important Notification
 * 5. Timer
 * 6. Battery
 * 7. Connectivity
 * 8. Idle
 */
object IslandPriorityEngine {

    private const val TRANSIENT_ALERT_WINDOW_MS = 8_000L

    fun buildPrioritizedQueue(
        callState: CallActivityState,
        mediaState: MediaSessionState,
        notifications: List<IslandNotificationItem>,
        timerState: TimerActivityState,
        batteryState: BatteryActivityState,
        connectivityState: ConnectivityActivityState,
        nowMs: Long = System.currentTimeMillis()
    ): List<IslandActivityPayload> {
        val candidates = mutableListOf<IslandActivityPayload>()

        // 1 & 2: Incoming or Ongoing Call
        if (callState.status == CallStatus.INCOMING || callState.status == CallStatus.ONGOING) {
            candidates.add(IslandActivityPayload.CallPayload(callState))
        }

        // 3: Active Music
        if (mediaState.isActive) {
            candidates.add(IslandActivityPayload.MusicPayload(mediaState))
        }

        // 4: Important Notifications (sorted by priorityLevel desc, then newest first)
        val sortedNotifications = notifications.sortedWith(
            compareByDescending<IslandNotificationItem> { it.priorityLevel }
                .thenByDescending { it.timestamp }
        )
        for (notif in sortedNotifications) {
            candidates.add(IslandActivityPayload.NotificationPayload(notif))
        }

        // 5: Timer
        if (timerState.isActive) {
            candidates.add(IslandActivityPayload.TimerPayload(timerState))
        }

        // 6: Battery (shown when charging, low <= 20%, full, or recently triggered within alert window)
        val isBatteryRecentAlert = (nowMs - batteryState.lastAlertTimestamp) in 0..TRANSIENT_ALERT_WINDOW_MS
        if (batteryState.isCharging || batteryState.isLow || batteryState.isFull || isBatteryRecentAlert) {
            candidates.add(IslandActivityPayload.BatteryPayload(batteryState))
        }

        // 7: Connectivity / System Transient Activity
        val isConnectivityRecent = (nowMs - connectivityState.transientBannerTimestamp) in 0..TRANSIENT_ALERT_WINDOW_MS
        val headline = connectivityState.transientBannerMessage
            ?: when {
                connectivityState.isFlashlightOn -> "Flashlight On"
                connectivityState.isAirplaneMode -> "Airplane Mode"
                connectivityState.activeDownloadTitle != null -> "Downloading: ${connectivityState.activeDownloadTitle}"
                connectivityState.isBluetoothConnected && isConnectivityRecent -> "Connected: ${connectivityState.bluetoothDeviceName}"
                else -> null
            }
        if (headline != null) {
            candidates.add(IslandActivityPayload.ConnectivityPayload(connectivityState, headline))
        }

        // Sort strictly by priority rank ascending (1 is highest), preserving internal order for ties
        val sorted = candidates.sortedBy { it.priority.rank }

        return if (sorted.isEmpty()) {
            listOf(IslandActivityPayload.IdlePayload)
        } else {
            sorted
        }
    }
}
