param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$logs = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$jdk = Get-ChildItem "$logs/tools" -Directory -Filter 'jdk-*' | Where-Object { Test-Path (Join-Path $_.FullName 'bin/java.exe') } | Select-Object -First 1
if (-not $jdk) { throw 'Install Java 17 under .local/tools/jdk-* first. See MERGE-NOTES.md.' }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME/bin;$logs/tools/apache-maven-3.9.11/bin;$env:Path"
$python = Join-Path $projectRoot '.venv/Scripts/python.exe'
if (-not (Test-Path $python)) { throw 'Install the Python environment first. See MERGE-NOTES.md.' }
if ($Build -or -not (Test-Path 'backend/target/smartlearn-backend-0.0.1-SNAPSHOT.jar')) {
    & "$logs/tools/apache-maven-3.9.11/bin/mvn.cmd" '-Dmaven.repo.local=.local/m2' -f backend/pom.xml package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
}
function Port-Open([int]$Port) {
    foreach ($hostName in @('127.0.0.1', '::1')) {
        $client = [Net.Sockets.TcpClient]::new()
        try { $client.Connect($hostName, $Port); return $true } catch {} finally { $client.Dispose() }
    }
    return $false
}
$services = @(
    @{ Name='recommendations'; Port=8000; Exe=$python; Args=@('-m','uvicorn','main:app','--app-dir','mla-service','--host','127.0.0.1','--port','8000') },
    @{ Name='analysis'; Port=8001; Exe=$python; Args=@('-m','uvicorn','main:app','--app-dir','ia-service','--host','127.0.0.1','--port','8001') },
    @{ Name='backend'; Port=8081; Exe="$env:JAVA_HOME/bin/java.exe"; Args=@('-jar','backend/target/smartlearn-backend-0.0.1-SNAPSHOT.jar','--spring.profiles.active=local','--server.address=127.0.0.1') },
    @{ Name='frontend'; Port=4200; Exe=(Get-Command node.exe).Source; Args=@('frontend/node_modules/@angular/cli/bin/ng.js','serve','--host','localhost'); Directory="$projectRoot/frontend" }
)
$services[3].Args = @('node_modules/@angular/cli/bin/ng.js','serve','--host','localhost')
$owned = @{}
$pidFile = Join-Path $logs 'mergee-processes.json'
if (Test-Path $pidFile) {
    foreach ($entry in (Get-Content $pidFile | ConvertFrom-Json)) {
        $process = Get-Process -Id $entry.Id -ErrorAction SilentlyContinue
        if ($process -and $process.StartTime.ToUniversalTime().Ticks -eq $entry.StartTimeTicks) { $owned[$entry.Name] = $entry }
    }
}
foreach ($service in $services) {
    if ((Port-Open $service.Port) -and -not $owned.ContainsKey($service.Name)) {
        throw "Port $($service.Port) is occupied by another application. Stop that application before starting mergee."
    }
}
foreach ($service in $services) {
    if (Port-Open $service.Port) { continue }
    $directory = if ($service.Directory) { $service.Directory } else { $projectRoot }
    $process = Start-Process -FilePath $service.Exe -ArgumentList $service.Args -WorkingDirectory $directory -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logs/$($service.Name).stdout.log" -RedirectStandardError "$logs/$($service.Name).stderr.log"
    $owned[$service.Name] = [pscustomobject]@{ Name=$service.Name; Id=$process.Id; StartTimeTicks=$process.StartTime.ToUniversalTime().Ticks }
    @($owned.Values) | ConvertTo-Json | Set-Content $pidFile
    $deadline = (Get-Date).AddSeconds(90)
    while (-not (Port-Open $service.Port)) {
        if ($process.HasExited -or (Get-Date) -gt $deadline) { throw "$($service.Name) failed to start. See $logs/$($service.Name).stderr.log" }
        Start-Sleep -Milliseconds 500
    }
    Write-Host "$($service.Name) ready on port $($service.Port)"
}
Write-Host 'SmartLearn: http://localhost:4200/#/login'
Write-Host 'Local admin: admin@smartlearn.local / admin123 (override with SMARTLEARN_ADMIN_PASSWORD before first launch).'
