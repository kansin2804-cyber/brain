# Open ShiftSleep (not ju-calc) on connected phone
$ErrorActionPreference = "Stop"
$sdk = "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
if (Test-Path $sdk) { $env:Path = "$sdk;$env:Path" }

$dev = (adb devices | Select-String "`tdevice$" | Select-Object -First 1)
if (-not $dev) { throw "No adb device. Plug phone + USB debugging ON." }
$id = ($dev -split "\s+")[0]
Write-Host "Device: $id"

$pkg = adb -s $id shell pm path com.shiftsleep.app
if (-not $pkg) {
    Write-Host "com.shiftsleep.app NOT installed. Run from shift-sleep folder:"
    Write-Host "  .\scripts\smoke.ps1"
    exit 1
}
Write-Host $pkg

adb -s $id shell am force-stop com.juhousing.calc 2>$null
adb -s $id shell am start -n com.shiftsleep.app/.MainActivity
Write-Host "Launched com.shiftsleep.app/.MainActivity"
Write-Host "Phone should show '교대수면' (NOT 목조계산기)."
Write-Host ""
Write-Host "Android Studio tip: File > Open > ...\Documents\brain\shift-sleep"
Write-Host "Do NOT Run the ju-calc Capacitor project if you want ShiftSleep."
