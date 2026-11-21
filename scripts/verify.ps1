. "$PSScriptRoot/common.ps1"
Push-Location $ProjectRoot
try { & "$ProjectRoot/mvnw.cmd" -B -ntp clean verify; Assert-NativeSuccess 'Maven verification failed' } finally { Pop-Location }
