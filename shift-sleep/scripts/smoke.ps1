# ShiftSleep smoke — Windows. Requires JDK 17 or 21 (not Java 25).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Get-JavaVersionText([string]$JavaExe) {
    # java -version prints to stderr; use cmd so PowerShell does not throw
    return ((cmd /c "`"$JavaExe`" -version 2>&1") | Out-String)
}

function Test-JavaOk([string]$Text) {
    return ($Text -match 'version "(17|21)\.')
}

function Add-Candidate([System.Collections.Generic.List[string]]$List, [string]$Path) {
    if (-not $Path) { return }
    if (-not (Test-Path $Path)) { return }
    $r = (Resolve-Path $Path).Path
    if ($List -notcontains $r) { [void]$List.Add($r) }
}

function Find-Jdk {
    $list = [System.Collections.Generic.List[string]]::new()
    Add-Candidate $list $env:JAVA_HOME
    @(
        "$env:ProgramFiles\Android\Android Studio\jbr"
        "$env:LOCALAPPDATA\Programs\Android\Android Studio\jbr"
        "${env:ProgramFiles(x86)}\Android\Android Studio\jbr"
    ) | ForEach-Object { Add-Candidate $list $_ }

    @(
        "$env:ProgramFiles\Eclipse Adoptium"
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
        "$env:ProgramFiles\Microsoft"
        "$env:ProgramFiles\Java"
        "$env:ProgramFiles\BellSoft"
        "$env:ProgramFiles\Amazon Corretto"
    ) | ForEach-Object {
        if (-not (Test-Path $_)) { return }
        Get-ChildItem $_ -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'jdk-?(17|21)|jdk17|jdk21|corretto-17|corretto-21' } |
            ForEach-Object { Add-Candidate $list $_.FullName }
    }

    # Newest matching folders first
    $ordered = $list | Sort-Object -Descending
    foreach ($c in $ordered) {
        $exe = Join-Path $c "bin\java.exe"
        if (-not (Test-Path $exe)) { continue }
        $ver = Get-JavaVersionText $exe
        if (Test-JavaOk $ver) {
            return @{ Home = $c; Ver = ($ver -split "`n" | Select-Object -First 1).Trim() }
        }
    }
    return $null
}

function Install-Jdk17 {
    Write-Host ""
    Write-Host "=== Installing Temurin JDK 17 via winget ===" -ForegroundColor Yellow
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $winget) {
        Write-Host "winget not found. Install manually:"
        Write-Host "  https://adoptium.net/temurin/releases/?version=17"
        Write-Host "Or install Android Studio, then re-run this script."
        return $false
    }
    # Non-interactive install
    winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne -1978335189) {
        # -1978335189 often means already installed
        Write-Host "winget exit code: $LASTEXITCODE (may still be OK if already installed)"
    }
    return $true
}

# --- main ---
$jdk = Find-Jdk
if (-not $jdk) {
    Write-Host ""
    Write-Host "[ERROR] No JDK 17/21 found. Current default java is probably 25." -ForegroundColor Red
    Write-Host "This Android project cannot build with Java 25."
    Write-Host ""
    Write-Host "Auto-install JDK 17 now? (Y/N)"
    $ans = Read-Host
    if ($ans -match '^(Y|y|yes)$') {
        $ok = Install-Jdk17
        if ($ok) {
            # Refresh PATH from Machine + User for this session
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                        [System.Environment]::GetEnvironmentVariable("Path", "User")
            $jdk = Find-Jdk
        }
    }
}

if (-not $jdk) {
    Write-Host ""
    Write-Host "Still no JDK 17/21. Do ONE of these, then OPEN A NEW PowerShell:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  1) winget install --id EclipseAdoptium.Temurin.17.JDK -e"
    Write-Host "  2) Install Android Studio (includes a usable JBR)"
    Write-Host "  3) Download: https://adoptium.net/temurin/releases/?version=17"
    Write-Host ""
    Write-Host "Then:"
    Write-Host "  cd $Root"
    Write-Host "  .\scripts\smoke.ps1"
    Write-Host ""
    Write-Host "Current java -version:"
    cmd /c "java -version 2>&1"
    exit 1
}

$env:JAVA_HOME = $jdk.Home
$env:Path = "$env:JAVA_HOME\bin;" + (
    ($env:Path -split ';' | Where-Object { $_ -and ($_ -notmatch '\\(java|jdk|jbr|Adoptium)\\') }) -join ';'
)

Write-Host "== JAVA_HOME=$env:JAVA_HOME ==" -ForegroundColor Green
Write-Host $jdk.Ver

if (-not $env:ANDROID_HOME) {
    if (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
}
if ($env:ANDROID_HOME) {
    $env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
    Write-Host "== ANDROID_HOME=$env:ANDROID_HOME =="
} else {
    Write-Host "[WARN] ANDROID_HOME missing. Install Android Studio SDK if Gradle fails."
}

Write-Host "== unit tests =="
.\gradlew.bat :plan_engine:test :app:testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Host "Unit tests FAILED. Scroll up for Gradle error." -ForegroundColor Red
    exit 1
}

$deviceLine = adb devices 2>$null | Select-String "`tdevice$" | Select-Object -First 1
if (-not $deviceLine) {
    Write-Host ""
    Write-Host "Unit tests PASSED." -ForegroundColor Green
    Write-Host "No adb device — UI smoke skipped."
    Write-Host "Start Android Studio emulator (Play) or USB-debug phone, then re-run:"
    Write-Host "  .\scripts\smoke.ps1"
    exit 0
}

$Device = ($deviceLine -split "\s+")[0]
Write-Host "== installDebug on $Device =="
.\gradlew.bat :app:installDebug
if ($LASTEXITCODE -ne 0) { Write-Host "installDebug failed"; exit 1 }

Write-Host "== launch app =="
adb -s $Device shell am force-stop com.shiftsleep.app 2>$null
adb -s $Device shell pm clear com.shiftsleep.app 2>$null
adb -s $Device shell am start -n com.shiftsleep.app/.MainActivity
Start-Sleep -Seconds 3

# Confirm package is installed and focused (ASCII-only — avoid Korean regex/encoding issues)
$pkg = adb -s $Device shell pm path com.shiftsleep.app 2>$null
Write-Host "Installed package: $pkg"
$focus = adb -s $Device shell dumpsys window 2>$null | Select-String "mCurrentFocus|mFocusedApp" | Select-Object -First 3
Write-Host $focus

adb -s $Device shell uiautomator dump /sdcard/shiftsleep-ui.xml 2>$null
adb -s $Device pull /sdcard/shiftsleep-ui.xml "$Root\shiftsleep-ui.xml" 2>$null | Out-Null
if (Test-Path "$Root\shiftsleep-ui.xml") {
    $ui = Get-Content "$Root\shiftsleep-ui.xml" -Raw -ErrorAction SilentlyContinue
    # SimpleContains (not Select-String regex) for Korean text
    $needles = @("교대수면", "시작하기", "병원", "오늘", "근무표", "설정", "취침", "기상")
    foreach ($n in $needles) {
        if ($ui -and $ui.Contains($n)) { Write-Host "UI has: $n" -ForegroundColor Green }
    }
}

Write-Host ""
Write-Host "If phone shows Wooden Calculator (ju-calc), Android Studio opened the WRONG project." -ForegroundColor Yellow
Write-Host "ShiftSleep package is: com.shiftsleep.app  (launcher name: 교대수면)"
Write-Host "Open in Studio: File > Open > C:\Users\kansi\Documents\brain\shift-sleep"
Write-Host "Or on phone: find app icon '교대수면' (not 목조계산기)."
Write-Host "Manual check: Start -> Home plan -> Schedule -> Settings"
