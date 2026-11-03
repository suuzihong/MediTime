package com.meditime.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.meditime.app.MainActivity
import com.meditime.app.R
import com.meditime.app.data.Medicine
import com.meditime.app.data.TimeUtil

object NotificationHelper {
    const val CHANNEL_ID = "dose_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 320, 180, 320)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun notificationId(medicineId: String, time: String): Int =
        ("$medicineId@$time").hashCode() and 0x7FFFFFFF

    fun showDoseReminder(
        context: Context,
        medicine: Medicine,
        time: String,
        withActions: Boolean = true
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val date = TimeUtil.today()
        val id = notificationId(medicine.id, time)

        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TODAY, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val detail = buildString {
            append(medicine.dosage)
            if (medicine.note.isNotBlank()) {
                append(" · ")
                append(medicine.note)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("该吃药了 · ${medicine.name}")
            .setContentText(detail)
            .setSubText("计划时间 $time")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$detail\n计划时间 $time，服完记得在 App 里打卡。")
            )

        if (withActions) {
            val takenIntent = PendingIntent.getBroadcast(
                context,
                id + 1,
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_TAKEN
                    putExtra(NotificationActionReceiver.EXTRA_MEDICINE_ID, medicine.id)
                    putExtra(NotificationActionReceiver.EXTRA_MEDICINE_NAME, medicine.name)
                    putExtra(NotificationActionReceiver.EXTRA_TIME, time)
                    putExtra(NotificationActionReceiver.EXTRA_DATE, date)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = PendingIntent.getBroadcast(
                context,
                id + 2,
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(NotificationActionReceiver.EXTRA_MEDICINE_ID, medicine.id)
                    putExtra(NotificationActionReceiver.EXTRA_TIME, time)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, "已服用", takenIntent)
            builder.addAction(0, "稍后 10 分钟", snoozeIntent)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }

    fun cancel(context: Context, medicineId: String, time: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationId(medicineId, time))
        }
    }
}
