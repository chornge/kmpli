# Kmpli launcher - auto-downloads and runs the appropriate binary for Windows

$ErrorActionPreference = "Stop"

$REPO = "chornge/kmpli"
$CACHE_DIR = "$env:USERPROFILE\.kmpli\bin"
$BINARY_NAME = "kmpli-windows-x64.exe"
$DISPLAY_NAME = "Windows x64"

# Check for locally built binary first (for development)
$LOCAL_BUILD_PATH = ".\composeApp\build\bin\mingwX64\releaseExecutable\kmpli.exe"
if (Test-Path $LOCAL_BUILD_PATH)
{
    & $LOCAL_BUILD_PATH $args
    exit $LASTEXITCODE
}

# Check for cached binary
$CACHED_BINARY = Join-Path $CACHE_DIR $BINARY_NAME
if (Test-Path $CACHED_BINARY)
{
    & $CACHED_BINARY $args
    exit $LASTEXITCODE
}

# Download binary from GitHub releases
Write-Host "🔍 Detecting platform: $DISPLAY_NAME" -ForegroundColor Cyan
Write-Host "📦 Downloading kmpli binary..." -ForegroundColor Cyan

# Create cache directory
New-Item -ItemType Directory -Force -Path $CACHE_DIR | Out-Null

# Get the latest release tag
try
{
    $releaseInfo = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/latest"
    $LATEST_TAG = $releaseInfo.tag_name
}
catch
{
    Write-Host "❌ Failed to fetch latest release information" -ForegroundColor Red
    Write-Host "   Please check your internet connection or visit:" -ForegroundColor Yellow
    Write-Host "   https://github.com/$REPO/releases" -ForegroundColor Yellow
    exit 1
}

if (-not $LATEST_TAG)
{
    Write-Host "❌ Failed to determine latest release version" -ForegroundColor Red
    exit 1
}

# Validate tag format (prevents command injection)
if (-not ($LATEST_TAG -match '^v\d+\.\d+\.\d+$'))
{
    Write-Host "❌ Invalid release tag format: $LATEST_TAG" -ForegroundColor Red
    Write-Host "   Expected format: vX.Y.Z (e.g., v1.2.5)" -ForegroundColor Yellow
    exit 1
}

Write-Host "📥 Downloading version $LATEST_TAG..." -ForegroundColor Cyan

# Download the binary
$DOWNLOAD_URL = "https://github.com/$REPO/releases/download/$LATEST_TAG/$BINARY_NAME"
$TEMP_FILE = Join-Path $CACHE_DIR "$BINARY_NAME.tmp"

try
{
    Invoke-WebRequest -Uri $DOWNLOAD_URL -OutFile $TEMP_FILE -ErrorAction Stop
}
catch
{
    Write-Host "❌ Failed to download binary from:" -ForegroundColor Red
    Write-Host "   $DOWNLOAD_URL" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Yellow
    Write-Host "   This might mean:" -ForegroundColor Yellow
    Write-Host "   1. No release exists for your platform ($DISPLAY_NAME)" -ForegroundColor Yellow
    Write-Host "   2. You need to build from source:" -ForegroundColor Yellow
    Write-Host "      .\gradlew.bat build" -ForegroundColor Yellow
    exit 1
}

# Move to final location
Move-Item -Path $TEMP_FILE -Destination $CACHED_BINARY -Force

Write-Host "✅ Successfully downloaded kmpli" -ForegroundColor Green
Write-Host ""

# Execute the binary
& $CACHED_BINARY $args
exit $LASTEXITCODE
