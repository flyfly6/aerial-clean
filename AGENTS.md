# AerialClean — 项目规则

## 这是什么

一个仓库装两件事：`lite/`（Android 4.4+ 电视桌面，Java）与 `tools/`（源自 cleanyourtv 的 adb 去广告脚本）。二者在安装顺序上耦合，在代码上无关。

## 技术基线（不可随手改）

| 项 | 值 | 原因 |
|---|---|---|
| `minSdk` | 19 | Android 4.4 是硬需求 |
| `compileSdk` / `targetSdk` | 34 / 28 | 本机 SDK 只有 31+；target 28 避开 29+ 的后台与存储行为变更 |
| 语言 | Java 8 语法（D8 脱糖） | 不引入 Kotlin 运行时；512MB 设备省内存 |
| UI | 原生 View / Canvas / RecyclerView | Compose 地板是 API 21 |
| 依赖 | `androidx.core:core:1.13.1`、`androidx.recyclerview:recyclerview:1.3.2` | 两者 minSdk 分别 19 / 14；更高版本会把地板抬到 21 |
| 权限 | 无 `INTERNET` | 不联网：无更新检查、无遥测、无手机配对（v1） |

新增依赖前先查它的 `minSdk`；抬到 21 就等于放弃本项目的存在理由。

## 4.4 兼容规则

写代码前读 `.trellis/spec/lite/api19-compatibility.md`。核心：不用 Compose、Media3、`java.time`、`java.util.stream`、`View.setElevation`、`RenderEffect`、`Context.getColor(int)`、通知渠道、`pm grant`。

## 构建与验证

- 构建命令只用来产出 APK：`cd lite && ./gradlew :app:assembleDebug`。构建成功不是验证结论。
- 验证结论必须来自设备行为：`adb install` 后按 Home、`dumpsys window | grep mCurrentFocus`、冷启动耗时、滚动与内存。
- 本机没有 API 19 模拟器镜像，本地运行验证只能用 `android-21;android-tv;x86`；真机 4.4 结论必须单独标注来源，没跑过就写"未验证"。
- 静态检查：`./gradlew :app:lintDebug`，`NewApi` 必须 0 error。

## Trellis

- 计划阶段（Phase 1）只写 `.trellis/tasks/<task>/` 下的产物，不写 app 代码。`prd.md` 完成后必须停下来等用户确认；`prd.md` 通过 = 可以 `task.py start`，不等于可以直接提交。
- 复杂任务需要 `prd.md` + `design.md` + `implement.md` + 非空 `implement.jsonl` / `check.jsonl`。
- 提交前向用户汇报变更范围与验证结果，得到当前对话里的明确同意才 `git commit`。
- 提交本次工作时把 `.trellis/tasks/<task>/` 一起提交。

## 协作约定

- 不派发子代理；调研、实现、验证都在主会话完成。
- 注释只写非直观的边界与原因，不给显而易见的代码加注释。
- 文档、提交信息用中文；标识符用英文。

## 上游关系

- `lite/` 与 [aerial-tv](https://github.com/pengchujin/aerial-tv) 无代码复用，只借视觉与交互意图；任何要抄的具体实现先确认许可证与 API 19 兼容。
- `tools/` 直接取自 [cleanyourtv](https://github.com/Gathin23/cleanyourtv)（MIT）。改动上游脚本时优先做成薄包装，别 fork 它的内部逻辑，否则以后无法跟上游更新。
