$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
function Assert-NativeSuccess([string] $Message) { if ($LASTEXITCODE -ne 0) { throw $Message } }
function Import-LocalEnvironment {
    $envPath=Join-Path $ProjectRoot '.env'
    if (!(Test-Path -LiteralPath $envPath)) { throw 'Run scripts/dev-up.ps1 first to generate local credentials.' }
    foreach($line in Get-Content -LiteralPath $envPath) {
        if($line -match '^([A-Z_]+)=(.+)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process') }
    }
}
