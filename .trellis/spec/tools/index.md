# tools/ — adb 去广告与还原

源自 [cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的脚本，负责在电脑上通过 `adb` 对电视执行 `pm disable-user`，并保证任何一步都能还原。

## 规范

| 文档 | 范围 | 状态 |
|---|---|---|
| [停用安全](debloat-safety.md) | 顺序、白名单、还原记录、4.4 差异 | 已填写 |

## 开发前检查

- 改动脚本前先读 [停用安全](debloat-safety.md)。
- 任何新增的 `pm` / `settings` / `cmd` 命令，都要在 4.4 上确认存在，或写回退。
- 不把上游脚本改成"能跑但语义变了"：优先在项目自己的 `install.sh` / `restore.sh` 里编排，`tools/` 保持与上游同构，方便跟更新。

## 质量检查

- `bash -n` 语法检查；Python 侧 `python -m py_compile`。
- 无设备时：用假 tv 命令（`TV_HOST` 指向本地脚本）验证生成的命令批次与顺序。
- 有设备时：先 `--dry-run` 看计划，再执行；执行后 `dumpsys window | grep mCurrentFocus` 与重启一次。
