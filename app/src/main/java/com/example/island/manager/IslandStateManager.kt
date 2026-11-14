package com.example.island.manager

import com.example.domain.manager.IslandPriorityEngine
import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.CallStatus
import com.example.domain.model.ConnectivityActivityState
import com.example.domain.model.IslandActivityPayload
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.IslandDisplayMode
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.MediaSessionState
import com.example.domain.model.TimerActivityState
import com.example.domain.model.TimerMode
import com.example.island.state.IslandRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Central state coordinator and Priority Queue manager for LEOVNISLAND.
 * Handles multi-activity arbitration, automatic expansion/collapse timeouts,
 * anti-flicker stabilization, and interactive transport callbacks.
 */
object IslandStateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(IslandRuntimeState())
    val state: StateFlow<IslandRuntimeState> = _state.asStateFlow()

    private var autoCollapseJob: Job? = null
    private var timerTickerJob: Job? = null
    private var callDurationTickerJob: Job? = null
    private val notificationTimeoutJobs = mutableMapOf<String, Job>()

    // Transport and system action hooks registered by active services
    var mediaPlayPauseAction: (() -> Unit)? = null
    var mediaNextAction: (() -> Unit)? = null
    var mediaPreviousAction: (() -> Unit)? = null
    var mediaSeekAction: ((Long) -> Unit)? = null
    var flashlightToggleAction: (() -> Unit)? = null
    var openMainAppAction: (() -> Unit)? = null
    var playCustomNotificationToneAction: (() -> Unit)? = null

    private const val MIN_FLICKER_GUARD_MS = 120L
    private const val MAX_NOTIFICATION_QUEUE = 12

    fun setOverlayServiceRunning(running: Boolean) {
        _state.update { it.copy(isOverlayServiceRunning = running) }
    }

    fun setNotificationListenerConnected(connected: Boolean) {
        _state.update { it.copy(isNotificationListenerConnected = connected) }
    }

    fun updateConfiguration(config: IslandConfiguration) {
        _state.update { current ->
            val filteredNotifs = current.notifications.filter { notif ->
                config.enabledAppCategories.contains(notif.category.name)
            }
            recomputeQueue(
                current.copy(
                    configuration = config,
                    notifications = filteredNotifs
                )
            )
        }
    }

    fun updateCutoutAndOrientation(safeTopInsetDp: Float, isLandscape: Boolean) {
        _state.update {
            it.copy(
                cutoutSafeTopInsetDp = safeTopInsetDp,
                isInLandscape = isLandscape
            )
        }
    }

    // ---- Gesture & Display Mode Transitions ----

    fun expandIsland(autoCollapseAfterSeconds: Int? = null) {
        autoCollapseJob?.cancel()
        _state.update { current ->
            val targetMode = if (current.primaryActivity is IslandActivityPayload.IdlePayload &&
                current.activeQueue.size <= 1
            ) {
                IslandDisplayMode.CONTROL_PANEL
            } else {
                IslandDisplayMode.EXPANDED
            }
            current.copy(displayMode = targetMode)
        }
        val timeoutSec = autoCollapseAfterSeconds ?: _state.value.configuration.autoCollapseSeconds
        if (timeoutSec > 0) {
            scheduleAutoCollapse(timeoutSec * 1000L)
        }
    }

    fun collapseIsland() {
        autoCollapseJob?.cancel()
        _state.update { current ->
            val fallbackMode = if (current.primaryActivity is IslandActivityPayload.IdlePayload) {
                IslandDisplayMode.MINIMAL
            } else {
                IslandDisplayMode.COMPACT
            }
            current.copy(displayMode = fallbackMode)
        }
    }

    fun setMinimalMode() {
        autoCollapseJob?.cancel()
        _state.update { it.copy(displayMode = IslandDisplayMode.MINIMAL) }
    }

    fun openControlPanel() {
        autoCollapseJob?.cancel()
        _state.update { it.copy(displayMode = IslandDisplayMode.CONTROL_PANEL) }
        scheduleAutoCollapse((_state.value.configuration.autoCollapseSeconds + 4) * 1000L)
    }

    fun cycleNextActivity() {
        _state.update { current ->
            val size = current.activeQueue.size
            if (size <= 1) return@update current
            val nextIdx = (current.selectedQueueIndex + 1) % size
            val nextPrimary = current.activeQueue[nextIdx]
            val nextSecondary = current.activeQueue.getOrNull((nextIdx + 1) % size)
                ?.takeIf { it.id != nextPrimary.id }
            current.copy(
                selectedQueueIndex = nextIdx,
                primaryActivity = nextPrimary,
                secondaryActivity = nextSecondary
            )
        }
    }

    fun cyclePreviousActivity() {
        _state.update { current ->
            val size = current.activeQueue.size
            if (size <= 1) return@update current
            val prevIdx = if (current.selectedQueueIndex - 1 < 0) size - 1 else current.selectedQueueIndex - 1
            val nextPrimary = current.activeQueue[prevIdx]
            val nextSecondary = current.activeQueue.getOrNull((prevIdx + 1) % size)
                ?.takeIf { it.id != nextPrimary.id }
            current.copy(
                selectedQueueIndex = prevIdx,
                primaryActivity = nextPrimary,
                secondaryActivity = nextSecondary
            )
        }
    }

    fun updateDragOffset(deltaXDp: Float, deltaYDp: Float) {
        _state.update { current ->
            if (!current.configuration.isDraggable) return@update current
            val newX = (current.configuration.horizontalOffsetDp + deltaXDp).coerceIn(-160f, 160f)
            val newY = (current.configuration.verticalOffsetDp + deltaYDp).coerceIn(0f, 180f)
            current.copy(
                configuration = current.configuration.copy(
                    horizontalOffsetDp = newX,
                    verticalOffsetDp = newY
                )
            )
        }
    }

    // ---- Media Session Integration ----

    fun updateMediaSession(mediaSessionState: MediaSessionState) {
        _state.update { current ->
            val now = System.currentTimeMillis()
            // Anti-flicker stabilization when metadata updates rapidly
            val updated = current.copy(
                mediaState = mediaSessionState,
                lastStatusChangeMs = now
            )
            recomputeQueue(updated, now)
        }
    }

    fun triggerPlayPause() {
        val hook = mediaPlayPauseAction
        if (hook != null) {
            hook.invoke()
        } else {
            // Local fallback for unit/Robolectric tests or preview sessions
            _state.update { current ->
                val toggledPlaying = !current.mediaState.isPlaying
                val updatedMedia = current.mediaState.copy(
                    isActive = true,
                    isPlaying = toggledPlaying,
                    lastUpdatedMs = System.currentTimeMillis()
                )
                recomputeQueue(current.copy(mediaState = updatedMedia))
            }
        }
    }

    fun triggerNextTrack() {
        val hook = mediaNextAction
        if (hook != null) {
            hook.invoke()
        } else {
            _state.update { current ->
                val updatedMedia = current.mediaState.copy(
                    positionMs = 0L,
                    lastUpdatedMs = System.currentTimeMillis()
                )
                recomputeQueue(current.copy(mediaState = updatedMedia))
            }
        }
    }

    fun triggerPreviousTrack() {
        val hook = mediaPreviousAction
        if (hook != null) {
            hook.invoke()
        } else {
            _state.update { current ->
                val updatedMedia = current.mediaState.copy(
                    positionMs = 0L,
                    lastUpdatedMs = System.currentTimeMillis()
                )
                recomputeQueue(current.copy(mediaState = updatedMedia))
            }
        }
    }

    fun triggerSeekTo(positionMs: Long) {
        mediaSeekAction?.invoke(positionMs)
        _state.update { current ->
            val clamped = positionMs.coerceIn(0L, current.mediaState.durationMs.coerceAtLeast(1L))
            current.copy(mediaState = current.mediaState.copy(positionMs = clamped))
        }
    }

    // ---- Smart Notification Queue ----

    fun enqueueNotification(
        item: IslandNotificationItem,
        autoTimeoutMs: Long? = null
    ) {
        val config = _state.value.configuration
        if (!config.enabledAppCategories.contains(item.category.name)) {
            return
        }
        if (config.respectDoNotDisturb && _state.value.connectivityState.isMutedOrDnd) {
            // Still enqueue silently in compact queue without auto-expanding or playing sound
            insertNotificationIntoQueue(item, expandAutomatically = false, timeoutMs = autoTimeoutMs)
            return
        }

        if (config.customNotificationSound) {
            playCustomNotificationToneAction?.invoke()
        }
        insertNotificationIntoQueue(item, expandAutomatically = true, timeoutMs = autoTimeoutMs)
    }

    private fun insertNotificationIntoQueue(
        item: IslandNotificationItem,
        expandAutomatically: Boolean,
        timeoutMs: Long?
    ) {
        _state.update { current ->
            val mutable = current.notifications.toMutableList()
            // Group notifications from same groupKey or same package+sender
            val existingIndex = mutable.indexOfFirst {
                it.id == item.id || (item.groupKey != null && it.groupKey == item.groupKey && it.packageName == item.packageName)
            }
            val mergedItem = if (existingIndex >= 0) {
                val prev = mutable.removeAt(existingIndex)
                item.copy(groupCount = prev.groupCount + 1)
            } else {
                item
            }
            mutable.add(0, mergedItem)
            val trimmed = mutable.take(MAX_NOTIFICATION_QUEUE)
            val nextMode = if (expandAutomatically) IslandDisplayMode.EXPANDED else current.displayMode
            recomputeQueue(
                current.copy(
                    notifications = trimmed,
                    displayMode = nextMode,
                    selectedQueueIndex = 0
                )
            )
        }

        val durationMs = timeoutMs ?: (_state.value.configuration.autoCollapseSeconds * 1000L).coerceAtLeast(3000L)
        if (expandAutomatically) {
            scheduleAutoCollapse(durationMs)
        }

        notificationTimeoutJobs[item.id]?.cancel()
        notificationTimeoutJobs[item.id] = scope.launch {
            delay(durationMs + 4000L)
            dismissNotification(item.id)
        }
    }

    fun dismissNotification(notificationId: String) {
        notificationTimeoutJobs.remove(notificationId)?.cancel()
        _state.update { current ->
            val remaining = current.notifications.filterNot { it.id == notificationId }
            val recomputed = recomputeQueue(current.copy(notifications = remaining))
            if (recomputed.primaryActivity is IslandActivityPayload.IdlePayload) {
                recomputed.copy(displayMode = IslandDisplayMode.COMPACT)
            } else {
                recomputed
            }
        }
    }

    fun clearAllNotifications() {
        notificationTimeoutJobs.values.forEach { it.cancel() }
        notificationTimeoutJobs.clear()
        _state.update { current ->
            recomputeQueue(current.copy(notifications = emptyList()))
        }
    }

    // ---- Phone Calls ----

    fun updateCallState(callState: CallActivityState) {
        callDurationTickerJob?.cancel()
        _state.update { current ->
            val nextMode = when (callState.status) {
                CallStatus.INCOMING -> IslandDisplayMode.EXPANDED
                CallStatus.ONGOING -> IslandDisplayMode.COMPACT
                else -> current.displayMode
            }
            recomputeQueue(
                current.copy(
                    callState = callState,
                    displayMode = nextMode,
                    selectedQueueIndex = 0
                )
            )
        }

        if (callState.status == CallStatus.ONGOING) {
            val baseStart = if (callState.callStartTimestampMs > 0L) {
                callState.callStartTimestampMs
            } else {
                System.currentTimeMillis()
            }
            callDurationTickerJob = scope.launch {
                while (true) {
                    delay(1000L)
                    val elapsedSec = ((System.currentTimeMillis() - baseStart) / 1000L).coerceAtLeast(0L)
                    _state.update { current ->
                        if (current.callState.status != CallStatus.ONGOING) return@update current
                        val updatedCall = current.callState.copy(durationSeconds = elapsedSec)
                        recomputeQueue(current.copy(callState = updatedCall))
                    }
                }
            }
        }
    }

    // ---- Battery Live Activity ----

    fun updateBatteryState(batteryState: BatteryActivityState, showTransientExpand: Boolean = false) {
        _state.update { current ->
            val nextMode = if (showTransientExpand) IslandDisplayMode.EXPANDED else current.displayMode
            recomputeQueue(
                current.copy(
                    batteryState = batteryState,
                    displayMode = nextMode
                )
            )
        }
        if (showTransientExpand) {
            scheduleAutoCollapse(_state.value.configuration.autoCollapseSeconds * 1000L)
        }
    }

    // ---- Live Timer & Stopwatch ----

    fun startCountdownTimer(durationMs: Long, label: String = "Countdown") {
        val validDuration = durationMs.coerceAtLeast(1000L)
        _state.update { current ->
            val timer = TimerActivityState(
                isActive = true,
                isRunning = true,
                isCompleted = false,
                mode = TimerMode.COUNTDOWN,
                totalDurationMs = validDuration,
                remainingOrElapsedMs = validDuration,
                label = label
            )
            recomputeQueue(current.copy(timerState = timer, displayMode = IslandDisplayMode.COMPACT))
        }
        startTimerTicker()
    }

    fun startStopwatch(label: String = "Stopwatch") {
        _state.update { current ->
            val timer = TimerActivityState(
                isActive = true,
                isRunning = true,
                isCompleted = false,
                mode = TimerMode.STOPWATCH,
                totalDurationMs = 0L,
                remainingOrElapsedMs = 0L,
                label = label
            )
            recomputeQueue(current.copy(timerState = timer, displayMode = IslandDisplayMode.COMPACT))
        }
        startTimerTicker()
    }

    fun toggleTimerPauseResume() {
        val currentTimer = _state.value.timerState
        if (!currentTimer.isActive) {
            startCountdownTimer(300_000L, "Quick 5m Timer")
            return
        }
        val nowRunning = !currentTimer.isRunning
        _state.update { current ->
            val updatedTimer = current.timerState.copy(isRunning = nowRunning)
            recomputeQueue(current.copy(timerState = updatedTimer))
        }
        if (nowRunning) {
            startTimerTicker()
        } else {
            timerTickerJob?.cancel()
        }
    }

    fun stopAndResetTimer() {
        timerTickerJob?.cancel()
        _state.update { current ->
            recomputeQueue(current.copy(timerState = TimerActivityState(isActive = false)))
        }
    }

    private fun startTimerTicker() {
        timerTickerJob?.cancel()
        timerTickerJob = scope.launch {
            while (true) {
                delay(1000L)
                var completedNow = false
                _state.update { current ->
                    val t = current.timerState
                    if (!t.isActive || !t.isRunning) return@update current
                    val updatedTimer = when (t.mode) {
                        TimerMode.COUNTDOWN -> {
                            val nextRemaining = (t.remainingOrElapsedMs - 1000L).coerceAtLeast(0L)
                            if (nextRemaining == 0L) {
                                completedNow = true
                                t.copy(
                                    remainingOrElapsedMs = 0L,
                                    isRunning = false,
                                    isCompleted = true
                                )
                            } else {
                                t.copy(remainingOrElapsedMs = nextRemaining)
                            }
                        }
                        TimerMode.STOPWATCH -> {
                            t.copy(remainingOrElapsedMs = t.remainingOrElapsedMs + 1000L)
                        }
                    }
                    val nextMode = if (completedNow) IslandDisplayMode.EXPANDED else current.displayMode
                    recomputeQueue(current.copy(timerState = updatedTimer, displayMode = nextMode))
                }
                if (completedNow) {
                    scheduleAutoCollapse(5000L)
                    break
                }
            }
        }
    }

    // ---- Connectivity & Other Live Activities ----

    fun updateConnectivityState(
        transform: (ConnectivityActivityState) -> ConnectivityActivityState,
        transientHeadline: String? = null
    ) {
        _state.update { current ->
            val now = System.currentTimeMillis()
            val updatedConn = transform(current.connectivityState).let { base ->
                if (transientHeadline != null) {
                    base.copy(
                        transientBannerMessage = transientHeadline,
                        transientBannerTimestamp = now
                    )
                } else {
                    base
                }
            }
            recomputeQueue(current.copy(connectivityState = updatedConn), now)
        }
        if (transientHeadline != null) {
            scheduleAutoCollapse(_state.value.configuration.autoCollapseSeconds * 1000L)
        }
    }

    fun resetAllForTesting() {
        autoCollapseJob?.cancel()
        timerTickerJob?.cancel()
        callDurationTickerJob?.cancel()
        notificationTimeoutJobs.values.forEach { it.cancel() }
        notificationTimeoutJobs.clear()
        _state.value = recomputeQueue(IslandRuntimeState())
    }

    private fun scheduleAutoCollapse(delayMs: Long) {
        autoCollapseJob?.cancel()
        autoCollapseJob = scope.launch {
            delay(delayMs.coerceAtLeast(1000L))
            _state.update { current ->
                if (current.displayMode == IslandDisplayMode.EXPANDED ||
                    current.displayMode == IslandDisplayMode.CONTROL_PANEL
                ) {
                    current.copy(displayMode = IslandDisplayMode.COMPACT)
                } else {
                    current
                }
            }
        }
    }

    private fun recomputeQueue(
        base: IslandRuntimeState,
        nowMs: Long = System.currentTimeMillis()
    ): IslandRuntimeState {
        val queue = IslandPriorityEngine.buildPrioritizedQueue(
            callState = base.callState,
            mediaState = base.mediaState,
            notifications = base.notifications,
            timerState = base.timerState,
            batteryState = base.batteryState,
            connectivityState = base.connectivityState,
            nowMs = nowMs
        )
        val clampedIndex = base.selectedQueueIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
        // Always prioritize Incoming Call or newly arrived high-priority item at index 0
        val effectiveIndex = if (queue.firstOrNull() is IslandActivityPayload.CallPayload) 0 else clampedIndex
        val primary = queue.getOrElse(effectiveIndex) { IslandActivityPayload.IdlePayload }
        val secondary = if (queue.size > 1) {
            queue.getOrNull((effectiveIndex + 1) % queue.size)?.takeIf { it.id != primary.id }
        } else {
            null
        }
        return base.copy(
            activeQueue = queue,
            selectedQueueIndex = effectiveIndex,
            primaryActivity = primary,
            secondaryActivity = secondary
        )
    }
}
