package com.example.cloud

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.CloudNotificationItem

object AppNotificationHelper {

    private const val CHANNEL_ID = "lavelle_admin_channel"
    private const val CHANNEL_NAME = "Lavelle Xabarlari"
    private const val PREFS_SEEN = "lavelle_seen_notifications"
    private const val KEY_SEEN_IDS = "seen_ids"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Lavelle admin e'lonlari va yangi shablon xabarlari"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showUnseenNotifications(context: Context, items: List<CloudNotificationItem>) {
        if (items.isEmpty()) return
        ensureChannel(context)

        val prefs = context.getSharedPreferences(PREFS_SEEN, Context.MODE_PRIVATE)
        val seenSet = prefs.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        val unseen = items.filter { it.id.isNotBlank() && !seenSet.contains(it.id) }
        if (unseen.isEmpty()) return

        if (hasNotificationPermission(context)) {
            unseen.sortedBy { it.sentAt }.forEachIndexed { idx, item ->
                postNotification(context, item, (item.id.hashCode() and 0x7FFFFFFF) + idx)
            }
        }

        unseen.forEach { seenSet.add(it.id) }
        prefs.edit().putStringSet(KEY_SEEN_IDS, seenSet).apply()
    }

    fun postNotification(context: Context, item: CloudNotificationItem, notificationId: Int) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_notification_id", item.id)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(item.title.ifBlank { "Lavelle Xabar" })
            .setContentText(item.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }
}
