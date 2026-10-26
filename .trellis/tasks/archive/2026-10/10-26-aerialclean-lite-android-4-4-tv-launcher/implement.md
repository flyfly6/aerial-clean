# 实施计划：AerialClean Lite

前置约定：
- `./gradlew :app:assembleDebug` 只用于产出可安装的 APK，**构建通过不算验证**。每个里程碑的"验收"必须是设备上的行为。
- 每个里程碑结束时仓库都可构建、可安装；失败只回退当前里程碑。

## M0 环境（阻塞项）

> 平台是 macOS（arm64）。以下为实测可用的取值。

1. JDK 17 已在 `/opt/homebrew/opt/openjdk`（不在 PATH）：命令前 `export JAVA_HOME=/opt/homebrew/opt/openjdk`。
2. `ANDROID_HOME=/Users/fenglei/Library/Android/sdk`；`cmdline-tools/latest` 用 JDK 17 可运行（旧 `tools/bin/sdkmanager` 在 17 上因缺 JAXB 起不来）。
3. 已装：`platforms;android-34`、`build-tools;34.0.0`、`system-images;android-33;android-tv;arm64-v8a`（Apple Silicon 只能用 arm64 镜像；4.4 的 x86 镜像在本机跑不了）。
4. wrapper 已生成（Gradle 8.9），`lite/local.properties` 写 `sdk.dir=/Users/fenglei/Library/Android/sdk`（已 gitignore）。
5. 模拟器：`emulator -avd aerial_tv_api33`（1080p TV，Android 13），`adb` 在 `$ANDROID_HOME/platform-tools`。

验证：
```bash
java -version          # 17.x
cd lite && ./gradlew --version
adb devices            # 能列出模拟器/真机
```

回滚：无。未完成不得进入 M1。

## M1 工程与 Home 接管

1. `lite/settings.gradle.kts`、`lite/build.gradle.kts`、`lite/app/build.gradle.kts`：`minSdk 19`、`compileSdk 34`、`targetSdk 28`、Java 8。
2. `AndroidManifest.xml`：`HomeActivity` 带 `MAIN` + `HOME` + `DEFAULT`，`launchMode="singleTask"`，`exported="true"`；另带 `LAUNCHER` 便于从旧桌面启动。
3. `HomeActivity` 只显示纯色背景与一行文字，验证能接管 Home。
4. `res/drawable/ic_launcher.xml` 用 layer-list 画图标（19 不支持 adaptive icon，也不需要二进制资源）。

验证：
```bash
cd lite && ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell "dumpsys window | grep mCurrentFocus"    # 含 io.github.flyfly6.aerialclean
adb shell input keyevent KEYCODE_HOME
adb shell "dumpsys window | grep mCurrentFocus"
```
在模拟器里选"始终"，再按一次 Home。

回滚：只影响 M1 新增文件。

## M2 应用枚举、网格、启动

1. `AppEntry`、`AppRepository`（含 `AppSource` 接口）、`IconLoader`。
2. `GridView`：`RecyclerView` + `GridLayoutManager(5)`，`TileView` 绑定图标与名称，无图标时字母磁贴。
3. 启动逻辑与失败 Toast。
4. 后台枚举 + 首帧后回填。

验证：
```bash
adb shell pm list packages | wc -l
adb shell "dumpsys meminfo io.github.flyfly6.aerialclean | grep TOTAL"
# 目视：LEANBACK-only 的电视 App 出现在网格里；点开能启动；返回回到桌面
```

## M3 dock、跨容器焦点、操作面板

1. `DockView`（横向 `RecyclerView`）+ 玻璃底板。
2. 跨容器 `nextFocus` 接线：dock 下 → 网格首行；网格首行上 → dock；dock 上 → 设置入口。
3. `ActionSheet`：长按 OK / MENU 打开，动作：加入首排、移出首排、隐藏、应用信息。
4. 空 dock 的退化显示（"长按 App 加入首排"提示）。

验证：遥控器键盘模拟
```bash
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_CENTER
adb shell input keyevent KEYCODE_MENU
adb shell input keyevent KEYCODE_BACK
```
每一项焦点都必须可见；没有任何一次按键把焦点送到不可见位置。

## M4 持久化与设置覆盖层

1. `LayoutStore` 读写 + 未知包名过滤。
2. `SettingsPanel`：列数 4–6、dock 数 4–6、时钟开关、隐藏管理（列表 + 恢复）。
3. 变更立即生效（`columns` 改动重建 LayoutManager，`dock_count` 截断/补足）。

验证：
```bash
adb shell am force-stop io.github.flyfly6.aerialclean
adb shell am start -n io.github.flyfly6.aerialclean/.HomeActivity
# 目视：改动全部保留；隐藏的 App 不出现在网格，但在设置里可恢复
```

## M5 视觉

1. `BackdropView`：单层 `LinearGradient` + 底部轻微提亮。
2. 玻璃底板：圆角 + 12% 白 + 顶部 1dp 高光。
3. 焦点态：缩放 1.08（`ViewPropertyAnimator`，120ms）+ 底板亮度。
4. `ClockView`：`TextClock` + 日期 + 网络状态；每分钟刷新日期。
5. 尺寸入 `dimens.xml`，加 `values-sw720dp`。

验证：`dumpsys gfxinfo io.github.flyfly6.aerialclean framestats` 滚动 30s，janky < 10%。

## M6 屏保

1. 空闲计时（`Handler` + `SystemClock.uptimeMillis`），任意按键/触摸重置。
2. `ClockSaver` 全屏视图，时钟每分钟漂移 ±40dp。

验证：静置 60s 出现；按任意键退出；`adb shell input keyevent` 各方向键都能退出。

## M7 单测与静态检查

1. `lite/app/src/test`：`LayoutStoreTest`（列数边界、未知包名、空 dock、dock 顺序往返）、`AppRepositoryTest`（LAUNCHER/LEANBACK 合并、去重、排除自身、排序）。
2. lint 配置只保留 `NewApi` 为 error（其余 warning 不阻塞）。

验证：
```bash
cd lite && ./gradlew :app:testDebugUnitTest :app:lintDebug
grep -c "NewApi" app/build/reports/lint-results-debug.txt   # 0
```

## M8 设备验收与文档

1. 按 PRD 的 A1–A8 逐条执行，记录到 `docs/lite-acceptance.md`（机型、系统版本、命令、原始输出摘要）。
2. 模拟器结果与真机结果分开写；没有真机就写"未验证"。
3. 补 `docs/devices/<model>.md`（若试了真机）。

验证：A1–A8 全部有结论（通过 / 未验证 / 失败 + 原因）。

## 提交点

- 每个里程碑完成后先汇报改动与验证结果，得到用户同意再 `git commit`。
- 提交信息格式：`feat(lite): <里程碑>`；任务目录 `.trellis/tasks/10-26-aerialclean-lite-android-4-4-tv-launcher/` 一并提交。
