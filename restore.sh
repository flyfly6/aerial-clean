#!/usr/bin/env bash
# 还原入口：按 install.sh 写下的记录恢复被停用的包与原桌面。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PY=$(command -v python3 || command -v python || true)
[ -n "$PY" ] || { echo "需要 Python 3.8+（找不到 python3）" >&2; exit 3; }
exec "$PY" "$ROOT/lib/installer.py" restore "$@"
