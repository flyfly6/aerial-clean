#!/usr/bin/env python3
"""AerialClean 安装器核心：装桌面 → 让它接管 Home →（可选）按列表停用预装包。

    installer.py install [--host H] [--apk F] [--list F] [--no-debloat]
                         [--keep-stock-home] [--stock-home PKG] [--dry-run]
    installer.py restore [--host H] [--uninstall] [--dry-run]

顺序是产品需求（.trellis/spec/tools/debloat-safety.md）：停用原桌面之前，必须先证明新桌面
装得上、起得来、是被系统认可的 HOME 候选；停用之后立刻验证 Home，失败当场 enable 回来。
停用本身仍交给上游 tools/disable.sh，本文件只决定"什么时候可以动"和"怎么还原"。
"""
import datetime
import hashlib
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
import time
from pathlib import Path

EXIT_OK, EXIT_USAGE, EXIT_DEP, EXIT_DEVICE, EXIT_APK, EXIT_HOME, EXIT_DEBLOAT = 0, 2, 3, 4, 5, 6, 7

REPO = Path(__file__).resolve().parent.parent
PKG_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")
FOCUS_RE = re.compile(r"mCurrentFocus=Window\{[^}]*?\s([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)\}")
COMPONENT_RE = re.compile(r"^([A-Za-z_][A-Za-z0-9_.]*)/([A-Za-z0-9_.$]+)$")
DEFAULT_APK = REPO / "lite/app/build/outputs/apk/debug/app-debug.apk"
STATE_DIR = Path.home() / ".aerialclean/state"
FOCUS_TRIES = 8   # Home 键之后焦点要等窗口切换，读一次太早会读到 null
FOCUS_GAP_S = 0.5

USAGE_INSTALL = """用法: ./install.sh [选项]

  --host HOST        电视地址，也可用环境变量 TV_HOST；没有端口时补 :5555
  --apk PATH         要安装的桌面 APK（默认 lite/app/build/outputs/apk/debug/app-debug.apk）
  --list FILE        接管 Home 之后要执行的停用列表
  --no-debloat       不停用任何包（优先于 --list）
  --keep-stock-home  保留原桌面；不能保证 Home 交给新桌面
  --stock-home PKG   显式指定原桌面包名（Android 7 以下必填，否则不禁用）
  --dry-run          打印完整命令批次，不改动设备
  --help             显示本帮助
"""

USAGE_RESTORE = """用法: ./restore.sh [选项]

  --host HOST    电视地址，也可用环境变量 TV_HOST
  --uninstall    还原之后卸载 AerialClean
  --dry-run      打印完整命令批次，不改动设备
  --help         显示本帮助
"""


class Abort(Exception):
    def __init__(self, code, message):
        super().__init__(message)
        self.code = code
        self.message = message


def say(message=""):
    print(message, flush=True)


def warn(message):
    print(message, file=sys.stderr, flush=True)


# ---------------------------------------------------------------- 进程与设备

def adb_binary():
    binary = os.environ.get("ADB")
    if binary:
        if not (shutil.which(binary) or Path(binary).is_file()):
            raise Abort(EXIT_DEP, f"ADB={binary} 不存在：改成 adb 的真实路径，或删掉这个变量从 PATH 里找。")
        return binary
    found = shutil.which("adb")
    if not found:
        raise Abort(EXIT_DEP, "找不到 adb：把它放进 PATH，或用 ADB=/path/to/adb 指定。")
    return found


class Adb:
    """一台设备上的 adb。dry_run 只影响会改动设备的命令，读状态的命令照常执行。"""

    def __init__(self, serial, dry_run):
        self.base = [adb_binary()] + (["-s", serial] if serial else [])
        self.dry_run = dry_run

    def run(self, args, mutating=False, check=True):
        if mutating and self.dry_run:
            say("$ " + shlex.join(self.base + args))
            return ""
        if not mutating:
            say("$ " + shlex.join(self.base + args))
        proc = subprocess.run(self.base + args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        if check and proc.returncode != 0:
            raise Abort(EXIT_DEVICE, f"adb {' '.join(args)} 失败（退出码 {proc.returncode}）：\n{proc.stdout.strip()}")
        if mutating and proc.stdout.strip():
            say(proc.stdout.rstrip())
        return proc.stdout

    def shell(self, command, mutating=False, check=True):
        return self.run(["shell", command], mutating=mutating, check=check)

    def focus(self):
        out = self.shell("dumpsys window | grep mCurrentFocus")
        found = FOCUS_RE.search(out)
        return (found.group(1), found.group(2)) if found else (None, None)


def connect(host):
    """TV_HOST → 可用的 adb 序列号。-s 的取值就是 host[:port]。"""
    serial = host if ":" in host else f"{host}:5555"
    proc = subprocess.run([adb_binary(), "connect", serial], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    out = proc.stdout.strip()
    if proc.returncode != 0 or "connected" not in out:
        raise Abort(EXIT_DEVICE, f"连不上 {serial}：\n{out}")
    if out != f"already connected to {serial}":
        say(out)
    return serial


def pick_serial(host):
    if host:
        return connect(host)
    proc = subprocess.run([adb_binary(), "devices"], stdout=subprocess.PIPE, text=True)
    ready = [line.split()[0] for line in proc.stdout.splitlines()[1:] if line.strip().endswith("device")]
    if not ready:
        raise Abort(EXIT_DEVICE, "没有已连接的设备：把电视的 ADB 打开，用 --host <电视IP> 或 TV_HOST 指定地址。")
    if len(ready) > 1:
        raise Abort(EXIT_DEVICE, "连接了多台设备，用 --host/TV_HOST 指定一台：\n  " + "\n  ".join(ready))
    return ready[0]


def device_facts(adb):
    sdk = adb.shell("getprop ro.build.version.sdk").strip()
    if not sdk.isdigit():
        raise Abort(EXIT_DEVICE, f"读不到设备 SDK 版本（getprop 返回 {sdk!r}）。")
    facts = {
        "sdk": int(sdk),
        "model": adb.shell("getprop ro.product.model").strip(),
        "serialno": adb.shell("getprop ro.serialno").strip(),
        "installed": package_set(adb),
        "disabled": package_set(adb, "-d"),
    }
    if "tv" not in adb.shell("getprop ro.build.characteristics").strip().lower():
        if "leanback" not in adb.shell("pm list features"):
            warn(f"警告: {facts['model']} 看起来不是电视（没有 tv 特征也没有 leanback），继续。")
    return facts


def package_set(adb, flag=""):
    out = adb.shell(f"pm list packages {flag}".strip())
    return {line.strip()[len("package:"):] for line in out.splitlines() if line.startswith("package:")}


def state_key(facts, serial):
    raw = facts.get("serialno") or serial or facts.get("model") or "device"
    return re.sub(r"[^A-Za-z0-9_.-]", "_", raw)


def load_state(key):
    path = STATE_DIR / f"{key}.json"
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as error:
        raise Abort(EXIT_DEP, f"记录文件读不出来：{path}（{error}）；删掉它再跑一次。")


def save_state(key, state):
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    path = STATE_DIR / f"{key}.json"
    path.write_text(json.dumps(state, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return path


# ---------------------------------------------------------------- APK 识别

def aapt2():
    if os.environ.get("AAPT2") and Path(os.environ["AAPT2"]).exists():
        return os.environ["AAPT2"]
    for root in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"),
                 str(Path.home() / "Library/Android/sdk"), str(Path.home() / "Android/Sdk")):
        if not root:
            continue
        build_tools = Path(root) / "build-tools"
        if build_tools.is_dir():
            found = sorted((p / "aapt2" for p in build_tools.iterdir() if (p / "aapt2").exists()), reverse=True)
            if found:
                return str(found[0])
    return shutil.which("aapt2")


def badging(apk):
    tool = aapt2()
    if not tool:
        return ""
    proc = subprocess.run([tool, "dump", "badging", str(apk)], stdout=subprocess.PIPE,
                          stderr=subprocess.DEVNULL, text=True)
    return proc.stdout if proc.returncode == 0 else ""


def apk_identity(apk):
    """(包名, Home activity 全名)。aapt2 优先，读不到就退回仓库里的 build.gradle.kts。"""
    out = badging(apk)
    package = re.search(r"^package: name='([^']+)'", out, re.M)
    if package:
        activity = re.search(r"^launchable-activity: name='([^']+)'", out, re.M)
        return package.group(1), (activity.group(1) if activity else None)
    if apk == DEFAULT_APK:
        gradle = (REPO / "lite/app/build.gradle.kts").read_text(encoding="utf-8")
        package = re.search(r'applicationId\s*=\s*"([^"]+)"', gradle)
        if package:
            return package.group(1), f"{package.group(1)}.HomeActivity"
    raise Abort(EXIT_DEP, f"读不出 {apk} 的包名：装 build-tools 里的 aapt2，或把 AAPT2 指过去。")


def min_sdk(apk):
    found = re.search(r"(?:min)?[Ss]dkVersion:'(\d+)'", badging(apk))
    if found:
        return int(found.group(1))
    if apk == DEFAULT_APK:
        found = re.search(r"minSdk\s*=\s*(\d+)", (REPO / "lite/app/build.gradle.kts").read_text(encoding="utf-8"))
        if found:
            return int(found.group(1))
    return None


def home_candidates(adb):
    """设备上所有能应答 HOME 的组件（Android 7 以下没有这条命令，返回空集）。"""
    out = adb.shell("cmd package query-activities --brief -a android.intent.action.MAIN "
                    "-c android.intent.category.HOME", check=False)
    found = set()
    for line in out.splitlines():
        token = line.strip().split(" ")[0] if line.strip() else ""
        if COMPONENT_RE.match(token):
            found.add(token)
    return found


# ---------------------------------------------------------------- 桌面与 Home

def default_home(adb, facts):
    """现在谁在应答 Home：(包名, 组件或 None)；无人应答时 (None, None)。"""
    if facts["sdk"] >= 24:
        out = adb.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN "
                        "-c android.intent.category.HOME", check=False)
        for line in reversed(out.splitlines()):
            found = COMPONENT_RE.match(line.strip())
            if found:
                return found.group(1), line.strip()
    found = re.search(r"mHomeProcess:.*[: ]([A-Za-z0-9_.]+)/[A-Za-z0-9_.]+", adb.shell("dumpsys activity", check=False))
    return (found.group(1), None) if found else (None, None)


def decide_stock_home(adb, facts, args, package, state):
    """要停用的原桌面包名；None 表示不动它（--keep-stock-home、未知或没有原桌面）。"""
    if args["keep_stock_home"]:
        say("--keep-stock-home：不停用原桌面。")
        return None
    if args["stock_home"]:
        stock = args["stock_home"]
        if not PKG_RE.match(stock):
            raise Abort(EXIT_USAGE, f"--stock-home 不是包名：{stock!r}")
        if stock == package:
            raise Abort(EXIT_USAGE, f"--stock-home 指到了我自己（{stock}），会把自己的桌面停掉。")
        if stock not in facts["installed"]:
            raise Abort(EXIT_USAGE, f"--stock-home {stock} 没装在这台电视上；设备状态没有任何改动。"
                                    "先 pm list packages 查到正确的包名再来。")
        return stock
    if state.get("stock_home"):
        say(f"沿用上次记录的 原桌面: {state['stock_home']}")
        return state["stock_home"]
    found, _ = default_home(adb, facts)
    if found and found not in ("android", package):
        say(f"安装前应答 Home 的是 {found}，把它当作原桌面。")
        return found
    warn("无法确定原桌面，所以不禁用它：确认之后用 --stock-home <包名> 重跑。")
    return None


def matches_component(candidate, package, activity):
    """设备回报的组件可能是 pkg/.Act 的简写，直接比字符串会漏。"""
    if "/" not in candidate:
        return False
    candidate_pkg, candidate_activity = candidate.split("/", 1)
    if candidate_pkg != package:
        return False
    if candidate_activity == activity:
        return True
    short = "." + activity.rsplit(".", 1)[-1]
    return candidate_activity in (short, activity.rsplit(".", 1)[-1])


def press_home(adb):
    adb.shell("input keyevent KEYCODE_HOME", mutating=True)


def focus_on(adb, package):
    """按 Home 之后焦点是否落在 package 上；窗口切换有延迟，抖几次再看。"""
    if adb.dry_run:
        adb.shell("dumpsys window | grep mCurrentFocus")
        return True
    for attempt in range(FOCUS_TRIES):
        if attempt:
            time.sleep(FOCUS_GAP_S)
        found, activity = adb.focus()
        if found == package:
            say(f"Home 验证通过：焦点在 {found}/{activity}")
            return True
    warn(f"焦点不在 {package}（当前 {found}/{activity}），先直接拉起再验一次。")
    return False


def current_focus(adb):
    """按 Home 之后读焦点：窗口切换期间是 null，多读几次再下结论。"""
    found, activity = adb.focus()
    for _ in range(FOCUS_TRIES - 1):
        if found:
            return found, activity
        time.sleep(FOCUS_GAP_S)
        found, activity = adb.focus()
    return found, activity


def start_launcher(adb, component, package):
    """直接拉起桌面并确认它真的起来了（不是 Home 解析，只证明这个 App 能跑）。"""
    adb.shell(f"am start -n {component}", mutating=True)
    if not focus_on(adb, package):
        return False
    return True


# ---------------------------------------------------------------- 上游脚本

def venv_python():
    for candidate in (REPO / "tools/venv/bin/python", REPO / "tools/venv/Scripts/python.exe"):
        if candidate.exists():
            return candidate
    return None


def require_venv():
    if venv_python() is None:
        raise Abort(EXIT_DEP, "上游脚本要 tools/venv：\n"
                              "  python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt")
    for script in ("disable.sh", "restore.sh"):
        if not (REPO / "tools" / script).exists():
            raise Abort(EXIT_DEP, f"缺少 tools/{script}")


def run_upstream(adb, script, args):
    command = ["bash", str(REPO / "tools" / script), *args]
    if adb.dry_run:
        say(f"$ TV_HOST={os.environ.get('TV_HOST', '<电视IP>')} " + shlex.join(command))
        return 0
    proc = subprocess.run(command, cwd=str(REPO), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if proc.stdout.strip():
        say(proc.stdout.rstrip())
    return proc.returncode


def stock_list_file(key, package):
    folder = Path.home() / ".aerialclean/lists"
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / f"{key}-stock-home.txt"
    path.write_text("# install.sh 生成：本机安装前的原桌面。新桌面验证通过之后才停用它。\n"
                    f"{package}\n", encoding="utf-8")
    return path


def validate_list(path):
    path = Path(path).expanduser().resolve()
    if not path.is_file():
        raise Abort(EXIT_DEP, f"停用列表不存在：{path}")
    names = [line.split("#")[0].strip() for line in path.read_text(encoding="utf-8-sig").splitlines()]
    names = [name for name in names if name]
    if not names:
        raise Abort(EXIT_DEP, f"停用列表里没有包名：{path}")
    bad = [name for name in names if not PKG_RE.match(name)]
    if bad:
        raise Abort(EXIT_DEP, f"列表里有不是包名的行：{', '.join(bad)}")
    return path


def restore_hint():
    return "./restore.sh"


def require_network_adb():
    if not os.environ.get("TV_HOST"):
        raise Abort(EXIT_DEP, "停用/还原走的是上游脚本的网络 ADB：用 --host <电视IP> 或 TV_HOST 指定地址。")


# ---------------------------------------------------------------- install

def parse_install_args(argv):
    args = {"host": os.environ.get("TV_HOST", ""), "apk": str(DEFAULT_APK), "list": None,
            "no_debloat": False, "keep_stock_home": False, "stock_home": None, "dry_run": False}
    takes_value = {"--host": "host", "--apk": "apk", "--list": "list", "--stock-home": "stock_home"}
    flags = {"--no-debloat": "no_debloat", "--keep-stock-home": "keep_stock_home", "--dry-run": "dry_run"}
    i = 0
    while i < len(argv):
        arg = argv[i]
        if arg in ("--help", "-h"):
            say(USAGE_INSTALL.rstrip())
            raise SystemExit(EXIT_OK)
        if arg in flags:
            args[flags[arg]] = True
            i += 1
        elif arg in takes_value and i + 1 < len(argv):
            args[takes_value[arg]] = argv[i + 1]
            i += 2
        else:
            raise Abort(EXIT_USAGE, f"不认识的参数：{arg}\n\n{USAGE_INSTALL.rstrip()}")
    return args


def cmd_install(argv):
    args = parse_install_args(argv)
    apk = Path(args["apk"]).expanduser()
    if not apk.is_file():
        raise Abort(EXIT_DEP, f"APK 不存在：{apk}（先跑 cd lite && ./gradlew :app:assembleDebug，或用 --apk 指定）")
    listing = None if args["no_debloat"] else (validate_list(args["list"]) if args["list"] else None)
    if listing or not args["keep_stock_home"]:
        require_venv()

    serial = pick_serial(args["host"])
    adb = Adb(serial, args["dry_run"])
    facts = device_facts(adb)
    if args["host"]:
        os.environ["TV_HOST"] = serial

    wanted = min_sdk(apk)
    if wanted is not None and wanted > facts["sdk"]:
        raise Abort(EXIT_APK, f"{apk.name} 要 minSdk {wanted}，电视是 Android {facts['sdk']}：装不上。")
    package, activity = apk_identity(apk)
    if not activity:
        raise Abort(EXIT_APK, f"{apk.name} 没有可启动的 Activity，不像桌面 APK。")
    component = f"{package}/{activity}"
    key = state_key(facts, serial)
    state = load_state(key)
    say(f"设备 {facts['model']} / Android {facts['sdk']}；APK {apk.name} → {package}")
    if facts["sdk"] < 24:
        say("Android 7 以下没有 cmd package query-activities/resolve-activity：原桌面必须用 --stock-home "
            "显式指定，接管方式同样是停用原桌面（可逆）。")

    # 停用清单是只读决定，放在安装之前：参数错误时设备一个字节都没动（验收 A7）。
    stock = decide_stock_home(adb, facts, args, package, state)
    if stock and not args["dry_run"]:
        require_network_adb()
    installed_now = package not in facts["installed"]

    if package in facts["disabled"]:
        say(f"{package} 现在是被停用状态（上一轮 restore 留下的），先启用。")
        adb.shell(f"pm enable {package}", mutating=True, check=False)
    out = adb.run(["install", "-r", str(apk)], mutating=True)
    if not adb.dry_run and "Success" not in out:
        raise Abort(EXIT_APK, f"安装失败：\n{out.strip()}")

    disabled_stock = []

    def rollback(reason, code=EXIT_HOME):
        if installed_now and not adb.dry_run:
            adb.run(["uninstall", package], mutating=True, check=False)
            warn(f"已卸载本次安装的 {package}，设备回到安装前的状态。")
        if disabled_stock and not adb.dry_run:
            adb.shell(f"pm enable {disabled_stock[0]}", mutating=True, check=False)
            warn(f"已重新启用原桌面 {disabled_stock[0]}。")
        raise Abort(code, reason)

    # 闸门 1：桌面自己起得来（直接按组件拉起，不涉及 Home 解析）。
    if not start_launcher(adb, component, package):
        rollback("新桌面装上了但起不来，已中止：没有停用任何包。")

    # 闸门 2：它是系统认可的 HOME 候选（Android 7 以下没有 query-activities，靠 aapt2 的清单证明）。
    if facts["sdk"] >= 24 and not adb.dry_run:
        candidates = home_candidates(adb)
        if not any(matches_component(candidate, package, activity) for candidate in candidates):
            rollback(f"{package} 没有被列为 HOME 候选（清单里没有 CATEGORY_HOME 或包被停用），已中止。")
        say(f"Home 候选：{package}")

    # 闸门 3：按 Home 键是否就落到新桌面。
    press_home(adb)
    home_ok = focus_on(adb, package)

    if adb.dry_run and stock and not args["keep_stock_home"]:
        say("# 仅当上一步焦点没有落到新桌面时才执行下面三行：")
        run_upstream(adb, "disable.sh", [str(stock_list_file(key, stock))])
        press_home(adb)
        focus_on(adb, package)

    if not home_ok and stock and not args["keep_stock_home"]:
        # Android 官方的三条路在当前固件上都不生效（见 .trellis/spec/tools/debloat-safety.md）：
        # set-home-activity 报 Success 但解析不变、HOME role 只是记账、清单 priority 被镜像忽略。
        # 唯一可靠的办法是让新桌面成为唯一的 Home 候选；停用原件是可逆的，失败立刻 enable 回来。
        stock_file = stock_list_file(key, stock)
        say(f"Home 还没交出来：停用原桌面 {stock}（列表 {stock_file}），失败会立刻启用回来。")
        if run_upstream(adb, "disable.sh", [str(stock_file)]) != 0:
            rollback(f"停用原桌面失败。\n还原：{restore_hint()}", EXIT_DEBLOAT)
        disabled_stock.append(stock)
        press_home(adb)
        home_ok = focus_on(adb, package)
        if not home_ok:
            adb.shell(f"am start -n {component}", mutating=True)
            home_ok = focus_on(adb, package)
        if not home_ok:
            rollback("停用原桌面之后 Home 仍然没落到新桌面，已把原桌面启用回来。")
        state["stock_home"] = stock
        state["stock_home_disabled"] = True
        state["stock_list"] = str(stock_file)
    elif not home_ok:
        if not args["keep_stock_home"]:
            rollback(f"Home 切换失败（没有可停用的原桌面可以腾位置）。\n还原：{restore_hint()}")
        warn(f"Home 仍在原桌面：{package} 已安装并可直接启动，但没有接管 Home。"
             "要接管就去掉 --keep-stock-home 再跑一次。")

    if listing:
        say(f"执行停用列表 {listing}")
        if run_upstream(adb, "disable.sh", [str(listing)]) != 0:
            raise Abort(EXIT_DEBLOAT, f"停用列表执行失败（可能只做了一部分）。\n还原：{restore_hint()}")
        if home_ok:   # --keep-stock-home 时 Home 本来就在原桌面，不该再拿它当失败
            press_home(adb)
            if not focus_on(adb, package):
                raise Abort(EXIT_DEBLOAT, f"停用之后 Home 不再回到 {package}。\n还原：{restore_hint()}")

    paths = [entry["path"] for entry in (state.get("lists") or []) if Path(entry["path"]).is_file()]
    for used in (state.get("stock_list"), str(listing) if listing else None):
        if used and used not in paths:
            paths.append(used)
    entries = [{"path": path} for path in paths]

    if not adb.dry_run:
        state.update({
            "package": package,
            "home_component": component,
            "apk_sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
            "installed_at": state.get("installed_at")
                            or datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat(),
            "lists": entries,
            "launcher_disabled": False,
        })
        say(f"记录已写入 {save_state(key, state)}")
    say(f"完成。还原：{restore_hint()}")
    return EXIT_OK


# ---------------------------------------------------------------- restore

def parse_restore_args(argv):
    args = {"host": os.environ.get("TV_HOST", ""), "uninstall": False, "dry_run": False}
    i = 0
    while i < len(argv):
        arg = argv[i]
        if arg in ("--help", "-h"):
            say(USAGE_RESTORE.rstrip())
            raise SystemExit(EXIT_OK)
        elif arg == "--host" and i + 1 < len(argv):
            args["host"] = argv[i + 1]
            i += 2
        elif arg == "--uninstall":
            args["uninstall"] = True
            i += 1
        elif arg == "--dry-run":
            args["dry_run"] = True
            i += 1
        else:
            raise Abort(EXIT_USAGE, f"不认识的参数：{arg}\n\n{USAGE_RESTORE.rstrip()}")
    return args


def cmd_restore(argv):
    args = parse_restore_args(argv)
    serial = pick_serial(args["host"])
    adb = Adb(serial, args["dry_run"])
    facts = device_facts(adb)
    if args["host"]:
        os.environ["TV_HOST"] = serial
    key = state_key(facts, serial)
    state = load_state(key)
    if not state:
        raise Abort(EXIT_DEP, f"没有 {key} 的安装记录（{STATE_DIR}/{key}.json）："
                              "这台电视不是用 install.sh 装的，没有需要还原的状态。")

    package = state.get("package")
    stock = state.get("stock_home")
    entries = [entry for entry in state.get("lists") or [] if Path(entry["path"]).is_file()]
    for entry in state.get("lists") or []:
        if not Path(entry["path"]).is_file():
            warn(f"记录里的列表已不存在，跳过：{entry['path']}")

    if entries:
        require_venv()
        require_network_adb()
        for entry in entries:
            say(f"还原 {entry['path']}")
            if run_upstream(adb, "restore.sh", [entry["path"], stock or ""]) != 0:
                raise Abort(EXIT_DEBLOAT, f"还原失败：{entry['path']}（再跑一次通常会从断点继续）")

    if stock:
        out = adb.shell(f"pm enable {stock}", mutating=True, check=False)
        if out.strip():
            say(out.strip())

    if package:
        if state.get("launcher_disabled"):
            adb.shell(f"pm enable {package}", mutating=True, check=False)

    press_home(adb)
    if not adb.dry_run:
        found, activity = current_focus(adb)
        if stock and found != stock:
            # 还有第二个 Home 候选时，系统按 priority / 偏好决定谁是 Home：把新桌面停用（可逆）才能
            # 保证 Home 真的回到原桌面，否则"恢复原桌面"只是嘴上说说。
            say(f"Home 还在 {found}/{activity}：先停用 {package}（可逆），让 Home 回到 {stock}。")
            adb.shell(f"pm disable-user --user 0 {package}", mutating=True, check=False)
            state["launcher_disabled"] = True
            save_state(key, state)
            press_home(adb)
            found, activity = current_focus(adb)
            if found != stock:
                raise Abort(EXIT_HOME, f"Home 现在是 {found}/{activity}，不是原桌面 {stock}："
                                       "包都已恢复，按一次 Home 在选择器里选原桌面即可。")
        say(f"当前 Home：{found}/{activity}")

    if args["uninstall"] and package:
        adb.run(["uninstall", package], mutating=True, check=False)
        if not adb.dry_run:
            press_home(adb)
            found, activity = current_focus(adb)
            if stock and found != stock:
                raise Abort(EXIT_HOME, f"卸载后 Home 是 {found}/{activity}，不是 {stock}：按一次 Home 选择原桌面。")
            say(f"已卸载 {package}")
        state["launcher_disabled"] = False
        save_state(key, state)

    say(f"完成。安装记录保留在 {STATE_DIR / f'{key}.json'}")
    return EXIT_OK


def main(argv):
    if not argv or argv[0] in ("--help", "-h"):
        say(USAGE_INSTALL.rstrip())
        return EXIT_OK
    command, rest = argv[0], argv[1:]
    try:
        if command == "install":
            return cmd_install(rest)
        if command == "restore":
            return cmd_restore(rest)
        raise Abort(EXIT_USAGE, f"不认识的动作：{command}")
    except Abort as error:
        warn(error.message)
        return error.code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
