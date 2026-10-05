# Device/emulator smoke for ShiftSleep (Windows PowerShell)
# Requires JDK 17 or 21 (Android Studio JBR). Java 25 breaks AGP 8.7.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Get-JavaVersionText {
    param([string]$JavaExe)
    # java -version writes to stderr; PowerShell treats that as error without this wrapper
    $lines = cmd /c "`"$JavaExe`" -version 2>&1"
    return ($lines | Out-String)
}

function Test-JavaVersionOk {
    param([string]$VersionText)
    return ($VersionText -match 'version "(17|21)\.')
}

function Add-JdkCandidate {
    param([System.Collections.Generic.List[string]]$List, [string]$Path)
    if ($Path -and (Test-Path $Path)) {
        $resolved = (Resolve-Path $Path).Path
        if ($List -notcontains $resolved) { [void]$List.Add($resolved) }
    }
}

function Find-CompatibleJdk {
    $candidates = [System.Collections.Generic.List[string]]::new()

    # Explicit override (run once: $env:JAVA_HOME = '...\jdk-17...')
    Add-JdkCandidate $candidates $env:JAVA_HOME

    # Android Studio JBR (17 or 21)
    @(
        "$env:ProgramFiles\Android\Android Studio\jbr"
        "$env:LOCALAPPDATA\Programs\Android\Android Studio\jbr"
        "${env:ProgramFiles(x86)}\Android\Android Studio\jbr"
    ) | ForEach-Object { Add-JdkCandidate $candidates $_ }

    # Temurin / Microsoft / Oracle JDK 17 or 21
    @(
        "$env:ProgramFiles\Eclipse Adoptium"
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
        "$env:ProgramFiles\Microsoft"
        "$env:ProgramFiles\Java"
        "$env:ProgramFiles\BellSoft"
    ) | ForEach-Object {
        if (-not (Test-Path $_)) { return }
        Get-ChildItem $_ -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'jdk-?(17|21)' } |
            ForEach-Object { Add-JdkCandidate $candidates $_.FullName }
    }

    foreach ($c in $candidates) {
        $javaExe = Join-Path $c "bin\java.exe"
        if (-not (Test-Path $javaExe)) { continue }
        $ver = Get-JavaVersionText $javaExe
        if (Test-JavaVersionOk $ver) {
            return @{ Home = $c; Version = ($ver -split "`n" | Select-Object -First 1).Trim() }
        }
    }
    return $null
}

$jdkInfo = Find-CompatibleJdk
if (-not $jdkInfo) {
    Write-Host ""
    Write-Host "[ERROR] JDK 17 또는 21이 필요합니다. (지금 PC 기본 Java는 25일 가능성 큼)" -ForegroundColor Red
    Write-Host ""
    Write-Host "빠른 해결 (하나만):" -ForegroundColor Yellow
    Write-Host "  A) Android Studio 설치 → 다시 .\scripts\smoke.ps1"
    Write-Host "  B) Temurin 17 설치:"
    Write-Host "       winget install EclipseAdoptium.Temurin.17.JDK"
    Write-Host "     설치 후 새 PowerShell에서:"
    Write-Host "       cd $Root"
    Write-Host "       .\scripts\smoke.ps1"
    Write-Host ""
    Write-Host "  C) 이미 JDK 17 경로를 알면 이 세션만:"
    Write-Host "       `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot'"
    Write-Host "       .\scripts\smoke.ps1"
    Write-Host ""
    Write-Host "현재 PATH의 java:"
    cmd /c "java -version 2>&1"
    throw "Unsupported Java — install JDK 17 or 21"
}

$env:JAVA_HOME = $jdkInfo.Home
# Put chosen JDK first; remove other java from front of PATH confusion
$env:Path = "$env:JAVA_HOME\bin;" + ($env:Path -split ';' | Where-Object { $_ -and ($_ -notmatch '\\java\\|\\jdk|\\jbr') } | Select-Object -Unique) -join ';'

Write-Host "== JAVA_HOME=$env:JAVA_HOME ==" -ForegroundColor Green
Write-Host $jdkInfo.Version
Get-JavaVersionText "$env:JAVA_HOME\bin\java.exe"

if (-not $env:ANDROID_HOME) {
    if (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
}
if ($env:ANDROID_HOME) {
    $env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
    Write-Host "== ANDROID_HOME=$env:ANDROID_HOME =="
} else {
    Write-Host "[WARN] ANDROID_HOME not set — Android Studio SDK 설치 권장"
}

Write-Host "== unit tests =="
.\gradlew.bat :plan_engine:test :app:testDebugUnitTest
if ($LASTEXITCODE -ne 0) { throw "unit tests failed — scroll up for Gradle 'What went wrong'" }

$deviceLine = adb devices | Select-String "`tdevice$" | Select-Object -First 1
if (-not $deviceLine) {
    Write-Host ""
    Write-Host "Unit tests PASSED." -ForegroundColor Green
    Write-Host "No adb device — UI smoke skipped."
    Write-Host "Android Studio 에뮬 Play 또는 USB 디버깅 후 다시 .\scripts\smoke.ps1"
    exit 0
}
$Device = ($deviceLine -split "\s+")[0]
Write-Host "== install debug APK on $Device =="
.\gradlew.bat :app:installDebug
if ($LASTEXITCODE -ne 0) { throw "installDebug failed" }

Write-Host "== launch + dump UI =="
adb -s $Device shell am force-stop com.shiftsleep.app 2>$null
adb -s $Device shell pm clear com.shiftsleep.app 2>$null
adb -s $Device shell am start -n com.shiftsleep.app/.MainActivity
Start-Sleep -Seconds 3
adb -s $Device shell uiautomator dump /sdcard/shiftsleep-ui.xml
$xml = adb -s $Device shell cat /sdcard/shiftsleep-ui.xml
$xml -split ">" | Select-String -Pattern "교대수면|시작하기|병원|오늘|근무표|설정|면책|취침|기상" | Select-Object -First 40

Write-Host ""
Write-Host "Smoke dump complete. 수동: 시작하기 → 홈 → 근무표 → 설정"
