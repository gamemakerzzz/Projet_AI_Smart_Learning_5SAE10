param([switch]$Build)
$ErrorActionPreference = 'Stop'
& (Join-Path (Split-Path -Parent $PSScriptRoot) 'Start-SmartLearn.ps1') -Build:$Build
