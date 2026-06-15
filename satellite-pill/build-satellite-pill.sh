#!/bin/bash
#
# Build Satellite Pill for Whisper OS
#
# Usage:
#   ./build-satellite-pill.sh v0
#   ./build-satellite-pill.sh v1
#   ./build-satellite-pill.sh v1-copy /path/to/pier
#
# Versions:
#   v0 - Copy known-good brass pill (default)
#   v1 - Show build steps for %base + %nativeplanet-mobile
#   v1-copy - Copy a completed .satellite output from a build pier
#

set -euo pipefail

VERSION="${1:-v0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/out"

mkdir -p "$OUT_DIR"

case "$VERSION" in
    v0)
        echo "=== Satellite Pill v0 ==="
        echo "v0 is an alias for the known-good brass pill."
        echo ""
        echo "To create satellite.pill for v0:"
        echo "  cp /path/to/urbit-v4.3.pill $OUT_DIR/satellite.pill"
        echo ""
        echo "Then copy to ROM prebuilts:"
        echo "  cp $OUT_DIR/satellite.pill /path/to/rom/vendor/nativeplanet/prebuilts/pill/"
        ;;

    v1)
        echo "=== Satellite Pill v1 ==="
        echo "v1 requires %nativeplanet-mobile desk."
        echo ""
        cat <<'EOF'
Build steps:

1. Boot a disposable fake ship.
2. In Dojo:
     |merge %nativeplanet-mobile our %base
     |mount %nativeplanet-mobile
3. In another shell, overlay the desk source into the mounted desk:
     cp satellite-pill/desks/nativeplanet-mobile/desk.bill <pier>/nativeplanet-mobile/desk.bill
     cp satellite-pill/desks/nativeplanet-mobile/sys.kelvin <pier>/nativeplanet-mobile/sys.kelvin
     cp satellite-pill/desks/nativeplanet-mobile/app/nativeplanet-mobile.hoon <pier>/nativeplanet-mobile/app/nativeplanet-mobile.hoon
4. In Dojo:
     |commit %nativeplanet-mobile
     |install our %nativeplanet-mobile
     .satellite +pill/brass %base %nativeplanet-mobile
5. Copy the generated jamfile:
     ./satellite-pill/build-satellite-pill.sh v1-copy <pier>

The Dojo dot sink writes the pill jamfile to:
  <pier>/.urb/put/.satellite
EOF
        echo ""
        echo "Output after v1-copy: $OUT_DIR/satellite.pill"
        ;;

    v1-copy)
        PIER="${2:-}"
        if [[ -z "$PIER" ]]; then
            echo "Usage: $0 v1-copy /path/to/pier" >&2
            exit 1
        fi

        SRC="$PIER/.urb/put/.satellite"
        DEST="$OUT_DIR/satellite.pill"
        if [[ ! -f "$SRC" ]]; then
            echo "Missing generated pill output: $SRC" >&2
            echo "Run this in Dojo first:" >&2
            echo "  .satellite +pill/brass %base %nativeplanet-mobile" >&2
            exit 1
        fi

        cp "$SRC" "$DEST"
        echo "Copied $SRC"
        echo "to     $DEST"
        echo ""
        wc -c "$DEST"
        sha256sum "$DEST"
        ;;

    *)
        echo "Unknown version: $VERSION"
        echo "Usage: $0 [v0|v1|v1-copy]"
        exit 1
        ;;
esac

echo ""
echo "Output directory: $OUT_DIR"
