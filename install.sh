#!/usr/bin/env bash
# AerialClean 安装器入口。逻辑只在 lib/installer.py 里写一份：bash 与 PowerShell 都是启动器，
# 两个平台的命令顺序因此不会各写一遍再走样。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PY=$(command -v python3 || command -v python || true)
[ -n "$PY" ] || { echo "需要 Python 3.8+（找不到 python3）" >&2; exit 3; }
exec "$PY" "$ROOT/lib/installer.py" install "$@"
