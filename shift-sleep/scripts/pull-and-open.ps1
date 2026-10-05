# Pull latest ShiftSleep branch and open Android Studio.
# Safe to re-run. Does not touch ju-calc.

$ErrorActionPreference = "Stop"

$candidates = @(
    (Join-Path $HOME "Documents\brain"),
    (Join-Path $HOME "Documents\GitHub\brain"),
    (Join-Path (Get-Location) "."),
    (Join-Path (Get-Location) "..")
)

$repo = $null
foreach ($c in $candidates) {
    if (Test-Path (Join-Path $c ".git")) {
        $repo = (Resolve-Path $c).Path
        break
    }
}

if (-not $repo) {
    Write-Host "brain 폴더를 못 찾았습니다. 아래처럼 경로만 바꿔 실행하세요:"
    Write-Host '  cd C:\Users\kansi\Documents\brain'
    Write-Host '  .\shift-sleep\scripts\pull-and-open.ps1'
    exit 1
}

Write-Host "repo: $repo"
Set-Location $repo
git fetch origin
git checkout cursor/shift-sleep-mvp-c6cd
git pull origin cursor/shift-sleep-mvp-c6cd

$ss = Join-Path $repo "shift-sleep"
if (-not (Test-Path $ss)) {
    Write-Host "shift-sleep 폴더 없음: $ss"
    exit 1
}

Set-Location $ss
Write-Host "ShiftSleep 최신 받음. Studio 엽니다..."
& .\scripts\open-shiftsleep.ps1
