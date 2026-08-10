$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$pidPath = Join-Path $projectRoot "data\exchange-rates\runtime\global-watch.pid"

if (-not (Test-Path -LiteralPath $pidPath)) {
    Write-Output "Global collector is not running (PID file not found)."
    exit 0
}

$state = Get-Content -LiteralPath $pidPath -Raw | ConvertFrom-Json
$collectorPid = [int]$state.process_id
$process = Get-Process -Id $collectorPid -ErrorAction SilentlyContinue
if (-not $process) {
    Remove-Item -LiteralPath $pidPath -Force
    Write-Output "Removed stale PID file; process $collectorPid no longer exists."
    exit 0
}

if ($process.ProcessName -notlike "python*") {
    throw "PID $collectorPid is not a Python process; refusing to stop it."
}

$actualStartedAt = $process.StartTime.ToUniversalTime()
$recordedStartedAt = [datetime]::Parse($state.started_at_utc).ToUniversalTime()
$startDifference = [math]::Abs(($actualStartedAt - $recordedStartedAt).TotalSeconds)
if ($startDifference -gt 1 -or $process.Path -ne $state.python_path) {
    throw "PID $collectorPid no longer matches the recorded collector process; refusing to stop it."
}

Stop-Process -Id $collectorPid
Remove-Item -LiteralPath $pidPath -Force
Write-Output "Stopped global collector PID $collectorPid."
