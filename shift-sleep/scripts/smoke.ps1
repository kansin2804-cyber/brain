# Device/emulator smoke for ShiftSleep (Windows PowerShell)
# Prerequisites: JDK 17, Android SDK, adb on PATH, device or emulator online.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not $env:ANDROID_HOME) {
  if (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
  }
}
if ($env:ANDROID_HOME) {
  $env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
}

Write-Host "== unit tests =="
.\gradlew.bat :plan_engine:test :app:testDebugUnitTest --quiet
if ($LASTEXITCODE -ne 0) { throw "unit tests failed" }

$deviceLine = adb devices | Select-String "`tdevice$" | Select-Object -First 1
if (-not $deviceLine) {
  Write-Host "No adb device online — unit tests passed. Connect a phone/emulator for UI smoke."
  exit 0
}
$Device = ($deviceLine -split "\s+")[0]
Write-Host "== install debug APK on $Device =="
.\gradlew.bat :app:installDebug --quiet
if ($LASTEXITCODE -ne 0) { throw "installDebug failed" }

Write-Host "== launch + dump UI hierarchy snippets =="
adb -s $Device shell am force-stop com.shiftsleep.app 2>$null
adb -s $Device shell pm clear com.shiftsleep.app 2>$null
adb -s $Device shell am start -n com.shiftsleep.app/.MainActivity
Start-Sleep -Seconds 3
adb -s $Device shell uiautomator dump /sdcard/shiftsleep-ui.xml
$xml = adb -s $Device shell cat /sdcard/shiftsleep-ui.xml
$xml -split ">" | Select-String -Pattern "교대수면|시작하기|병원|오늘|근무표|설정|면책|취침|기상" | Select-Object -First 40

Write-Host ""
Write-Host "Smoke dump complete. Manually tap: 시작하기 → 홈 플랜 → 근무표 변경 → 설정 알림."
Write-Host "Checklist: docs in Agent Store — shift-sleep-laptop-smoke.md"
