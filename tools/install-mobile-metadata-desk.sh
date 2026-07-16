#!/usr/bin/env bash
#
# Install the %nativeplanet-mobile metadata desk onto a running moon.
#
# The desk serves /gx/nativeplanet-mobile/apps/json, which the controller's
# hosted-app poller merges with docket discovery to add the mobile app
# curation (recommended flags, launch-mode hints). It is a small static-serving
# Gall stub forked from %base.
#
# This is the per-moon install path. It is used because the desk cannot be
# baked into a Tlon-carrying pill on an offline build ship: the Tlon %groups
# desk's metagrab agent needs network and crash-loops on a --fake build ship,
# which breaks the pill generator. Installing onto an already-running,
# networked moon avoids that entirely.
#
# Usage:
#   ./tools/install-mobile-metadata-desk.sh            # target the adb device's moon
#   MOON_SHIP=<bare-ship> ./tools/install-mobile-metadata-desk.sh
#
# Requires: adb (device connected, adb root available), node, the desk source
# under satellite-pill/desks/nativeplanet-mobile.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESK="$ROOT/satellite-pill/desks/nativeplanet-mobile"
CLIENT="$ROOT/tools/conn-client.js"
PORT=12343

adb root >/dev/null 2>&1 || true
sleep 1

SHIP="${MOON_SHIP:-}"
if [[ -z "$SHIP" ]]; then
  SHIP="$(adb shell 'cat /data/nativeplanet/boot-package.json 2>/dev/null' \
          | sed -n 's/.*"ship"[^"]*"\([^"]*\)".*/\1/p' | head -1)"
fi
if [[ -z "$SHIP" ]]; then
  echo "Could not determine moon ship; set MOON_SHIP=<bare-ship>." >&2
  exit 1
fi

SOCK="/data/nativeplanet/ships/$SHIP/.urb/conn.sock"
MP="/data/nativeplanet/ships/$SHIP/nativeplanet-mobile"
echo "Installing %nativeplanet-mobile onto ~$SHIP"

adb forward tcp:$PORT localfilesystem:"$SOCK" >/dev/null
trap 'adb forward --remove tcp:'$PORT' >/dev/null 2>&1 || true' EXIT

evl() { node "$CLIENT" --port $PORT eval "$1" 2>&1 | tail -1; }

echo "1/5 merge from %base"
evl '=/  m  (strand ,vase)  ;<  b=beak  bind:m  get-beak  ;<  ~  bind:m  (poke [p.b %hood] %kiln-merge !>([syd=%nativeplanet-mobile her=p.b sud=%base cas=r.b gem=%init]))  (pure:m !>(%ok))'

echo "2/5 mount"
evl '=/  m  (strand ,vase)  ;<  b=beak  bind:m  get-beak  =/  pax=path  /(scot %p p.b)/nativeplanet-mobile/(scot r.b)  ;<  ~  bind:m  (poke [p.b %hood] %kiln-mount !>([pax %nativeplanet-mobile]))  (pure:m !>(%ok))'
sleep 3

echo "3/5 overlay desk source"
adb push "$DESK/sys.kelvin"                       "$MP/sys.kelvin"                       >/dev/null
adb push "$DESK/desk.bill"                        "$MP/desk.bill"                        >/dev/null
adb push "$DESK/app/nativeplanet-mobile.hoon"     "$MP/app/nativeplanet-mobile.hoon"     >/dev/null
adb shell "chown -R shell:shell '$MP'; restorecon -R '$MP' 2>/dev/null" || true
sleep 1

echo "4/5 commit"
evl '=/  m  (strand ,vase)  ;<  b=beak  bind:m  get-beak  ;<  ~  bind:m  (poke [p.b %hood] %kiln-commit !>([%nativeplanet-mobile |]))  (pure:m !>(%ok))'
sleep 3

echo "5/5 install"
evl '=/  m  (strand ,vase)  ;<  b=beak  bind:m  get-beak  ;<  ~  bind:m  (poke [p.b %hood] %kiln-install !>([%nativeplanet-mobile p.b %nativeplanet-mobile]))  (pure:m !>(%ok))'
sleep 4

echo "verify: /gx/nativeplanet-mobile/apps/json"
evl '=/  m  (strand ,vase)  ;<  b=beak  bind:m  get-beak  =/  u  (mole |.(.^(json %gx /(scot %p p.b)/nativeplanet-mobile/(scot r.b)/apps/json)))  (pure:m !>(?~(u %not-serving %serving)))'

echo "Done. The controller poll will report source=docket+nativeplanet-mobile."
