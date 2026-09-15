#!/usr/bin/env bash
# Build the delegation test harness from scratch: a fake ~zod (the planet, with
# Tlon's desk and the Artemis desk carrying %satellite), its fake moon
# ~doznec-dozzod-dozzod (the phone, with %nativeplanet-mobile paired to zod),
# and a fake ~bud (a DM peer). Everything runs under tools/fakeship.py.
#
#   tools/fake-harness.sh <harness-dir>
#
# Never run scries against not-yet-running agents in these dojos: a blocked
# scry wedges the dojo for good. Verify with tools/conn-client.js instead.
set -euo pipefail
H="${1:?harness dir}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/.." && pwd)"
TOOLS="/Users/austinnelsen/Desktop/Urbit Development/tools"
PILL="$TOOLS/urbit-v4.6.pill"
ARTEMIS="/Users/austinnelsen/Desktop/Urbit Development/artemis/desk"
MOBILE="$REPO/satellite-pill/desks/nativeplanet-mobile"
FS="python3 $HERE/fakeship.py"
MOON=doznec-dozzod-dozzod

log() { printf '\n== %s\n' "$*"; }
wait_prompt() { # wait until a ship's terminal shows an idle dojo prompt
  local pier="$1" i
  for i in $(seq 1 120); do
    if [ -f "$pier.tty.log" ] && tail -c 400 "$pier.tty.log" | tr -d '\r' | grep -q 'dojo> *$'; then return 0; fi
    sleep 5
  done
  echo "timeout waiting for $pier"; return 1
}
run() { $FS run "$1" "$2" "${3:-240}" | grep -v 'dojo> + /' | tail -${4:-4}; }

log "stopping any fake ships"
pkill -f "vere64-edge -F (zod|$MOON|bud)" 2>/dev/null || true
pkill -f "vere64-edge (zod|moon|bud) " 2>/dev/null || true
sleep 8
rm -rf "$H"; mkdir -p "$H"; cd "$H"

log "booting zod, moon, bud"
$FS start "$H/zod"  -F zod   -B "$PILL" -c zod  --http-port 8093
$FS start "$H/moon" -F $MOON -B "$PILL" -c moon --http-port 8094
$FS start "$H/bud"  -F bud   -B "$PILL" -c bud  --http-port 8095
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
cp "$MOBILE/desk.bill" "$D/desk.bill"; printf '[%%zuse 408]\n' > "$D/sys.kelvin"
run "$H/moon" "|commit %nativeplanet-mobile" 300 3
run "$H/moon" "|install ~$MOON %nativeplanet-mobile" 300 6
run "$H/moon" ":nativeplanet-mobile [%pair ~zod]" 60 3

log "bud: Tlon desk from zod"
run "$H/bud" "|install ~zod %groups" 300 3

log "done. codes: zod lidlut-tabwed-pillex-ridrup · moon pacmul-pollur-lignub-novhus · bud (see +code)"
