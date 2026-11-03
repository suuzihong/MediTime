package com.meditime.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meditime.app.data.DoseRecord
import com.meditime.app.data.Medicine
import com.meditime.app.data.TimeUtil
import com.meditime.app.ui.components.EmptyState
import com.meditime.app.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayStat(val expected: Int, val taken: Int, val skipped: Int) {
    val remaining: Int get() = (expected - taken - skipped).coerceAtLeast(0)
}

private fun computeDayStat(
    date: LocalDate,
    medicines: List<Medicine>,
    records: List<DoseRecord>,
    zone: ZoneId
): DayStat {
    val dateKey = TimeUtil.dateKey(date)
    var expected = 0
    medicines.forEach { medicine ->
        if (!medicine.enabled) return@forEach
        val started = Instant.ofEpochMilli(medicine.createdAt)
            .atZone(zone)
            .toLocalDate()
        if (started.isAfter(date)) return@forEach
        expected += medicine.times.size
    }
    val todayRecords = records.filter { it.date == dateKey }
    return DayStat(
        expected = expected,
        taken = todayRecords.count { it.status == DoseRecord.STATUS_TAKEN },
        skipped = todayRecords.count { it.status == DoseRecord.STATUS_SKIPPED }
    )
}

@Composable
fun HistoryScreen(
    medicines: List<Medicine>,
    records: List<DoseRecord>,
    nowMillis: Long
) {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    val lastSeven = (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        day to computeDayStat(day, medicines, records, zone)
    }
    val expectedSum = lastSeven.sumOf { it.second.expected }
    val takenSum = lastSeven.sumOf { it.second.taken }
    val adherence = if (expectedSum == 0) 0f else takenSum.toFloat() / expectedSum

    var streak = 0
    var cursor = today.minusDays(1)
    var guard = 0
    while (guard < 365) {
        val stat = computeDayStat(cursor, medicines, records, zone)
        if (stat.expected == 0) break
        if (stat.taken >= stat.expected) {
            streak++
            cursor = cursor.minusDays(1)
            guard++
        } else {
            break
        }
    }
    val todayStat = computeDayStat(today, medicines, records, zone)
    if (todayStat.expected > 0 && todayStat.taken >= todayStat.expected) streak++

    val totalTaken = records.count { it.status == DoseRecord.STATUS_TAKEN }

    val grouped = records
        .groupBy { it.date }
        .toSortedMap(compareByDescending<String> { it })
        .map { (date, list) -> date to list.sortedBy { it.time } }
        .take(30)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "服药记录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "看看自己坚持得怎么样",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            StatsCard(
                adherence = adherence,
                expected = expectedSum,
                taken = takenSum,
                streak = streak,
                totalTaken = totalTaken
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(
                title = "历史打卡",
                trailing = if (grouped.isEmpty()) null else "共 ${grouped.size} 天"
            )
        }

        if (grouped.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = "还没有记录",
                    description = "在「今日」页点「服了」，记录就会出现在这里。"
                )
            }
        } else {
            items(grouped, key = { it.first }) { (date, list) ->
                val stat = computeDayStat(TimeUtil.parseDate(date), medicines, records, zone)
                DayGroupCard(date = date, records = list, stat = stat)
            }
        }
    }
}

@Composable
private fun StatsCard(
    adherence: Float,
    expected: Int,
    taken: Int,
    streak: Int,
    totalTaken: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "近 7 天依从率",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(adherence * 100).toInt()}%",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$taken / $expected 次",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "应服药次数",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                LinearProgressIndicator(
                    progress = { adherence.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                StatCell(
                    value = "$streak",
                    label = "连续达标天数",
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    value = "$totalTaken",
                    label = "累计服药次数",
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    value = "$taken",
                    label = "近 7 天有效打卡",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayGroupCard(date: String, records: List<DoseRecord>, stat: DayStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = TimeUtil.prettyDate(date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (stat.expected > 0) {
                    Text(
                        text = "已服 ${stat.taken}/${stat.expected}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            records.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.time,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp)
                    )
                    Text(
                        text = record.medicineName,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (record.status == DoseRecord.STATUS_TAKEN) "已服" else "跳过",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (record.status == DoseRecord.STATUS_TAKEN) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (record.status == DoseRecord.STATUS_TAKEN) {
                        Text(
                            text = "  ${TimeUtil.formatMillis(record.takenAt)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
