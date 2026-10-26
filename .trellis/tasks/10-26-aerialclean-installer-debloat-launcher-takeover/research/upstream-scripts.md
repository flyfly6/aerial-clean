# 上游脚本清单（可直接复用的部分）

来源：`Gathin23/cleanyourtv`，MIT。以下为 2026-10-26 clone 实测。

## tools/disable.sh

- 输入：一个 `debloat.txt`（每行一个包名，`#` 注释）。
- 校验：包名形状；核心包白名单 `CORE_RE`（`android`、`systemui`、`settings`、`packageinstaller`、`permissioncontroller`、`ext.services`、`modulemetadata`、overlay 等）→ 命中即整单拒绝。
- Setupwraith 保护：禁用前用 `dumpsys wifi` 判断当前 Wi-Fi 是否由它持有，是则跳过（除非 `FORCE_SETUPWRAITH=1`）。
- Home guard：`cmd package resolve-activity --brief -a MAIN -c HOME`（脚本注释：Android 7 加入）；查不到时回退 `dumpsys activity`。没有第二个 HOME 提供者时拒绝禁用原桌面（`FORCE_HOME=1` 可强制）。
- 执行：`pm disable-user --user 0 <pkg>`，随后回读结果确认是否真的被接受。
- 副作用：写还原记录（供 `restore.sh` 使用）。

## tools/restore.sh

- 用法：`restore.sh <list> [stockLauncherPackage]`。
- 调 `tools/recovery.py --commands <list> <stock>` 生成批次（基于记录，已是禁用状态的保持禁用），再由 `tv.py --batch` 执行。

## launcher/build-and-install.sh（本仓不复用，但顺序照抄）

- 从 `launcher/app/build.gradle` 读 `minSdk`，先与设备 SDK 比对，避免 `INSTALL_FAILED_OLDER_SDK`。
- `gradlew assembleRelease` → `b64push.py` 推送 → `pm install`。
- `STOCK_LAUNCHER=<pkg>` 指定原桌面，`STOCK_LAUNCHER=none` 不禁用。
- 安装后先 `am force-stop` 再 `am start -W`，确认 `Status: ok` 才禁用原桌面。

## 结论

本仓 `install.sh` 的差异化只有三点：APK 来源（Lite）、4.4 的显式 `--stock-home`、以及"验证 Home"作为独立闸门（上游 guard 只防禁到最后一个 Home，不防新桌面起不来）。
