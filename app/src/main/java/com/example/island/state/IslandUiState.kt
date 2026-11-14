package com.example.island.state

import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.ConnectivityActivityState
import com.example.domain.model.IslandActivityPayload
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.IslandDisplayMode
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.MediaSessionState
import com.example.domain.model.TimerActivityState

data class IslandRuntimeState(
    val isOverlayServiceRunning: Boolean = false,
    val isNotificationListenerConnected: Boolean = false,
    val displayMode: IslandDisplayMode = IslandDisplayMode.COMPACT,
    val primaryActivity: IslandActivityPayload = IslandActivityPayload.IdlePayload,
    val secondaryActivity: IslandActivityPayload? = null,
    val activeQueue: List<IslandActivityPayload> = emptyList(),
    val selectedQueueIndex: Int = 0,
    val mediaState: MediaSessionState = MediaSessionState(),
    val notifications: List<IslandNotificationItem> = emptyList(),
    val callState: CallActivityState = CallActivityState(),
    val batteryState: BatteryActivityState = BatteryActivityState(),
    val timerState: TimerActivityState = TimerActivityState(),
    val connectivityState: ConnectivityActivityState = ConnectivityActivityState(),
    val configuration: IslandConfiguration = IslandConfiguration(),
    val cutoutSafeTopInsetDp: Float = 0f,
    val isInLandscape: Boolean = false,
    val lastStatusChangeMs: Long = 0L
)
