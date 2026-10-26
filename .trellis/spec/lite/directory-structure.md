# 目录结构

```
lite/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradle/wrapper/            Gradle 8.9 wrapper
└─ app/
   ├─ build.gradle.kts        minSdk 19 / compileSdk 34 / targetSdk 28
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ java/io/github/flyfly6/aerialclean/
      │  │  ├─ HomeActivity.java        唯一 HOME Activity：布局装配、按键路由
      │  │  ├─ app/
      │  │  │  ├─ AppEntry.java         一条可显示的应用记录
      │  │  │  ├─ AppRepository.java    枚举、合并、按包名去重、排除自身
      │  │  │  ├─ LayoutStore.java      dock 顺序、隐藏集合、列数等（SharedPreferences）
      │  │  │  └─ IconLoader.java       异步加载 + LruCache
      │  │  ├─ ui/
      │  │  │  ├─ BackdropView.java     单层渐变背景（无模糊）
      │  │  │  ├─ DockView.java         首排：横向 RecyclerView + 玻璃底板
      │  │  │  ├─ GridView.java         应用网格：RecyclerView + 列数切换
      │  │  │  ├─ TileView.java         图标 + 名称 + 焦点态
      │  │  │  ├─ ClockView.java        时间 / 日期 / 网络状态行
      │  │  │  ├─ ActionSheet.java      长按菜单（加入首排 / 隐藏 / 应用信息）
      │  │  │  ├─ SettingsPanel.java    列数、dock 数、时钟开关、隐藏管理
      │  │  │  └─ ClockSaver.java       空闲时钟屏保（漂移防烧屏）
      │  │  └─ sys/
      │  │     ├─ Launcher.java         启动 App、失败提示
      │  │     └─ DeviceInfo.java       网络名、屏幕尺寸
      │  └─ res/
      │     ├─ drawable/                ic_launcher.xml（layer-list，兼容 19）
      │     ├─ layout/                  activity_home.xml 等
      │     └─ values/                  colors.xml / dimens.xml / strings.xml
      └─ test/java/…                    纯 JVM 单测（无 Robolectric）
```

## 约定

- 一个文件一个职责；`HomeActivity` 只做装配与按键分发，不写绘制。
- 视图类不碰 `PackageManager`；数据类不碰 `Context` 之外的 Android UI。
- 尺寸全部走 `res/values/dimens.xml`，按 1080p 设计、用 `sw720dp` 变体适配 720p。
- 不新增子 module。单 module 的好处是构建快、4.4 上 dex 简单。
