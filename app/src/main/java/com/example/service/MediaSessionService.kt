package com.example.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import com.example.domain.model.MediaSessionState
import com.example.island.manager.IslandStateManager
import com.example.utils.SystemPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Integrates with Android MediaSessionManager and MediaController to track and control
 * real active media sessions (Spotify, YouTube Music, Apple Music, System Players, etc.).
 */
class MediaSessionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val binder = LocalBinder()

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var progressInterpolationJob: Job? = null
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindControllers(controllers.orEmpty())
        }

    inner class LocalBinder : Binder() {
        fun getService(): MediaSessionService = this@MediaSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        registerTransportHooks()
        refreshMediaSessions()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshMediaSessions()
        return START_STICKY
    }

    fun refreshMediaSessions() {
        val manager = mediaSessionManager ?: return
        if (!SystemPermissionHelper.isNotificationListenerEnabled(this)) {
            return
        }
        val listenerComponent = ComponentName(this, IslandNotificationListener::class.java)
        try {
            manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {
        }
        try {
            manager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent)
            val controllers = manager.getActiveSessions(listenerComponent)
            bindControllers(controllers)
        } catch (_: SecurityException) {
            // NotificationListener access not yet granted by system
        } catch (_: Exception) {
        }
    }

    fun bindControllers(controllers: List<MediaController>) {
        // Unregister old callbacks cleanly to prevent memory leaks
        controllerCallbacks.forEach { (controller, callback) ->
            runCatching { controller.unregisterCallback(callback) }
        }
        controllerCallbacks.clear()

        if (controllers.isEmpty()) {
            activeController = null
            progressInterpolationJob?.cancel()
            IslandStateManager.updateMediaSession(MediaSessionState(isActive = false))
            return
        }

        // Register callbacks on all active controllers so switching apps is detected immediately
        for (controller in controllers) {
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    selectPrimaryControllerAndSync(controllers)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    selectPrimaryControllerAndSync(controllers)
                }

                override fun onSessionDestroyed() {
                    refreshMediaSessions()
                }
            }
            runCatching {
                controller.registerCallback(callback)
                controllerCallbacks[controller] = callback
            }
        }

        selectPrimaryControllerAndSync(controllers)
    }

    private fun selectPrimaryControllerAndSync(controllers: List<MediaController>) {
        val chosen = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull {
            val st = it.playbackState?.state
            st == PlaybackState.STATE_PAUSED || st == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull()

        activeController = chosen
        if (chosen == null) {
            progressInterpolationJob?.cancel()
            IslandStateManager.updateMediaSession(MediaSessionState(isActive = false))
            return
        }

        syncControllerToIslandState(chosen)
    }

    private fun syncControllerToIslandState(controller: MediaController) {
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val stateCode = playbackState?.state ?: PlaybackState.STATE_NONE
        val isPlaying = stateCode == PlaybackState.STATE_PLAYING || stateCode == PlaybackState.STATE_BUFFERING
        val isSessionActive = stateCode != PlaybackState.STATE_NONE &&
            stateCode != PlaybackState.STATE_STOPPED &&
            stateCode != PlaybackState.STATE_ERROR &&
            metadata != null

        if (!isSessionActive && metadata == null) {
            progressInterpolationJob?.cancel()
            IslandStateManager.updateMediaSession(MediaSessionState(isActive = false))
            return
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: resolveAppName(controller.packageName)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val duration = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)
        val artwork: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val currentPos = computeCurrentPlaybackPosition(playbackState, duration)

        val mediaState = MediaSessionState(
            isActive = true,
            isPlaying = isPlaying,
            packageName = controller.packageName,
            appName = resolveAppName(controller.packageName),
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            positionMs = currentPos,
            artwork = artwork,
            lastUpdatedMs = System.currentTimeMillis()
        )

        IslandStateManager.updateMediaSession(mediaState)

        progressInterpolationJob?.cancel()
        if (isPlaying && duration > 0L) {
            progressInterpolationJob = serviceScope.launch {
                while (true) {
                    delay(1000L)
                    val active = activeController ?: break
                    val pb = active.playbackState ?: break
                    if (pb.state != PlaybackState.STATE_PLAYING) break
                    val pos = computeCurrentPlaybackPosition(pb, duration)
                    val currentMedia = IslandStateManager.state.value.mediaState
                    if (currentMedia.isActive) {
                        IslandStateManager.updateMediaSession(
                            currentMedia.copy(
                                positionMs = pos,
                                lastUpdatedMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun computeCurrentPlaybackPosition(playbackState: PlaybackState?, durationMs: Long): Long {
        if (playbackState == null) return 0L
        var position = playbackState.position.coerceAtLeast(0L)
        if (playbackState.state == PlaybackState.STATE_PLAYING) {
            val timeDelta = (SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime).coerceAtLeast(0L)
            val speed = if (playbackState.playbackSpeed > 0f) playbackState.playbackSpeed else 1f
            position += (timeDelta * speed).toLong()
        }
        return if (durationMs > 0L) position.coerceIn(0L, durationMs) else position
    }

    private fun registerTransportHooks() {
        IslandStateManager.mediaPlayPauseAction = {
            val controller = activeController
            if (controller != null) {
                val pbState = controller.playbackState?.state
                if (pbState == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            } else {
                // Toggle local preview state when no external system MediaController is bound
                val current = IslandStateManager.state.value.mediaState
                IslandStateManager.updateMediaSession(
                    current.copy(
                        isActive = true,
                        isPlaying = !current.isPlaying,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                )
            }
        }

        IslandStateManager.mediaNextAction = {
            val controller = activeController
            if (controller != null) {
                controller.transportControls.skipToNext()
            } else {
                val current = IslandStateManager.state.value.mediaState
                IslandStateManager.updateMediaSession(
                    current.copy(
                        positionMs = 0L,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                )
            }
        }

        IslandStateManager.mediaPreviousAction = {
            val controller = activeController
            if (controller != null) {
                controller.transportControls.skipToPrevious()
            } else {
                val current = IslandStateManager.state.value.mediaState
                IslandStateManager.updateMediaSession(
                    current.copy(
                        positionMs = 0L,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                )
            }
        }

        IslandStateManager.mediaSeekAction = { targetMs ->
            activeController?.transportControls?.seekTo(targetMs)
        }
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            when {
                packageName.contains("spotify", ignoreCase = true) -> "Spotify"
                packageName.contains("youtube", ignoreCase = true) -> "YouTube Music"
                else -> "Music Player"
            }
        }
    }

    override fun onDestroy() {
        progressInterpolationJob?.cancel()
        controllerCallbacks.forEach { (controller, callback) ->
            runCatching { controller.unregisterCallback(callback) }
        }
        controllerCallbacks.clear()
        runCatching {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        }
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: MediaSessionService? = null
            private set
    }
}
