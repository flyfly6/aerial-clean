# 技术设计：AerialClean Lite

## 1. 技术选型

| 决策 | 选择 | 理由 / 放弃的选项 |
|---|---|---|
| 语言 | Java 8 | 不引 Kotlin 运行时（~1.5MB + 反射初始化），512MB 设备上与 AGP 版本组合更少变量。放弃 Kotlin：收益小、风险大 |
| UI 框架 | 原生 View + RecyclerView | Compose 地板 21。放弃 Leanback：`leanback` 1.x 虽支持 17/19，但自带厚重主题与卡片体系，和 Aerial 的极简风格冲突。放弃自定义 ViewGroup 全自绘：RecyclerView 已经解决复用、焦点、滚动 |
| 构建 | AGP 8.7.3 + Gradle 8.9 + JDK 17 | AGP 8.x 仍支持 minSdk 19；AGP 9 将抬到 21 |
| AndroidX | `core:1.13.1`、`recyclerview:1.3.2` | 二者 minSdk 分别 19 / 14；更高版本抬到 21 |
| 持久化 | `SharedPreferences` | 数据量小（几十个包名 + 4 个标量）。放弃 JSON 文件与数据库：没有收益 |
| 时钟 | `TextClock`（17+） | 零代码跟随系统时间与 12/24 小时制；配 `Handler` 每分钟刷新日期 |
| 屏保 | 应用内覆盖层 | `DreamService` 在部分厂商 4.4 上没有系统入口，覆盖层 100% 可控 |

**包名**：`io.github.flyfly6.aerialclean`（唯一权威处：`lite/app/build.gradle.kts`）。
**HOME Activity**：`io.github.flyfly6.aerialclean.HomeActivity`。

## 2. 布局规格（1080p 基准）

```
┌──────────────────────────────────────────────┐
│ 22:41                        Wi-Fi  MyHome    │  顶栏 96dp，右侧状态
│ 10月26日 星期日                                │
│                                              │
│                                              │
│   ┌────────────────────────────────────┐     │  dock 底板：圆角 24dp
│   │ [icon] [icon] [icon] [icon] [icon] │     │  高 168dp，左右留 96dp
│   └────────────────────────────────────┘     │
│                                              │
│   [icon] [icon] [icon] [icon] [icon]         │  网格：5 列 × N 行
│   [icon] [icon] [icon] [icon] [icon]         │  单元 176×152dp，间距 16dp
└──────────────────────────────────────────────┘
```

- 底部安全区 48dp；网格可滚动，dock 固定。
- 尺寸全部进 `dimens.xml`，`values-sw720dp` 提供 720p 变体（主要差异：图标 56dp vs 64dp，dock 高 132dp）。
- 字体用系统 `sans-serif-light`，不打包 Outfit（省体积，4.4 字体渲染对自定义 TTF 兼容一般）。

## 3. 视觉降级（相对 aerial-tv）

| Aerial | Lite 做法 |
|---|---|
| 实时玻璃模糊 | 圆角矩形 + 12% 白色叠加 + 顶部 1dp 高光渐变（预渲染，一次绘制） |
| 焦点磁贴抬升 + 投影 | 缩放 1.08 + 底板亮度变化，无阴影（`setShadowLayer` 走软件层，19 上太慢） |
| 环境光晕取图标主色 | `Palette` 不可用（依赖 21）；用图标缩放后的平均色，降采样到 1×1 `Bitmap` 取像素，结果缓存 |
| 4K 航拍屏保 | 时钟漂移屏保 |

## 4. 数据模型

```java
final class AppEntry {
    final String pkg;          // 包名，唯一键
    final String component;    // 启动用 ComponentName 的扁平化字符串
    final CharSequence label;  // 显示名
    final boolean system;      // 系统应用，仅用于排序与提示
}
```

`LayoutStore`（SharedPreferences 名 `layout`）：

| key | 类型 | 语义 |
|---|---|---|
| `dock` | String | 逗号分隔包名，顺序即显示顺序；包名不含逗号 |
| `hidden` | StringSet | 隐藏的包名 |
| `columns` | int | 4–6，默认 5 |
| `dock_count` | int | 4–6，默认 5 |
| `show_clock` | boolean | 默认 true |
| `saver_idle_ms` | int | 默认 60000 |

写入用 `apply()`；读取时对未知包名做过滤（App 被卸载后不抛异常）。

## 5. 关键算法

### 5.1 应用枚举

```
query(ACTION_MAIN, CATEGORY_LAUNCHER)  ∪  query(ACTION_MAIN, "android.intent.category.LEANBACK_LAUNCHER")
  → 按 pkg 去重（同包取 LAUNCHER 优先）
  → 排除自身包名
  → 按 hidden 过滤（用于网格，不用于设置页）
  → 排序：dock 内的按 dock 顺序在前，其余按 label 的 Collator 排序
```

- 用 `"android.intent.category.LEANBACK_LAUNCHER"` 字符串，不用 `Intent.CATEGORY_LEANBACK_LAUNCHER`（21 才有）。
- 枚举在后台线程一次完成；耗时超过 300ms 时打点记录（4.4 上 100+ 包可能较慢）。

### 5.2 启动

```java
Intent i = new Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName.unflattenFromString(entry.component))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
try { startActivity(i); }
catch (ActivityNotFoundException | SecurityException e) { toast(R.string.launch_failed); }
```

### 5.3 焦点

- dock 与网格各用一个 `RecyclerView`；网格用 `GridLayoutManager`，`setOrientation` 不设。
- `TileView` 自己处理 `onFocusChange` 切换状态（缩放 + 底板）。
- 网格第一行按上 → dock；dock 按下 → 网格第一行；dock 按上 → 设置入口（右上角）。
- 显式设置 `nextFocusUp/Down/Left/Right` 只在跨容器时使用，容器内交给 LayoutManager。

### 5.4 屏保

`Handler` 每 5s 检查 `SystemClock.uptimeMillis() - lastInteraction`；超时则显示全屏 `ClockSaver`（时钟 + 日期，位置在 ±40dp 内按分钟随机漂移）。任意 `KeyEvent` / `TouchEvent` 退出并重置计时。不用 `DreamService` 是为了不依赖厂商的系统入口。

## 6. 性能预算

| 阶段 | 预算 |
|---|---|
| `onCreate` → 首帧 | 50ms：只 inflate 布局、画背景与空磁贴 |
| 枚举应用 | 后台线程，完成后 `post` 到主线程刷新列表 |
| 图标加载 | 每次 bind 触发，LruCache 命中直接返回；未命中则占位 + 异步加载（单线程 `ExecutorService`） |
| 背景绘制 | 一次 `LinearGradient` + 一次圆角矩形，无多层叠加 |
| 内存 | 图标缓存上限 `getMemoryClass()/8`，`onTrimMemory` 清空 |

## 7. 兼容与降级

- 所有新 API 使用点都必须有 19 分支，禁止空分支（见 `.trellis/spec/lite/api19-compatibility.md`）。
- 图标解码用 `BitmapFactory` + `inSampleSize`；`inBitmap` 复用不做（4.4 的复用规则严格，收益不抵风险）。
- `getResources().getColor()` / `getDrawable()` 在两个 API 上都可用，统一走封装，便于以后替换。

## 8. 测试策略

- **纯 JVM 单测**：`AppRepository` 接受一个 `AppSource` 接口（不直接依赖 `PackageManager`），用假数据测合并、去重、排除自身、排序；`LayoutStore` 用内存版 `SharedPreferences` 替身测边界（列数越界、未知包名、空 dock）。
- **不引入 Robolectric**：省依赖，且 4.4 的真实行为只能靠设备。
- **设备验证**：API 21 TV 模拟器（本机已有）+ 4.4 真机（若可用），命令见 `implement.md`。

## 9. 风险与回滚

- 若 4.4 真机上 `RecyclerView` 焦点行为异常，回滚点：`TileView` 改为单一 `GridLayout` 容器 + 手动焦点（工作量约 1 天，见 implement.md 的 M4 备选）。
- 若厂商硬编码 Home 导致无法接管，本任务仍交付"可安装可运行"的 App，接管失败记入 `docs/devices/<model>.md` 并交给任务 02 处理。
- 每个里程碑结束都有可安装产物，任一步失败不阻塞已完成的验收项。

## 10. 交付物

- `lite/` 可构建工程与全部源码。
- `lite/app/src/test` 单测。
- `docs/lite-acceptance.md`：模拟器与真机的实测记录（含机型、系统版本、命令输出摘要）。
