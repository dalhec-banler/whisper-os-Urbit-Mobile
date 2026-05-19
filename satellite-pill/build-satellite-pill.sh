#!/bin/bash
#
# Build Satellite Pill for Whisper OS
#
# Usage: ./build-satellite-pill.sh [version]
#
# Versions:
#   v0 - Copy known-good brass pill (default)
#   v1 - Build with %base + %nativeplanet-mobile
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
        echo "Build steps (manual for now):"
        echo "1. Boot a fake ship with the required desks"
        echo "2. Run: .brass/pill +brass %base %nativeplanet-mobile"
        echo "3. Copy the resulting pill to $OUT_DIR/satellite.pill"
        echo ""
        echo "Automated build coming in future versions."
        ;;

    *)
        echo "Unknown version: $VERSION"
        echo "Usage: $0 [v0|v1]"
        exit 1
        ;;
esac

echo ""
echo "Output directory: $OUT_DIR"
