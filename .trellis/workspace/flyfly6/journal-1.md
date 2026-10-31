# Journal - flyfly6 (Part 1)

> AI development session journal
> Started: 2026-10-26

---



## Session 1: AerialClean Lite 桌面：从零到 A1–A8 验收
<!-- trellis-session: v=2 fp=53550cb04dceb808 -->

**Date**: 2026-10-26
**Task**: AerialClean Lite 桌面：从零到 A1–A8 验收
**Branch**: `main`

### Summary

在 lite/ 从零建成 minSdk 19 的 TV 桌面并通过 A1–A8：Application 枚举（LAUNCHER∪LEANBACK 去重）、自适应网格 + dock、长按/MENU 操作面板、设置覆盖层、漂移时钟屏保、R8 裁剪的 release 构建（4.19MB→175KB）。验收在 Android 13 TV 模拟器 aerial_tv_api33 完成：80 个占位 App 下 PSS 50.1MB、冷启动 484ms、30s 滚动无 ANR、无 INTERNET 权限。无 4.4 真机，真机相关阈值与 Holo 渲染标注未验证。

### Main Changes

- lite/app 全套 Java 源码与资源；docs/lite-acceptance.md 验收记录；spec/lite/quality-guidelines.md 增「构建与验收陷阱」；release 构建配置与 proguard-rules.pro

### Git Commits

| Hash | Message |
|------|---------|
| `81e9999` | feat(lite): 枚举、网格、dock、操作面板与设置覆盖层 |
| `43d54c3` | feat(lite): 视觉层、时钟与自适应列宽 |
| `a96058c` | feat(lite): 漂移时钟屏保 |
| `2daea17` | test(lite): 布局与应用枚举单测 |
| `f99bf7f` | feat(lite): 可裁剪 release 构建与 A1–A8 验收 |

### Testing

- [OK] ./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug 全部通过（11 个单测、NewApi 0 error）；模拟器装机验证网格/dock/面板/设置/屏保/HOME 接管

### Status

[OK] **Completed**

### Next Steps

- P1（10-26-aerialclean-installer-debloat-launcher-takeover）仍是 planning，需用户确认后 task.py start；4.4 真机到手后重跑 A2–A5


## Session 2: 安装器落地：debloat + 桌面接管（含 Home 接管机制实测）
<!-- trellis-session: v=2 fp=c8f23d15e96ae721 -->

**Date**: 2026-10-26
**Task**: 安装器落地：debloat + 桌面接管（含 Home 接管机制实测）
**Branch**: `main`

### Summary

引入 cleanyourtv tools（固定 commit）并实现 lib/installer.py 单实现 + bash/PowerShell 启动器。实测发现 Android 13 TV 上 set-home-activity、HOME role、清单 priority 全部无效，接管 Home 改为『停用原桌面 → 立即复验 → 失败回滚』，规范/PRD C3 同步重写。AVD 上验证 A1/A2/A4/A5/A7 与 C5；未验证：4.4 全路径、重启持久性（模拟器重启会恢复 disable-user）、PowerShell 5.1、厂商 priority 差异。lib/installer.py、install.*、restore.*、tools/、examples/、docs/devices/、README。

### Git Commits

| Hash | Message |
|------|---------|
| `fa6325b` | feat(installer): 安装与还原编排（debloat + 桌面接管） |
| `9385b05` | feat(lite): HOME intent-filter 提高 priority |
| `9f78f2c` | docs(spec): 记录桌面接管的实测结论 |
| `31c6065` | chore(task): 更新 10-26 安装器任务规划件 |

### Status

[OK] **Completed**


## Session 3: 静态壁纸系统与 Dock 现代 TV 视觉重构
<!-- trellis-session: v=2 fp=3675da7146e5c894 -->

**Date**: 2026-10-31
**Task**: 静态壁纸系统与 Dock 现代 TV 视觉重构
**Branch**: `main`

### Summary

lite 桌面完成静态壁纸与顶栏/Dock 重构：新增 WallpaperManager/WallpaperConfig/BingWallpaperClient（必应每日 + 14 张内置 WebP、720p/1080p/4K 采样、离线缓存），BackdropView 全屏绘制壁纸；顶栏改为 Outfit 字体三行结构（公历+农历、大盘时钟、Open-Meteo 天气）+ 矢量 NetworkIconView 与设置齿轮；磁贴圆角/内边距与 5 槽位自适应调整；SettingsPanel 重写为卡片式暗黑弹窗（壁纸来源、采样、底板、列数、槽位、时钟开关）；ClockSaver 支持壁纸 Center-Crop + 75% 遮罩防烧屏 + 随机漂移。新增 weather/LunarCalendar、network 图标与 6 个单测文件（共 23 测试通过），NewApi 0 error。注意：本次在 AndroidManifest 中声明了 INTERNET 权限，与 AGENTS.md『权限：无 INTERNET』基线冲突，需确认是接受联网壁纸/天气还是回退为纯内置资源。

### Git Commits

| Hash | Message |
|------|---------|
| `6174481` | feat(lite): 实现静态壁纸系统与 Dock 现代 TV 视觉重构 |

### Status

[OK] **Completed**
