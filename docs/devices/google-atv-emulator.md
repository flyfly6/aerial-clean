# Google ATV 模拟器（`sdk_google_atv64_arm64` / Android 13）

本仓唯一能跑的设备：AVD `aerial_tv_api33`，1080p TV，2 GB RAM，headless 启动
（`-no-window -no-snapshot -no-audio -no-boot-anim -gpu swiftshader_indirect`）。
它是**行为代理**，不是电视：涉及 Android 4.4 的结论一律标注"未验证"。

## 实测记录（2026-10-26）

| 项 | 结果 | 证据 |
|---|---|---|
| `install.sh --host 127.0.0.1:5555 --list examples/google-atv-emulator/debloat.txt` | 通过（exit 0）：装 APK → 拉起 → 确认 HOME 候选 → 停用 `com.google.android.tvlauncher` → Home 落到 `io.github.flyfly6.aerialclean/.HomeActivity` → 停用列表 3 个包 → 再验 Home | 见下方"接管 Home" |
| 停用数量 | 3 = 列表去重后 3 行（`pm list packages -d` 前后对比） | — |
| `restore.sh --host …` | 通过（exit 0）：`pm enable` 全部 4 个包（3 + tvlauncher），Home 回到 `com.google.android.tvlauncher/.MainActivity` | — |
| `restore.sh --uninstall` | 通过（exit 0）：Lite 被卸载，Home 仍是原桌面 | — |
| `install.sh --stock-home com.nonexistent.pkg` | exit 2，设备零改动（`pm list packages -d` 与焦点都不变） | 验收 A7 |
| `install.sh --keep-stock-home --list …` | exit 0：停用列表照跑，Home 留在原桌面并给出警告 | — |
| `install.sh --dry-run` | exit 0，批次完整（安装 → 拉起 → 焦点 → Home → 条件块"停用原桌面" → 列表 → 复验）；前后 `pm list packages -d` 与焦点都不变 | 验收 A2 |
| `restore.sh --dry-run` | exit 0，打印两段上游 `restore.sh`、`pm enable`、Home 键与焦点检查 | — |
| 重复 `install.sh` | `disable.sh` 沿用已有还原记录（"keeping the recovery record…"），不会写第二份 | 验收 C5 |
| `pwsh -File install.ps1 -Help / -DryRun`、`restore.ps1 -DryRun` | 与 bash 版输出同一批命令、同一退出码（PowerShell 7.5.2） | 验收 A1 |

注：上面每条都是 2026-10-26 在 `aerial_tv_api33` 上跑出来的；`pwsh` 只在 7.5.2 上验证过，
Windows PowerShell 5.1 未验证。

## 接管 Home：为什么必须先停用原桌面

这台镜像上三条"官方"路子全部无效（详见 `.trellis/spec/tools/debloat-safety.md`）：

| 手段 | 实测 |
|---|---|
| `cmd package set-home-activity <comp>` | 打印 `Success`，`cmd package resolve-activity` 仍返回 tvlauncher；对 `FallbackHome` 同样无效 |
| `cmd role add-role-holder android.app.role.HOME <pkg>` | `dumpsys role` 里持有者变成 AerialClean，Home 解析不变 |
| 清单 `android:priority="1000"`（已保留） | `query-activities` 报告的 priority 仍是 0；tvlauncher 是 2，它赢 |

所以 `install.sh` 的可逆动作只有一步：把原桌面 `pm disable-user`，然后再按 Home 验证；
失败立刻 `pm enable` 回来。Lite 的清单里保留了 `priority="1000"`，在遵守 priority 的固件上
可以省掉这一步。

## 陷阱

- **`pm disable-user` 在这台镜像上不跨重启**：实测 `pm disable-user --user 0 com.google.android.feedback`
  → `sync` → `adb reboot` 之后它又变成 enabled；`pm list packages -d` 里只剩镜像自带的
  `com.android.vending`、`com.google.android.gms`。所以验收 A3 的"重启一次后仍成立"在模拟器上
  **无法验证**，真机待验。脚本没有为此加任何补偿动作：真机上 `disable-user` 是标准持久行为。
- 模拟器重启后 Home 会回到 tvlauncher（因为 tvlauncher 被重启重新启用），不是脚本的问题。
- `adb devices` 里同时有 `emulator-5554` 与 `127.0.0.1:5555` 时，`install.sh` 会要求用
  `--host`/`TV_HOST` 指定一台——这是有意的，避免改错设备。
- `cmd package resolve-activity` 在 Home 无默认时返回 `android/…ResolverActivity`；脚本把它当
  "没有原桌面"，不禁用、只警告。

## 做不到的事

- 4.4 真机 / 模拟器（本机没有 API 19 镜像）：`pm grant`、`cmd package …` 缺失时的降级路径
  只做过代码审查，**未验证**。
- 真实电视的厂商优先桌面（创维、TCL、小米的 priority 各不相同）：只验证了 tvlauncher 这一种。
