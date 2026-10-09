param([switch]$Build)

$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$taskJar = Join-Path $taskRoot 'target\YIYUAN-0.0.1-SNAPSHOT.jar'
$taskLogs = Join-Path $taskRoot 'target\portal-logs'
$taskPortals = @(
    @{ Mode = 'staff'; Port = 8083 },
    @{ Mode = 'patient'; Port = 8082 }
)

function Stop-ManagedPortal([int]$Port) {
    $taskListener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$taskListener) { return }
    $taskProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($taskListener.OwningProcess)"
    if (!$taskProcess -or !$taskProcess.CommandLine -or !$taskProcess.CommandLine.Contains($taskJar)) {
        throw "Port $Port belongs to another application; it was not stopped."
    }
    Stop-Process -Id $taskProcess.ProcessId -Force
}

foreach ($taskPortal in $taskPortals) { Stop-ManagedPortal $taskPortal.Port }
if ($Build) {
    Push-Location $taskRoot
    try {
        & .\mvnw.cmd -Pfrontend -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Frontend/backend build failed.' }
    } finally { Pop-Location }
}
if (!(Test-Path -LiteralPath $taskJar)) { throw 'Build first: .\mvnw.cmd -Pfrontend -DskipTests package' }
New-Item -ItemType Directory -Force -Path $taskLogs | Out-Null
$taskJava = (Get-Command java -ErrorAction Stop).Source

foreach ($taskPortal in $taskPortals) {
    $taskMode = $taskPortal.Mode
    $taskPort = $taskPortal.Port
    $taskProcess = Start-Process -FilePath $taskJava -ArgumentList @(
        '-jar', ('"' + $taskJar + '"'),
        "--spring.profiles.active=$taskMode",
        "--hospital.portal.mode=$taskMode",
        "--server.port=$taskPort"
    ) -WorkingDirectory $taskRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $taskLogs "$taskMode-out.log") `
        -RedirectStandardError (Join-Path $taskLogs "$taskMode-err.log")
    $taskReady = $false
    for ($taskAttempt = 0; $taskAttempt -lt 40; $taskAttempt++) {
        if ($taskProcess.HasExited) { throw "$taskMode exited. See target/portal-logs/$taskMode-out.log" }
        try {
            $taskResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$taskPort/api/auth/session" -UseBasicParsing -TimeoutSec 2
            $taskSession = $taskResponse.Content | ConvertFrom-Json
            if ($taskResponse.StatusCode -eq 200 -and $taskSession.data.portalMode -eq $taskMode) { $taskReady = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (!$taskReady) { throw "$taskMode did not become ready. See target/portal-logs/$taskMode-out.log" }
    Write-Output "$taskMode ready: http://localhost:$taskPort/app (PID $($taskProcess.Id))"
}
