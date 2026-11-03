package com.meditime.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.meditime.app.data.Medicine
import com.meditime.app.data.TimeUtil

/**
 * 用 AlarmManager 的精确闹钟为每个「药品 × 时间」安排下一次提醒，
 * 触发后由 ReminderReceiver 重新排下一天，形成循环。
 */
object ReminderScheduler {
    private const val PREFS = "meditime_schedule"
    private const val KEY_SCHEDULED = "scheduled_keys"

    fun rescheduleAll(context: Context, medicines: List<Medicine>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()

        previous.forEach { key ->
            val separator = key.lastIndexOf('|')
            if (separator > 0) {
                val medicineId = key.substring(0, separator)
                val time = key.substring(separator + 1)
                runCatching { alarmManager.cancel(pendingIntent(context, medicineId, time)) }
            }
        }

        val scheduled = mutableSetOf<String>()
        medicines.filter { it.enabled }.forEach { medicine ->
            medicine.times.forEach { time ->
                scheduleAt(context, alarmManager, medicine.id, time, TimeUtil.nextTriggerMillis(time))
                scheduled += key(medicine.id, time)
            }
        }
        prefs.edit().putStringSet(KEY_SCHEDULED, scheduled).apply()
    }

    /** 提醒触发后（或稍后提醒后），重新安排该时间点的下一次触发 */
    fun scheduleNext(context: Context, medicineId: String, time: String) {
        scheduleNextAt(context, medicineId, time, TimeUtil.nextTriggerMillis(time))
    }

    fun snooze(context: Context, medicineId: String, time: String, minutes: Int = 10) {
        scheduleNextAt(
            context,
            medicineId,
            time,
            System.currentTimeMillis() + minutes * 60_000L
        )
    }

    /** 发一条几秒后的测试提醒，用来确认通知权限和后台限制是否放行 */
    fun testIn(context: Context, delayMillis: Long = 5_000L) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MEDICINE_ID, "test")
            putExtra(ReminderReceiver.EXTRA_TIME, "测试")
            putExtra(ReminderReceiver.EXTRA_TEST_NAME, "测试提醒")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            424242,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMillis
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.onFailure {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private fun scheduleNextAt(context: Context, medicineId: String, time: String, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        scheduleAt(context, alarmManager, medicineId, time, triggerAt)
    }

    private fun scheduleAt(
        context: Context,
        alarmManager: AlarmManager,
        medicineId: String,
        time: String,
        triggerAt: Long
    ) {
        val intents = pendingIntent(context, medicineId, time)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intents)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intents)
            }
        }.onFailure {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intents)
            }
        }
    }

    private fun pendingIntent(context: Context, medicineId: String, time: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MEDICINE_ID, medicineId)
            putExtra(ReminderReceiver.EXTRA_TIME, time)
        }
        return PendingIntent.getBroadcast(
            context,
            ("$medicineId@$time").hashCode() and 0x7FFFFFFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun key(medicineId: String, time: String): String = "$medicineId|$time"
}
