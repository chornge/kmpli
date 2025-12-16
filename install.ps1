# =============================================================
# Windows/Powershell installer for Kmpli CLI
# Supports both: irm pipe and local git clone
# =============================================================

$installDir = "$env:USERPROFILE\.kmpli"
$launcherScript = ".\kmpli.ps1"
$repo = "chornge/kmpli"
$batchWrapper = "$installDir\kmpli.bat"

# Create install directory if it doesn't exist
Write-Host "Installing kmpli launcher..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $installDir | Out-Null

# Check if running from local clone or irm pipe
if (Test-Path $launcherScript)
{
    Write-Host "   Using local launcher script..." -ForegroundColor Gray
    try
    {
        Copy-Item $launcherScript -Destination "$installDir\kmpli.ps1" -Force -ErrorAction Stop
    }
    catch
    {
        Write-Host "Failed to copy launcher script: $_" -ForegroundColor Red
        exit 1
    }
}
else
{
    Write-Host "   Downloading launcher script..." -ForegroundColor Gray
    $launcherUrl = "https://raw.githubusercontent.com/$repo/main/kmpli.ps1"
    try
    {
        Invoke-WebRequest -Uri $launcherUrl -OutFile "$installDir\kmpli.ps1" -UseBasicParsing -ErrorAction Stop
    }
    catch
    {
        Write-Host "Failed to download launcher script: $_" -ForegroundColor Red
        exit 1
    }
}

# Create a batch wrapper to make it easier to call
$batchContent = @"
@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$installDir\kmpli.ps1" %*
"@

try
{
    Set-Content -Path $batchWrapper -Value $batchContent -Force -ErrorAction Stop
}
catch
{
    Write-Host "Failed to create batch wrapper: $_" -ForegroundColor Red
    exit 1
}

# Add to PATH if not already there
if (-not ($env:Path -split ";" | Where-Object { $_ -eq $installDir }))
{
    try
    {
        [Environment]::SetEnvironmentVariable("Path", "$env:Path;$installDir", [System.EnvironmentVariableTarget]::User)
        $env:Path = "$env:Path;$installDir"
        Write-Host "Successfully installed kmpli launcher at $installDir" -ForegroundColor Green
        Write-Host "   Added $installDir to PATH." -ForegroundColor Green
        Write-Host "" -ForegroundColor Green
        Write-Host "   The binary will be automatically downloaded on first run." -ForegroundColor Cyan
        Write-Host "   Restart your terminal and run 'kmpli --help' to get started." -ForegroundColor Yellow
    }
    catch
    {
        Write-Host "Failed to update PATH: $_" -ForegroundColor Red
        Write-Host "   You may need to manually add $installDir to your PATH." -ForegroundColor Yellow
        exit 1
    }
}
else
{
    Write-Host "Successfully installed kmpli launcher at $installDir" -ForegroundColor Green
    Write-Host "   'kmpli' is already in PATH." -ForegroundColor Green
    Write-Host "" -ForegroundColor Green
    Write-Host "   The binary will be automatically downloaded on first run." -ForegroundColor Cyan
    Write-Host "   Run 'kmpli --help' to get started." -ForegroundColor Yellow
}
