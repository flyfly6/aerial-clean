# 实现计划：Android TV 桌面静态壁纸与 Dock 样式美化

## 里程碑规划

```
M1: 资源打包与静态壁纸加载引擎
 ├─ 准备 14 张内置自然/航拍精选壁纸（WebP 格式压缩）
 ├─ 实现 WallpaperConfig 与 WallpaperManager
 └─ 实现 inSampleSize 动态降采样与 RGB_565 解码

M2: 必应每日壁纸与离线缓存
 ├─ AndroidManifest 增加 INTERNET 权限
 ├─ 轻量 BingWallpaperClient 异步拉取与本地缓存
 └─ 完善断网与冷启动回退兜底逻辑

M3: 首屏双层流转与焦点系统
 ├─ 重构 activity_home.xml：常用 Dock 悬浮层 + 全部应用网格层
 ├─ 升级 BackdropView：支持位图绘制、暗角渐变遮罩、纯渐变回退
 ├─ 升级 DockView：支持焦点态呼吸光晕（1.08x 放大）与半透黑降级底板
 └─ HomeActivity 完善 D-Pad 方向键与 Back 键流转路由

M4: 桌面壁纸与显示设置面板
 ├─ 创建 SettingsModal 弹窗布局与遥控器交互
 ├─ 接入来源切换、720p/1080p/4K 采样切换、底板样式切换
 ├─ 实现 14 张内置壁纸横向缩略图选择器
 └─ LayoutStore 持久化并在设置后即时刷新主界面

M5: 顶栏三行排版、矢量图标与 Dock 精密内边距
 ├─ 引入 Google Fonts Outfit 字体
 ├─ 实现 LunarCalendar 纯算法农历引擎与 WeatherClient 免 key 天气
 ├─ 实现 NetworkIconView 纯矢量 WiFi / RJ45 绘制与设置齿轮图标
 ├─ DockView 精密自适应高度测量与上下 20dp 对称内边距
 ├─ 全部应用网格去除非必要提示文案
 └─ ClockSaver 支持当前壁纸背景与 75% 沉浸防烧屏遮罩

M6: 验证、回归测试与静态门禁
 ├─ 单元测试：7 套测试（包含 LunarCalendarTest、WeatherClientTest、WallpaperManager 等共 23 个测试全数通过）
 ├─ ./gradlew :app:lintDebug 确保 NewApi 0 error
 ├─ 模拟器/设备实测验证冷启动耗时、焦点切换与内存 dumpsys
 └─ 同步 Pencil 设计图与任务预览文档
```

---

## 阶段执行清单

### M1 静态壁纸引擎
1. 在 `lite/app/src/main/assets/wallpapers/` 放入 14 张 1080p WebP 自然与航拍壁纸（命名 `wp_01.webp` ~ `wp_14.webp`）；
2. 编写 `WallpaperManager.java`：
   - 提供 `loadWallpaper(WallpaperConfig config)` 接口；
   - 封装 `calculateInSampleSize`，支持 720p、1080p、4K 采样目标；
   - 使用 `Bitmap.Config.RGB_565` 解码以减半内存；
   - 管理单例 Bitmap 引用并在切换前 `recycle()`。

### M2 必应每日壁纸
1. 在 `lite/app/src/main/AndroidManifest.xml` 中添加 `uses-permission android:name="android.permission.INTERNET"`；
2. 编写 `BingWallpaperClient.java`：
   - `HttpURLConnection` 请求 Bing API，解析图片 URL；
   - 将图片文件流直接写入 `context.getFilesDir() + "/wallpaper_bing_cache.jpg"`；
   - 失败时记 Log 并保留本地旧缓存，不崩溃。

### M3 首屏双层流转与 Dock 视觉升级
1. 修改 `BackdropView.java`：
   - 支持传入当前激活的 `Bitmap`；
   - 增加遮罩渐变与全部应用展开时的 85% 暗角渐变层；
2. 升级 `DockView.java` 与 `TileView.java`：
   - 绘制半透深色圆角底板与顶部高光，支持“毛玻璃 / 半透黑 / 极速极简”样式切换；
   - 磁贴获得焦点时使用 `ViewPropertyAnimator` 缩放至 1.08x，边框绘制天蓝色发光线；
3. 修改 `HomeActivity.java`：
   - 实现上下平滑滚动的流转机制；
   - 处理按键分发：Dock 按 DOWN 进入全部应用，全部应用顶部按 UP 或 BACK 回到 Dock。

### M4 设置面板
1. 创建 `SettingsModal.java` 与对应弹窗布局；
2. 遥控器可达的所有单选项与缩略图选择卡片；
3. 将选中的壁纸来源、分辨率、底板样式同步至 `LayoutStore` 并即时刷新 `BackdropView` 与 `DockView`。

### M5 质量验收
1. 编译验证：`./gradlew :app:assembleDebug`；
2. 静态检查：`./gradlew :app:lintDebug`，验证 `NewApi` 保持 0 error；
3. 内存与性能监控：`adb shell dumpsys meminfo`，验证 720p/1080p 下内存未超标。
