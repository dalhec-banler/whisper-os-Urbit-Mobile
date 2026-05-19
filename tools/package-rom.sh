#!/bin/bash
#
# Build and package GrapheneOS ROM with NativePlanet integration
#
# Usage: ./package-rom.sh [grapheneos-path] [device] [build-number]
#

set -euo pipefail

GRAPHENEOS_PATH="${1:-$HOME/grapheneos}"
DEVICE="${2:-husky}"
BUILD_NUMBER="${3:-$(date +%Y%m%d%H)}"

if [[ ! -d "$GRAPHENEOS_PATH" ]]; then
    echo "Error: GrapheneOS directory not found: $GRAPHENEOS_PATH"
    exit 1
fi

cd "$GRAPHENEOS_PATH"

echo "=== Building GrapheneOS ROM ==="
echo "Path: $GRAPHENEOS_PATH"
echo "Device: $DEVICE"
echo "Build Number: $BUILD_NUMBER"
echo ""

# Set up environment
echo "Setting up build environment..."
source build/envsetup.sh
lunch "$DEVICE"

# Build target files
echo "Building target files..."
m vendorbootimage vendorkernelbootimage target-files-package

# Build OTA tools
echo "Building OTA tools..."
m otatools-package

# Finalize
echo "Finalizing..."
./script/finalize.sh

# Generate release
echo "Generating release..."
echo "NOTE: You will be prompted for key passphrase if keys are encrypted"
./script/generate-release.sh "$DEVICE" "$BUILD_NUMBER"

RELEASE_DIR="releases/$BUILD_NUMBER/release-$DEVICE-$BUILD_NUMBER"

echo ""
echo "=== Build complete ==="
echo ""
echo "Release artifacts:"
ls -lh "$RELEASE_DIR"/*.zip
echo ""
echo "To flash:"
echo "  cd $RELEASE_DIR"
echo "  unzip $DEVICE-install-$BUILD_NUMBER.zip"
echo "  cd $DEVICE-install-$BUILD_NUMBER"
echo "  adb reboot bootloader"
echo "  bash flash-all.sh"
