#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${NP_PAIRING_ENV:-$ROOT_DIR/secrets/urbit/pairing.env}"

APPLY=0
if [[ "${1:-}" == "--apply" ]]; then
  APPLY=1
  shift
fi

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

if [[ -n "${NP_PAIRING_URL:-}" && -z "${NP_PAIRING_CODE:-}" && -t 0 ]]; then
  read -r -s -p "Parent +code: " NP_PAIRING_CODE
  printf '\n' >&2
  export NP_PAIRING_CODE
fi

if [[ -z "${NP_PAIRING_URL:-}" || -z "${NP_PAIRING_CODE:-}" ]]; then
  cat >&2 <<'EOF'
Missing parent pairing inputs.

Use an ignored local env file:

  secrets/urbit/pairing.env

With:

  NP_PAIRING_URL=https://example.tlon.network
  NP_PAIRING_CODE=<plus-code>

Then preview:

  ./tools/install-parent-mobile-desks.sh

Or apply installs to the connected phone moon:

  ./tools/install-parent-mobile-desks.sh --apply

This script never prints the code.
EOF
  exit 2
fi

url="${NP_PAIRING_URL%/}"
case "$url" in
  http://*|https://*) ;;
  *) url="https://$url" ;;
esac

if [[ "$APPLY" == "1" ]]; then
  if ! command -v adb >/dev/null 2>&1; then
    echo "adb is required for --apply" >&2
    exit 2
  fi
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
chmod 700 "$tmp_dir"

cookie_jar="$tmp_dir/cookies.txt"
login_form="$tmp_dir/login-form.txt"
pikes_json="$tmp_dir/pikes.json"
phone_pikes_json="$tmp_dir/phone-pikes.json"

python3 - "$NP_PAIRING_CODE" > "$login_form" <<'PY'
import sys
import urllib.parse

print("password=" + urllib.parse.quote_plus(sys.argv[1]), end="")
PY
chmod 600 "$login_form"

curl_common=(
  --silent
  --show-error
  --location
  --connect-timeout 10
  --max-time 30
  --cookie-jar "$cookie_jar"
  --cookie "$cookie_jar"
)

login_code="$(
  curl "${curl_common[@]}" \
    --request POST \
    --header 'content-type: application/x-www-form-urlencoded' \
    --data-binary "@$login_form" \
    --output /dev/null \
    --write-out '%{http_code}' \
    "$url/~/login" || true
)"

if ! grep -q 'urbauth' "$cookie_jar"; then
  echo "Parent login failed: HTTP $login_code, no session cookie" >&2
  exit 1
fi

pikes_code="$(
  curl "${curl_common[@]}" \
    --output "$pikes_json" \
    --write-out '%{http_code}' \
    "$url/~/scry/hood/kiln/pikes.json" || true
)"

if [[ "$pikes_code" != "200" ]]; then
  echo "Parent pikes scry failed: HTTP $pikes_code" >&2
  exit 1
fi

if [[ "$#" -gt 0 ]]; then
  desks=("$@")
elif [[ -n "${NP_MOBILE_DESKS:-}" ]]; then
  # shellcheck disable=SC2206
  desks=($NP_MOBILE_DESKS)
else
  desks=(groups webterm landscape grove kin)
fi

echo "Parent: $url"
echo "Mode: $([[ "$APPLY" == "1" ]] && echo apply || echo dry-run)"
echo ""

for desk in "${desks[@]}"; do
  clean_desk="${desk#%}"
  publisher="$(jq -r --arg desk "$clean_desk" '.[$desk].sync.ship // empty' "$pikes_json")"
  remote_desk="$(jq -r --arg desk "$clean_desk" '.[$desk].sync.desk // empty' "$pikes_json")"
  zest="$(jq -r --arg desk "$clean_desk" '.[$desk].zest // "missing"' "$pikes_json")"
  hash="$(jq -r --arg desk "$clean_desk" '.[$desk].hash // "0v0"' "$pikes_json")"

  if [[ -z "$publisher" || -z "$remote_desk" ]]; then
    printf 'skip %%%-12s parent desk missing or not synced (zest=%s hash=%s)\n' \
      "$clean_desk" "$zest" "$hash"
    continue
  fi

  printf 'desk %%%-12s publisher=%s remote=%%%s parent=%s %s\n' \
    "$clean_desk" "$publisher" "$remote_desk" "$zest" "$hash"

  if [[ "$APPLY" == "1" ]]; then
    hoon="=/  m  (strand ,vase)  ;<  our=@p  bind:m  get-our  ;<  ~  bind:m  (poke [our %hood] %kiln-install !>([%${clean_desk} ${publisher} %${remote_desk}]))  (pure:m !>('success'))"
    node "$ROOT_DIR/tools/conn-client.js" --adb eval "$hoon" >/dev/null
    printf '  requested install on phone moon\n'
  fi
done

if [[ "$APPLY" == "1" ]]; then
  echo ""
  echo "Phone moon Kiln state after requests:"
  sleep 5
  code="$(
    adb shell content call --uri content://io.nativeplanet.controller --method getWebLoginCode 2>/dev/null |
      sed -n 's/.*"code":"\([^"]*\)".*/\1/p'
  )"
  if [[ -n "$code" ]]; then
    phone_cookie="$tmp_dir/phone-cookies.txt"
    phone_form="$tmp_dir/phone-login-form.txt"
    python3 - "$code" > "$phone_form" <<'PY'
import sys
import urllib.parse

print("password=" + urllib.parse.quote_plus(sys.argv[1]), end="")
PY
    adb forward tcp:18080 tcp:8080 >/dev/null 2>&1 || true
    curl --silent --show-error --cookie-jar "$phone_cookie" --cookie "$phone_cookie" \
      --request POST --header 'content-type: application/x-www-form-urlencoded' \
      --data-binary "@$phone_form" --output /dev/null \
      http://127.0.0.1:18080/~/login || true
    curl --silent --show-error --cookie "$phone_cookie" \
      --output "$phone_pikes_json" \
      http://127.0.0.1:18080/~/scry/hood/kiln/pikes.json || true
    jq '{groups, webterm, landscape, grove, kin}' "$phone_pikes_json" 2>/dev/null || true
  fi
fi
