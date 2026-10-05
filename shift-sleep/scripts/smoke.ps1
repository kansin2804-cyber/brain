# Device/emulator smoke for ShiftSleep (Windows PowerShell)
# Requires JDK 17 (or Android Studio JBR). Java 25 breaks AGP 8.7.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Find-Jdk17 {
  $candidates = @()
  if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) { $candidates += $env:JAVA_HOME }
  $studioJbr = "$env:ProgramFiles\Android\Android Studio\jbr"
  if (Test-Path $studioJbr) { $candidates += $studioJbr }
  $candidates += Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "jdk-17|jdk17" } | ForEach-Object { $_.FullName }
  $candidates += Get-ChildItem "C:\Program Files\Microsoft" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "jdk-17" } | ForEach-Object { $_.FullName }
  $candidates += Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "jdk-17" } | ForEach-Object { $_.FullName }
  foreach ($c in $candidates) {
    $javaExe = Join-Path $c "bin\java.exe"
    if (-not (Test-Path $javaExe)) { continue }
    $verLine = & $javaExe -version 2>&1 | Out-String
    if ($verLine -match 'version "17\.') { return $c }
    # Android Studio JBR 17 or 21 are both OK for this project; prefer 17, allow 21
    if ($c -like "*Android Studio\jbr*" -and ($verLine -match 'version "(17|21)\.')) { return $c }
  }
  return $null
}

$jdk = Find-Jdk17
if (-not $jdk) {
  Write-Host @"
[ERROR] JDK 17 (또는 Android Studio JBR)이 필요합니다.
지금 Gradle 에러에 '25.0.3'만 보이면 → 시스템 Java가 25라서 AGP가 실패한 것입니다.

해결:
  1) Android Studio 설치 (권장) — 자동으로 JBR 사용
  2) 또는 Temurin JDK 17 설치 후:
     `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot'
"@
  throw "Unsupported Java — install JDK 17"
}

$env:JAVA_HOME = $jdk
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "== JAVA_HOME=$env:JAVA_HOME =="
& "$env:JAVA_HOME\bin\java.exe" -version

if (-not $env:ANDROID_HOME) {
  if (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
  }
}
if ($env:ANDROID_HOME) {
  $env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
  Write-Host "== ANDROID_HOME=$env:ANDROID_HOME =="
} else {
  Write-Host "[WARN] ANDROID_HOME not set — install Android Studio SDK if build fails"
}

Write-Host "== unit tests (no --quiet so errors are visible) =="
.\gradlew.bat :plan_engine:test :app:testDebugUnitTest
if ($LASTEXITCODE -ne 0) { throw "unit tests failed — scroll up for Gradle 'What went wrong'" }

$deviceLine = adb devices | Select-String "`tdevice$" | Select-Object -First 1
if (-not $deviceLine) {
  Write-Host ""
  Write-Host "Unit tests PASSED."
  Write-Host "No adb device online — UI smoke skipped."
  Write-Host "Next: Android Studio에서 에뮬레이터 Play 또는 폰 USB 디버깅 ON 후 다시 .\scripts\smoke.ps1"
  exit 0
}
$Device = ($deviceLine -split "\s+")[0]
Write-Host "== install debug APK on $Device =="
.\gradlew.bat :app:installDebug
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
