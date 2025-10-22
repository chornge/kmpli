#!/usr/bin/env bash
# =============================================================
# Unix installer for Kmpli CLI
# =============================================================

set -e

APP_NAME="kmpli"
TARGET="/usr/local/bin/$APP_NAME"

# Detect OS
OS="$(uname -s)"
ARCH="$(uname -m)"

case "$OS" in
  Darwin)
    if [[ "$ARCH" == "arm64" ]]; then
      SOURCE="composeApp/build/bin/macosArm64/releaseExecutable/$APP_NAME.kexe"
    else
      SOURCE="composeApp/build/bin/macosX64/releaseExecutable/$APP_NAME.kexe"
    fi
    ;;
  Linux)
    if [[ "$ARCH" == "aarch64" ]]; then
      SOURCE="composeApp/build/bin/linuxArm64/releaseExecutable/$APP_NAME.kexe"
    else
      SOURCE="composeApp/build/bin/linuxX64/releaseExecutable/$APP_NAME.kexe"
    fi
    ;;
esac

sudo cp "$SOURCE" "$TARGET"
sudo chmod +x "$TARGET"

echo "Installed $APP_NAME at $TARGET"
echo "Run '$APP_NAME --help' to verify."
