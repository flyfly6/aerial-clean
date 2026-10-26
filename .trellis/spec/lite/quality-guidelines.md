# 质量基线

目标设备：1GB RAM、双核 A53/A7、Mali-400/450 级 GPU、720p/1080p。这里的数字是验收线，不是愿望。

## 性能

| 指标 | 目标 | 测法 |
|---|---|---|
| 冷启动到首帧 | < 1.5s | `adb logcat -s AerialClean:I` 打点，或 `am start -W` 的 TotalTime |
| 首屏后图标补全 | < 3s（80 个 App） | 目视 + 打点 |
| 滚动帧率 | 不出现明显卡顿 | `dumpsys gfxinfo` 看 janky frames |
| 内存 | PSS < 60MB（80 个 App） | `adb shell dumpsys meminfo <pkg>` |

做法：

- 首帧只画背景、dock 骨架与空磁贴；图标在后台线程逐个回填。
- 背景只有一层 `LinearGradient`，不叠多层半透明，不开 `setLayerType(LAYER_TYPE_SOFTWARE)`。
- 不使用 `View.setShadowLayer`（软件层，19 上很慢）。
- 列表项复用 `RecyclerView.ViewHolder`，`onBindViewHolder` 里不做 I/O。

## 内存

- 图标缓存用 `LruCache`，上限按 `ActivityManager.getMemoryClass() / 8` 计算，并给每张图 `BitmapFactory.Options.inJustDecodeBounds` + `inSampleSize` 缩到 96dp。
- `onTrimMemory(TRIM_MEMORY_*)` 里清缓存。
- 不持有 `Activity` 的静态引用。

## 焦点与按键

- 所有可交互元素 `focusable`，`nextFocus*` 显式指定，不依赖默认顺序。
- 只有 `HomeActivity` 处理 `KEYCODE_BACK`：有覆盖层先关覆盖层，否则不退出（桌面不该被 Back 杀掉）。
- 长按 = `KEYCODE_DPAD_CENTER` 长按或 `KEYCODE_MENU`，两条路径都要通。
- 焦点变化必须有可见反馈（缩放 + 底板变亮），遥控器用户不能靠鼠标悬停。

## 错误处理

- App 启动失败（`ActivityNotFoundException` / 权限拒绝）：Toast 提示并保持桌面在前台，不崩溃。
- `PackageManager` 返回 null 图标：用首字母 + 派生底色。
- `SharedPreferences` 里的包名可能已被卸载：加载时过滤，不抛异常。

## 不做什么

- 不发通知、不申请 `INTERNET`、不做统计。
- 不在 `onCreate` 里做超过 50ms 的同步工作。
- 不写 `catch (Exception ignored)`；至少要记 `Log.w` 并给出降级行为。
