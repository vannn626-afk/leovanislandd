package com.example.island.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.CallStatus
import com.example.domain.model.ConnectivityActivityState
import com.example.domain.model.DoubleTapAction
import com.example.domain.model.IslandActivityPayload
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.IslandDisplayMode
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.IslandThemeStyle
import com.example.domain.model.MediaSessionState
import com.example.domain.model.TimerActivityState
import com.example.island.animation.AnimatedEqualizerBars
import com.example.island.animation.IslandAnimations
import com.example.island.manager.IslandStateManager
import com.example.island.state.IslandRuntimeState
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DynamicIslandCapsule(
    runtimeState: IslandRuntimeState,
    modifier: Modifier = Modifier,
    onDragDeltaDp: (Float, Float) -> Unit = { dx, dy ->
        IslandStateManager.updateDragOffset(dx, dy)
    },
    onDragFinished: () -> Unit = {}
) {
    val config = runtimeState.configuration
    if (!config.isIslandVisible) return

    val context = LocalContext.current
    val density = LocalDensity.current

    val targetWidth: Dp = when (runtimeState.displayMode) {
        IslandDisplayMode.MINIMAL -> (88f * config.minimalSizeScale).dp
        IslandDisplayMode.COMPACT -> {
            val baseW = if (runtimeState.primaryActivity is IslandActivityPayload.IdlePayload) {
                config.compactWidthDp
            } else {
                (config.compactWidthDp + 58f).coerceAtMost(260f)
            }
            (baseW * config.compactSizeScale).dp
        }
        IslandDisplayMode.EXPANDED -> (config.expandedWidthDp * config.expandedSizeScale).dp
        IslandDisplayMode.CONTROL_PANEL -> (config.expandedWidthDp * config.expandedSizeScale).dp
    }

    val targetHeight: Dp = when (runtimeState.displayMode) {
        IslandDisplayMode.MINIMAL -> (28f * config.minimalSizeScale).dp
        IslandDisplayMode.COMPACT -> (config.compactHeightDp * config.compactSizeScale).dp
        IslandDisplayMode.EXPANDED -> {
            val baseH = when (runtimeState.primaryActivity) {
                is IslandActivityPayload.MusicPayload -> config.expandedHeightDp
                is IslandActivityPayload.NotificationPayload -> (config.expandedHeightDp - 14f).coerceAtLeast(148f)
                is IslandActivityPayload.CallPayload -> 146f
                is IslandActivityPayload.TimerPayload -> 148f
                else -> 136f
            }
            (baseH * config.expandedSizeScale).dp
        }
        IslandDisplayMode.CONTROL_PANEL -> 168.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = IslandAnimations.morphSpringSpec(config),
        label = "island_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = IslandAnimations.morphSpringSpec(config),
        label = "island_height"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = when (runtimeState.displayMode) {
            IslandDisplayMode.EXPANDED, IslandDisplayMode.CONTROL_PANEL -> (config.cornerRadiusDp + 8f).dp
            else -> config.cornerRadiusDp.dp
        },
        animationSpec = IslandAnimations.morphSpringSpec(config),
        label = "island_corner_radius"
    )

    val (surfaceBrush, borderBrush, accentColor) = rememberIslandVisualPalette(config)

    var accumSwipeX by remember { mutableFloatStateOf(0f) }
    var accumSwipeY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .testTag("dynamic_island_capsule")
            .width(animatedWidth)
            .height(animatedHeight)
            .shadow(
                elevation = if (runtimeState.displayMode == IslandDisplayMode.EXPANDED) 20.dp else 10.dp,
                shape = RoundedCornerShape(animatedCornerRadius),
                ambientColor = Color.Black,
                spotColor = accentColor.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(animatedCornerRadius))
            .pointerInput(config.doubleTapAction, runtimeState.displayMode) {
                detectTapGestures(
                    onTap = {
                        if (runtimeState.displayMode == IslandDisplayMode.EXPANDED ||
                            runtimeState.displayMode == IslandDisplayMode.CONTROL_PANEL
                        ) {
                            IslandStateManager.collapseIsland()
                        } else {
                            IslandStateManager.expandIsland()
                        }
                    },
                    onDoubleTap = {
                        handleDoubleTap(context, config.doubleTapAction)
                    },
                    onLongPress = {
                        IslandStateManager.openControlPanel()
                    }
                )
            }
            .pointerInput(config.isDraggable, runtimeState.displayMode) {
                detectDragGestures(
                    onDragStart = {
                        accumSwipeX = 0f
                        accumSwipeY = 0f
                    },
                    onDragEnd = {
                        val swipeThresholdPx = with(density) { 42.dp.toPx() }
                        if (abs(accumSwipeX) > abs(accumSwipeY) && abs(accumSwipeX) > swipeThresholdPx) {
                            if (accumSwipeX < 0) {
                                IslandStateManager.cycleNextActivity()
                            } else {
                                IslandStateManager.cyclePreviousActivity()
                            }
                        } else if (abs(accumSwipeY) > swipeThresholdPx) {
                            if (accumSwipeY > 0) {
                                IslandStateManager.expandIsland()
                            } else {
                                IslandStateManager.collapseIsland()
                            }
                        }
                        onDragFinished()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumSwipeX += dragAmount.x
                        accumSwipeY += dragAmount.y
                        if (config.isDraggable && runtimeState.displayMode == IslandDisplayMode.COMPACT) {
                            val dxDp = with(density) { dragAmount.x.toDp().value }
                            val dyDp = with(density) { dragAmount.y.toDp().value }
                            onDragDeltaDp(dxDp, dyDp)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(animatedCornerRadius),
        color = Color.Transparent,
        border = BorderStroke(
            width = (0.8f + config.borderIntensity * 1.2f).dp,
            brush = borderBrush
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceBrush)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = Pair(runtimeState.displayMode, runtimeState.primaryActivity),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "island_mode_content"
            ) { (mode, activity) ->
                when (mode) {
                    IslandDisplayMode.MINIMAL -> MinimalIslandContent(
                        activity = activity,
                        accentColor = accentColor,
                        batterySaver = config.batteryFriendlyMode
                    )
                    IslandDisplayMode.COMPACT -> CompactIslandContent(
                        activity = activity,
                        secondaryActivity = runtimeState.secondaryActivity,
                        queueSize = runtimeState.activeQueue.size,
                        accentColor = accentColor,
                        batterySaver = config.batteryFriendlyMode
                    )
                    IslandDisplayMode.EXPANDED -> ExpandedIslandContent(
                        activity = activity,
                        runtimeState = runtimeState,
                        accentColor = accentColor,
                        batterySaver = config.batteryFriendlyMode
                    )
                    IslandDisplayMode.CONTROL_PANEL -> IslandQuickControlPanel(
                        runtimeState = runtimeState,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberIslandVisualPalette(
    config: IslandConfiguration
): Triple<Brush, Brush, Color> {
    val alpha = config.transparencyAlpha.coerceIn(0.65f, 1.0f)
    val borderAlpha = (config.borderIntensity * 0.75f).coerceIn(0.08f, 0.85f)
    val customAccent = Color(config.customAccentHex)

    return when (config.themeStyle) {
        IslandThemeStyle.LIQUID_GLASS -> Triple(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0E131F).copy(alpha = alpha),
                    Color(0xFF06080E).copy(alpha = (alpha + 0.04f).coerceAtMost(1f))
                )
            ),
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF94A3B8).copy(alpha = borderAlpha),
                    Color(0xFF38BDF8).copy(alpha = borderAlpha * 0.65f),
                    Color(0xFF1E293B).copy(alpha = borderAlpha * 0.4f)
                )
            ),
            Color(0xFF38BDF8)
        )

        IslandThemeStyle.PREMIUM_DARK -> Triple(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF111318).copy(alpha = alpha),
                    Color(0xFF07080B).copy(alpha = 0.98f)
                )
            ),
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE2E8F0).copy(alpha = borderAlpha * 0.7f),
                    Color(0xFF475569).copy(alpha = borderAlpha * 0.35f)
                )
            ),
            Color(0xFFA5B4FC)
        )

        IslandThemeStyle.CLASSIC_IOS -> Triple(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF040406).copy(alpha = 0.98f),
                    Color(0xFF000000).copy(alpha = 1.0f)
                )
            ),
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF27272A).copy(alpha = borderAlpha * 0.6f),
                    Color(0xFF18181B).copy(alpha = borderAlpha * 0.3f)
                )
            ),
            Color(0xFF60A5FA)
        )

        IslandThemeStyle.MINIMAL_BLACK -> Triple(
            Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.98f),
                    Color.Black
                )
            ),
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = borderAlpha * 0.3f),
                    Color(0xFF0F172A).copy(alpha = borderAlpha * 0.15f)
                )
            ),
            Color(0xFF94A3B8)
        )

        IslandThemeStyle.CUSTOM -> Triple(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0D111A).copy(alpha = alpha),
                    Color(0xFF05070B).copy(alpha = (alpha + 0.04f).coerceAtMost(1f))
                )
            ),
            Brush.linearGradient(
                colors = listOf(
                    customAccent.copy(alpha = borderAlpha),
                    Color(0xFF334155).copy(alpha = borderAlpha * 0.45f)
                )
            ),
            customAccent
        )
    }
}

private fun handleDoubleTap(context: Context, action: DoubleTapAction) {
    when (action) {
        DoubleTapAction.PLAY_PAUSE_MEDIA -> IslandStateManager.triggerPlayPause()
        DoubleTapAction.DISMISS_CURRENT -> {
            val primary = IslandStateManager.state.value.primaryActivity
            if (primary is IslandActivityPayload.NotificationPayload) {
                IslandStateManager.dismissNotification(primary.notification.id)
            } else {
                IslandStateManager.collapseIsland()
            }
        }
        DoubleTapAction.TOGGLE_TIMER -> IslandStateManager.toggleTimerPauseResume()
        DoubleTapAction.OPEN_APP -> {
            IslandStateManager.openMainAppAction?.invoke() ?: run {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launchIntent != null) {
                    runCatching { context.startActivity(launchIntent) }
                }
            }
        }
        DoubleTapAction.TOGGLE_FLASHLIGHT -> {
            IslandStateManager.flashlightToggleAction?.invoke()
        }
    }
}

@Composable
private fun MinimalIslandContent(
    activity: IslandActivityPayload,
    accentColor: Color,
    batterySaver: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .testTag("island_minimal_content"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when (activity) {
                        is IslandActivityPayload.CallPayload -> Color(0xFF22C55E)
                        is IslandActivityPayload.MusicPayload -> accentColor
                        is IslandActivityPayload.NotificationPayload -> Color(activity.notification.category.accentColorHex)
                        is IslandActivityPayload.TimerPayload -> Color(0xFFF59E0B)
                        is IslandActivityPayload.BatteryPayload -> Color(0xFF4ADE80)
                        else -> accentColor.copy(alpha = 0.65f)
                    }
                )
        )
        if (activity is IslandActivityPayload.MusicPayload && activity.mediaState.isPlaying) {
            AnimatedEqualizerBars(
                isPlaying = true,
                barColor = accentColor,
                barCount = 3,
                maxHeight = 12.dp,
                batterySaver = batterySaver
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF475569))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactIslandContent(
    activity: IslandActivityPayload,
    secondaryActivity: IslandActivityPayload?,
    queueSize: Int,
    accentColor: Color,
    batterySaver: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .testTag("island_compact_content"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (activity) {
            is IslandActivityPayload.MusicPayload -> {
                val media = activity.mediaState
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AlbumArtThumbnail(
                        mediaState = media,
                        size = 24.dp,
                        modifier = Modifier.testTag("compact_album_art")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${media.title} • ${media.artist}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                            .testTag("compact_song_title")
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedEqualizerBars(
                    isPlaying = media.isPlaying,
                    barColor = accentColor,
                    barCount = 4,
                    maxHeight = 14.dp,
                    batterySaver = batterySaver
                )
            }

            is IslandActivityPayload.NotificationPayload -> {
                val notif = activity.notification
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(notif.category.accentColorHex).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = notif.appName,
                            tint = Color(notif.category.accentColorHex),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${notif.senderName}: ${notif.messagePreview}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compact_notification_text")
                    )
                }
                if (queueSize > 1 || notif.groupCount > 1) {
                    val count = maxOf(queueSize, notif.groupCount)
                    Text(
                        text = "+$count",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            is IslandActivityPayload.CallPayload -> {
                val call = activity.callState
                val isIncoming = call.status == CallStatus.INCOMING
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Phone Call",
                        tint = if (isIncoming) Color(0xFF38BDF8) else Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = call.callerName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (isIncoming) "RINGING" else formatDurationSeconds(call.durationSeconds),
                    color = if (isIncoming) Color(0xFF38BDF8) else Color(0xFF22C55E),
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            is IslandActivityPayload.TimerPayload -> {
                val timer = activity.timerState
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timer.label,
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatDurationMs(timer.remainingOrElapsedMs),
                    color = Color(0xFFF59E0B),
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("compact_timer_value")
                )
            }

            is IslandActivityPayload.BatteryPayload -> {
                val bat = activity.batteryState
                val tint = when {
                    bat.isLow -> Color(0xFFEF4444)
                    bat.isCharging -> Color(0xFF4ADE80)
                    else -> Color.White
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (bat.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        contentDescription = "Battery",
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bat.isCharging) "Charging" else "Battery",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${bat.percentage}%",
                    color = tint,
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("compact_battery_percent")
                )
            }

            is IslandActivityPayload.ConnectivityPayload -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (activity.connectivityState.isFlashlightOn) {
                            Icons.Default.FlashlightOn
                        } else if (activity.connectivityState.isBluetoothConnected) {
                            Icons.Default.BluetoothConnected
                        } else {
                            Icons.Default.Wifi
                        },
                        contentDescription = "System Status",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activity.headline,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IslandActivityPayload.IdlePayload -> {
                // Sleek lens & status pill when idle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LEOVAN",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.8f))
                    )
                }
            }
        }

        // Secondary multi-activity pill indicator if another activity is queued behind primary
        if (secondaryActivity != null && activity !is IslandActivityPayload.IdlePayload) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                val secIcon = when (secondaryActivity) {
                    is IslandActivityPayload.MusicPayload -> Icons.Default.MusicNote
                    is IslandActivityPayload.TimerPayload -> Icons.Default.Timer
                    is IslandActivityPayload.NotificationPayload -> Icons.Default.Notifications
                    else -> Icons.Default.BatteryFull
                }
                Icon(
                    imageVector = secIcon,
                    contentDescription = "Queued Activity",
                    tint = accentColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedIslandContent(
    activity: IslandActivityPayload,
    runtimeState: IslandRuntimeState,
    accentColor: Color,
    batterySaver: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("island_expanded_content"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (activity) {
            is IslandActivityPayload.MusicPayload -> ExpandedMusicPlayerModule(
                mediaState = activity.mediaState,
                accentColor = accentColor,
                batterySaver = batterySaver
            )

            is IslandActivityPayload.NotificationPayload -> ExpandedNotificationModule(
                notification = activity.notification,
                totalInQueue = runtimeState.notifications.size,
                accentColor = accentColor
            )

            is IslandActivityPayload.CallPayload -> ExpandedCallModule(
                callState = activity.callState
            )

            is IslandActivityPayload.TimerPayload -> ExpandedTimerModule(
                timerState = activity.timerState
            )

            is IslandActivityPayload.BatteryPayload -> ExpandedBatteryModule(
                batteryState = activity.batteryState
            )

            is IslandActivityPayload.ConnectivityPayload -> ExpandedConnectivityModule(
                connectivity = activity.connectivityState,
                headline = activity.headline,
                accentColor = accentColor
            )

            IslandActivityPayload.IdlePayload -> IslandQuickControlPanel(
                runtimeState = runtimeState,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun ExpandedMusicPlayerModule(
    mediaState: MediaSessionState,
    accentColor: Color,
    batterySaver: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_music_player"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtThumbnail(
                mediaState = mediaState,
                size = 54.dp,
                modifier = Modifier.testTag("expanded_album_art")
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaState.appName.uppercase(Locale.getDefault()),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mediaState.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("expanded_song_title")
                )
                Text(
                    text = mediaState.artist,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("expanded_song_artist")
                )
            }
            AnimatedEqualizerBars(
                isPlaying = mediaState.isPlaying,
                barColor = accentColor,
                barCount = 4,
                maxHeight = 20.dp,
                batterySaver = batterySaver
            )
        }

        // Progress Slider & Timestamps
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = mediaState.progressFraction,
                onValueChange = { fraction ->
                    if (mediaState.durationMs > 0L) {
                        IslandStateManager.triggerSeekTo((fraction * mediaState.durationMs).toLong())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .testTag("music_progress_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMs(mediaState.positionMs),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Text(
                    text = formatDurationMs(mediaState.durationMs),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        // Media Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { IslandStateManager.triggerPreviousTrack() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("music_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.22f),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = { IslandStateManager.triggerPlayPause() },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("music_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            IconButton(
                onClick = { IslandStateManager.triggerNextTrack() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("music_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedNotificationModule(
    notification: IslandNotificationItem,
    totalInQueue: Int,
    accentColor: Color
) {
    val context = LocalContext.current
    val catColor = Color(notification.category.accentColorHex)
    val timeFormatted = remember(notification.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_notification_panel"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = notification.appName,
                        tint = catColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = notification.appName,
                        color = catColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = notification.senderName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("expanded_notification_sender")
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (totalInQueue > 1) {
                    Text(
                        text = "Queue ($totalInQueue)",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = timeFormatted,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Text(
            text = notification.messagePreview,
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("expanded_notification_message")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("notification_open_button")
                    .clickable {
                        try {
                            if (notification.contentIntent != null) {
                                notification.contentIntent.send()
                            } else {
                                val launch = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (launch != null) {
                                    context.startActivity(launch)
                                }
                            }
                        } catch (_: Exception) {
                        }
                        IslandStateManager.dismissNotification(notification.id)
                    },
                shape = RoundedCornerShape(12.dp),
                color = catColor.copy(alpha = 0.25f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Open ${notification.appName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .width(96.dp)
                    .height(38.dp)
                    .testTag("notification_dismiss_button")
                    .clickable {
                        IslandStateManager.dismissNotification(notification.id)
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Dismiss",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedCallModule(callState: CallActivityState) {
    val isIncoming = callState.status == CallStatus.INCOMING
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_call_panel"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncoming) Color(0xFF38BDF8).copy(alpha = 0.2f)
                            else Color(0xFF22C55E).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = if (isIncoming) Color(0xFF38BDF8) else Color(0xFF22C55E)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isIncoming) "INCOMING CALL" else "ONGOING CALL",
                        color = if (isIncoming) Color(0xFF38BDF8) else Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = callState.callerName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (callState.callerNumber.isNotBlank()) {
                        Text(
                            text = callState.callerNumber,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text(
                text = if (isIncoming) "RINGING" else formatDurationSeconds(callState.durationSeconds),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clickable { IslandStateManager.collapseIsland() },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Minimize to Pill",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .width(96.dp)
                    .height(36.dp)
                    .clickable {
                        IslandStateManager.updateCallState(CallActivityState(status = CallStatus.NONE))
                    },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.25f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hide Call Indicator",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTimerModule(timerState: TimerActivityState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_timer_panel"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = timerState.label.uppercase(Locale.getDefault()),
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (timerState.isCompleted) "Time's Up!" else if (timerState.isRunning) "Running" else "Paused",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = formatDurationMs(timerState.remainingOrElapsedMs),
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("expanded_timer_value")
            )
        }

        LinearProgressIndicator(
            progress = { timerState.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFF59E0B),
            trackColor = Color(0xFF1E293B)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("timer_pause_resume_button")
                    .clickable { IslandStateManager.toggleTimerPauseResume() },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.25f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause/Resume Timer",
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (timerState.isRunning) "Pause" else "Resume",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .width(96.dp)
                    .height(36.dp)
                    .testTag("timer_stop_button")
                    .clickable { IslandStateManager.stopAndResetTimer() },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Timer",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stop",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedBatteryModule(batteryState: BatteryActivityState) {
    val accent = when {
        batteryState.isLow -> Color(0xFFEF4444)
        batteryState.isCharging -> Color(0xFF4ADE80)
        else -> Color(0xFF38BDF8)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_battery_panel"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (batteryState.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            batteryState.isFull -> "FULLY CHARGED"
                            batteryState.isCharging -> "CHARGING ACTIVE"
                            batteryState.isLow -> "LOW BATTERY ALERT"
                            else -> "BATTERY STATUS"
                        },
                        color = accent,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${batteryState.powerSource} • ${batteryState.temperatureCelsius}°C",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "${batteryState.percentage}%",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
        }

        LinearProgressIndicator(
            progress = { (batteryState.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accent,
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun ExpandedConnectivityModule(
    connectivity: ConnectivityActivityState,
    headline: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expanded_connectivity_panel"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE SYSTEM TELEMETRY",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = headline,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Vol ${connectivity.currentVolumePercent}%",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontFamily = JetBrainsMonoFontFamily
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = connectivity.networkLabel,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp
            )
            if (connectivity.nextAlarmFormatted != null) {
                Text(
                    text = "Alarm: ${connectivity.nextAlarmFormatted}",
                    color = Color(0xFFFDE68A),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun IslandQuickControlPanel(
    runtimeState: IslandRuntimeState,
    accentColor: Color
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("island_control_panel"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LEOVNISLAND QUICK HUB",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "System & Live Activity Controls",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(
                onClick = { IslandStateManager.collapseIsland() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Hub",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickHubActionButton(
                label = if (runtimeState.connectivityState.isFlashlightOn) "Torch On" else "Torch",
                icon = Icons.Default.FlashlightOn,
                active = runtimeState.connectivityState.isFlashlightOn,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { IslandStateManager.flashlightToggleAction?.invoke() }
            )
            QuickHubActionButton(
                label = if (runtimeState.timerState.isActive) "Timer On" else "5m Timer",
                icon = Icons.Default.Timer,
                active = runtimeState.timerState.isActive,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = {
                    if (runtimeState.timerState.isActive) {
                        IslandStateManager.toggleTimerPauseResume()
                    } else {
                        IslandStateManager.startCountdownTimer(300_000L, "Quick 5m Timer")
                    }
                }
            )
            QuickHubActionButton(
                label = "Minimal",
                icon = Icons.Default.Refresh,
                active = false,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { IslandStateManager.setMinimalMode() }
            )
            QuickHubActionButton(
                label = "Studio",
                icon = Icons.Default.Settings,
                active = false,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (launch != null) {
                        runCatching { context.startActivity(launch) }
                    }
                    IslandStateManager.collapseIsland()
                }
            )
        }
    }
}

@Composable
private fun QuickHubActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (active) accentColor.copy(alpha = 0.25f) else Color(0xFF161B26)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) accentColor else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (active) Color.White else Color(0xFF94A3B8),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AlbumArtThumbnail(
    mediaState: MediaSessionState,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bmp = mediaState.artwork
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Album Artwork",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape((size.value * 0.28f).dp))
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_default_album_art_1794620920059),
            contentDescription = "Default Album Artwork",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape((size.value * 0.28f).dp))
        )
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L)) / 1000L
    return formatDurationSeconds(totalSeconds)
}

fun formatDurationSeconds(totalSeconds: Long): String {
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
