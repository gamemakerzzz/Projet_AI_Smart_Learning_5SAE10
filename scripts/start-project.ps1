param([string]$XamppPath = 'C:\xampp')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$logDirectory = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null

function Test-Port([int]$Port) {
    foreach ($hostName in @('127.0.0.1', '::1')) {
        $client = [Net.Sockets.TcpClient]::new()
        try {
            $task = $client.ConnectAsync($hostName, $Port)
            if ($task.Wait(500) -and $client.Connected) { return $true }
        } catch {} finally { $client.Dispose() }
    }
    return $false
}
function Wait-Port([int]$Port, [string]$Name) {
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) { Write-Host "$Name is listening on port $Port."; return }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not start. See logs in $logDirectory."
}
function Quote-Ps([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
function Start-ServiceProcess([string]$Name, [string]$Command) {
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($Command))
    $shellPath = (Get-Process -Id $PID).Path
    $process = Start-Process -FilePath $shellPath -ArgumentList '-NoProfile', '-NonInteractive', '-EncodedCommand', $encoded `
        -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDirectory "$Name.stdout.log") `
        -RedirectStandardError (Join-Path $logDirectory "$Name.stderr.log")
    Write-Host "Starting $Name (process $($process.Id))..."
}

if (-not (Test-Port 3306)) {
    $mysqlExecutable = Join-Path $XamppPath 'mysql\bin\mysqld.exe'
    $mysqlConfig = Join-Path $XamppPath 'mysql\bin\my.ini'
    if (-not (Test-Path $mysqlExecutable)) { throw 'Start your MySQL server first, or pass -XamppPath with the XAMPP folder.' }
    Start-ServiceProcess 'mysql' ("& " + (Quote-Ps $mysqlExecutable) + " " + (Quote-Ps "--defaults-file=$mysqlConfig") + " --standalone")
    Wait-Port 3306 'MySQL'
}
if (-not (Test-Port 8000)) {
    $pythonExecutable = (Get-Command python -ErrorAction Stop).Source
    & $pythonExecutable -c 'import uvicorn, fastapi, sklearn' 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Install Python dependencies: python -m pip install -r mla-service/requirements.txt' }
    Start-ServiceProcess 'ml' ("& " + (Quote-Ps $pythonExecutable) + " -m uvicorn main:app --host 127.0.0.1 --port 8000 --app-dir " + (Quote-Ps (Join-Path $projectRoot 'mla-service')))
    Wait-Port 8000 'Recommendation service'
}
if (-not (Test-Port 8081)) {
    $mavenExecutable = (Get-Command mvn.cmd -ErrorAction Stop).Source
    Start-ServiceProcess 'backend' ("& " + (Quote-Ps $mavenExecutable) + " -f " + (Quote-Ps (Join-Path $projectRoot 'backend\pom.xml')) + " spring-boot:run")
    Wait-Port 8081 'Backend'
}
if (-not (Test-Port 4200)) {
    $npmExecutable = (Get-Command npm.cmd -ErrorAction Stop).Source
    if (-not (Test-Path (Join-Path $projectRoot 'frontend\node_modules'))) { throw 'Install frontend dependencies: cd frontend; npm ci' }
    Start-ServiceProcess 'frontend' ("Set-Location " + (Quote-Ps (Join-Path $projectRoot 'frontend')) + "; & " + (Quote-Ps $npmExecutable) + " start -- --host localhost")
    Wait-Port 4200 'Frontend'
}
Invoke-RestMethod 'http://localhost:8000/' -TimeoutSec 5 | Out-Null
Invoke-RestMethod 'http://localhost:8081/api/auth/csrf' -TimeoutSec 5 | Out-Null
Write-Host 'SmartLearn: http://localhost:4200/#/login'
Write-Host "Logs: $logDirectory"
Write-Host 'Existing services were reused. For source changes, restart an already-running backend from its original terminal.'
