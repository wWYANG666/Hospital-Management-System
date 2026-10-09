$ErrorActionPreference = 'Stop'
$taskJar = Join-Path $PSScriptRoot 'target\YIYUAN-0.0.1-SNAPSHOT.jar'
foreach ($taskPort in @(8082, 8083)) {
    $taskListener = Get-NetTCPConnection -State Listen -LocalPort $taskPort -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$taskListener) { continue }
    $taskProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($taskListener.OwningProcess)"
    if (!$taskProcess -or !$taskProcess.CommandLine -or !$taskProcess.CommandLine.Contains($taskJar)) {
        throw "Port $taskPort belongs to another application; it was not stopped."
    }
    Stop-Process -Id $taskProcess.ProcessId -Force
    Write-Output "Hospital portal on $taskPort stopped."
}
