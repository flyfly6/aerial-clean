# 实施计划：安装器

前置约定：
- 每个里程碑结束都要能跑 `bash -n` / `py_compile` / `pwsh` 解析；涉及设备的验收必须贴原始输出。
- 真机验证前先确认电视的 ADB 已开、电脑与电视同网段。
- 顺序是产品需求（`.trellis/spec/tools/debloat-safety.md`）：任何"先停用后证明"的写法都算缺陷，
  唯一允许的形态见该规范第 1 条。

## I0 引入上游工具（固定版本）

1. 从 cleanyourtv 取 `tools/{disable.sh,disable.ps1,restore.sh,restore.ps1,tv.py,recovery.py,check.py}`
   与 `tools/requirements.txt`，上游 commit 记进 `tools/UPSTREAM.md`。
2. `python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt`。
3. 不修改 `tools/` 内的逻辑（`git diff tools/` 只应看到上游自己的改动）。

验证：
```bash
bash -n tools/disable.sh tools/restore.sh
python3 -m py_compile tools/tv.py tools/recovery.py tools/check.py
TV_HOST=127.0.0.1:5555 tools/venv/bin/python -I tools/tv.py --probe
```

## I1 入口与预检

1. `lib/installer.py`：参数解析、`--help`、退出码、`adb` 定位（`ADB` 环境变量 / PATH）、
   设备选择（`--host` / `TV_HOST` / 单台已连设备，多台要求显式指定）。
2. 预检：APK 存在、minSdk ≤ 设备 SDK、读包名与启动 Activity（aapt2，缺失时读 `lite/app/build.gradle.kts`）、
   设备是不是电视（不满足只警告）、`--list` 存在且每行是包名。
3. `--dry-run` 只打印批次，不改设备。

验证：
```bash
./install.sh --help; echo $?                       # 0
./install.sh --nope; echo $?                       # 2
./install.sh --dry-run --apk /nonexistent.apk; echo $?   # 3
ADB=/nonexistent/adb ./install.sh --dry-run; echo $?     # 3
ADB=/usr/bin/true ./install.sh --dry-run; echo $?        # 4
```

## I2 安装 + 三道 Home 闸门 + 回滚

1. 已停用的 Lite 先 `pm enable`；`adb install -r`；失败按输出给 5。
2. 闸门 1：`am start -n <组件>`，焦点必须是它（证明起得来）。
3. 闸门 2：`cmd package query-activities -a MAIN -c HOME` 里必须有它（7.0 以下跳过并说明）。
4. 闸门 3：`input keyevent KEYCODE_HOME` 后焦点是它（多读几次，窗口切换期间是 null）。
5. 闸门 3 失败且原桌面已知 → `tools/disable.sh <只含原桌面的列表>` → 再验；仍失败 → `pm enable`
   原桌面 + 回滚。
6. 任何回滚都打印"已卸载本次安装的包 / 已重新启用原桌面"。

验证（AVD）：
```bash
./install.sh --host 127.0.0.1:5555 --list examples/google-atv-emulator/debloat.txt
adb shell "pm list packages -d"
adb shell "dumpsys window | grep mCurrentFocus"
```

## I3 停用列表与二次验证

1. 调 `tools/disable.sh <list>`，保留它的白名单、Home guard 与记录。
2. 只在**已经接管 Home** 时才做停用后的 Home 复验（`--keep-stock-home` 时 Home 本来在原桌面）。
3. 重复运行沿用上游"第一份记录为准"，不产生第二份。

验证：
```bash
adb shell "pm list packages -d" | grep -c .        # 停用前后计数差 = 列表行数
adb shell input keyevent KEYCODE_HOME; adb shell "dumpsys window | grep mCurrentFocus"
```

## I4 还原

1. 读 `~/.aerialclean/state/<序列号>.json`；没有记录就明确报错（3）。
2. 每个列表跑 `tools/restore.sh <list> <stock_pkg>`；再 `pm enable <stock>`。
3. 按 Home 验焦点：还在 Lite → `pm disable-user` 停用它（可逆，写进记录）再验。
4. `--uninstall` 卸载 Lite 并再验一次原桌面。

验证：
```bash
./restore.sh --host 127.0.0.1:5555 --dry-run
./restore.sh --host 127.0.0.1:5555
./restore.sh --host 127.0.0.1:5555 --uninstall
```

## I5 PowerShell 启动器

1. `install.ps1` / `restore.ps1`：参数映射到同一份 `lib/installer.py`，找 `python3`/`python`/`py -3`。
2. 语法检查 + `-Help` / `-DryRun` 实跑。

验证：
```bash
pwsh -NoProfile -Command '& { [void][ScriptBlock]::Create((Get-Content -Raw install.ps1)); "ok" }'
pwsh -NoProfile -File install.ps1 -Help
pwsh -NoProfile -File install.ps1 -TvHost 127.0.0.1:5555 -DryRun
```

## I6 文档、样例与验收

1. `docs/devices/<model>.md`：机型、Android 版本、RAM、实测输出、陷阱。
2. `examples/<model>/debloat.txt` + `tv.json`；未实测的条目在文件头注明。
3. `README`：安装、还原、退出码、`--keep-stock-home` 的含义。
4. 走一遍 PRD 的 A1–A7，结果写进 `docs/devices/google-atv-emulator.md`。

## 提交点

- 每个里程碑汇报改动与验证结果，得到用户同意再 `git commit`。
- 提交信息：`feat(installer): <里程碑>`；任务目录一并提交。
