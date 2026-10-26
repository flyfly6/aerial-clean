# 停用安全

去广告的失败模式是**把电视变成砖**，不是"少禁了一个包"。这份文档记录上游脚本已经踩过的坑，改脚本前必须对照。

## 铁律

1. **先有 Home，再禁 Home。** 只有当电视上已经存在另一个能应答 `CATEGORY_HOME` 的 App（且当前在跑）时，才允许 `pm disable-user` 原桌面。
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
