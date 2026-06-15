#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mapfile -t scan_files < <(
  git ls-files --cached --others --exclude-standard \
    ':!:tools/check-repo-hygiene.sh' \
    ':!:launcher/gradle/wrapper/gradle-wrapper.jar' \
    ':!:**/*.png' \
    ':!:**/*.jpg' \
    ':!:**/*.jpeg' \
    ':!:**/*.webp' \
    ':!:**/*.ttf' \
    ':!:**/*.otf' \
    ':!:**/*.woff' \
    ':!:**/*.woff2' \
    ':!:**/*.apk' \
    ':!:**/*.pill'
)

if [[ "${#scan_files[@]}" -eq 0 ]]; then
  echo "No files found"
  exit 0
fi

status=0

check_pattern() {
  local label="$1"
  local pattern="$2"
  if grep -nE "$pattern" -- "${scan_files[@]}" >/tmp/nativeplanet-hygiene-matches.$$ 2>/dev/null; then
    echo "FAIL: $label"
    cat /tmp/nativeplanet-hygiene-matches.$$
    status=1
  else
    echo "PASS: $label"
  fi
  rm -f /tmp/nativeplanet-hygiene-matches.$$
}

check_pattern "no local home paths" '(/home/|/Users/|grapheneos-[0-9]|dev/mobile-vere)'
check_pattern "no local build owner labels" '(eng\.[A-Za-z0-9_-]+|anoffice)'
check_pattern "no plus-code fragments in secret contexts" '(NP_PAIRING_CODE|password=|accessCode|access code|\+code).*[a-z]{6}-[a-z]{6}-[a-z]{6}-[a-z]{6}'
check_pattern "no raw moon keys" '(^|[^A-Za-z0-9])0w[0-9A-Za-z.~_-]{20,}'

exit "$status"
