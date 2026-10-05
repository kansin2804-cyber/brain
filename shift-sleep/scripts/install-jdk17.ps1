# One-shot: install Temurin JDK 17 then print next steps.
# Run:  powershell -ExecutionPolicy Bypass -File .\scripts\install-jdk17.ps1
$ErrorActionPreference = "Stop"
Write-Host "Installing Eclipse Temurin JDK 17 (winget)..." -ForegroundColor Yellow

$winget = Get-Command winget -ErrorAction SilentlyContinue
if (-not $winget) {
    Write-Host "winget missing. Open this URL and install JDK 17 (.msi):"
    Write-Host "https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64&package=jdk"
    exit 1
}

winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-source-agreements --accept-package-agreements
Write-Host ""
Write-Host "IMPORTANT: Close this PowerShell and open a NEW window." -ForegroundColor Green
Write-Host "Then run:"
Write-Host "  cd C:\Users\kansi\Documents\brain\shift-sleep"
Write-Host "  .\scripts\smoke.ps1"
Write-Host ""
Write-Host "Check after reopen:"
Write-Host "  & `"$env:ProgramFiles\Eclipse Adoptium\*\bin\java.exe`" -version"
