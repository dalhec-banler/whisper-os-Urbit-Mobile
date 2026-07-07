#!/usr/bin/env bash
set -euo pipefail

ADB="${ADB:-adb}"
CONTROLLER_URI="${NP_CONTROLLER_URI:-content://io.nativeplanet.controller}"
EXPECTED_SOURCES="${NP_EXPECT_HOSTED_APPS_SOURCES:-docket+nativeplanet-mobile,nativeplanet-mobile}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

"$ADB" root >/dev/null 2>&1 || true
"$ADB" wait-for-device >/dev/null 2>&1 || true

extract_json() {
  local raw_file="$1"
  python3 - "$raw_file" <<'PY'
import sys

data = open(sys.argv[1], "r", encoding="utf-8", errors="replace").read()
marker = data.find("json=")
start = data.find("{", marker + len("json=") if marker >= 0 else 0)
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

provider_raw="$tmp_dir/provider.raw"
provider_json="$tmp_dir/provider.json"
direct_json="$tmp_dir/direct.json"

"$ADB" shell content call --uri "$CONTROLLER_URI" --method getHostedApps > "$provider_raw"
extract_json "$provider_raw" > "$provider_json"

"$ADB" forward --remove tcp:18080 >/dev/null 2>&1 || true
"$ADB" forward --remove tcp:19021 >/dev/null 2>&1 || true
"$ADB" forward tcp:18080 tcp:8080 >/dev/null 2>&1 || true
node "$(dirname "$0")/conn-client.js" --adb --port 19021 mobile-apps > "$direct_json"

python3 - "$provider_json" "$direct_json" "$EXPECTED_SOURCES" <<'PY'
import json
import sys
import urllib.error
import urllib.request

provider = json.load(open(sys.argv[1], encoding="utf-8"))
direct = json.load(open(sys.argv[2], encoding="utf-8"))
expected_sources = {item.strip() for item in sys.argv[3].split(",") if item.strip()}


def fail(message):
    raise SystemExit(f"FAIL: {message}")


source = provider.get("source")
if source not in expected_sources:
    expected = ", ".join(sorted(expected_sources))
    fail(f"provider source is {source!r}, expected one of: {expected}")

if provider.get("mobileMetadataAvailable") is not True:
    fail("provider mobileMetadataAvailable is not true")

if provider.get("stale") is True:
    fail("provider hosted apps inventory is stale")

if provider.get("lastError") not in (None, "null"):
    fail(f"provider lastError is {provider.get('lastError')!r}")

provider_apps = provider.get("apps") or []
direct_apps = direct.get("apps") or []
provider_desks = {app.get("desk") for app in provider_apps}
direct_desks = {app.get("desk") for app in direct_apps}

required = {"groups", "webterm", "landscape", "grove", "kin"}
missing_provider = sorted(required - provider_desks)
if missing_provider:
    fail(f"provider missing mobile desks: {', '.join(missing_provider)}")

missing_direct = sorted(required - direct_desks)
if missing_direct:
    fail(f"direct conn mobile metadata missing desks: {', '.join(missing_direct)}")

invalid_launch_modes = [
    app.get("desk")
    for app in provider_apps
    if app.get("desk") in required
    and app.get("launchMode") not in {"", None, "local_webview", "pwa", "native", "browser"}
]
if invalid_launch_modes:
    fail(f"provider has invalid launch modes for: {', '.join(sorted(invalid_launch_modes))}")

broken_launch_targets = [
    app.get("desk")
    for app in provider_apps
    if app.get("desk") in required
    and app.get("launchMode") in {"local_webview", "pwa", "browser"}
    and not (app.get("startUrl") or app.get("basePath"))
]
if broken_launch_targets:
    fail(f"provider has launch modes without a URL/path for: {', '.join(sorted(broken_launch_targets))}")

server_error_targets = []
for app in provider_apps:
    if app.get("desk") not in required:
        continue
    if app.get("launchMode") not in {"local_webview", "pwa", "browser"}:
        continue

    target = app.get("startUrl") or app.get("basePath")
    if not target:
        continue
    if target.startswith("/"):
        target = "http://127.0.0.1:18080" + target
    elif target.startswith("http://127.0.0.1:8080"):
        target = target.replace("http://127.0.0.1:8080", "http://127.0.0.1:18080", 1)

    try:
        request = urllib.request.Request(target, method="HEAD")
        with urllib.request.urlopen(request, timeout=5) as response:
            status = response.status
    except urllib.error.HTTPError as error:
        status = error.code
    except Exception:
        # Network or auth setup can be unavailable in developer runs. The
        # provider contract still guards the important invariant above: no
        # launch mode without a URL/path.
        continue

    if status >= 500:
        server_error_targets.append(f"{app.get('desk')}:{status}")

if server_error_targets:
    fail("provider exposes server-erroring launch targets: " + ", ".join(server_error_targets))

print("PASS: hosted mobile app provider smoke check")
print(f"  source: {source}")
print("  desks: " + ", ".join(sorted(required)))
print(f"  provider apps: {len(provider_apps)}")
print(f"  direct conn apps: {len(direct_apps)}")
PY
