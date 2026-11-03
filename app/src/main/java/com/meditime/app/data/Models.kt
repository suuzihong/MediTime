package com.meditime.app.data

import kotlinx.serialization.Serializable

/** 一种药及其每日提醒时间 */
@Serializable
data class Medicine(
    val id: String,
    val name: String,
    val dosage: String = "1 片",
    /** 每日提醒时间，格式 HH:mm，按时间升序保存 */
    val times: List<String> = listOf("08:00"),
    val note: String = "",
    /** 0..5，对应界面上的药品颜色 */
    val colorIndex: Int = 0,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/** 某一天、某个计划时间点的服药记录 */
@Serializable
data class DoseRecord(
    val id: String,
    val medicineId: String,
    val medicineName: String,
    /** yyyy-MM-dd */
    val date: String,
    /** HH:mm */
    val time: String,
    val status: String = STATUS_TAKEN,
    val takenAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_TAKEN = "TAKEN"
        const val STATUS_SKIPPED = "SKIPPED"
    }
}

@Serializable
data class AppData(
    val medicines: List<Medicine> = emptyList(),
    val records: List<DoseRecord> = emptyList()
)
