package com.meditime.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meditime.app.data.DoseRecord
import com.meditime.app.data.Medicine
import com.meditime.app.data.MediRepository
import com.meditime.app.data.TimeUtil
import com.meditime.app.ui.components.EmptyState
import com.meditime.app.ui.components.MedicineDot
import com.meditime.app.ui.components.ProgressRing
import com.meditime.app.ui.components.SectionHeader
import com.meditime.app.ui.theme.medicineColor
import java.time.Instant
import java.time.ZoneId

enum class DoseUiStatus { TAKEN, SKIPPED, DUE, PENDING, MISSED }

data class PlannedDose(
    val medicine: Medicine,
    val time: String,
    val record: DoseRecord?,
    val status: DoseUiStatus
)

private const val MISS_AFTER_MINUTES = 90

fun buildTodayDoses(
    medicines: List<Medicine>,
    records: List<DoseRecord>,
    todayKey: String,
    nowMinutes: Int
): List<PlannedDose> = medicines
    .filter { it.enabled }
    .flatMap { medicine ->
        medicine.times.map { time ->
            val record = records.firstOrNull {
                it.id == MediRepository.recordId(medicine.id, todayKey, time)
            }
            val minutes = TimeUtil.minutesOf(time)
            val status = when {
                record?.status == DoseRecord.STATUS_TAKEN -> DoseUiStatus.TAKEN
                record?.status == DoseRecord.STATUS_SKIPPED -> DoseUiStatus.SKIPPED
                nowMinutes > minutes + MISS_AFTER_MINUTES -> DoseUiStatus.MISSED
                nowMinutes >= minutes -> DoseUiStatus.DUE
                else -> DoseUiStatus.PENDING
            }
            PlannedDose(medicine, time, record, status)
        }
    }
    .sortedBy { TimeUtil.minutesOf(it.time) }

@Composable
fun TodayScreen(
    medicines: List<Medicine>,
    records: List<DoseRecord>,
    nowMillis: Long,
    showPermissionHint: Boolean,
    onOpenSettings: () -> Unit,
    onTake: (Medicine, String) -> Unit,
    onSkip: (Medicine, String) -> Unit,
    onUndo: (String) -> Unit,
    onAddMedicine: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val localNow = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val today = localNow.toLocalDate()
    val todayKey = TimeUtil.dateKey(today)
    val nowMinutes = localNow.hour * 60 + localNow.minute

    val doses = buildTodayDoses(medicines, records, todayKey, nowMinutes)
    val total = doses.size
    val taken = doses.count { it.status == DoseUiStatus.TAKEN }
    val skipped = doses.count { it.status == DoseUiStatus.SKIPPED }
    val missed = doses.count { it.status == DoseUiStatus.MISSED }
    val remaining = total - taken - skipped
    val progress = if (total == 0) 0f else taken.toFloat() / total.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "药时记",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = TimeUtil.prettyDate(today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "提醒设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SummaryCard(
                taken = taken,
                total = total,
                skipped = skipped,
                missed = missed,
                remaining = remaining,
                progress = progress
            )
        }

        if (showPermissionHint) {
            item {
                PermissionHintCard(onClick = onOpenSettings)
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(
                title = "今日服药",
                trailing = when {
                    total == 0 -> null
                    remaining > 0 -> "还剩 $remaining 次"
                    else -> "全部完成"
                }
            )
        }

        if (total == 0) {
            item {
                EmptyState(
                    icon = Icons.Filled.Favorite,
                    title = if (medicines.isEmpty()) "还没有添加药品" else "今天没有需要服的药",
                    description = if (medicines.isEmpty()) {
                        "添加第一种药并设定每日提醒时间，到点会收到通知。"
                    } else {
                        "当前没有启用的药品。可以在「药品」页开启或新增。"
                    },
                    actionLabel = if (medicines.isEmpty()) "添加第一种药" else null,
                    onAction = if (medicines.isEmpty()) onAddMedicine else null
                )
            }
        } else {
            items(doses, key = { "${it.medicine.id}@${it.time}" }) { dose ->
                DoseCard(
                    dose = dose,
                    onTake = { onTake(dose.medicine, dose.time) },
                    onSkip = { onSkip(dose.medicine, dose.time) },
                    onUndo = { dose.record?.let { onUndo(it.id) } }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    taken: Int,
    total: Int,
    skipped: Int,
    missed: Int,
    remaining: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = progress,
                size = 92.dp,
                strokeWidth = 9.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$taken",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "/ $total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        total == 0 -> "今天还没有服药计划"
                        remaining == 0 -> "今天的药都完成啦"
                        else -> "今天还需服 $remaining 次"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("已完成 ${(progress * 100).toInt()}%")
                        if (skipped > 0) append(" · 跳过 $skipped")
                        if (missed > 0) append(" · 逾期 $missed")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PermissionHintCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "提醒可能不会响",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "通知或精确闹钟权限没有打开，点这里检查",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DoseCard(
    dose: PlannedDose,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val takenColor = if (dark) Color(0xFF8BD996) else Color(0xFF2E7D32)
    val warnColor = if (dark) Color(0xFFFFB74D) else Color(0xFFC06A00)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val color = medicineColor(dose.medicine.colorIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.width(56.dp)) {
                Text(
                    text = dose.time,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusLabel(dose.status),
                    fontSize = 11.sp,
                    color = statusColor(dose.status, takenColor, warnColor, muted)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedicineDot(color = color, size = 7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dose.medicine.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val sub = buildString {
                    append(dose.medicine.dosage)
                    if (dose.medicine.note.isNotBlank()) append(" · ").append(dose.medicine.note)
                }
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            when (dose.status) {
                DoseUiStatus.TAKEN -> Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = dose.record?.let { "${TimeUtil.formatMillis(it.takenAt)} 已服" } ?: "已服",
                        fontSize = 11.sp,
                        color = takenColor,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = onUndo,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("撤销", fontSize = 11.sp, color = muted)
                    }
                }

                DoseUiStatus.SKIPPED -> TextButton(onClick = onUndo) {
                    Text("撤销", fontSize = 12.sp, color = muted)
                }

                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onTake,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("服了", fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("跳过", fontSize = 12.sp, color = muted)
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: DoseUiStatus): String = when (status) {
    DoseUiStatus.TAKEN -> "已服"
    DoseUiStatus.SKIPPED -> "跳过"
    DoseUiStatus.DUE -> "待服"
    DoseUiStatus.PENDING -> "待服"
    DoseUiStatus.MISSED -> "逾期"
}

private fun statusColor(
    status: DoseUiStatus,
    taken: Color,
    warn: Color,
    muted: Color
): Color = when (status) {
    DoseUiStatus.TAKEN -> taken
    DoseUiStatus.SKIPPED -> muted
    DoseUiStatus.MISSED -> warn
    DoseUiStatus.DUE -> Color(0xFF0B8578)
    DoseUiStatus.PENDING -> muted
}
