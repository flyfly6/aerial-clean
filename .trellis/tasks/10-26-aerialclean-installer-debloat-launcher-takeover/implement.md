# 实施计划：安装器

前置约定：
- 每个里程碑结束都要能跑 `bash -n` / `py_compile`；涉及设备的验收必须贴原始 `adb` 输出。
- 真机验证前先确认电视的 ADB 已开、电脑与电视同网段。
- 顺序是产品需求，不是实现细节：任何"先禁用后验证"的写法都视为缺陷。

## I0 引入上游工具（固定版本）

1. 从 cleanyourtv 取 `tools/{disable.sh,restore.sh,tv.py,recovery.py,check.py}` 与 `tools/requirements.txt`，记录上游 commit 到 `tools/UPSTREAM.md`。
2. `tools/venv` 安装依赖（`tv.py` 需要 `adb-shell` 之类的依赖，按上游 requirements 为准）。
3. 不修改 `tools/` 内的逻辑；确需改动时写进 `tools/PATCHES.md` 并说明原因。

验证：
```bash
bash -n tools/disable.sh tools/restore.sh
python -m py_compile tools/tv.py tools/recovery.py tools/check.py
python -c "import sys; print(sys.version)"
```

## I1 `install.sh` 骨架：参数、预检、dry-run

1. 参数解析：`TV_HOST`（或用 `--host`）、`--apk`、`--list`、`--no-debloat`、`--keep-stock-home`、`--stock-home <pkg>`、`--dry-run`、`--help`。
2. 预检：`adb`、`python`、APK 文件是否存在；`adb connect`；`getprop ro.build.version.sdk`；APK 的 `minSdk`（`aapt2 dump badging` 或 `apkanalyzer`，都没有时从 `lite/app/build.gradle.kts` 读）。
3. `--dry-run`：只打印将要执行的命令，一行一条，不改变设备状态。

验证：
```bash
./install.sh --help
./install.sh --dry-run --apk /nonexistent.apk    # 退出码 3，明确指出缺什么
./install.sh --dry-run --apk <真apk> --stock-home com.example/.Home
```

## I2 安装 + Home 验证 + 回滚

1. `adb install -r <apk>`；失败时打印原始输出并以 5/6 退出。
2. `am start -W -a android.intent.action.MAIN -c android.intent.category.HOME`。
3. `dumpsys window | grep mCurrentFocus` 与目标包名比对；不匹配先尝试直接 `am start -n <pkg>/.HomeActivity` 再比一次。
4. 仍不匹配：卸载本次安装的包，退出码 6，并提示"该机型可能硬编码 Home"。

验证（模拟器/真机）：
```bash
adb shell "dumpsys window | grep mCurrentFocus"
adb shell input keyevent KEYCODE_HOME
adb shell "dumpsys window | grep mCurrentFocus"
```

## I3 原桌面禁用 + 列表执行

1. `stock_home` 决策：`--keep-stock-home` > `--stock-home` > 记录文件里的旧值 > 尝试自动探测（SDK ≥ 24 用 `cmd package resolve-activity`）> 4.4 且无法探测时中止。
2. 禁用原桌面前再查一次"是否还有别的 App 能应答 HOME"（`pm list packages` + `dumpsys`，SDK 允许时用 `cmd package query-activities`）。
3. 调 `tools/disable.sh <list>`，保留它的白名单与 guard；结束后再验 Home。
4. `--no-debloat` 时跳过整段。

验证：
```bash
adb shell "pm list packages -d" | grep -c .        # 禁用前后计数变化
adb shell input keyevent KEYCODE_HOME && adb shell "dumpsys window | grep mCurrentFocus"
```

## I4 `restore.sh`

1. 读记录 → `tools/restore.sh <list> [stock_home]` 恢复禁用项。
2. `pm enable <stock_home>`（如果当初禁用过），`am start -W -a MAIN -c HOME` 验证原桌面回来。
3. `--uninstall` 时 `pm uninstall <pkg>`。
4. 保留记录文件（不删），便于再次安装时复用 `stock_home`。

验证：
```bash
./restore.sh --dry-run
./restore.sh
adb shell "dumpsys window | grep mCurrentFocus"    # 原桌面
```

## I5 PowerShell 对齐

1. `install.ps1` / `restore.ps1`：参数、顺序、退出码与 bash 版一致；共享逻辑调同一个 Python。
2. 语法检查 + 用假 `adb.cmd`（PATH 前置）跑 `-DryRun`。

验证：
```powershell
pwsh -NoProfile -File install.ps1 -Help
pwsh -NoProfile -File install.ps1 -DryRun -Apk <apk>
```

## I6 文档、样例与 4.4 验收

1. `docs/devices/<model>.md` 模板：机型、SoC、Android 版本、RAM、ADB 入口、陷阱、实测输出。
2. `examples/<model>/debloat.txt` + `tv.json`；未实测的条目在文件头注明。
3. README 补：安装、还原、4.4 的差异（`--stock-home` 必须显式给）。
4. 走一遍 PRD 的 A1–A7。

## 提交点

- 每个里程碑汇报改动与验证结果，得到用户同意再 `git commit`。
- 提交信息：`feat(installer): <里程碑>`；任务目录一并提交。
