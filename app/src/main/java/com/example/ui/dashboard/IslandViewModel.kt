package com.example.ui.dashboard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.repository.IslandRepository
import com.example.domain.model.AnimationPreset
import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.CallStatus
import com.example.domain.model.CutoutAdaptationMode
import com.example.domain.model.DoubleTapAction
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.IslandThemeStyle
import com.example.domain.model.MediaSessionState
import com.example.domain.model.SupportedAppCategory
import com.example.island.manager.IslandStateManager
import com.example.island.state.IslandRuntimeState
import com.example.service.ActivityMonitorService
import com.example.service.IslandOverlayService
import com.example.service.MediaSessionService
import com.example.utils.SystemPermissionHelper
import com.example.utils.SystemPermissionsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IslandRepository.getInstance(application)

    val runtimeState: StateFlow<IslandRuntimeState> = IslandStateManager.state

    private val _permissionsSnapshot = MutableStateFlow(
        SystemPermissionHelper.captureSnapshot(application)
    )
    val permissionsSnapshot: StateFlow<SystemPermissionsSnapshot> = _permissionsSnapshot.asStateFlow()

    private var demoTrackIndex = 0
    private val sampleTracks = listOf(
        Triple("Midnight Liquid Glass", "LEOVAN Audio Lab", "Spotify"),
        Triple("Titanium Horizon", "Aether & Pulse", "YouTube Music"),
        Triple("Obsidian Resonance", "Kora Synth", "Spotify"),
        Triple("Jakarta Afterglow", "Nusantara Wave", "Apple Music")
    )

    init {
        viewModelScope.launch {
            repository.configurationFlow.collectLatest { config ->
                IslandStateManager.updateConfiguration(config)
            }
        }
        refreshPermissionsSnapshot()
    }

    fun refreshPermissionsSnapshot() {
        val ctx = getApplication<Application>()
        _permissionsSnapshot.value = SystemPermissionHelper.captureSnapshot(ctx)
        MediaSessionService.instance?.refreshMediaSessions()
        ActivityMonitorService.instance?.refreshCallMonitoringIfPermitted()
    }

    fun toggleOverlayService(enable: Boolean) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            repository.setIslandEnabled(enable)
        }
        if (enable) {
            IslandOverlayService.start(ctx)
        } else {
            IslandOverlayService.stop(ctx)
        }
    }

    fun updateConfig(transform: (IslandConfiguration) -> IslandConfiguration) {
        viewModelScope.launch {
            repository.updateConfiguration(transform)
        }
    }

    fun setThemeStyle(style: IslandThemeStyle) {
        updateConfig { it.copy(themeStyle = style) }
    }

    fun setAnimationPreset(preset: AnimationPreset) {
        updateConfig { it.copy(animationPreset = preset) }
    }

    fun setCutoutMode(mode: CutoutAdaptationMode) {
        updateConfig { it.copy(cutoutMode = mode) }
    }

    fun setDoubleTapAction(action: DoubleTapAction) {
        updateConfig { it.copy(doubleTapAction = action) }
    }

    fun toggleAppCategory(category: SupportedAppCategory, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAppCategoryFilter(category, enabled)
        }
    }

    fun resetStudioDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }

    // ---- Interactive Verification & Live System Triggers ----

    fun activateOrCycleMediaSession() {
        val current = runtimeState.value.mediaState
        if (!current.isActive) {
            val (title, artist, app) = sampleTracks[demoTrackIndex % sampleTracks.size]
            IslandStateManager.updateMediaSession(
                MediaSessionState(
                    isActive = true,
                    isPlaying = true,
                    packageName = "com.spotify.music",
                    appName = app,
                    title = title,
                    artist = artist,
                    album = "LEOVNISLAND Sessions",
                    durationMs = 214_000L,
                    positionMs = 48_000L
                )
            )
            IslandStateManager.expandIsland()
        } else {
            demoTrackIndex = (demoTrackIndex + 1) % sampleTracks.size
            val (title, artist, app) = sampleTracks[demoTrackIndex]
            IslandStateManager.updateMediaSession(
                current.copy(
                    isActive = true,
                    isPlaying = true,
                    appName = app,
                    title = title,
                    artist = artist,
                    positionMs = 12_000L,
                    durationMs = 228_000L,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun stopMediaSession() {
        IslandStateManager.updateMediaSession(MediaSessionState(isActive = false))
    }

    fun dispatchSmartNotification(category: SupportedAppCategory) {
        val (sender, preview) = when (category) {
            SupportedAppCategory.WHATSAPP -> "Raka Pratama" to "Bro, build LEOVNISLAND udah siap di-test di device fisik!"
            SupportedAppCategory.TELEGRAM -> "Kotlin Android ID" to "Release 1.0.0: WindowManager overlay & MediaSession berjalan 60fps."
            SupportedAppCategory.INSTAGRAM -> "leovan.design" to "Mentioned you in their story: Liquid Glass UI looks clean."
            SupportedAppCategory.DISCORD -> "DevOps Server #builds" to "CI Pipeline passed all 18 verification checks."
            SupportedAppCategory.GMAIL -> "Google Play Console" to "Your app bundle com.aistudio.leovnisland is ready for review."
            SupportedAppCategory.MESSENGER -> "Nadia Putri" to "Sent a voice note (0:24)"
            SupportedAppCategory.SMS -> "Bank BCA" to "Kode OTP transaksi Anda adalah 849201. Jangan berikan kepada siapapun."
            SupportedAppCategory.OTHER -> "Android System" to "USB debugging connected & telemetry active."
        }
        val item = IslandNotificationItem(
            id = "${category.packageName}_${System.currentTimeMillis()}",
            packageName = category.packageName,
            appName = category.displayName,
            senderName = sender,
            title = sender,
            messagePreview = preview,
            timestamp = System.currentTimeMillis(),
            category = category,
            groupKey = "group_${category.name}",
            priorityLevel = 2
        )
        IslandStateManager.enqueueNotification(item)

        // Also post a real system notification if permitted so NotificationListenerService receives it
        postRealSystemNotification(category.displayName, sender, preview)
    }

    private fun postRealSystemNotification(appLabel: String, title: String, message: String) {
        val ctx = getApplication<Application>()
        if (!SystemPermissionHelper.hasPostNotificationsPermission(ctx)) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "leovnisland_smart_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LEOVNISLAND Smart Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_island_tile)
            .setSubText(appLabel)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        runCatching {
            nm.notify((System.currentTimeMillis() % 10000).toInt() + 2000, notif)
        }
    }

    fun triggerIncomingCall() {
        IslandStateManager.updateCallState(
            CallActivityState(
                status = CallStatus.INCOMING,
                callerName = "Leovan Studio HQ",
                callerNumber = "+62 812-9400-8821",
                callStartTimestampMs = System.currentTimeMillis(),
                durationSeconds = 0L
            )
        )
    }

    fun triggerOngoingCall() {
        IslandStateManager.updateCallState(
            CallActivityState(
                status = CallStatus.ONGOING,
                callerName = "Leovan Studio HQ",
                callerNumber = "+62 812-9400-8821",
                callStartTimestampMs = System.currentTimeMillis(),
                durationSeconds = 0L
            )
        )
    }

    fun endCallActivity() {
        IslandStateManager.updateCallState(CallActivityState(status = CallStatus.NONE))
    }

    fun triggerBatteryAlert(charging: Boolean, percentage: Int) {
        IslandStateManager.updateBatteryState(
            BatteryActivityState(
                percentage = percentage,
                isCharging = charging,
                isFull = percentage >= 100 && charging,
                isLow = !charging && percentage <= 20,
                powerSource = if (charging) "SuperVOOC / Fast AC" else "Battery",
                temperatureCelsius = 31.4f,
                lastAlertTimestamp = System.currentTimeMillis()
            ),
            showTransientExpand = true
        )
    }
}
