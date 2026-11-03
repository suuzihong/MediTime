package com.meditime.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditime.app.MediTimeApp
import com.meditime.app.data.DoseRecord
import com.meditime.app.data.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 通知上的「已服用」「稍后 10 分钟」按钮 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(EXTRA_MEDICINE_ID) ?: return
        val time = intent.getStringExtra(EXTRA_TIME) ?: return
        val app = context.applicationContext as? MediTimeApp ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TAKEN -> {
                        val name = intent.getStringExtra(EXTRA_MEDICINE_NAME)
                            ?: app.repository.medicineById(medicineId)?.name
                            ?: "药品"
                        val date = intent.getStringExtra(EXTRA_DATE) ?: TimeUtil.today()
                        app.repository.markDose(
                            medicineId = medicineId,
                            medicineName = name,
                            date = date,
                            time = time,
                            status = DoseRecord.STATUS_TAKEN
                        )
                        NotificationHelper.cancel(context, medicineId, time)
                    }

                    ACTION_SNOOZE -> {
                        ReminderScheduler.snooze(context, medicineId, time, 10)
                        NotificationHelper.cancel(context, medicineId, time)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.meditime.app.action.TAKEN"
        const val ACTION_SNOOZE = "com.meditime.app.action.SNOOZE"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DATE = "extra_date"
    }
}
