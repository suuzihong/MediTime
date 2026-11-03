package com.meditime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.meditime.app.data.Medicine
import com.meditime.app.data.TimeUtil
import com.meditime.app.ui.theme.MedicinePalette
import com.meditime.app.ui.theme.medicineColor
import java.util.Locale

private data class TimeEdit(val index: Int, val initial: String)

@Composable
fun EditMedicineScreen(
    initial: Medicine?,
    onDismiss: () -> Unit,
    onSave: (Medicine) -> Unit,
    onDelete: (() -> Unit)?,
    newId: () -> String
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var dosage by remember { mutableStateOf(initial?.dosage ?: "1 片") }
    var times by remember { mutableStateOf(initial?.times ?: listOf("08:00")) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var colorIndex by remember { mutableIntStateOf(initial?.colorIndex ?: 0) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var timeEdit by remember { mutableStateOf<TimeEdit?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && times.isNotEmpty()
    val accent = medicineColor(colorIndex)

    fun save() {
        if (!canSave) return
        onSave(
            Medicine(
                id = initial?.id ?: newId(),
                name = name.trim(),
                dosage = dosage.trim().ifBlank { "1 片" },
                times = times.distinct().sortedBy { TimeUtil.minutesOf(it) },
                note = note.trim(),
                colorIndex = colorIndex,
                enabled = enabled,
                createdAt = initial?.createdAt ?: System.currentTimeMillis()
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = if (initial == null) "添加药品" else "编辑药品",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { save() }, enabled = canSave) {
                Text("保存", fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("药品名称") },
                placeholder = { Text("例如：维生素 D") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("每次剂量") },
                placeholder = { Text("例如：1 片 / 5 ml") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text(
                    text = "每日提醒时间",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    times.forEachIndexed { index, time ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = time,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accent,
                                    modifier = Modifier.clickable {
                                        timeEdit = TimeEdit(index, time)
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        if (times.size > 1) {
                                            times = times.filterIndexed { i, _ -> i != index }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "删除时间",
                                        tint = accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable {
                            timeEdit = TimeEdit(-1, "12:00")
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("添加时间", fontSize = 13.sp)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                placeholder = { Text("例如：饭后服用") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text(
                    text = "标识颜色",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MedicinePalette.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (index == colorIndex) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { colorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == colorIndex) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("启用提醒", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "关闭后不再提醒，记录仍保留",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }

            if (onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除这个药品", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    timeEdit?.let { edit ->
        TimePickerDialog(
            initial = edit.initial,
            onDismiss = { timeEdit = null },
            onConfirm = { value ->
                times = if (edit.index < 0) {
                    (times + value)
                } else {
                    times.mapIndexed { i, t -> if (i == edit.index) value else t }
                }
                timeEdit = null
            }
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除「${initial?.name ?: name}」？") },
            text = { Text("删除后不再提醒，已记录的历史打卡仍会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initial.split(":")
    val state = rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8,
        initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "选择提醒时间",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = {
                        onConfirm(
                            String.format(Locale.CHINA, "%02d:%02d", state.hour, state.minute)
                        )
                    }) { Text("确定") }
                }
            }
        }
    }
}
