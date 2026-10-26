# AerialClean Lite 验收记录

日期：2026-10-26
任务：`.trellis/tasks/10-26-aerialclean-lite-android-4-4-tv-launcher`
构建：`lite/`，AGP 8.7.3 / Gradle 8.9 / JDK 17，compileSdk 34、minSdk 19、targetSdk 28

## 验证环境

|项|值|
|---|---|
|模拟器|AVD `aerial_tv_api33`，Android 13 TV（API 33），arm64-v8a，1080p / 320dpi = 960×540dp|
|真机|**无。本机没有 Android 4.4 设备，也没有 API 19/21 的 TV 镜像**（Apple Silicon 的 TV 镜像最早 API 31）|
|桌面接管前提|`pm disable-user --user 0 com.google.android.tvlauncher`（TV 镜像把 HOME 硬绑在原桌面，force-stop 后会自动复活；可 `pm enable` 复原）|
|安装前提|关闭安装校验器：`settings put global verifier_verify_adb_installs 0`、`settings put global package_verifier_enable 0`，并 `pm disable-user --user 0 com.android.vending` / `com.google.android.gms`。不关的话本机无网状态下 `adb install` 永久挂起|

**结论适用范围**：除 A1/A6/A7 这类静态项外，所有设备结论都来自 API 33 TV 模拟器。Android 4.4 真机行为（尤其是 `Theme.Holo` 渲染、`RecyclerView` 低端机帧率、PackageManager 枚举耗时）**未验证**。

## 验收结果

|编号|结论|证据（模拟器）|
|---|---|---|
|A1|✅ 通过|`./gradlew :app:assembleDebug` BUILD SUCCESSFUL；`aapt dump badging` → `package: name='io.github.flyfly6.aerialclean' versionCode='1' versionName='1.0' compileSdkVersion='34'`、`sdkVersion:'19'`、`targetSdkVersion:'28'`、`launchable-activity: name='io.github.flyfly6.aerialclean.HomeActivity'`|
|A2|✅ 通过（API 33 代 API 21）|按 HOME 后 `dumpsys window \| grep mCurrentFocus` → `Window{… io.github.flyfly6.aerialclean/io.github.flyfly6.aerialclean.HomeActivity}`；`topResumedActivity` 同为 HomeActivity|
|A3|✅ 通过|`am start -W -n …/.HomeActivity` → `TotalTime: 484ms`（release）、`576ms` / `529ms`（debug），阈值 3000ms；真机阈值 1500ms **未验证**|
|A4|✅ 通过|安装 80 个占位启动器（`io.github.flyfly6.dummy01`…`dummy80`）后：启动日志 `grid=82 dock=1 in 302ms`，`dumpsys meminfo` TOTAL PSS **51,255KB ≈ 50.1MB**（< 60MB）；随后 30 秒内 127 次按键 + 滑动滚动，PSS 46,550KB，logcat 中 `ANR in` / `FATAL EXCEPTION` 计数 0，仍停留在 HomeActivity|
|A5|✅ 通过|通过 UI 操作：设置里把列数改成 6 → `columns value="6"`；MENU→隐藏 三次 → `hidden` 集合含 `io.github.flyfly6.dummy01`、`io.github.flyfly6.dummy14`、`com.google.android.tv.remote.service`；MENU→加到固定栏 → `dock` 由 `com.google.android.youtube.tv` 变 `com.google.android.youtube.tv,io.github.flyfly6.dummy10`。`am force-stop` 后重启，日志 `grid=78 dock=2 in 272ms`，prefs 三项全部保持|
|A6|✅ 通过|`./gradlew :app:lintDebug` BUILD SUCCESSFUL；`lint-results-debug.txt` 中 `NewApi` 0 条、`severity="Error"` 0 条|
|A7|✅ 通过|`LayoutStoreTest`（5 用例：`clampSlots` 边界、`parseDock`/`joinDock` 往返与空值）+ `AppRepositoryTest`（6 用例：合并去重 / 排除自身 / LAUNCHER 优先 / 隐藏过滤 / dock 保序截断 / grid 排序）→ `tests="11" failures="0"`|
|A8|✅ 通过|安装后未出现任何权限弹窗（`ACCESS_NETWORK_STATE` 是普通权限）；设备上 APK 实测 `aapt dump permissions base.apk` 只有 `android.permission.ACCESS_NETWORK_STATE` + 自动生成的 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，**无 `INTERNET`**。`POST_NOTIFICATIONS` 由 androidx.core 清单合并进来，已用 `tools:node="remove"` 移除|

## C5（APK < 1.5MB）

|构建|体积|说明|
|---|---|---|
|debug|4,390,907 B ≈ 4.19MB|不压缩、不裁剪。体积几乎全是 `androidx.core:1.13.1` 传递依赖的 kotlin-stdlib + coroutines|
|release（`isMinifyEnabled` + `isShrinkResources`）|**179,234 B ≈ 175KB**|✅ 达标；已装到模拟器实跑：冷启动 484ms、`grid=78 dock=2 in 361ms`、磁贴渲染正常、无崩溃|

release 需要在 `app/proguard-rules.pro` 里保留 `android.view.View` 子类的三个构造器 —— 布局 XML 按类名实例化自定义 View，R8 看不到这个引用。构建产物用 debug 签名（`signingConfig = signingConfigs.getByName("debug")`），只为本地验收。

## 已知偏差与风险

1. **没有 4.4 真机**：A3 的真机阈值、`Theme.Holo` 下的焦点态与文字渲染、低端机的滚动帧率都只有"代码层面按 API 19 写"的依据，没有实测。真机到手后需重跑 A2–A5。
2. **模拟器 HOME 劫持**：TV 镜像把 `com.google.android.tvlauncher` 设为不可替换的默认桌面，必须禁用它才能验证 A2；这不代表真实 4.4 设备的行为（4.4 上 HOME 选择器可正常弹窗）。
3. **`androidx.core:1.13.1` 的传递依赖**：调试包里的 kotlin-stdlib 只影响 debug 体积，release 已经裁掉；但 debug 包 4.19MB 说明依赖确实进来了，若将来改用别的 androidx 组件需要复查 minSdk。
4. **A4 的 80 个 App 是占位包**：只有 manifest、没有 dex（`android:hasCode="false"`），因此测的是枚举与图标回退路径的内存，不含真实 App 图标位图的内存峰值。真实设备上 80 个大图标 App 的 PSS 可能高于 50.1MB。
5. **`ACCESS_NETWORK_STATE`**：为了显示网络类型必须声明（未声明时 `getActiveNetworkInfo()` 抛 `SecurityException`，实际崩溃过一次）。它是普通权限、无弹窗，但确实出现在权限清单里；如果不接受，可去掉网络状态文案退回纯时钟。
