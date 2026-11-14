package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.repository.IslandRepository
import com.example.domain.model.CutoutAdaptationMode
import com.example.island.components.DynamicIslandCapsule
import com.example.island.manager.IslandStateManager
import com.example.ui.theme.LeovnIslandTheme
import com.example.utils.SystemPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Core foreground service managing the real Android WindowManager TYPE_APPLICATION_OVERLAY
 * Dynamic Island view across all apps, handling display cutouts, orientation changes,
 * non-focusable keyboard safety, and lifecycle cleanup.
 */
class IslandOverlayService :
    Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val binder = LocalBinder()

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private lateinit var repository: IslandRepository

    inner class LocalBinder : Binder() {
        fun getService(): IslandOverlayService = this@IslandOverlayService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        repository = IslandRepository.getInstance(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        IslandStateManager.playCustomNotificationToneAction = {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
                ringtone?.play()
            }
        }

        startInForegroundSafely()
        startCompanionServices()
        attachOverlayWindowIfPermitted()
        observeConfigurationAndPosition()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startInForegroundSafely()
        attachOverlayWindowIfPermitted()
        IslandStateManager.setOverlayServiceRunning(true)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        IslandStateManager.updateCutoutAndOrientation(
            safeTopInsetDp = IslandStateManager.state.value.cutoutSafeTopInsetDp,
            isLandscape = isLandscape
        )
        updateWindowPositionFromConfig()
    }

    private fun startInForegroundSafely() {
        val channelId = NOTIFICATION_CHANNEL_ID
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.island_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.island_service_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_island_tile)
            .setContentTitle(getString(R.string.island_service_notification_title))
            .setContentText(getString(R.string.island_service_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Fallback in unit/Robolectric environments
        }
    }

    private fun startCompanionServices() {
        runCatching {
            startService(Intent(this, MediaSessionService::class.java))
            startService(Intent(this, ActivityMonitorService::class.java))
        }
    }

    private fun attachOverlayWindowIfPermitted() {
        // Prevent duplicate overlays
        if (overlayComposeView != null) return
        if (!SystemPermissionHelper.canDrawOverlays(this)) return
        val wm = windowManager ?: return

        val density = resources.displayMetrics.density
        val config = IslandStateManager.state.value.configuration

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = resolveGravity(config.cutoutMode)
            x = (config.horizontalOffsetDp * density).roundToInt()
            y = (config.verticalOffsetDp * density).roundToInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandOverlayService)
            setViewTreeViewModelStoreOwner(this@IslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayService)
            setContent {
                LeovnIslandTheme {
                    val runtimeState by IslandStateManager.state.collectAsState()
                    val shouldHideLandscape = runtimeState.isInLandscape &&
                        runtimeState.configuration.hideInFullscreenLandscape
                    if (!shouldHideLandscape && runtimeState.configuration.isIslandEnabled) {
                        DynamicIslandCapsule(
                            runtimeState = runtimeState,
                            onDragDeltaDp = { dx, dy ->
                                IslandStateManager.updateDragOffset(dx, dy)
                                updateWindowPositionFromConfig()
                            },
                            onDragFinished = {
                                val latest = IslandStateManager.state.value.configuration
                                serviceScope.launch {
                                    repository.updatePositionOffset(
                                        latest.horizontalOffsetDp,
                                        latest.verticalOffsetDp
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        try {
            wm.addView(composeView, params)
            overlayComposeView = composeView
        } catch (_: Exception) {
            // Safely handle WindowManager exceptions in headless test environments
        }
    }

    private fun observeConfigurationAndPosition() {
        serviceScope.launch {
            repository.configurationFlow.collectLatest { config ->
                IslandStateManager.updateConfiguration(config)
                updateWindowPositionFromConfig()
            }
        }
    }

    private fun updateWindowPositionFromConfig() {
        val view = overlayComposeView ?: return
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val config = IslandStateManager.state.value.configuration
        val density = resources.displayMetrics.density

        params.gravity = resolveGravity(config.cutoutMode)
        params.x = (config.horizontalOffsetDp * density).roundToInt()
        params.y = (config.verticalOffsetDp * density).roundToInt()
        runCatching {
            wm.updateViewLayout(view, params)
        }
    }

    private fun resolveGravity(cutoutMode: CutoutAdaptationMode): Int {
        return when (cutoutMode) {
            CutoutAdaptationMode.LEFT_PUNCH -> Gravity.TOP or Gravity.START
            else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    private fun removeOverlayWindowSafely() {
        val view = overlayComposeView ?: return
        val wm = windowManager
        runCatching {
            wm?.removeViewImmediate(view)
        }
        overlayComposeView = null
        layoutParams = null
    }

    override fun onDestroy() {
        IslandStateManager.setOverlayServiceRunning(false)
        removeOverlayWindowSafely()
        runCatching {
            stopService(Intent(this, MediaSessionService::class.java))
            stopService(Intent(this, ActivityMonitorService::class.java))
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "leovnisland_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                runCatching { context.startService(intent) }
            }
            IslandStateManager.setOverlayServiceRunning(true)
        }

        fun stop(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java)
            runCatching { context.stopService(intent) }
            IslandStateManager.setOverlayServiceRunning(false)
        }
    }
}
