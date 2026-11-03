package com.meditime.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meditime.app.MediTimeApp
import com.meditime.app.data.AppData
import com.meditime.app.data.Medicine
import com.meditime.app.reminder.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as MediTimeApp).repository

    val data: StateFlow<AppData> = repo.data

    /** 每 20 秒刷新一次，让「已到点 / 已逾期」状态自己更新 */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _now.value = System.currentTimeMillis()
                delay(20_000)
            }
        }
        viewModelScope.launch {
            repo.data.map { it.medicines }.distinctUntilChanged().collect { medicines ->
                ReminderScheduler.rescheduleAll(getApplication(), medicines)
            }
        }
    }

    fun newId(): String = repo.newId()

    fun saveMedicine(medicine: Medicine) {
        viewModelScope.launch { repo.upsertMedicine(medicine) }
    }

    fun deleteMedicine(id: String) {
        viewModelScope.launch { repo.deleteMedicine(id) }
    }

    fun markDose(medicine: Medicine, date: String, time: String, status: String) {
        viewModelScope.launch {
            repo.markDose(medicine.id, medicine.name, date, time, status)
        }
    }

    fun removeDose(recordId: String) {
        viewModelScope.launch { repo.removeDose(recordId) }
    }
}
