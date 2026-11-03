# 药时记（MediTime）

一个纯本地的吃药提醒 + 每日服药记录 Android App。

- Gitee: https://gitee.com/su_zihong/MediTime
- GitHub: https://github.com/suuzihong/MediTime

应用包名：`com.meditime.app`

## 功能

- **药品管理**：添加/编辑/删除药品，可设名称、每次剂量、备注、标识颜色、启用开关；一种药可以设多个每日提醒时间。
- **每日提醒**：用系统精确闹钟（`AlarmManager.setExactAndAllowWhileIdle`）在设定时间弹出高优先级通知，通知上可直接点「已服用」或「稍后 10 分钟」。
- **今日打卡**：今日页按时间列出所有待服项目，点「服了 / 跳过」，已服自动记录打卡时间；过点未服的标为「逾期」。
- **服药记录**：按天分组的历史打卡，近 7 天依从率、连续达标天数、累计服药次数。
- **开机恢复**：重启、应用更新、修改系统时间/时区后自动重排闹钟。
- **自检**：提醒设置里有「5 秒后发一条测试提醒」，用来确认系统是否拦截通知。

数据全部保存在本机（DataStore + JSON），不联网、不上传。

## 安装

从 [Releases](../../releases) 下载 APK 安装即可。首次启动时系统会询问通知权限，请选择允许。

## 提醒权限

部分国产定制系统（EMUI、MIUI、HyperOS 等）默认会限制后台应用，导致提醒不响。建议在系统设置里为「药时记」打开：

1. **通知**：允许通知、允许横幅和锁屏通知；
2. **权限**：允许「自启动」和「后台活动」；
3. **电池**：关闭「省电优化」（设为不限制）。

App 内「今日页右上角齿轮 → 提醒设置」可以跳转到这些页面、查看当前权限状态，也可以直接发一条测试提醒验证。

## 从源码构建

环境要求：

- JDK 17 或更高版本
- Android SDK：compileSdk 34、build-tools 34.0.0
- Gradle 8.9（或使用项目自带的 wrapper）

命令行构建：

```bash
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk
gradle assembleDebug
```

也可以直接用 Android Studio 打开项目，等待同步完成后点 Run。

说明：

- `local.properties` 由 Android Studio 或 Gradle 自动生成，用于指向本机 SDK，**不要提交到仓库**（已在 `.gitignore` 中排除）。
- Kotlin 编译默认使用 `in-process` 策略，避免在受限环境中启动编译守护进程失败。
- 首次构建需要联网下载依赖；依赖下载完成后可加 `--offline` 离线构建。

技术栈：Kotlin 2.0.20、Jetpack Compose（Material 3）、DataStore、kotlinx.serialization；构建配置 AGP 8.5.2 / Gradle 8.9 / compileSdk 34 / minSdk 26。

## 目录结构

```
MediTime/
├─ app/src/main/java/com/meditime/app/
│  ├─ MainActivity.kt           入口（edge-to-edge）
│  ├─ MediTimeApp.kt            Application：通知渠道、启动时排闹钟
│  ├─ data/
│  │  ├─ Models.kt              Medicine / DoseRecord / AppData
│  │  ├─ MediRepository.kt      DataStore + StateFlow，全部读写入口
│  │  └─ TimeUtil.kt            日期时间工具
│  ├─ reminder/
│  │  ├─ ReminderScheduler.kt   精确闹钟的排程/取消/稍后/测试
│  │  ├─ ReminderReceiver.kt    到点弹通知
│  │  ├─ NotificationActionReceiver.kt  通知上的按钮
│  │  ├─ BootReceiver.kt        开机/改时间后重排
│  │  └─ NotificationHelper.kt  通知构造
│  └─ ui/
│     ├─ MediTimeRoot.kt        导航、设置对话框、权限检查
│     ├─ AppViewModel.kt        状态与业务动作
│     ├─ screens/               今日 / 药品 / 记录 / 编辑
│     └─ theme/                 配色
└─ app/src/main/res/            图标、主题、字符串
```
