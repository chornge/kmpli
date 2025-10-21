#!/usr/bin/env bash
# =============================================================
# Unix installer for Kmpli CLI
# =============================================================

set -e

TARGET="/usr/local/bin/kmpli"
SOURCE="composeApp/build/bin/macosX64/releaseExecutable/kmpli.kexe"

sudo cp "$SOURCE" "$TARGET"
sudo chmod +x "$TARGET"

echo "Installed kmpli to $TARGET"
echo "Run 'kmpli --help' to verify."
