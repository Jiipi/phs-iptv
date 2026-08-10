$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$pidPath = Join-Path $projectRoot "data\exchange-rates\runtime\vcb-watch.pid"

if (-not (Test-Path -LiteralPath $pidPath)) {
    Write-Output "VCB collector is not running (PID file not found)."
    exit 0
}

$rawState = (Get-Content -LiteralPath $pidPath -Raw).Trim()
$legacyState = $rawState -match "^\d+$"
if ($legacyState) {
    $collectorPid = [int]$rawState
    $state = $null
} else {
    $state = $rawState | ConvertFrom-Json
    $collectorPid = [int]$state.process_id
}

$process = Get-Process -Id $collectorPid -ErrorAction SilentlyContinue
if (-not $process) {
    Remove-Item -LiteralPath $pidPath -Force
    Write-Output "Removed stale PID file; process $collectorPid no longer exists."
    exit 0
}

if ($process.ProcessName -notlike "python*") {
    throw "PID $collectorPid is not a Python process; refusing to stop it."
}

if (-not $legacyState) {
    $actualStartedAt = $process.StartTime.ToUniversalTime()
    $recordedStartedAt = [datetime]::Parse($state.started_at_utc).ToUniversalTime()
    $startDifference = [math]::Abs(($actualStartedAt - $recordedStartedAt).TotalSeconds)
    if ($startDifference -gt 1 -or $process.Path -ne $state.python_path) {
        throw "PID $collectorPid no longer matches the recorded collector process; refusing to stop it."
    }
}

Stop-Process -Id $collectorPid
Remove-Item -LiteralPath $pidPath -Force
Write-Output "Stopped VCB collector PID $collectorPid."
