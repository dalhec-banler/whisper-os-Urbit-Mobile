#!/usr/bin/env bash
set -euo pipefail

ADB="${ADB:-adb}"
REMOTE_XML="${NP_UI_DUMP_PATH:-/sdcard/nativeplanet-launcher3-smoke.xml}"
EXPECTED_HOME="com.android.launcher3/.uioverrides.QuickstepLauncher"
CONTROLLER_URI="content://io.nativeplanet.controller"

tmp_xml="$(mktemp)"
trap 'rm -f "$tmp_xml"; "$ADB" shell rm -f "$REMOTE_XML" >/dev/null 2>&1 || true' EXIT

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  printf 'Visible safe labels:\n' >&2
  grep -o 'text="[^"]*"\|content-desc="[^"]*"' "$tmp_xml" 2>/dev/null \
    | sed 's/^text="//; s/^content-desc="//; s/"$//' \
    | grep -E '^(My Urbit Apps|Landscape|Terminal|Tlon|App Store|Phone|Messaging|Vanadium|Camera|Home|Browser|Close)$' \
    | sort -u >&2 || true
  exit 1
}

has_text() {
  local text="$1"
  grep -Fq "text=\"$text\"" "$tmp_xml" || grep -Fq "content-desc=\"$text\"" "$tmp_xml"
}

resolved_home="$("$ADB" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null \
  | tr -d '\r' | tail -1)"

[[ "$resolved_home" == "$EXPECTED_HOME" ]] \
  || fail "default HOME is $resolved_home, expected $EXPECTED_HOME"

"$ADB" shell input keyevent WAKEUP >/dev/null 2>&1 || true
"$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
"$ADB" shell am start -W -a android.intent.action.MAIN \
  -c android.intent.category.HOME >/dev/null 2>&1 || true
sleep "${NP_UI_LAUNCH_DELAY_SECONDS:-2}"

"$ADB" shell uiautomator dump "$REMOTE_XML" >/dev/null
"$ADB" exec-out cat "$REMOTE_XML" > "$tmp_xml"

grep -Fq 'package="com.android.launcher3"' "$tmp_xml" \
  || fail "Launcher3 UI is not visible; unlock the phone and rerun"

has_text "My Urbit Apps" || fail "My Urbit Apps workspace entry not visible"

if ! has_text "Landscape" && ! has_text "Terminal" && ! has_text "Tlon"; then
  fail "no pinned hosted Urbit app label visible on workspace"
fi

"$ADB" shell dumpsys package com.android.launcher3 2>/dev/null \
  | grep -q "WhisperHostedAppsActivity" \
  || fail "WhisperHostedAppsActivity is not registered"

python3 - <<'PY'
import json
import re
import subprocess
import sys

uri = "content://io.nativeplanet.controller"
raw = subprocess.check_output(
    ["adb", "shell", "content", "call", "--uri", uri, "--method", "getHostedApps"],
    text=True,
    stderr=subprocess.DEVNULL,
)
match = re.search(r"json=(\{.*\})\}\]$", raw.strip())
if not match:
    print("FAIL: hosted apps provider did not return JSON", file=sys.stderr)
    sys.exit(1)

data = json.loads(match.group(1))
apps = data.get("apps") or []
titles = {app.get("title") for app in apps}
required = {"Landscape", "Terminal", "Tlon"}
missing = sorted(required - titles)
if missing:
    print(f"FAIL: hosted app inventory missing {', '.join(missing)}", file=sys.stderr)
    sys.exit(1)
if data.get("stale") is True or data.get("lastError") not in (None, "null"):
    print("FAIL: hosted app inventory is stale or has lastError", file=sys.stderr)
    sys.exit(1)
PY

printf 'PASS: launcher3 home smoke check\n'
printf '  default HOME: %s\n' "$resolved_home"
printf '  My Urbit Apps: visible\n'
printf '  pinned hosted app: visible\n'
printf '  hosted provider: Landscape, Terminal, Tlon\n'
