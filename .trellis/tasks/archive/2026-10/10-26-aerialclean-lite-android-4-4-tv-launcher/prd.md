# AerialClean Lite：Android 4.4 电视桌面

## Goal

从零实现一个能在 Android 4.4（API 19）电视/盒子上接管 Home 的桌面 App，交互与视觉意图取自 aerial-tv（底部 dock、下方网格、单层焦点、时钟、屏保），实现方式回到原生 View。

它是本仓安装器（任务 02）要安装的那个 App，也是 4.4 设备唯一可用的桌面方案——上游 aerial-tv 与 cleanyourtv launcher 分别止步于 API 21 和 26。

## Background

- `aerial-tv`：Kotlin + Compose 1.9.5 + Media3 1.8.1，`minSdk 21`，66 个 Kotlin 文件 / 17,500 行；`ui/` 7,070 行全是 Compose，`MainActivity` 727 行也依赖 Compose。API 19 上无法沿用。
- `cleanyourtv` launcher：纯 Java + RecyclerView，`minSdk 26`，106 文件 / 2,922 行；其 `AGENTS.md` 明确禁止降 `minSdk`，把它当 4.4 基础属于 fork 上游不接受的改动。
- 本机环境：无 JDK（`java` / `gradle` 不在 PATH，IntelliJ 目录下未找到可用 `java.exe`）；Android SDK 有 `platforms 31/34/35/36`、`build-tools 30.0.3/34/35/36`、`system-images android-21;android-tv;x86` 与 `android-36`；无 `cmdline-tools`，无 API 19 镜像。
- 因此：**本地运行验证的上限是 API 21 TV 模拟器**，4.4 结论只能来自真机，且必须单独标注。

## 用户与场景

- 2014–2015 年的安卓智能电视/盒子，系统是厂商定制的 4.4，原桌面塞满推荐流、会员角标、开机广告。
- 遥控器操作，无鼠标键盘；坐得远，字号和焦点必须明显。
- 期望：按 Home 直接进一块干净的桌面，常用 App 一排，其余往下；能看时间；不用的时候就安静。

## 范围

### 做（v1）

| 编号 | 需求 | 验收要点 |
|---|---|---|
| FR-1 | 声明 `CATEGORY_HOME`，可被设为默认桌面；按 Home 回到自己 | 安装后 `dumpsys window \| grep mCurrentFocus` 为本 App |
| FR-2 | 枚举可启动应用：`LAUNCHER` ∪ `LEANBACK_LAUNCHER`，按包名去重，排除自身 | LEANBACK-only 的电视 App 也出现 |
| FR-3 | 首排 dock（默认 5 个）+ 下方应用网格（默认 5 列），焦点单层高亮 | 遥控器上下左右可达所有项 |
| FR-4 | `DPAD_CENTER` / `ENTER` 启动 App；启动失败 Toast 并留在桌面 | 停用过的 App 不崩溃 |
| FR-5 | 长按 OK 或按 MENU 打开操作面板：加入首排 / 移出首排 / 隐藏 / 恢复隐藏 / 应用信息 | 每个动作立即生效并持久化 |
| FR-6 | 持久化 dock 顺序、隐藏集合、列数（4–6）、dock 数量（4–6）、时钟开关 | 重启后保持 |
| FR-7 | 顶部时钟 + 日期 + 网络状态（Wi-Fi 名或"有线/未连接"） | 系统时间变更后一分钟内跟上 |
| FR-8 | 空闲 60s 进入时钟屏保，任意按键退出，位置缓慢漂移 | 不烧屏、不挡按键 |
| FR-9 | 无图标/图标异常时用首字母 + 派生底色磁贴 | 不出现白块 |
| FR-10 | 设置覆盖层：改列数、dock 数、时钟开关、管理隐藏项 | 返回键关闭覆盖层，不退出桌面 |

### 不做（v1）

| 项 | 原因 | 去处 |
|---|---|---|
| 视频壁纸、4K 航拍屏保 | 4.4 无 HEVC 硬解，GPU 太弱；1080p H.264 也只在 v2 评估 | v2 |
| 手机扫码整理（局域网 Web 页） | 4.4 WebView 是 Chromium 33，需要 ES5 重写 | v2 |
| 亮度控制 | 4.4 无 `pm grant WRITE_SECURE_SETTINGS` | 不做 |
| 自动更新 | `REQUEST_INSTALL_PACKAGES` 权限模型是 8.0+ | 不做 |
| 文件夹、信号源图标、天气、农历 | 收益低，先保证核心可用 | v2 |
| 无障碍守护 | 4.4 没有后台启动限制，第三方 HOME 正常生效 | 不需要 |
| 开机自启接收器 | 作为 HOME 天然随启动器起来 | 不需要 |

## 约束

- C1 `minSdk 19`、`compileSdk 34`、`targetSdk 28`。
- C2 纯 Java 8 语法，不引入 Kotlin 运行时。
- C3 依赖只有 `androidx.core:core:1.13.1` 与 `androidx.recyclerview:recyclerview:1.3.2`。
- C4 不申请 `INTERNET`；不联网、不统计。
- C5 单 APK < 1.5MB；80 个 App 时 PSS < 60MB。
- C6 冷启动到首帧 < 1.5s（4.4 实机）/ < 3s（模拟器）。
- C7 全遥控器可操作；触屏能用但不优化。
- C8 不写系统设置、不申请 root、不依赖 `pm grant` 授予的权限。

## 验收标准

- A1 `./gradlew :app:assembleDebug` 通过，`aapt dump badging` 显示 `sdkVersion:'19'`。
- A2 API 21 TV 模拟器安装后：按 Home → `dumpsys window | grep mCurrentFocus` 含本 App 包名；冷启动后自动进入。
- A3 A2 场景下 `am start -W` 的 `TotalTime` < 3000ms；有 4.4 真机时 < 1500ms。
- A4 80 个 App 时 `dumpsys meminfo` PSS < 60MB，上下滚动 30 秒无 ANR。
- A5 改 dock 顺序、隐藏 3 个 App、改列数为 6，重启后全部保持。
- A6 `./gradlew :app:lintDebug` 中 `NewApi` 0 error。
- A7 单元测试覆盖 `LayoutStore`（排序/隐藏/列数边界）与 `AppRepository`（合并去重/排除自身/排序），全部通过。
- A8 安装与运行期间无权限弹窗；`adb shell dumpsys package <pkg>` 无 `INTERNET` 权限。

## 风险

| 风险 | 影响 | 兜底 |
|---|---|---|
| 厂商固件硬编码 Home（如 `persist.service.homepage.pkg`） | 装上也抢不到 Home | 真机先验证；抢不到则走任务 02 的"禁用原桌面"路径，仍不行就声明该机型不支持 |
| 本机无 JDK、无 API 19 镜像 | 无法在本机证明 4.4 行为 | 以 API 21 TV 模拟器为代理，4.4 结论标注"未验证" |
| 低端 GPU 上自绘背景掉帧 | 滚动卡顿 | 单层渐变、关闭阴影与模糊、必要时改为纯色 |
| 图标加载慢导致首屏空白 | 观感差 | 首帧先出字母磁贴，图标异步回填 |
| `LEANBACK_LAUNCHER` 常量是 21 才有 | lint 报错或 19 上崩溃 | 用字符串常量，见 API 19 规范 |

## 依赖的其他任务

- 无前置任务。任务 02（安装器）依赖本任务产出的 APK 包名与路径。
