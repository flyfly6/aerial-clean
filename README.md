# AerialClean

给老安卓电视的一块干净桌面，加一条命令的去广告安装器。

AerialClean 把两件事合到一个仓库：

- **桌面** — 从零写的 Android TV launcher，`minSdk 19`（Android 4.4 KitKat）起步，视觉沿用 [aerial-tv](https://github.com/pengchujin/aerial-tv) 的航拍风格：底部玻璃 dock、下方应用网格、单层焦点、时钟与屏保。**不用 Compose**，因为 Compose 的地板是 API 21。
- **安装器** — 复用 [cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的 `pm disable-user` 工具链（备份、Home guard、一条命令还原），负责把桌面装成 Home，再按机型列表停用广告/遥测/预装应用。

不插线、不 root、不卸载系统应用；每一步都能用一条命令还原。

> 状态：规划中。仓库目前只有 Trellis 计划产物与规范，没有任何 app 代码。

## 为什么不直接把两个项目合起来

`cleanyourtv` 的去广告不是 App 功能，是电脑端跑的 `adb shell pm disable-user` 脚本。普通 APK 无权停用别的包（除非 root / device owner），所以整合只能发生在脚本层：一个安装器仓库，加两个"要装什么"的来源。

`aerial-tv` 上不了 4.4 也不是 `minSdk` 写错了：`app/build.gradle.kts` 的 `minSdk 21` 是 Compose 1.9 与 Media3 1.8 的硬地板，`ui/` 7,000 行 Compose 全部作废。要 4.4，只能另写一个 View 体系的 Lite。

## 目录

```
aerial-clean/
├─ lite/       Android 4.4+ 桌面（Java，minSdk 19）
├─ tools/      debloat / restore 脚本（源自 cleanyourtv）
├─ examples/   按机型整理的停用列表
├─ install.sh  install.ps1   安装并按顺序接管
├─ restore.sh  restore.ps1   还原
├─ docs/       机型笔记与兼容说明
└─ .trellis/   计划、任务、规范
```

## 计划

| 任务 | 内容 | 状态 |
|---|---|---|
| 01 | AerialClean Lite：minSdk 19 桌面 | 规划 |
| 02 | 安装器：debloat + 桌面接管编排 | 规划 |

## 开发环境

- JDK 17（AGP 8.7 要求；当前机器未安装）
- Android SDK：`platforms;android-34`、`build-tools;34.0.0`
- 验证用：`system-images;android-21;android-tv;x86`（本机已有），或一台 Android 4.4 真机

## 许可

MIT。上游归属见 [NOTICE.md](NOTICE.md)。
