package com.meditime.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.mediDataStore by preferencesDataStore(name = "meditime_data")

/**
 * 全部数据以 JSON 存在 DataStore 里，内存中用 StateFlow 供界面订阅。
 * 数据量很小（几十种药 + 几年记录），整体读写足够快，也避免了数据库迁移成本。
 */
class MediRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val dataKey = stringPreferencesKey("app_data")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val loadJob = scope.launch { loadFromDisk() }

    private suspend fun loadFromDisk() {
        val raw = runCatching { appContext.mediDataStore.data.first()[dataKey] }.getOrNull()
        if (!raw.isNullOrBlank()) {
            runCatching { _data.value = json.decodeFromString<AppData>(raw) }
        }
    }

    suspend fun awaitLoaded() {
        loadJob.join()
    }

    private suspend fun persist(newData: AppData) {
        writeMutex.withLock {
            _data.value = newData
            runCatching { json.encodeToString(newData) }.getOrNull()?.let { encoded ->
                runCatching { appContext.mediDataStore.edit { it[dataKey] = encoded } }
            }
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    suspend fun upsertMedicine(medicine: Medicine) {
        awaitLoaded()
        val current = _data.value
        val exists = current.medicines.any { it.id == medicine.id }
        val medicines = if (exists) {
            current.medicines.map { if (it.id == medicine.id) medicine else it }
        } else {
            current.medicines + medicine
        }
        persist(current.copy(medicines = medicines.sortedBy { it.createdAt }))
    }

    suspend fun deleteMedicine(id: String) {
        awaitLoaded()
        val current = _data.value
        persist(current.copy(medicines = current.medicines.filterNot { it.id == id }))
    }

    suspend fun medicineById(id: String): Medicine? {
        awaitLoaded()
        return _data.value.medicines.find { it.id == id }
    }

    suspend fun markDose(
        medicineId: String,
        medicineName: String,
        date: String,
        time: String,
        status: String
    ) {
        awaitLoaded()
        val current = _data.value
        val recordId = recordId(medicineId, date, time)
        val record = DoseRecord(
            id = recordId,
            medicineId = medicineId,
            medicineName = medicineName,
            date = date,
            time = time,
            status = status,
            takenAt = System.currentTimeMillis()
        )
        persist(
            current.copy(
                records = (current.records.filterNot { it.id == recordId } + record)
                    .sortedBy { it.takenAt }
            )
        )
    }

    suspend fun removeDose(recordId: String) {
        awaitLoaded()
        val current = _data.value
        persist(current.copy(records = current.records.filterNot { it.id == recordId }))
    }

    suspend fun findRecord(date: String, medicineId: String, time: String): DoseRecord? {
        awaitLoaded()
        val id = recordId(medicineId, date, time)
        return _data.value.records.find { it.id == id }
    }

    companion object {
        fun recordId(medicineId: String, date: String, time: String): String =
            "$medicineId|$date|$time"
    }
}
