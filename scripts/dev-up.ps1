param([switch]$Build)
. "$PSScriptRoot/common.ps1"
Push-Location $ProjectRoot
try {
    if(!(Test-Path -LiteralPath '.env')) {
        $lines=foreach($key in @('DB_PASSWORD','ENGINE_PASSWORD','ANALYST_PASSWORD','AUDITOR_PASSWORD','ADMIN_A_PASSWORD','ADMIN_B_PASSWORD','GRAFANA_PASSWORD')) {
            $bytes=New-Object byte[] 32
            [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
            $key+'='+[Convert]::ToHexString($bytes).ToLowerInvariant()
        }
        [IO.File]::WriteAllLines((Join-Path $ProjectRoot '.env'),$lines)
    }
    if($Build) { docker compose up -d --build } else { docker compose up -d }
    Assert-NativeSuccess 'Compose startup failed'
} finally { Pop-Location }
