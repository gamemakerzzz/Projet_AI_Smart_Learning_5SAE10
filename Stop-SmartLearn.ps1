$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot '.local/mergee-processes.json'
if (Test-Path $pidFile) {
    foreach ($entry in (Get-Content $pidFile | ConvertFrom-Json)) {
        $process = Get-Process -Id $entry.Id -ErrorAction SilentlyContinue
        if ($process -and $process.StartTime.ToUniversalTime().Ticks -eq $entry.StartTimeTicks) {
            Stop-Process -Id $entry.Id
            Write-Host "Stopped $($entry.Name)."
        }
    }
    Remove-Item -LiteralPath $pidFile
}
