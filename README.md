# AerialClean

给老安卓电视的一块干净桌面，加一条命令的去广告安装器。

AerialClean 把两件事合到一个仓库：

- **桌面** — 从零写的 Android TV launcher，`minSdk 19`（Android 4.4 KitKat）起步，视觉沿用 [aerial-tv](https://github.com/pengchujin/aerial-tv) 的航拍风格：底部玻璃 dock、下方应用网格、单层焦点、时钟与屏保。**不用 Compose**，因为 Compose 的地板是 API 21。
- **安装器** — 复用 [cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的 `pm disable-user` 工具链（备份、Home guard、一条命令还原），负责把桌面装成 Home，再按机型列表停用广告/遥测/预装应用。

不插线、不 root、不卸载系统应用；每一步都能用一条命令还原。

## 安装

需要：电脑上有 `adb` 与 Python 3.8+，电视打开 ADB 调试并与电脑同一网段。

```bash
# 上游脚本的依赖，只需一次
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt

# 装桌面并接管 Home（先跑一遍 --dry-run 看清要执行什么）
./install.sh --host 192.168.1.50 --dry-run
TV_HOST=192.168.1.50 ./install.sh --apk lite/app/build/outputs/apk/debug/app-debug.apk

# 顺手去广告（列表先用 examples/ 里最接近的机型）
TV_HOST=192.168.1.50 ./install.sh --list examples/google-atv-emulator/debloat.txt
```

Windows 用 PowerShell 版本，参数一一对应：

```powershell
.\install.ps1 -TvHost 192.168.1.50 -DryRun
.\install.ps1 -TvHost 192.168.1.50 -List examples\google-atv-emulator\debloat.txt
```

顺序是产品需求，不是实现细节：装 APK → 拉起它 → 确认它是系统的 HOME 候选 → 按 Home 看焦点
→ 焦点不在新桌面时**才**停用原桌面（失败立刻启用回来）→ 跑停用列表 → 再验一次 Home。
`--keep-stock-home` 表示"只去广告，别动我的桌面"，这时 Home 会留在原桌面，脚本会明说。

停止条件与退出码：`2` 用法、`3` 依赖缺失、`4` 设备不可达、`5` APK 装不上、`6` Home 没切过来
（已回滚）、`7` 停用阶段失败。任何非 0 退出最后一行都打印还原命令。

## 还原

```bash
TV_HOST=192.168.1.50 ./restore.sh              # 恢复被停用的包 + 把 Home 还给原桌面
TV_HOST=192.168.1.50 ./restore.sh --uninstall  # 再把 AerialClean 卸掉
```

`restore.sh` 读 `~/.aerialclean/state/<设备序列号>.json`（`install.sh` 写的）与上游
`recovery.py` 的还原记录。还原顺序：`pm enable` 所有记录的包 → 启用原桌面 → 按 Home 验证焦点；
若新桌面还占着 Home，会**停用**它（可逆，再跑一次 `install.sh` 自动启用回来）。

## 机型与陷阱

- `examples/<机型>/debloat.txt`：停用列表，一行一个包名；`tv.json` 记录来源与验证状态。
- `docs/devices/<机型>.md`：实测输出与陷阱。已有：[Google ATV 模拟器](docs/devices/google-atv-emulator.md)。
- 首次做新机型：先只 `--keep-stock-home`（不碰桌面），列表一次只加几个包，重启验证后再加。

## 目录

```
aerial-clean/
├─ lite/            Android 4.4+ 桌面（Java，minSdk 19）
├─ lib/installer.py 安装/还原的全部逻辑（bash 与 PowerShell 都只是启动器）
├─ tools/           debloat / restore 脚本（原样取自 cleanyourtv，见 tools/UPSTREAM.md）
├─ examples/        按机型整理的停用列表
├─ install.sh  install.ps1    安装并按顺序接管
├─ restore.sh  restore.ps1    还原
├─ docs/            机型笔记与兼容说明
└─ .trellis/        计划、任务、规范
```

## 开发环境

- JDK 17（AGP 要求；`/opt/homebrew/opt/openjdk`）
- Android SDK：`platforms;android-34`、`build-tools;34.0.0`
- 验证用设备：AVD `aerial_tv_api33`（`system-images;android-33;android-tv;arm64-v8a`）。
  本机没有 API 19/21 镜像，4.4 相关结论一律标注"未验证"。

## 许可

MIT。上游归属见 [NOTICE.md](NOTICE.md)。
