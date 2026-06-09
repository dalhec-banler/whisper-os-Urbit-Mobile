#!/usr/bin/env bash
set -euo pipefail

ADB="${ADB:-adb}"
PACKAGE="${NP_LAUNCHER_PACKAGE:-io.nativeplanet.launcher}"
REMOTE_XML="${NP_UI_DUMP_PATH:-/sdcard/nativeplanet-ui-smoke.xml}"
EXPECT_RUNNING="${NP_EXPECT_RUNTIME_RUNNING:-0}"

tmp_xml="$(mktemp)"
trap 'rm -f "$tmp_xml"; "$ADB" shell rm -f "$REMOTE_XML" >/dev/null 2>&1 || true' EXIT

"$ADB" shell am force-stop "$PACKAGE" >/dev/null
"$ADB" shell monkey -p "$PACKAGE" 1 >/dev/null 2>&1
sleep "${NP_UI_LAUNCH_DELAY_SECONDS:-2}"

"$ADB" shell uiautomator dump "$REMOTE_XML" >/dev/null
"$ADB" exec-out cat "$REMOTE_XML" > "$tmp_xml"

has_text() {
  local text="$1"
  grep -Fq "text=\"$text\"" "$tmp_xml"
}

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  printf 'Visible safe labels:\n' >&2
  grep -o 'text="[^"]*"' "$tmp_xml" \
    | sed 's/^text="//; s/"$//' \
    | grep -E '^(NativePlanet|RUNTIME|BOOT PACKAGE|NETWORK|DIAGNOSTICS|RUNNING|Running|STOPPED|Stopped|Add identity|Set up moon|Pair with planet|Use moon key)$' \
    | sort -u >&2 || true
  exit 1
}

has_text "NativePlanet" || fail "launcher home title not visible"
has_text "RUNTIME" || fail "runtime panel not visible"
has_text "BOOT PACKAGE" || fail "boot package panel not visible"
has_text "NETWORK" || fail "network panel not visible"

if [[ "$EXPECT_RUNNING" == "1" ]]; then
  if ! has_text "RUNNING" && ! has_text "Running"; then
    fail "running state not visible"
  fi
fi

printf 'PASS: launcher home smoke check\n'
printf '  package: %s\n' "$PACKAGE"
printf '  runtime panel: visible\n'
printf '  boot package panel: visible\n'
printf '  network panel: visible\n'
if [[ "$EXPECT_RUNNING" == "1" ]]; then
  printf '  running state: visible\n'
fi
