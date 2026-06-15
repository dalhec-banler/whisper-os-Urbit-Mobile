#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
URBIT_BIN="${URBIT_BIN:-urbit}"

APP="$ROOT/satellite-pill/desks/nativeplanet-mobile/app/nativeplanet-mobile.hoon"

if ! command -v "$URBIT_BIN" >/dev/null 2>&1; then
  echo "FAIL: urbit binary not found. Set URBIT_BIN=/path/to/urbit." >&2
  exit 1
fi

if [[ ! -f "$APP" ]]; then
  echo "FAIL: missing $APP" >&2
  exit 1
fi

"$URBIT_BIN" eval < "$APP" >/dev/null
echo "PASS: nativeplanet-mobile Gall app parses and typechecks under urbit eval"
