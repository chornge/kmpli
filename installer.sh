#!/usr/bin/env bash
# =============================================================
# Universal installer for Kmpli CLI
# Works on macOS, Linux, and Windows (via Git Bash / WSL)
# =============================================================

set -e

APP_NAME="kmpli"
DIST_DIR="composeApp/dist"

# Detect OS and architecture
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"

case "$OS" in
  darwin*)
    if [[ "$ARCH" == "arm64" ]]; then
      BINARY="${DIST_DIR}/${APP_NAME}-macos-arm64"
    else
      BINARY="${DIST_DIR}/${APP_NAME}-macos-x64"
    fi
    INSTALL_PATH="/usr/local/bin/${APP_NAME}"
    ;;
  linux*)
    BINARY="${DIST_DIR}/${APP_NAME}-linux-x64"
    INSTALL_PATH="/usr/local/bin/${APP_NAME}"
    ;;
  msys*|cygwin*|mingw*)
    BINARY="${DIST_DIR}/${APP_NAME}-windows-x64.exe"
    INSTALL_PATH="/c/Program Files/${APP_NAME}/${APP_NAME}.exe"
    ;;
  *)
    echo "❌ Unsupported OS: $OS"
    exit 1
    ;;
esac

# Verify binary exists
if [[ ! -f "$BINARY" ]]; then
  echo "❌ Binary not found: $BINARY"
  echo "Please build it first with:"
  echo "  ./gradlew copyBinariesToDist"
  exit 1
fi

echo "🔍 Detected OS: $OS ($ARCH)"
echo "📦 Installing binary: $BINARY"

# macOS/Linux install
if [[ "$OS" == "darwin"* || "$OS" == "linux"* ]]; then
  sudo mkdir -p "$(dirname "$INSTALL_PATH")"
  sudo cp "$BINARY" "$INSTALL_PATH"
  sudo chmod +x "$INSTALL_PATH"
  echo "✅ Installed to $INSTALL_PATH"
  echo "👉 You can now run: $APP_NAME --help"
fi

# Windows install
if [[ "$OS" == *"mingw"* || "$OS" == *"cygwin"* || "$OS" == *"msys"* ]]; then
  echo "🔧 Installing on Windows..."
  mkdir -p "$(dirname "$INSTALL_PATH")"
  cp "$BINARY" "$INSTALL_PATH"

  # Add to PATH if not already there
  WIN_PATH="$(echo "$PATH" | grep -i 'Program Files' || true)"
  if [[ -z "$WIN_PATH" ]]; then
    setx PATH "%PATH%;C:\\Program Files\\${APP_NAME}\\"
    echo "📂 Added ${APP_NAME} to PATH"
  fi
  echo "✅ Installed to $INSTALL_PATH"
  echo "👉 You can now run: ${APP_NAME}.exe --help"
fi

echo "🎉 Installation complete!"
