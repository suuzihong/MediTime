package com.meditime.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meditime.app.data.DoseRecord
import com.meditime.app.data.Medicine
import com.meditime.app.data.TimeUtil
import com.meditime.app.reminder.NotificationHelper
import com.meditime.app.reminder.ReminderScheduler
import com.meditime.app.ui.screens.EditMedicineScreen
import com.meditime.app.ui.screens.HistoryScreen
import com.meditime.app.ui.screens.MedicinesScreen
import com.meditime.app.ui.screens.TodayScreen
import java.time.Instant
import java.time.ZoneId

private data class PermissionState(val notifications: Boolean, val exactAlarm: Boolean) {
    val allOk: Boolean get() = notifications && exactAlarm
}

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val medicine: Medicine) : EditorTarget
}

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val data by viewModel.data.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    var permissions by remember { mutableStateOf(readPermissionState(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = readPermissionState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions = readPermissionState(context) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val zone = ZoneId.systemDefault()
    val todayKey = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().let { TimeUtil.dateKey(it) }

    BackHandler(enabled = editor != null) { editor = null }

    when (val target = editor) {
        null -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                            label = { Text("今日") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                            label = { Text("药品") }
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Filled.List, contentDescription = null) },
                            label = { Text("记录") }
                        )
                    }
                },
                floatingActionButton = {
                    if (tab == 1) {
                        FloatingActionButton(
                            onClick = { editor = EditorTarget.New },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "添加药品")
                        }
                    }
                }
            ) { inner ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(inner)
                ) {
                    when (tab) {
                        0 -> TodayScreen(
                            medicines = data.medicines,
                            records = data.records,
                            nowMillis = now,
                            showPermissionHint = !permissions.allOk,
                            onOpenSettings = { showSettings = true },
                            onTake = { medicine, time ->
                                viewModel.markDose(medicine, todayKey, time, DoseRecord.STATUS_TAKEN)
                                NotificationHelper.cancel(context, medicine.id, time)
                            },
                            onSkip = { medicine, time ->
                                viewModel.markDose(medicine, todayKey, time, DoseRecord.STATUS_SKIPPED)
                                NotificationHelper.cancel(context, medicine.id, time)
                            },
                            onUndo = { viewModel.removeDose(it) },
                            onAddMedicine = { editor = EditorTarget.New }
                        )

                        1 -> MedicinesScreen(
                            medicines = data.medicines,
                            onEdit = { editor = EditorTarget.Edit(it) },
                            onAdd = { editor = EditorTarget.New },
                            onToggle = { medicine, enabled ->
                                viewModel.saveMedicine(medicine.copy(enabled = enabled))
                            },
                            onDelete = { viewModel.deleteMedicine(it.id) }
                        )

                        else -> HistoryScreen(
                            medicines = data.medicines,
                            records = data.records,
                            nowMillis = now
                        )
                    }
                }
            }
        }

        EditorTarget.New -> EditMedicineScreen(
            initial = null,
            onDismiss = { editor = null },
            onSave = {
                viewModel.saveMedicine(it)
                editor = null
            },
            onDelete = null,
            newId = viewModel::newId
        )

        is EditorTarget.Edit -> EditMedicineScreen(
            initial = target.medicine,
            onDismiss = { editor = null },
            onSave = {
                viewModel.saveMedicine(it)
                editor = null
            },
            onDelete = {
                viewModel.deleteMedicine(target.medicine.id)
                editor = null
            },
            newId = viewModel::newId
        )
    }

    if (showSettings) {
        SettingsDialog(
            permissions = permissions,
            onDismiss = { showSettings = false }
        )
    }
}

private fun readPermissionState(context: Context): PermissionState {
    val notifications = runCatching {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(true)
    val exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: true
        }.getOrDefault(true)
    } else {
        true
    }
    return PermissionState(notifications = notifications, exactAlarm = exactAlarm)
}

@Composable
private fun SettingsDialog(permissions: PermissionState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提醒设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionRow(
                    title = "通知提醒",
                    granted = permissions.notifications,
                    actionLabel = "去开启",
                    onClick = { openNotificationSettings(context) }
                )
                PermissionRow(
                    title = "精确闹钟",
                    granted = permissions.exactAlarm,
                    actionLabel = "去设置",
                    onClick = { openExactAlarmSettings(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "部分手机系统会限制后台应用。请在「应用信息」里允许自启动和后台活动，并关闭省电限制，否则提醒可能被系统拦截。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { openAppDetails(context) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("打开应用信息页")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "不确定提醒会不会响？发一条测试提醒看看。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        ReminderScheduler.testIn(context)
                        onDismiss()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("5 秒后发一条测试提醒")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (granted) "已开启" else "未开启",
                fontSize = 12.sp,
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        if (!granted) {
            TextButton(onClick = onClick) { Text(actionLabel) }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
