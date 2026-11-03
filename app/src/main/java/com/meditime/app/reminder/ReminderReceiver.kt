package com.meditime.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditime.app.MediTimeApp
import com.meditime.app.data.Medicine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 闹钟到点：弹出通知，并把下一次提醒排进闹钟 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(EXTRA_MEDICINE_ID) ?: return
        val time = intent.getStringExtra(EXTRA_TIME) ?: return
        val testName = intent.getStringExtra(EXTRA_TEST_NAME)
        val app = context.applicationContext as? MediTimeApp ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (testName != null) {
                    // 测试提醒：不需要数据库里的药品，也不排下一次
                    val test = Medicine(
                        id = medicineId,
                        name = testName,
                        dosage = "这是一条测试通知",
                        times = listOf(time),
                        enabled = true
                    )
                    NotificationHelper.showDoseReminder(
                        context = context,
                        medicine = test,
                        time = time,
                        withActions = false
                    )
                } else {
                    val medicine = app.repository.medicineById(medicineId)
                    if (medicine != null && medicine.enabled) {
                        NotificationHelper.showDoseReminder(context, medicine, time)
                        ReminderScheduler.scheduleNext(context, medicineId, time)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TEST_NAME = "extra_test_name"
    }
}
