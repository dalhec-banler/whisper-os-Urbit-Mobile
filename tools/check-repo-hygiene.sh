#!/usr/bin/env bash
# Scan tracked and untracked-but-not-ignored files for local paths, build-owner
# labels, web login codes and raw moon keys. Exit 1 on any match.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

scan_files=()
while IFS= read -r f; do
  scan_files+=("$f")
done < <(
  git ls-files --cached --others --exclude-standard \
    ':!:tools/check-repo-hygiene.sh' \
    ':!:**/gradle-wrapper.jar' \
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

matches="$(mktemp)"
trap 'rm -f "$matches"' EXIT

status=0

check_pattern() {
  local label="$1"
  local pattern="$2"
  if grep -nE "$pattern" -- "${scan_files[@]}" >"$matches" 2>/dev/null; then
    echo "FAIL: $label"
    cat "$matches"
    status=1
  else
    echo "PASS: $label"
  fi
}

# (^|[^.]) keeps relative markdown links like ../home/README.md from matching;
# only absolute machine paths are a leak.
check_pattern "no local home paths" '((^|[^.])/home/|(^|[^.])/Users/|grapheneos-[0-9]|dev/mobile-vere)'
check_pattern "no local build owner labels" '(eng\.[A-Za-z0-9_-]+|anoffice)'
check_pattern "no plus-code fragments in secret contexts" '(NP_PAIRING_CODE|password=|accessCode|access code|\+code).*[a-z]{6}-[a-z]{6}-[a-z]{6}-[a-z]{6}'
check_pattern "no raw moon keys" '(^|[^A-Za-z0-9])0w[0-9A-Za-z.~_-]{20,}'

exit "$status"
