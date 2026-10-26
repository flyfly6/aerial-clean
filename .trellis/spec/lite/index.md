# lite/ — Android 4.4 桌面

单 module Gradle 工程，Java，`minSdk 19`。目标设备是 2014–2015 年的安卓电视与盒子：720p/1080p 面板、1GB RAM、armeabi-v7a、无 HEVC 硬解。

## 规范

| 文档 | 范围 | 状态 |
|---|---|---|
| [API 19 兼容](api19-compatibility.md) | 可用/禁用 API 与替代写法 | 已填写 |
| [目录结构](directory-structure.md) | 包划分与文件职责 | 已填写 |
| [质量基线](quality-guidelines.md) | 性能、内存、焦点、错误处理 | 已填写 |

## 开发前检查

- 读 [API 19 兼容](api19-compatibility.md)，确认要用的每个 API 在 19 上存在。
- 新增依赖先看它的 `minSdk`；任何 21+ 的依赖都等于砍掉项目目标。
- 改动涉及 Home / 应用枚举时，确认改动后仍能在没有 LEANBACK 声明的机型上列出 App。
- 涉及焦点或按键时，用遥控器路径（DPAD + OK + BACK + MENU）走一遍，不只看触屏。

## 质量检查

- `./gradlew :app:lintDebug`：`NewApi` 0 error。
- `./gradlew :app:testDebugUnitTest`：`LayoutStore`、`AppRepository` 的排序/隐藏/合并逻辑有单测。
- 装到 API 21 TV 模拟器：按 Home 后 `dumpsys window | grep mCurrentFocus` 是自己的 Activity；冷启动、滚动、返回键无异常。
- 有 4.4 真机时同样跑一遍，并在任务里记录机型与系统版本。
