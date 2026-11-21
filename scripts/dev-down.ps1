. "$PSScriptRoot/common.ps1"
Push-Location $ProjectRoot
try { docker compose down; Assert-NativeSuccess 'Compose shutdown failed' } finally { Pop-Location }
