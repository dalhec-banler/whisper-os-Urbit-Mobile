#!/usr/bin/env bash
#
# Build upstream vere (develop) for the phone: aarch64, static musl, 64-bit
# loom, edge pace. Runs in a Linux container because vere pins zig 0.15.2,
# which cannot link its own build runner on current macOS.
#
# Usage: tools/build-vere-android.sh /path/to/vere [output-dir]
#
# Needs a docker CLI with a Linux daemon (colima start, or Docker Desktop).
# Output: <output-dir>/vere64-develop-<short-sha>-linux-aarch64
set -euo pipefail

VERE_SRC="${1:?vere source dir}"
OUT_DIR="${2:-$VERE_SRC/zig-out}"
ZIG_VERSION="${ZIG_VERSION:-0.15.2}"

[[ -f "$VERE_SRC/build.zig" ]] || { echo "not a vere checkout: $VERE_SRC" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "no Linux docker daemon; run 'colima start' first" >&2; exit 1; }

docker run --rm --platform linux/arm64 -v "$VERE_SRC:/src" -w /src debian:bookworm-slim bash -lc "
  set -e
  apt-get update -qq >/dev/null
  apt-get install -y -qq curl xz-utils ca-certificates >/dev/null
  curl -sL https://ziglang.org/download/$ZIG_VERSION/zig-aarch64-linux-$ZIG_VERSION.tar.xz | tar -xJ -C /opt
  export PATH=/opt/zig-aarch64-linux-$ZIG_VERSION:\$PATH
  rm -rf .zig-cache zig-out
  zig build -Dtarget=aarch64-linux-musl -Drelease -Dpace=edge -Dvere64=true
  zig-out/aarch64-linux-musl/urbit --version | head -1
"

sha="$(git -C "$VERE_SRC" rev-parse --short HEAD)"
mkdir -p "$OUT_DIR"
cp "$VERE_SRC/zig-out/aarch64-linux-musl/urbit" "$OUT_DIR/vere64-develop-$sha-linux-aarch64"
shasum -a 256 "$OUT_DIR/vere64-develop-$sha-linux-aarch64"
