#!/usr/bin/env bash
# =============================================================
# Unix installer for Kmpli CLI
# =============================================================

set -e

APP_NAME="kmpli"
LAUNCHER_SCRIPT="./kmpli"
TARGET="/usr/local/bin/$APP_NAME"

# Detect OS
OS="$(uname -s)"

case "$OS" in
  Darwin|Linux)
    # Supported
    ;;
  *)
    echo "❌ Unsupported operating system: $OS"
    echo "   This installer supports macOS and Linux only."
    echo "   For Windows, use install.ps1 instead."
    exit 1
    ;;
esac

# Check if launcher script exists
if [ ! -f "$LAUNCHER_SCRIPT" ]; then
  echo "❌ Launcher script not found: $LAUNCHER_SCRIPT"
  echo "   Please run this installer from the kmpli project root directory."
  exit 1
fi

# Install the launcher
echo "Installing $APP_NAME launcher..."
sudo cp "$LAUNCHER_SCRIPT" "$TARGET"
sudo chmod +x "$TARGET"

echo "✅ Successfully installed $APP_NAME at $TARGET"
echo ""
echo "   The binary will be automatically downloaded on first run."
echo "   Run '$APP_NAME --help' to get started."
