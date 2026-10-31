# 给编程助手的提示词

把下面分隔线后的整段贴给 Claude Code、Codex 或 Gemini CLI，并把 `<机型>` 换成你的电视。
前提：电脑与电视同一局域网、电视已开 ADB 调试；**仓库已克隆并由助手打开**——提示词里让助手
复用本仓的 `install.sh` / `restore.sh`，没有仓库它会另写一套，备份、Home 回滚和退出码都不在了。

提示词只约束「怎么用这套编排」，不解释原理；原理见 [README.md](README.md) 与
`docs/devices/`。

---

我要给 <机型> 清理预装广告、把桌面换成 AerialClean。在真机上按下面的顺序做，每步都用
`adb shell dumpsys window | grep mCurrentFocus` 或电视截图验证，不要跳步。

只使用本仓库现成的编排，不要另写安装脚本，也不要改 `tools/`（那是上游原样代码，见
`tools/UPSTREAM.md`）：

- 安装/接管：`./install.sh --host <电视IP> [--apk PATH] [--list FILE] [--keep-stock-home]`
- 还原：`./restore.sh --host <电视IP> [--uninstall]`
- 全部逻辑在 `lib/installer.py`；bash 与 PowerShell 只是启动器。

0. 动手前先问我三件事：机型与 Android 版本；我平时用哪些应用（决定停用列表）；原桌面要不要保留
   （`--keep-stock-home` 等于「只去广告，别动我的桌面」）。
1. 连接：`adb connect <电视IP>:5555`。电视上弹「允许 USB 调试」时我会点允许，你等我说点完了再继续。
   记下 `getprop ro.product.model`、`ro.build.version.sdk` 与 `pm list packages` 的结果。
   连不上先看 `docs/devices/` 里有没有这台机型的记录再排查。`adb install` 若长时间不返回，可能是在
   等电视上的确认弹窗：先让我去点，不要 Ctrl-C 后自己换别的安装方式重试。
2. 干跑：`./install.sh --host <电视IP> --dry-run`，把完整命令批次贴给我，等我确认再继续。
3. 构建并安装：`cd lite && ./gradlew :app:assembleDebug`（需 JDK 17 与 Android SDK 34），然后
   `./install.sh --host <电视IP>`。脚本自己会：装 APK → 拉起 → 确认是 HOME 候选 → 按 Home 验焦点 →
   焦点不在新桌面时**才**停用原桌面（失败立刻启用回来）→ 再验一次。你不要手动插 `pm disable` 去
   「帮忙」完成某一步。
4. 只去广告的第一次：加 `--keep-stock-home`（APK 仍会装上并可启动，但 Home 留在原桌面，脚本会明说），
   从 `examples/` 挑最接近的列表，重启电视后再验一次 Home 与常用应用的启动；列表一次只加几个包。
   新机型把实测结果按 `docs/devices/` 的格式写成记录。
5. 退出码不是 0 就停手，不要重试：`2` 用法、`3` 依赖缺失、`4` 设备不可达、`5` APK 装不上、
   `6` Home 没切过来（已回滚）、`7` 停用阶段失败。把最后一行打印的还原命令原文贴给我。
6. 收尾：告诉我这台电视上哪些结论是实测的、哪些只是在模拟器（`aerial_tv_api33`）上验证的；
   API 19 / 4.4 真机没跑过的必须写成「未验证」，不要写成已验证。

不要做：root、`pm uninstall` 系统应用、修改或绕过 `lib/installer.py` 的顺序、把别的机型的停用列表
直接套到我这台电视上、在没有点「允许 USB 调试」之前反复重连。
