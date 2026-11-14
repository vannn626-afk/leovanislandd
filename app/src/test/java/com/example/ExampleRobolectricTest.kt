package com.example

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.BatteryManager
import android.service.notification.StatusBarNotification
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.CallStatus
import com.example.domain.model.IslandActivityPayload
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.IslandDisplayMode
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.MediaSessionState
import com.example.domain.model.SupportedAppCategory
import com.example.island.components.DynamicIslandCapsule
import com.example.island.manager.IslandStateManager
import com.example.service.ActivityMonitorService
import com.example.service.IslandNotificationListener
import com.example.service.IslandOverlayService
import com.example.ui.theme.LeovnIslandTheme
import com.example.utils.SystemPermissionHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Full 18-point verification test suite for LEOVNISLAND (Section 14 Specification).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        IslandStateManager.resetAllForTesting()
        SystemPermissionHelper.testOverlayPermissionOverride = null
        SystemPermissionHelper.testNotificationListenerOverride = null
    }

    @After
    fun tearDown() {
        IslandStateManager.resetAllForTesting()
        SystemPermissionHelper.testOverlayPermissionOverride = null
        SystemPermissionHelper.testNotificationListenerOverride = null
    }

    // 1. Overlay permission
    @Test
    fun test01_overlayPermissionCheckAndIntent() {
        SystemPermissionHelper.testOverlayPermissionOverride = false
        assertFalse(SystemPermissionHelper.canDrawOverlays(context))

        SystemPermissionHelper.testOverlayPermissionOverride = true
        assertTrue(SystemPermissionHelper.canDrawOverlays(context))

        val intent = SystemPermissionHelper.createOverlayPermissionIntent(context)
        assertEquals(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
    }

    // 2. Start and stop service
    @Test
    fun test02_startAndStopOverlayService() {
        SystemPermissionHelper.testOverlayPermissionOverride = true
        IslandOverlayService.start(context)
        assertTrue(IslandStateManager.state.value.isOverlayServiceRunning)

        IslandOverlayService.stop(context)
        assertFalse(IslandStateManager.state.value.isOverlayServiceRunning)
    }

    // 3. Music session detection
    @Test
    fun test03_musicSessionDetection() {
        val media = MediaSessionState(
            isActive = true,
            isPlaying = true,
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Midnight Liquid Glass",
            artist = "LEOVAN Audio Lab",
            durationMs = 200_000L,
            positionMs = 50_000L
        )
        IslandStateManager.updateMediaSession(media)

        val state = IslandStateManager.state.value
        assertTrue(state.mediaState.isActive)
        assertEquals("Midnight Liquid Glass", state.mediaState.title)
        assertTrue(state.primaryActivity is IslandActivityPayload.MusicPayload)
    }

    // 4. Play/pause
    @Test
    fun test04_musicPlayPauseTransportControl() {
        IslandStateManager.mediaPlayPauseAction = null
        IslandStateManager.updateMediaSession(
            MediaSessionState(
                isActive = true,
                isPlaying = true,
                title = "Track A",
                artist = "Artist A"
            )
        )
        assertTrue(IslandStateManager.state.value.mediaState.isPlaying)

        IslandStateManager.triggerPlayPause()
        assertFalse(IslandStateManager.state.value.mediaState.isPlaying)

        IslandStateManager.triggerPlayPause()
        assertTrue(IslandStateManager.state.value.mediaState.isPlaying)
    }

    // 5. Next/previous
    @Test
    fun test05_musicNextAndPreviousControls() {
        var nextInvoked = false
        var prevInvoked = false
        IslandStateManager.mediaNextAction = { nextInvoked = true }
        IslandStateManager.mediaPreviousAction = { prevInvoked = true }

        IslandStateManager.triggerNextTrack()
        assertTrue(nextInvoked)

        IslandStateManager.triggerPreviousTrack()
        assertTrue(prevInvoked)
    }

    // 6. Album artwork
    @Test
    fun test06_albumArtworkRenderingAndMetadata() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val media = MediaSessionState(
            isActive = true,
            isPlaying = true,
            title = "Artwork Song",
            artist = "Artwork Artist",
            artwork = bitmap
        )
        IslandStateManager.updateMediaSession(media)
        assertNotNull(IslandStateManager.state.value.mediaState.artwork)
        assertEquals(64, IslandStateManager.state.value.mediaState.artwork?.width)
    }

    // 7. Notification listener
    @Test
    fun test07_notificationListenerConnectionState() {
        val listenerController = Robolectric.buildService(IslandNotificationListener::class.java).create()
        val listener = listenerController.get()
        listener.onListenerConnected()
        assertTrue(IslandStateManager.state.value.isNotificationListenerConnected)

        listener.onListenerDisconnected()
        assertFalse(IslandStateManager.state.value.isNotificationListenerConnected)
    }

    // 8. Notification preview
    @Test
    fun test08_notificationPreviewAndCategoryParsing() {
        val notif = IslandNotificationItem(
            id = "wa_101",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            senderName = "Budi Santoso",
            title = "Budi Santoso",
            messagePreview = "Halo, tes Dynamic Island!",
            category = SupportedAppCategory.fromPackage("com.whatsapp")
        )
        IslandStateManager.enqueueNotification(notif)

        val state = IslandStateManager.state.value
        assertEquals(1, state.notifications.size)
        assertEquals(SupportedAppCategory.WHATSAPP, state.notifications.first().category)
        assertEquals("Halo, tes Dynamic Island!", state.notifications.first().messagePreview)
    }

    // 9. Multiple notification queue & Priority ordering
    @Test
    fun test09_multipleNotificationQueueAndPriorityHierarchy() {
        val n1 = IslandNotificationItem(
            id = "tg_1",
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            senderName = "Alice",
            title = "Alice",
            messagePreview = "First message",
            timestamp = 1000L
        )
        val n2 = IslandNotificationItem(
            id = "dc_2",
            packageName = "com.discord",
            appName = "Discord",
            senderName = "Bob",
            title = "Bob",
            messagePreview = "Second message",
            timestamp = 2000L
        )
        IslandStateManager.enqueueNotification(n1)
        IslandStateManager.enqueueNotification(n2)
        assertEquals(2, IslandStateManager.state.value.notifications.size)

        // Incoming Call has rank 1, so it must preempt notifications in primaryActivity
        IslandStateManager.updateCallState(
            CallActivityState(status = CallStatus.INCOMING, callerName = "VIP Caller")
        )
        assertTrue(IslandStateManager.state.value.primaryActivity is IslandActivityPayload.CallPayload)

        // When call ends, queue restores previous activity automatically
        IslandStateManager.updateCallState(CallActivityState(status = CallStatus.NONE))
        assertTrue(IslandStateManager.state.value.primaryActivity is IslandActivityPayload.NotificationPayload)
    }

    // 10. Tap notification
    @Test
    fun test10_tapNotificationActionAndDismissal() {
        val intent = Intent(Intent.ACTION_MAIN)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = IslandNotificationItem(
            id = "gm_1",
            packageName = "com.google.android.gm",
            appName = "Gmail",
            senderName = "Security Team",
            title = "Security Alert",
            messagePreview = "Tap to open email",
            contentIntent = pendingIntent
        )
        IslandStateManager.enqueueNotification(notif)
        assertEquals(IslandDisplayMode.EXPANDED, IslandStateManager.state.value.displayMode)

        composeTestRule.setContent {
            LeovnIslandTheme {
                DynamicIslandCapsule(runtimeState = IslandStateManager.state.value)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("notification_open_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(IslandStateManager.state.value.notifications.isEmpty())
    }

    // 11. Battery status
    @Test
    fun test11_batteryStatusParsingAndLiveActivity() {
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 88)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC)
            putExtra(BatteryManager.EXTRA_TEMPERATURE, 305)
        }
        val parsed: BatteryActivityState = ActivityMonitorService.parseBatteryIntent(batteryIntent, isAlert = true)
        assertEquals(88, parsed.percentage)
        assertTrue(parsed.isCharging)
        assertEquals("Fast AC Charger", parsed.powerSource)

        IslandStateManager.updateBatteryState(parsed, showTransientExpand = true)
        assertEquals(88, IslandStateManager.state.value.batteryState.percentage)
        assertTrue(IslandStateManager.state.value.primaryActivity is IslandActivityPayload.BatteryPayload)
    }

    // 12. Timer
    @Test
    fun test12_timerCountdownStopwatchAndPauseResume() {
        IslandStateManager.startCountdownTimer(durationMs = 120_000L, label = "Deep Work")
        assertTrue(IslandStateManager.state.value.timerState.isActive)
        assertTrue(IslandStateManager.state.value.timerState.isRunning)
        assertEquals(120_000L, IslandStateManager.state.value.timerState.remainingOrElapsedMs)

        IslandStateManager.toggleTimerPauseResume()
        assertFalse(IslandStateManager.state.value.timerState.isRunning)

        IslandStateManager.stopAndResetTimer()
        assertFalse(IslandStateManager.state.value.timerState.isActive)
    }

    // 13. Drag Island
    @Test
    fun test13_dragIslandPositioningAndBounds() {
        IslandStateManager.updateConfiguration(
            IslandConfiguration(isDraggable = true, horizontalOffsetDp = 0f, verticalOffsetDp = 10f)
        )
        IslandStateManager.updateDragOffset(deltaXDp = 35f, deltaYDp = 20f)
        assertEquals(35f, IslandStateManager.state.value.configuration.horizontalOffsetDp, 0.01f)
        assertEquals(30f, IslandStateManager.state.value.configuration.verticalOffsetDp, 0.01f)
    }

    // 14. Expand/collapse
    @Test
    fun test14_expandAndCollapseTransitions() {
        IslandStateManager.updateMediaSession(
            MediaSessionState(isActive = true, isPlaying = true, title = "Song", artist = "Artist")
        )
        IslandStateManager.collapseIsland()
        assertEquals(IslandDisplayMode.COMPACT, IslandStateManager.state.value.displayMode)

        IslandStateManager.expandIsland()
        assertEquals(IslandDisplayMode.EXPANDED, IslandStateManager.state.value.displayMode)

        IslandStateManager.collapseIsland()
        assertEquals(IslandDisplayMode.COMPACT, IslandStateManager.state.value.displayMode)
    }

    // 15. App background behavior
    @Test
    fun test15_appBackgroundOverlayPersistence() {
        SystemPermissionHelper.testOverlayPermissionOverride = true
        val serviceController = Robolectric.buildService(IslandOverlayService::class.java)
            .create()
            .startCommand(0, 1)
        assertTrue(IslandStateManager.state.value.isOverlayServiceRunning)

        // Simulate MainActivity going to background while service stays alive
        val activityController = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .pause()
            .stop()
        assertNotNull(activityController.get())
        // Overlay service remains running independently of MainActivity lifecycle
        assertTrue(IslandStateManager.state.value.isOverlayServiceRunning)
        serviceController.destroy()
    }

    // 16. Service cleanup
    @Test
    fun test16_serviceDestroyCleanupReleasesResources() {
        val controller = Robolectric.buildService(IslandOverlayService::class.java)
            .create()
            .startCommand(0, 1)
        assertTrue(IslandStateManager.state.value.isOverlayServiceRunning)

        controller.destroy()
        assertFalse(IslandStateManager.state.value.isOverlayServiceRunning)
    }

    // 17. Device rotation
    @Test
    fun test17_deviceRotationLandscapeHandling() {
        IslandStateManager.updateConfiguration(
            IslandConfiguration(hideInFullscreenLandscape = true)
        )
        val serviceController = Robolectric.buildService(IslandOverlayService::class.java).create()
        val landscapeConfig = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }
        serviceController.get().onConfigurationChanged(landscapeConfig)
        assertTrue(IslandStateManager.state.value.isInLandscape)
        serviceController.destroy()
    }

    // 18. Different screen sizes
    @Test
    @Config(sdk = [36], qualifiers = "sw600dp-w800dp-h1280dp-xhdpi")
    fun test18_differentScreenSizesAndCutoutScaling() {
        IslandStateManager.updateConfiguration(
            IslandConfiguration(
                compactWidthDp = 160f,
                expandedWidthDp = 370f,
                compactSizeScale = 1.15f,
                expandedSizeScale = 1.10f
            )
        )
        composeTestRule.setContent {
            LeovnIslandTheme {
                DynamicIslandCapsule(runtimeState = IslandStateManager.state.value)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dynamic_island_capsule").assertExists()
    }
}
