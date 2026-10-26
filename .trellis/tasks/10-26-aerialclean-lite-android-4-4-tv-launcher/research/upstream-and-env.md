# 上游约束与本机环境（2026-10-26 实测）

## aerial-tv

- 仓库：https://github.com/pengchujin/aerial-tv（MIT）
- `app/build.gradle.kts`：`compileSdk 36`、`minSdk 21`、`targetSdk 34`、JDK 17、`coreLibraryDesugaring`、Compose BOM 2025.11.01（Compose 1.9.5）、`media3-*:1.8.1`、nanohttpd、zxing。
- 源码规模：66 个 `.kt` / 17,500 行；`ui/` 23 文件 / 7,070 行；`MainActivity.kt` 727 行；`apps/` 2,255 行；`guard/` 1,903 行；`system/` 1,999 行；`wallpaper/` 1,863 行。
- 34 个文件直接 import `androidx.compose`。
- 构建文件注释自述："Compose 1.10 and later need 6.0, so the Compose BOM below stays on 1.9" 与 "Media3 1.8 is the last that runs on Android 5.0"。即：API 21 已是上游刻意维护的地板，再往下的路不存在。
- README 明确：不支持 Android 4.4 及更老系统。

## cleanyourtv

- 仓库：https://github.com/Gathin23/cleanyourtv（MIT）
- `launcher/app/build.gradle`：纯 Java、`minSdk 26`、`compileSdk 34`、`androidx.recyclerview:1.3.2`、`androidx.dynamicanimation:1.0.0`。
- launcher 规模：106 个 `.java` / 2,922 行。
- `AGENTS.md`：`minSdk 26` 不许降；"A whole new launcher … a lower minSdk" 属于 fork 到自己的仓库。
- `tools/disable.sh`：`pm disable-user --user 0`、核心包白名单 `CORE_RE`、Home guard（`cmd package resolve-activity` / `query-activities`，脚本注释注明是 Android 7 加入的）、setupwraith 保护、ADB 记录。
- `tools/restore.sh` + `recovery.py`：按记录还原，`pm enable`。
- `launcher/build-and-install.sh`：先读 Gradle 的 minSdk 预检、`am start -W` 验证、再禁用原桌面，支持 `STOCK_LAUNCHER=none`。
- 上游覆盖机型最低 Android 5.1；文档中多次出现"换桌面需要 Android 8.0"的告警。

## 4.4 上可用性

| 能力 | 4.4 | 依据 |
|---|---|---|
| `pm disable-user --user 0` | 可用 | 上游脚本与 Android 4.2+ 行为 |
| `pm grant` | 不可用 | API 23 引入 |
| `cmd package resolve-activity` | 不可用 | 上游注释：Android 7 加入 |
| `settings put secure enabled_accessibility_services` | 可用 | 上游在用 |
| `cmd notification allow_listener` | 不可用 | 7.0 引入；替代为 `settings put secure enabled_notification_listeners` |
| `appops set … SYSTEM_ALERT_WINDOW` | 不需要 | 6.0 才引入运行时授予；4.4 安装即授予 |
| `dumpsys window \| grep mCurrentFocus` | 可用 | ICS 起存在 |
| `CATEGORY_HOME` 第三方接管 | 可用 | 厂商是否硬编码因机型而异 |
| `DreamService` | 可用（17+） | 系统 Daydream 入口部分机型缺失 |
| `NotificationListenerService` | 可用（18+） | 本项目 v1 不用 |

## 本机环境

- `java`、`gradle`、`adb` 均不在 PATH；`JAVA_HOME` 空；`C:\Program Files\Java`、Eclipse Adoptium、Android Studio `jbr` 均不存在。
- Android SDK：`C:\Users\fenglei\AppData\Local\Android\Sdk`，含 `platforms 31/34/35/36`、`build-tools 30.0.3/34.0.0/35.0.0/35.0.1/36.0.0`、`platform-tools/adb.exe`。
- `system-images`：`android-21;android-tv;x86`、`android-36;google_apis_playstore;x86_64`。无 API 19 镜像，无 `cmdline-tools`（因此也不能用 `sdkmanager` 现场装 19）。
- 结论：**不装 JDK 17 就无法产出 APK**；不接 4.4 真机就无法得到 4.4 运行结论。
