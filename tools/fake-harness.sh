#!/usr/bin/env bash
# Build the delegation test harness from scratch: a fake ~zod (the planet, with
# Tlon's desk and the Artemis desk carrying %satellite), its fake moon
# ~doznec-dozzod-dozzod (the phone, with %nativeplanet-mobile paired to zod),
# and a fake ~bud (a DM peer). Everything runs under tools/fake-ship.py.
#
#   tools/fake-harness.sh <harness-dir>
#
# Inputs (env, with defaults relative to the repo checkout):
#   NP_TOOLS_DIR      dir holding the pill and vere binary   (default: <repo>/../tools)
#   NP_ARTEMIS_DESK   Artemis desk source                    (default: <repo>/../artemis/desk)
#   VERE              vere binary for fake-ship.py           (default: $NP_TOOLS_DIR/vere64-edge)
#
# Never run scries against not-yet-running agents in these dojos: a blocked
# scry wedges the dojo for good. Verify with tools/conn-client.js instead.
set -euo pipefail
H="${1:?harness dir}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/.." && pwd)"
TOOLS="${NP_TOOLS_DIR:-$REPO/../tools}"
ARTEMIS="${NP_ARTEMIS_DESK:-$REPO/../artemis/desk}"
export VERE="${VERE:-$TOOLS/vere64-edge}"
PILL="$TOOLS/urbit-v4.6.pill"
MOBILE="$REPO/satellite-pill/desks/nativeplanet-mobile"

if [[ ! -d "$TOOLS" ]]; then
  echo "Error: tools dir not found: $TOOLS (set NP_TOOLS_DIR)"; exit 1
fi
if [[ ! -f "$PILL" ]]; then
  echo "Error: pill not found: $PILL (set NP_TOOLS_DIR)"; exit 1
fi
if [[ ! -d "$ARTEMIS" ]]; then
  echo "Error: Artemis desk not found: $ARTEMIS (set NP_ARTEMIS_DESK)"; exit 1
fi
if [[ ! -x "$VERE" ]]; then
  echo "Error: vere binary not found: $VERE (set VERE)"; exit 1
fi

fs() { python3 "$HERE/fake-ship.py" "$@"; }
MOON=doznec-dozzod-dozzod
RUN_TIMEOUT=240   # seconds to wait for the dojo to go idle after a command
RUN_TAIL=4        # lines of dojo output to keep

log() { printf '\n== %s\n' "$*"; }
wait_prompt() { # wait until a ship's terminal shows an idle dojo prompt
  local pier="$1" i
  for i in $(seq 1 120); do
    if [ -f "$pier.tty.log" ] && tail -c 400 "$pier.tty.log" | tr -d '\r' | grep -q 'dojo> *$'; then return 0; fi
    sleep 5
  done
  echo "timeout waiting for $pier"; return 1
}
# run <pier> <command> [timeout-seconds] [tail-lines]
run() {
  local pier="$1" cmd="$2" timeout="${3:-$RUN_TIMEOUT}" tail_lines="${4:-$RUN_TAIL}"
  fs run "$pier" "$cmd" "$timeout" | grep -v 'dojo> + /' | tail -n "$tail_lines"
}

log "stopping any fake ships"
pkill -f "$(basename "$VERE") -F (zod|$MOON|bud)" 2>/dev/null || true
sleep 8
rm -rf "$H"; mkdir -p "$H"; cd "$H"

log "booting zod, moon, bud"
fs start "$H/zod"  -F zod   -B "$PILL" -c zod  --http-port 8093
fs start "$H/moon" -F $MOON -B "$PILL" -c moon --http-port 8094
fs start "$H/bud"  -F bud   -B "$PILL" -c bud  --http-port 8095
wait_prompt "$H/zod"; wait_prompt "$H/moon"; wait_prompt "$H/bud"
sleep 20

log "zod: Tlon desk"
run "$H/zod" "|install ~zod %groups" 300 3
log "zod: Artemis desk with %satellite"
run "$H/zod" "|new-desk %artemis" 60 2
run "$H/zod" "|mount %artemis" 60 2
sleep 3
rm -rf "$H/zod/artemis"/* && cp -R "$ARTEMIS/." "$H/zod/artemis/" && rm -rf "$H/zod/artemis/.git" "$H/zod/artemis/.DS_Store"
run "$H/zod" "|commit %artemis" 300 3
run "$H/zod" "|install ~zod %artemis" 300 6
run "$H/zod" ":satellite [%allow ~$MOON]" 60 3

log "moon: Tlon desk from zod"
run "$H/moon" "|install ~zod %groups" 300 3
log "moon: mobile desk with the mirror"
run "$H/moon" "|new-desk %nativeplanet-mobile" 60 2
run "$H/moon" "|mount %nativeplanet-mobile" 60 2
sleep 3
D="$H/moon/nativeplanet-mobile"; mkdir -p "$D/app" "$D/lib" "$D/mar"
cp "$MOBILE/app/nativeplanet-mobile.hoon" "$D/app/"
cp "$ARTEMIS/lib/default-agent.hoon" "$ARTEMIS/lib/dbug.hoon" "$ARTEMIS/lib/skeleton.hoon" "$D/lib/"
cp "$ARTEMIS/mar/json.hoon" "$ARTEMIS/mar/bill.hoon" "$ARTEMIS/mar/mime.hoon" "$ARTEMIS/mar/txt.hoon" "$D/mar/"
cp "$MOBILE/desk.bill" "$D/desk.bill"; cp "$MOBILE/sys.kelvin" "$D/sys.kelvin"
run "$H/moon" "|commit %nativeplanet-mobile" 300 3
run "$H/moon" "|install ~$MOON %nativeplanet-mobile" 300 6
run "$H/moon" ":nativeplanet-mobile [%pair ~zod]" 60 3

log "bud: Tlon desk from zod"
run "$H/bud" "|install ~zod %groups" 300 3

log "done. web login codes:"
CODE_RE='[a-z]{6}-[a-z]{6}-[a-z]{6}-[a-z]{6}'
for ship in zod moon bud; do
  printf '%s: ' "$ship"
  run "$H/$ship" "+code" 60 3 | grep -oE "$CODE_RE" | tail -n 1 || echo "(not printed; run +code in the dojo)"
done
