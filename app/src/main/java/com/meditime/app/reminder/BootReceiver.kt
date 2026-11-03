package com.meditime.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditime.app.MediTimeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 重启、应用更新、改时间/时区后，闹钟会被系统清空，这里重新排一遍 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val app = context.applicationContext as? MediTimeApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.awaitLoaded()
                ReminderScheduler.rescheduleAll(context, app.repository.data.value.medicines)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
