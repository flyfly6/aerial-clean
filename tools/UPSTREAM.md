# tools/ 来自上游 cleanyourtv

这些文件原样取自 [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)（MIT），
本仓不改它们的逻辑，只由顶层的 `install.sh` / `restore.sh` 编排调用。

- 上游 commit：`f592b6d2db0cfa0e28779660f25ff65386a5ff9b`
- 取的文件：`disable.sh`、`disable.ps1`、`restore.sh`、`restore.ps1`、`tv.py`、
  `recovery.py`、`check.py`、`requirements.txt`
- 取的时间：2026-10-26

## 升级

```bash
SHA=<新的上游 commit>
for f in disable.sh disable.ps1 restore.sh restore.ps1 tv.py recovery.py check.py requirements.txt; do
  curl -fsS -o "tools/$f" "https://raw.githubusercontent.com/Gathin23/cleanyourtv/$SHA/tools/$f"
done
tools/venv/bin/pip install -r tools/requirements.txt   # 依赖变了才需要
```

升级后把上面的 commit 改成新的；`git diff tools/` 应该只显示上游自己的改动。
本仓需要的顺序、校验与还原都在 `lib/installer.py`，不要往这里加代码。

## 依赖

`tv.py` 走网络 ADB（`adb-shell`），所以 `TV_HOST` 必须可达：

```bash
python3 -m venv tools/venv
tools/venv/bin/pip install -r tools/requirements.txt
```
