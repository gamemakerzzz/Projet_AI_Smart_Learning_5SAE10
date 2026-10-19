param([string]$Python = 'python')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtime = Join-Path $projectRoot '.local/voice-runtime'
$runtimePython = Join-Path $runtime 'Scripts/python.exe'
$models = Join-Path $projectRoot '.local/voices'
if (-not (Test-Path -LiteralPath $runtimePython)) {
    & $Python -m venv $runtime
    if ($LASTEXITCODE -ne 0) { throw 'Creating local voice runtime failed. Specify a working Python 3.12 executable with -Python.' }
}
& $runtimePython -m pip install 'piper-tts==1.8.0'
if ($LASTEXITCODE -ne 0) { throw 'Piper installation failed.' }
New-Item -ItemType Directory -Path $models -Force | Out-Null
& $runtimePython -m piper.download_voices --data-dir $models fr_FR-siwis-medium en_US-lessac-high ar_JO-kareem-medium
if ($LASTEXITCODE -ne 0) { throw 'Downloading free voice models failed.' }
$cards = @{
    'fr_FR-siwis-medium' = 'fr/fr_FR/siwis/medium'
    'en_US-lessac-high' = 'en/en_US/lessac/high'
    'ar_JO-kareem-medium' = 'ar/ar_JO/kareem/medium'
}
foreach ($model in $cards.Keys) {
    Invoke-WebRequest -Uri "https://huggingface.co/rhasspy/piper-voices/resolve/main/$($cards[$model])/MODEL_CARD" -OutFile (Join-Path $models "$model.MODEL_CARD")
}
Write-Output 'Free local Piper narration installed: French Siwis, English Lessac, Arabic Kareem. No API key is required.'
