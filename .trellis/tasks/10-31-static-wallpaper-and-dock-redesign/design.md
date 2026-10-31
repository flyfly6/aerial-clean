# 技术设计：Android TV 桌面静态壁纸与 Dock 样式美化

> **设计产物与预览**：
> - Pencil 设计源文件：[`design.pen`](./design.pen)
> - 设计图高清预览与交互说明：[`design-preview.md`](./design-preview.md)
> - 截图预览目录：[`preview/`](./preview/)

## 1. 架构总览

在现有 `lite` 原生 Java 桌面架构基础上，新增壁纸管理、内存降采样、双层平滑视图流转与全功能设置面板：

```
io.github.flyfly6.aerialclean/
├─ HomeActivity.java              桌面主入口：按键分发、双层流转调度、设置呼出与屏保唤醒
├─ app/
│  ├─ AppEntry.java               已安装应用模型
│  ├─ AppRepository.java          应用枚举与去重，槽位自适应补齐算法
│  ├─ LayoutStore.java            布局持久化（扩展壁纸来源、分辨率、底板样式、列数、槽位数）
│  └─ IconLoader.java             图标异步加载与 LruCache
├─ wallpaper/
│  ├─ WallpaperManager.java       核心壁纸引擎：异步拉取、本地缓存、Bitmap 回收
│  ├─ WallpaperConfig.java        数据模型：来源（必应/内置/渐变）、分辨率（720/1080/4K）
│  └─ BingWallpaperClient.java    轻量 HttpURLConnection 异步抓取，零第三方网络库
├─ weather/
│  ├─ LunarCalendar.java          1900-2050 纯算法农历月日计算引擎，零外部依赖
│  └─ WeatherClient.java          Open-Meteo 免 Key API 异步天气获取与本地缓存
└─ ui/
   ├─ BackdropView.java           双模背景：支持绘制位图壁纸 + 暗角遮罩 / 降级纯渐变
   ├─ DockView.java               常用应用底板：自适应测量、严格居中、上下对称内边距
   ├─ GridView.java               全部应用网格：4~8 列动态配置流式布局
   ├─ ClockView.java              左上角三行结构：农历公历 + Outfit 大时钟 + 实时天气
   ├─ NetworkIconView.java        右上角纯 Canvas 矢量自绘 WiFi / 以太网连接状态图标
   ├─ SettingsPanel.java          全屏遥控器适配的卡片式壁纸与显示设置面板
   ├─ ClockSaver.java             时钟屏保：壁纸背景 + 75% 防烧屏遮罩 + 大时钟漂移
   └─ TileView.java               磁贴组件：12dp 小圆角，垂直绝对居中，1.08x 天空蓝呼吸焦点态
```

---

## 2. 核心模块详细设计

### 2.1 静态壁纸引擎 (WallpaperManager)

#### 来源枚举
```java
public enum WallpaperSource {
    BING_DAILY,      // 必应每日精选（支持自动缓存）
    BUILT_IN,        // 14 张内置自然/航拍精选
    PURE_GRADIENT    // 纯代码绘制深冷色渐变（极速老电视专用）
}
```

#### 分辨率与动态采样计算
```java
public enum ResolutionTier {
    RES_720P(1280, 720),    // 内存约 1.8MB (RGB_565)
    RES_1080P(1920, 1080),  // 内存约 4.1MB (RGB_565)
    RES_4K(3840, 2160);     // 内存约 16.5MB (RGB_565)

    public final int targetWidth;
    public final int targetHeight;
    ResolutionTier(int w, int h) { this.targetWidth = w; this.targetHeight = h; }
}
```

#### 采样与解码算法（针对 API 19 严格防 OOM）
```java
public static Bitmap decodeSampledBitmap(InputStream is, int reqWidth, int reqHeight) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(is, null, options);
    
    // 计算 2 的整数次幂 inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
    
    // 强制使用 RGB_565：比 ARGB_8888 节约 50% 内存（老电视关键优化）
    options.inPreferredConfig = Bitmap.Config.RGB_565;
    options.inJustDecodeBounds = false;
    options.inPurgeable = true; // API 19 允许内存紧张时系统回收
    
    return BitmapFactory.decodeStream(is, null, options);
}
```

#### 内存治理生命周期
- 全局只保留**单张全屏壁纸 Bitmap** 引用；
- 切换壁纸前，必须在主线程将前一张 Bitmap 引用解除，并在新壁纸绘制就绪后调用 `oldBitmap.recycle()`；
- `onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL)` 触发时，若处于低配模式可降级重新加载 720p 采样。

---

### 2.2 农历与天气引擎

1. **农历计算 (`LunarCalendar`)**：
   - 基于二十四节气与新月朔望月天数的 1900-2050 年数据表压缩算法；
   - 零网络开销，零时区延迟，准确输出“九月廿二”、“正月初一”等中文表达。
2. **天气获取 (`WeatherClient`)**：
   - 采用 Open-Meteo 免 key API 配合 IP 粗定位，不依赖任何第三方 SDK；
   - 结果自动落盘本地缓存文件，断网或离线启动时立刻展示上次有效天气（如 `⛅ 6°C 北京 晴间多云 6°C`），绝无阻塞。

---

### 2.3 双层视图流转结构与遥控器焦点路由 (Key Flow)

重构为**两层平滑切层结构**（`layer_home` 与 `layer_grid`）：

```
┌────────────────────────────────────────────────────────┐
│ [ClockView]                                            │  ◄─ 第一层：首屏常驻桌面
│ 10月31日 周六 九月廿二                                  │     开阔、纯净、壁纸为主
│ 10:24 (Outfit)                 [必应·1080p] [WiFi] [⚙] │
│ ⛅ 6°C 北京 晴间多云 6°C                                │
│                                                        │
│             ┌────────────────────────────┐             │
│             │ [爱奇艺] [极光] [[小电视]] │             │  ◄─ 常用 Glass Dock
│             └────────────────────────────┘             │     (焦点放大1.08x + 光晕)
│                                                        │     (上下对称 20dp 内边距)
├────────────────────────────────────────────────────────┤  ◄─ 遥控器 [DOWN] 平滑切入
│ 全部应用 (77)                                          │  ◄─ 第二层：全部应用网格
│                                                        │     (85% 深色暗角遮罩)
│  ┌────┐   ┌────┐   ┌────┐   ┌────┐   ┌────┐   ┌────┐   │     (纯净无多余提示)
│  │App │   │App │   │App │   │App │   │App │   │App │   │
│  └────┘   └────┘   └────┘   └────┘   └────┘   └────┘   │
└────────────────────────────────────────────────────────┘
```

#### 按键分发规则 (HomeActivity.dispatchKeyEvent)
- **从 Dock 到网格**：
  在 Dock 磁贴上按遥控器 `KEYCODE_DPAD_DOWN`：
  隐藏首屏层 `layer_home`，显示网格层 `layer_grid`，背景叠加上层 85% 暗角，首项自动获焦。
- **从网格回 Dock**：
  在网格第 1 行按 `KEYCODE_DPAD_UP`，或者在网格任意位置按 `KEYCODE_BACK`：
  拦截事件，隐藏网格层，切回首屏层，背景恢复无暗角，焦点平滑回到上次选中的 Dock 磁贴。
- **常驻保护**：
  当焦点在 Dock 常用应用或首屏时，按 `KEYCODE_BACK` 消费并忽略，保证桌面不会被退出。
- **菜单呼出**：
  任何状态下按遥控器 `KEYCODE_MENU` 或点击右上角齿轮按钮，弹出全屏卡片式设置面板。

---

### 2.4 底部常用 Dock 尺寸与边距精密计算

为了彻底消除上下间距不一致问题，采用严格的自适应与对称约束：
1. **尺寸定义**：
   - 磁贴高：`tile_height = 150dp`，宽：`dock_tile_width = 138dp`；
   - 磁贴内图标：`tile_icon = 60dp`，文字居中并预留 8dp 间距；
   - 磁贴外框圆角调整为 **12dp**，内图标框圆角为 **10dp**。
2. **内边距对称性**：
   - 磁贴移除上下 `layout_margin`，仅保留左右 `tile_gap = 8dp`；
   - `DockView` 上下内边距设为 `dock_padding_v = 20dp`；
   - `DockView.onMeasure` 明确计算高度：`tileHeight + getPaddingTop() + getPaddingBottom() = 190dp`；
   - 容器外高度包裹 `wrap_content`，使未聚焦磁贴顶部距底板上沿 **20dp**，底部距底板下沿 **20dp**（绝对对称）；
   - 获焦时 1.08x 放大（高度扩增至 162dp），上下剩余呼吸空间各为 **14dp**，保持完美的动态居中。

---

### 2.5 屏保与低端硬件降级适配

1. **时钟屏保 (`ClockSaver`)**：
   - 桌面闲置 60 秒自动触发；
   - 原生 Canvas 将当前壁纸进行 Center-Crop 绘制，并在其上叠加 `0xBF000000`（75% 深色遮罩）；
   - Outfit 大时钟与本地日期定时微幅漂移（`driftPx = 40dp`），彻底杜绝 OLED / LCD 电视屏幕烧屏。
2. **底板 60fps 硬件加速**：
   - 默认采用“半透磨砂黑”（85% 半透明深色 + 1.5px 白色高光描边），纯 Canvas 硬件加速直出，0 GPU 额外开销，稳定 60fps。
