param(
    [ValidateRange(900, 86400)]
    [int]$Interval = 3600
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$collectorPath = Join-Path $PSScriptRoot "collector.py"
$runtimeDir = Join-Path $projectRoot "data\exchange-rates\runtime"
$pidPath = Join-Path $runtimeDir "global-watch.pid"
$stdoutPath = Join-Path $runtimeDir "global-watch.stdout.log"
$stderrPath = Join-Path $runtimeDir "global-watch.stderr.log"

New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null

if (Test-Path -LiteralPath $pidPath) {
    $rawState = (Get-Content -LiteralPath $pidPath -Raw).Trim()
    $state = $rawState | ConvertFrom-Json
    $existingPid = [int]$state.process_id
    $existingProcess = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
    if ($existingProcess -and $existingProcess.ProcessName -like "python*") {
        Write-Output "Global collector is already running with PID $existingPid."
        exit 0
    }
    Remove-Item -LiteralPath $pidPath -Force
}

$pythonPath = (Get-Command python -ErrorAction Stop).Source
$arguments = @(
    "-u",
    $collectorPath,
    "global-watch",
    "--date", "today",
    "--interval", $Interval,
    "--output-dir", (Join-Path $projectRoot "data\exchange-rates")
)

$process = Start-Process `
    -FilePath $pythonPath `
    -ArgumentList $arguments `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath `
    -WindowStyle Hidden `
    -PassThru

$state = [ordered]@{
    process_id = $process.Id
    started_at_utc = $process.StartTime.ToUniversalTime().ToString("o")
    python_path = $pythonPath
    collector_path = $collectorPath
}
$state | ConvertTo-Json | Set-Content -LiteralPath $pidPath -Encoding UTF8
Write-Output "Started global collector with PID $($process.Id)."
Write-Output "stdout: $stdoutPath"
Write-Output "stderr: $stderrPath"
