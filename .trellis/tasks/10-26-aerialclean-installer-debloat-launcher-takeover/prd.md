# AerialClean 安装器：debloat + 桌面接管

## Goal

把「装桌面」和「去广告」编排成一条可逆的流程：安装 `lite/` 的 APK → 确认它真的接管了 Home → 再按机型列表停用预装/广告/遥测包。任何一步失败都停在那里，不留半接管状态；一条命令可以完整还原。

去广告的能力来自 cleanyourtv 的 `pm disable-user` 工具链（本仓 `tools/`），本任务负责**顺序、校验与还原**，不改写它的语义。

## Background

- 已核实：`cleanyourtv` 的 `disable.sh` 内置 Home guard——当电视上没有第二个能应答 `CATEGORY_HOME` 的 App 时，它拒绝禁用原桌面（避免黑屏砖机）。因此"先装桌面、后禁桌面"不是优化，是前置条件。
- 已核实：`launcher/build-and-install.sh` 的顺序值得直接沿用——先读 Gradle 的 minSdk 预检，`am start -W` 确认 `Status: ok`，再动原桌面，且支持 `STOCK_LAUNCHER=none`。
- 已核实：4.4 上 `pm grant`（6.0+）、`cmd package resolve-activity`（7.0+）、`cmd notification allow_listener`（7.0+）不可用；`settings put secure …`、`pm disable-user --user 0`、`dumpsys window` 可用。
- 上游覆盖机型最低 Android 5.1，没有 4.4 样本；首批 4.4 列表必须小批量验证。

## 用户与场景

- 用户：有电脑、和电视同一 Wi-Fi，愿意在电视上打开 ADB 调试。
- 场景 A：只想要干净桌面 → 装 Lite，不禁用任何包。
- 场景 B：桌面 + 去广告 → 装 Lite，确认接管后跑机型列表。
- 场景 C：后悔了 → `restore.sh` 恢复原桌面与被禁包，保留或卸载 Lite。

## 范围

### 做（v1）

| 编号 | 需求 | 验收要点 |
|---|---|---|
| FR-1 | `install.sh` / `install.ps1`：参数 `TV_HOST`、`--apk <path>`、`--list <file>`、`--no-debloat`、`--keep-stock-home`、`--dry-run` | 无参数时打印用法并以非 0 退出 |
| FR-2 | 安装前预检：`adb connect` 可达、APK 的 minSdk ≤ 设备 SDK、设备是 TV（`ro.build.characteristics` 含 tv 或 `pm list features` 有 leanback，取其一即可，不满足只警告） | 设备是 4.4 时给出明确提示，不静默失败 |
| FR-3 | 安装并接管 Home：`pm install -r` → `am start -n <新桌面组件>` 并确认焦点在它上面 → 确认它是系统列出的 HOME 候选（`cmd package query-activities`，7.0+）→ 按 Home 键看焦点 → 焦点不在新桌面时才停用原桌面，再验一次 | 新桌面起不来或不是 HOME 候选时中止，一个包都不停用；停用原桌面后仍失败则立刻 enable 回来并退出 6 |
| FR-4 | 原桌面识别与处置：`--keep-stock-home` 跳过；`--stock-home <pkg>` 显式指定（必须已安装）；否则 SDK ≥ 24 用 `resolve-activity` 自动识别，7.0 以下只警告不猜 | 4.4 上不猜、不盲禁；`--stock-home` 指到不存在的包时设备零改动 |
| FR-5 | 复用 `tools/disable.sh` 执行列表；记录与 Home guard 行为不变 | 禁用后再次确认 Home 仍是 Lite |
| FR-6 | `restore.sh` / `restore.ps1`：`pm enable` 全部记录项 → 恢复原桌面 → `am start` 验证 → 可选 `--uninstall` 卸载 Lite | 恢复后 `mCurrentFocus` 是原桌面 |
| FR-7 | `--dry-run` 输出完整命令批次，不执行 | 输出可逐条人工核对 |
| FR-8 | `examples/<model>/`：`debloat.txt` + `tv.json`（品牌、型号、Android 版本、RAM、备注） | 列表带来源与验证状态 |
| FR-9 | 文档：`README` 的安装/还原、`docs/devices/<model>.md` 记录陷阱 | 每一步都能被用户复现 |

### 不做（v1）

| 项 | 原因 |
|---|---|
| 图形界面 / 手机 App | 上游已有社区 App；本仓定位是 CLI 与可审计的脚本 |
| 自动发现电视 IP | 依赖 mDNS/SSDP，失败模式多，收益低 |
| 自动生成 debloat 列表 | 需要人工判断"这个包用户是否在用"，自动推断会误禁 |
| 修改 cleanyourtv 上游脚本语义 | 未来无法跟上游更新 |
| 装 ADB/平台工具 | 让用户用系统包管理器或 Android SDK |

## 约束

- C1 不改 `tools/` 中上游脚本的核心逻辑；新增能力放在 `install.sh` / `restore.sh` 里编排。
- C2 只 `disable-user`，绝不 `uninstall`（除 `restore.sh --uninstall` 卸载本项目的 Lite）。
- C3 顺序硬约束：**只有在新桌面已安装、已拉起、且被系统列为 HOME 候选之后，才允许停用原桌面这一个包；停用后立刻验证 Home，失败当场 `pm enable` 回来**。除此之外不允许任何 `pm disable-user` 排在 Home 验证之前。（理由：Android 13 TV 镜像上 `set-home-activity`、HOME role、清单 priority 都不生效，见 `.trellis/spec/tools/debloat-safety.md`）
- C4 支持 Windows（PowerShell 优先，Git Bash 可用）与 macOS/Linux。
- C5 脚本必须可重复执行（幂等）：重复 `install.sh` 不产生重复禁用记录。
- C6 所有对外命令都打印原始 `adb` 输出，不吞错误。

## 验收标准

- A1 `bash -n install.sh restore.sh`、`python -m py_compile tools/*.py` 通过；PowerShell 侧 `pwsh -NoProfile -Command "& { [void][ScriptBlock]::Create((Get-Content -Raw install.ps1)) }"` 无解析错误。
- A2 `./install.sh --dry-run` 在无设备时给出依赖缺失的明确错误（非 0 退出），在有设备时输出完整批次且不改变设备状态。
- A3 `install.sh` 后 `dumpsys window | grep mCurrentFocus` 含 `io.github.flyfly6.aerialclean`；重启一次后仍成立。前半段已在 AVD 上验证；后半段在该模拟器上**无法验证**（镜像重启会恢复 `pm disable-user` 状态），真机待验。
- A4 真机：带 `--list` 执行后，禁用项数 = 列表去重后的包数；再按一次 Home 仍回到 Lite。
- A5 真机：`restore.sh` 后原桌面恢复、被禁包全部 enable；`restore.sh --uninstall` 后 Lite 被卸载。
- A6 4.4 真机或模拟器：`install.sh` 在 `pm grant` / `cmd` 缺失时不报错、不静默跳过安全步骤，并在输出中说明跳过了什么。
- A7 顺序证明：人为把 `--stock-home` 指到一个不存在的包时，脚本在禁用前中止，设备无任何变更。

## 风险

| 风险 | 影响 | 兜底 |
|---|---|---|
| 禁用依赖 ADB 入口的包（如创维 8R40 的 `com.tianci.de`） | 电视再也连不上，只能恢复出厂 | 首批列表逐批小步验证；文档记录该机型陷阱；`disable.sh` 的核心包白名单不放宽 |
| 4.4 上无法自动识别原桌面 | 猜错 = 黑屏 | 强制显式 `--stock-home`，默认不动原桌面 |
| 用户在同一台电视上重复运行 install | 重复记录、状态混乱 | 幂等设计 + 运行前读上一次记录并提示 |
| Windows 与 bash 行为不一致 | 一半用户跑不通 | 逻辑只有 `lib/installer.py` 一份，`*.sh` 与 `*.ps1` 是同一入口的启动器；本地对两者做语法检查 |
| 官方换桌面手段在部分固件失效（`set-home-activity` / HOME role / 清单 priority） | 装上去抢不过原桌面，Home 不交出来 | 已实测并写进 `.trellis/spec/tools/debloat-safety.md`；唯一可靠路径是停用原桌面，配套三道闸门与失败回滚 |

## 依赖

- 依赖任务 01 产出的 APK 路径与包名：`lite/app/build/outputs/apk/debug/app-debug.apk`、`io.github.flyfly6.aerialclean`。任务 01 未完成时，可以用 `--apk` 指向任意第三方桌面包做联调。
