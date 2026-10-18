param([switch]$Voices, [string]$Language, [string]$TextFile, [string]$OutputFile)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$speech = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $installed = @($speech.GetInstalledVoices() | Where-Object { $_.Enabled } | ForEach-Object { $_.VoiceInfo })
    if ($Voices) {
        @($installed | ForEach-Object { $_.Culture.TwoLetterISOLanguageName } | Select-Object -Unique) | ConvertTo-Json -Compress
        exit 0
    }
    $voice = $installed | Where-Object { $_.Culture.TwoLetterISOLanguageName -eq $Language } | Select-Object -First 1
    if (-not $voice) { throw 'No installed voice for this language' }
    $speech.SelectVoice($voice.Name)
    $speech.Rate = -1
    $speech.SetOutputToWaveFile($OutputFile)
    $speech.Speak([System.IO.File]::ReadAllText($TextFile, [System.Text.Encoding]::UTF8))
} finally { $speech.Dispose() }
