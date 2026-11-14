package com.example.service

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.domain.model.IslandNotificationItem
import com.example.domain.model.SupportedAppCategory
import com.example.island.manager.IslandStateManager

/**
 * Smart NotificationListenerService supporting WhatsApp, Telegram, Instagram, Messenger,
 * SMS, Gmail, Discord, and all standard Android apps.
 * Respects private/redacted notifications and cleans up when notifications are dismissed.
 */
class IslandNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        IslandStateManager.setNotificationListenerConnected(true)
        MediaSessionService.instance?.refreshMediaSessions()
    }

    override fun onListenerDisconnected() {
        IslandStateManager.setNotificationListenerConnected(false)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notificationSbn = sbn ?: return
        val pkg = notificationSbn.packageName ?: return

        // Ignore our own foreground service notification
        if (pkg == packageName) return

        val notification = notificationSbn.notification ?: return

        // Ignore pure media transport notifications that are already tracked via MediaSession
        val isMediaStyle = notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
        if (isMediaStyle) {
            MediaSessionService.instance?.refreshMediaSessions()
            return
        }

        // Ignore silent ongoing summary headers that have no user-facing message content
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        val item = parseStatusBarNotification(notificationSbn) ?: return
        IslandStateManager.enqueueNotification(item)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val notificationSbn = sbn ?: return
        val id = notificationSbn.key ?: "${notificationSbn.packageName}_${notificationSbn.id}"
        IslandStateManager.dismissNotification(id)
    }

    fun parseStatusBarNotification(sbn: StatusBarNotification): IslandNotificationItem? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras
        val pkg = sbn.packageName.orEmpty()

        val isPrivateOrSecret = notification.visibility == Notification.VISIBILITY_PRIVATE ||
            notification.visibility == Notification.VISIBILITY_SECRET

        val appName = resolveAppLabel(pkg)
        val rawTitle = extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: appName

        val rawText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: ""

        if (rawTitle.isBlank() && rawText.isBlank()) return null

        val sender = if (isPrivateOrSecret) appName else rawTitle.ifBlank { appName }
        val preview = if (isPrivateOrSecret) {
            "Private notification content hidden"
        } else {
            rawText.ifBlank { rawTitle }
        }

        val largeIconBitmap: Bitmap? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val icon = notification.getLargeIcon()
                val drawable = icon?.loadDrawable(this)
                (drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        }.getOrNull()

        val priorityLevel = when {
            notification.category == Notification.CATEGORY_CALL -> 3
            notification.category == Notification.CATEGORY_MESSAGE -> 2
            notification.category == Notification.CATEGORY_ALARM -> 2
            else -> 1
        }

        val uniqueKey = sbn.key ?: "${pkg}_${sbn.id}"

        return IslandNotificationItem(
            id = uniqueKey,
            packageName = pkg,
            appName = appName,
            senderName = sender,
            title = rawTitle.ifBlank { appName },
            messagePreview = preview,
            timestamp = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis(),
            category = SupportedAppCategory.fromPackage(pkg),
            isPrivateRedacted = isPrivateOrSecret,
            groupKey = sbn.groupKey,
            groupCount = 1,
            priorityLevel = priorityLevel,
            contentIntent = notification.contentIntent,
            avatarBitmap = largeIconBitmap
        )
    }

    private fun resolveAppLabel(pkg: String): String {
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            SupportedAppCategory.fromPackage(pkg).displayName
        }
    }
}
