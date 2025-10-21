# =============================================================
# Windows/Powershell installer for Kmpli CLI
# =============================================================

$binDir = "$env:USERPROFILE\.kmpli\bin"
$exePath = "composeApp\build\bin\mingwX64\releaseExecutable\kmpli.exe"

# Create bin directory if it doesn't exist
New-Item -ItemType Directory -Force -Path $binDir | Out-Null

# Copy executable
Copy-Item $exePath -Destination "$binDir\kmpli.exe" -Force

# Add to PATH if not already there
if (-not ($env:Path -split ";" | Where-Object { $_ -eq $binDir }))
{
    setx PATH "$( $env:Path );$binDir"
    Write-Host "Added $binDir to PATH. Restart your terminal to use 'kmpli'."
}
else
{
    Write-Host "'kmpli' is already in PATH."
}

Write-Host "Installed 'kmpli' to $binDir"