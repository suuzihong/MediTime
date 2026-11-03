package com.meditime.app

import android.app.Application
import com.meditime.app.data.MediRepository
import com.meditime.app.reminder.NotificationHelper
import com.meditime.app.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediTimeApp : Application() {
    val repository: MediRepository by lazy { MediRepository(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        appScope.launch {
            repository.awaitLoaded()
            ReminderScheduler.rescheduleAll(this@MediTimeApp, repository.data.value.medicines)
        }
    }
}
