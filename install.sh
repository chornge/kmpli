#!/usr/bin/env bash
# =============================================================
# Unix installer for Kmpli CLI
# Supports both: curl pipe and local git clone
# =============================================================

set -e

APP_NAME="kmpli"
REPO="chornge/kmpli"
LAUNCHER_SCRIPT="./kmpli"
TARGET="/usr/local/bin/$APP_NAME"

# Detect OS
OS="$(uname -s)"

case "$OS" in
  Darwin|Linux)
    # Supported
    ;;
  *)
    echo "Unsupported operating system: $OS"
    echo "   This installer supports macOS and Linux only."
    echo "   For Windows, use install.ps1 instead."
    exit 1
    ;;
esac

# Check if running from local clone or curl pipe
if [ -f "$LAUNCHER_SCRIPT" ]; then
  echo "Installing $APP_NAME launcher from local clone..."
  sudo cp "$LAUNCHER_SCRIPT" "$TARGET"
else
  echo "Downloading $APP_NAME launcher..."
  LAUNCHER_URL="https://raw.githubusercontent.com/$REPO/main/kmpli"

  # Download to temp file first
  TEMP_FILE=$(mktemp)
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$LAUNCHER_URL" -o "$TEMP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$LAUNCHER_URL" -O "$TEMP_FILE"
  else
    echo "Error: curl or wget is required to download the launcher"
    rm -f "$TEMP_FILE"
    exit 1
  fi

  sudo cp "$TEMP_FILE" "$TARGET"
  rm -f "$TEMP_FILE"
fi

sudo chmod +x "$TARGET"

echo "Successfully installed $APP_NAME at $TARGET"
echo ""
echo "   The binary will be automatically downloaded on first run."
echo "   Run '$APP_NAME --help' to get started."
