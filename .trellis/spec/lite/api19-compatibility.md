# API 19 兼容

`minSdk 19` 意味着编译期能看到的 API 有三分之二在运行时不保证存在。本文件是禁令清单，写代码前对照。

## 禁用（有替代写法）

| 禁用 | 引入版本 | 替代 |
|---|---|---|
| Jetpack Compose（全部） | 21 | 原生 View / Canvas |
| `androidx.media3.*` | 21 | `MediaPlayer` + `SurfaceView`（v2 屏保才需要） |
| `androidx.core:core` ≥ 1.16 / `activity-compose` / `lifecycle` ≥ 2.9 | 21 | `core:1.13.1`；不用 lifecycle |
| `java.time.*` | 26 | `java.util.Calendar` |
| `java.util.stream.*` | 24 | 普通循环 |
| `java.util.function.*`（作为 API 使用时） | 24 | 自定义接口 |
| `View.setElevation/setTranslationZ` | 21 | 焦点态用缩放 + 底色变化 |
| `RenderEffect` / `RenderNode` / `View.setRenderEffect` | 29 / 31 | 预渲染渐变与半透明层 |
| `Context.getColor(int)` / `Resources.getColor(int, Theme)` | 23 | `getResources().getColor(int)`（19 可用，已废弃） |
| `Context.getDrawable(int)` | 21 | `getResources().getDrawable(int)` 或 `ContextCompat` |
| 通知渠道 `NotificationChannel` | 26 | 本项目不发通知 |
| `ActivityOptions.makeSceneTransitionAnimation` | 21 | 不用转场动画 |
| `PackageManager.getLeanbackLaunchIntentForPackage` | 21 | 自行拼 `Intent`（见下） |
| `UsageStatsManager` | 21 | 不用 |
| `Settings.System.canWrite` / `WRITE_SECURE_SETTINGS` 运行时授予 | 23 | 不做亮度/系统设置写入 |
| `pm grant` / `cmd` 系 adb 命令 | 23 / 24 | 安装脚本不依赖它们（见 tools 规范） |
| `ImageDecoder` / `AnimatedImageDrawable` | 28 | `BitmapFactory` |
| `TextClock` 的 `format24Hour` 属性 | 17 ✓ 可用 | 可直接用 |

## 可用（19 上确实存在，别过度保守）

| API | 引入 | 用途 |
|---|---|---|
| `RecyclerView`（androidx 1.3.2） | 14 | 网格与 dock 列表 |
| `ViewPropertyAnimator` / `ObjectAnimator` | 12 / 11 | 焦点动画 |
| `TextClock` | 17 | 时钟 |
| `DreamService` | 17 | 系统屏保（v2） |
| `NotificationListenerService` | 18 | 不需要（v1） |
| `AccessibilityService` | 4 | 不需要（4.4 无后台启动限制） |
| `Canvas.drawRoundRect(RectF, float, float, Paint)` | 1 | 圆角底板 |
| `LinearGradient` / `RadialGradient` | 1 | 背景与玻璃高光 |
| `Hardware acceleration`（`android:hardwareAccelerated`） | 11 | 默认开启 |
| `SharedPreferences` | 1 | 布局持久化 |
| `LruCache`（`android.util`） | 12 | 图标缓存 |
| `PackageManager.queryIntentActivities` | 1 | 配合下面的字符串类别使用 |

## 应用枚举的正确写法

4.4 电视上很多 App 只声明 `LEANBACK_LAUNCHER`，不声明 `LAUNCHER`；也有系统 App 两个都不声明。

1. 查 `ACTION_MAIN` + `CATEGORY_LAUNCHER`；
2. 查 `ACTION_MAIN` + `CATEGORY_LEANBACK_LAUNCHER`（用字符串常量，避免 21 才有的 `Intent.CATEGORY_LEANBACK_LAUNCHER` 字段触发 lint）；
3. 两者并集按包名去重；
4. 兜底：`getInstalledApplications(0)` 里带 `FLAG_SYSTEM` 且有启动 Activity 的（可选，v1 不做）。

## 检查手段

- `lint` 的 `NewApi` 是唯一可靠的静态闸门：`./gradlew :app:lintDebug`。
- 提交前扫一遍 `Build.VERSION_CODES.`：出现 `L` 之后的常量且没有 `SDK_INT` 分支，就是 bug。
- 只在 `if (Build.VERSION.SDK_INT >= N)` 里使用新 API 时，必须给出 19 分支的实际行为，不能空着。
