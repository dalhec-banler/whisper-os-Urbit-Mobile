#!/usr/bin/env bash
set -euo pipefail

ADB="${ADB:-adb}"
URI="${NP_CONTROLLER_URI:-content://io.nativeplanet.controller/status}"
EXPECT_RUNNING="${NP_EXPECT_RUNTIME_RUNNING:-0}"
EXPECT_VALIDATED_NETWORK="${NP_EXPECT_VALIDATED_NETWORK:-0}"
CHECK_PROVISION_METHOD="${NP_CHECK_PROVISION_METHOD:-1}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

extract_json() {
  local raw_file="$1"
  python3 - "$raw_file" <<'PY'
import sys

path = sys.argv[1]
data = open(path, "r", encoding="utf-8", errors="replace").read()
json_marker = data.find("json=")
search_from = json_marker + len("json=") if json_marker >= 0 else 0
start = data.find("{", search_from)
if start < 0:
    raise SystemExit("no JSON object found")

depth = 0
in_string = False
escaped = False

for index in range(start, len(data)):
    char = data[index]
    if in_string:
        if escaped:
            escaped = False
        elif char == "\\":
            escaped = True
        elif char == '"':
            in_string = False
        continue

    if char == '"':
        in_string = True
    elif char == "{":
        depth += 1
    elif char == "}":
        depth -= 1
        if depth == 0:
            print(data[start:index + 1])
            raise SystemExit(0)

raise SystemExit("unterminated JSON object")
PY
}

call_method() {
  local method="$1"
  local out_file="$2"
  local raw_file="$tmp_dir/$method.raw"

  "$ADB" shell content call --uri "$URI" --method "$method" > "$raw_file"
  extract_json "$raw_file" > "$out_file"
}

runtime_json="$tmp_dir/runtime.json"
boot_json="$tmp_dir/boot.json"
network_json="$tmp_dir/network.json"
provision_json="$tmp_dir/provision.json"

call_method getRuntime "$runtime_json"
call_method getBootPackage "$boot_json"
call_method getNetwork "$network_json"

if [[ "$CHECK_PROVISION_METHOD" == "1" ]]; then
  call_method provisionMoon "$provision_json"
else
  printf '{"accepted":false,"code":"SKIPPED"}\n' > "$provision_json"
fi

python3 - "$runtime_json" "$boot_json" "$network_json" "$provision_json" "$EXPECT_RUNNING" "$EXPECT_VALIDATED_NETWORK" "$CHECK_PROVISION_METHOD" <<'PY'
import json
import sys

runtime = json.load(open(sys.argv[1], encoding="utf-8"))
boot = json.load(open(sys.argv[2], encoding="utf-8"))
network = json.load(open(sys.argv[3], encoding="utf-8"))
provision = json.load(open(sys.argv[4], encoding="utf-8"))
expect_running = sys.argv[5] == "1"
expect_validated_network = sys.argv[6] == "1"
check_provision_method = sys.argv[7] == "1"


def fail(message):
    raise SystemExit(f"FAIL: {message}")


state = runtime.get("state")
if state not in {"running", "stopped", "starting", "stopping", "error", "crashed", "uninitialized"}:
    fail(f"unexpected runtime state: {state!r}")

if expect_running:
    if state != "running":
        fail(f"runtime is {state!r}, expected running")
    if runtime.get("connSockAvailable") is not True:
        fail("conn.sock is not available")
    if not runtime.get("shipName"):
        fail("runtime ship name missing")

if boot.get("exists") is True:
    if boot.get("valid") is not True:
        fail("boot package exists but is not valid")
    if boot.get("pillExists") is not True:
        fail("boot package pill is missing")
    if boot.get("keyFileExists") is not True:
        fail("boot package key file is missing")

network_type = network.get("networkType") or network.get("type")
if expect_validated_network:
    if network_type == "NONE":
        fail("network is disconnected")
    if network.get("validated") is not True:
        fail("network is not validated")

if check_provision_method:
    if provision.get("accepted") is not False:
        fail("empty provisionMoon request unexpectedly accepted")
    if provision.get("code") != "MISSING_REQUEST":
        fail(f"empty provisionMoon returned {provision.get('code')!r}, expected MISSING_REQUEST")

print("PASS: controller provider smoke check")
print(f"  runtime state: {state}")
print(f"  boot package: {'valid' if boot.get('valid') else 'not configured'}")
print(f"  network: {network_type or 'unknown'}")
if check_provision_method:
    print("  provisionMoon empty-request guard: present")
PY
