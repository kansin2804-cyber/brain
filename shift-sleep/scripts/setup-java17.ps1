# JDK 17 설치 + JAVA_HOME 설정 (Windows, 1회)
# 관리자 PowerShell 권장
$ErrorActionPreference = "Stop"

Write-Host "== ShiftSleep: JDK 17 setup =="

$installed = Get-Command winget -ErrorAction SilentlyContinue
if ($installed) {
    Write-Host "Temurin JDK 17 설치 시도 (winget)..."
    winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-source-agreements --accept-package-agreements
} else {
    Write-Host "winget 없음. 브라우저에서 설치:"
    Write-Host "https://adoptium.net/temurin/releases/?version=17"
}

Write-Host ""
Write-Host "설치 후 **새 PowerShell** 열고:"
Write-Host "  cd C:\Users\kansi\Documents\brain\shift-sleep"
Write-Host "  .\scripts\smoke.ps1"
Write-Host ""
Write-Host "또는 Android Studio가 있으면 setup 없이 smoke.ps1 만 실행 (Studio jbr 자동 사용)."
