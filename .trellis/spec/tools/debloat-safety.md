# 停用安全

去广告的失败模式是**把电视变成砖**，不是"少禁了一个包"。这份文档记录上游脚本已经踩过的坑，改脚本前必须对照。

## 铁律

1. **先有 Home，再禁 Home。** 只有当电视上已经存在另一个能应答 `CATEGORY_HOME` 的 App（且当前在跑）时，才允许 `pm disable-user` 原桌面。

   **接管桌面的实测结论（2026-10-26，Android 13 TV 镜像）**：想让 Home 从原桌面交到新桌面，
   下面三条"官方"路子在这个镜像上都无效，别再试：

   | 手段 | 实测结果 |
   |---|---|
   | `cmd package set-home-activity <comp>` | 打印 `Success`，`resolve-activity` 不变；连 `com.android.tv.settings/.system.FallbackHome` 也设不上 |
   | `cmd role add-role-holder android.app.role.HOME <pkg>` | role 持有者变成新桌面，Home 解析不变（只记账，不参与 `resolveHomeActivity`） |
   | 清单里给 HOME filter 加 `android:priority="1000"` | 装机后 `dumpsys package`/`query-activities` 报告的 priority 仍是 0，抢不过预装桌面（tvlauncher 是 2） |

   唯一可靠的做法是让新桌面成为**唯一**的 Home 候选：停用原桌面。因此 `install.sh` 的闸门顺序是

   1. 装 APK
   2. 直接 `am start -n <新桌面组件>` 并确认焦点落在它上面（证明它起得来）
   3. 确认它是系统列出的 HOME 候选（`cmd package query-activities -a MAIN -c HOME`）
   4. 按 Home 键，焦点在它就结束（不需要停用原桌面）
   5. 不在 → **停用原桌面**（走 `tools/disable.sh`，因此有还原记录）→ 再按 Home 验一次
   6. 仍不在 → `pm enable` 原桌面，回滚，退出码 6

   第 5 步停用的对象只有"原桌面"一个包，且新桌面已装好、已跑起来、已被系统认可为 Home 候选；
   每一步失败都在同一进程里 `enable` 回来。这就是本仓对"先禁后验"的唯一允许形态，别的组合都不行。
2. **不碰核心包。** `android`、`systemui`、`settings`、`packageinstaller`、`permissioncontroller`、`ext.services` 这些停用后系统可能起不来。上游 `disable.sh` 有 `CORE_RE` 白名单与拒绝逻辑，保留它，不放宽。
3. **只 disable，不 uninstall。** `pm disable-user --user 0` 可逆；`pm uninstall` 不可逆（尤其 `--user 0` 的系统应用）。
4. **每次禁用都写记录。** 还原依赖"当时是什么状态"，不是"默认是什么状态"。上游用 `recovery.py` 的记录文件，不要绕过它直接拼命令。
5. **小批量试，每批验证。** 一次禁用 40 个包和一次禁用 4 个包，出问题的排查成本差一个数量级。
6. **ADB 入口本身可能是被禁用的对象。** 某些机型（如创维 8R40）的 ADB 开关藏在依赖特定包的设置页里，禁掉那个包就再也连不上，只能恢复出厂。

## 4.4（API 19）差异

| 上游用法 | 4.4 上 | 处理 |
|---|---|---|
| `pm disable-user --user 0` | 可用（4.2+） | 保留 |
| `pm enable` | 可用 | 保留 |
| `cmd package resolve-activity/query-activities` | 不可用（7.0+） | 回退 `dumpsys activity activities` + `pm list packages`，或直接要求用户指定原桌面包名 |
| `settings put secure enabled_accessibility_services` | 可用 | 保留 |
| `cmd notification allow_listener` | 不可用（7.0+） | 改 `settings put secure enabled_notification_listeners <pkg>/<cls>` |
| `pm grant … WRITE_SECURE_SETTINGS` | 不可用（6.0+） | 不做需要它的功能 |
| `appops set … SYSTEM_ALERT_WINDOW` | 不可用（6.0+） | 4.4 安装即授予，跳过 |
| `dumpsys window \| grep mCurrentFocus` | 可用 | 保留 |

## 验收（真机）

1. 安装桌面 App，`am start -W` 返回 `Status: ok`。
2. `dumpsys window | grep mCurrentFocus` 显示新桌面。
3. 跑停用列表，再次确认 Home 仍然回到新桌面。
4. 断电重启，重复第 2 步。
5. 跑一次 `restore.sh`，确认原桌面回来、被禁的包全部恢复。
