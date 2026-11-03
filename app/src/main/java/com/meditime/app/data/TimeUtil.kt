package com.meditime.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtil {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val prettyFormat = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

    fun today(): String = LocalDate.now().format(dateFormat)

    fun dateKey(date: LocalDate): String = date.format(dateFormat)

    fun parseDate(value: String): LocalDate = LocalDate.parse(value, dateFormat)

    fun minutesOf(time: String): Int {
        val parsed = LocalTime.parse(time)
        return parsed.hour * 60 + parsed.minute
    }

    fun nowMinutes(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    /** 下一次该时刻的绝对毫秒数：今天没到就今天，否则明天 */
    fun nextTriggerMillis(time: String): Long {
        val now = ZonedDateTime.now()
        val targetTime = LocalTime.parse(time)
        var target = now.with(targetTime).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    fun nowMillis(): Long = Instant.now().toEpochMilli()

    /** 2026-10-25 -> 10月25日 星期六 */
    fun prettyDate(date: String): String =
        runCatching { parseDate(date).format(prettyFormat) }.getOrDefault(date)

    fun prettyDate(date: LocalDate): String = date.format(prettyFormat)

    fun formatMillis(millis: Long): String {
        val local = Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        return String.format(Locale.CHINA, "%02d:%02d", local.hour, local.minute)
    }

    fun weekdayShort(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE", Locale.CHINA))
}
