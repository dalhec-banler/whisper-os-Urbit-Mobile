#!/bin/bash
#
# Build vere for Android ARM64
#
# Usage: ./build-vere-android.sh [path-to-vere-source] [path-to-zig]
#

set -euo pipefail

VERE_SRC="${1:-$HOME/dev/mobile-vere/vere}"
ZIG_PATH="${2:-$HOME/tools/zig}"

if [[ ! -d "$VERE_SRC" ]]; then
    echo "Error: Vere source directory not found: $VERE_SRC"
    echo "Usage: $0 [path-to-vere-source] [path-to-zig]"
    exit 1
fi

if [[ ! -x "$ZIG_PATH/zig" ]]; then
    echo "Error: Zig not found at: $ZIG_PATH/zig"
    echo "Usage: $0 [path-to-vere-source] [path-to-zig]"
    exit 1
fi

echo "=== Building vere for Android ARM64 ==="
echo "Source: $VERE_SRC"
echo "Zig: $ZIG_PATH"

cd "$VERE_SRC"

# Clean previous build
echo "Cleaning previous build..."
rm -rf .zig-cache zig-out

# Build
echo "Building..."
"$ZIG_PATH/zig" build \
    -Dtarget=aarch64-linux-musl \
    -Dandroid=true \
    -Drelease

# Verify
BINARY="zig-out/aarch64-linux-musl/urbit"
if [[ ! -f "$BINARY" ]]; then
    echo "Error: Build failed, binary not found"
    exit 1
fi

echo ""
echo "=== Build successful ==="
echo "Binary: $BINARY"
echo ""
echo "Verification:"
readelf -h "$BINARY" | grep -E 'Type|Entry'
echo ""
echo "Size: $(du -h "$BINARY" | cut -f1)"
echo ""
echo "To install:"
echo "  cp $BINARY /path/to/grapheneos/vendor/nativeplanet/prebuilts/bin/vere"
