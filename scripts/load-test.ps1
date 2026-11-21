param([ValidateSet('steady','burst','concurrency','duplicate','velocity')][string]$Scenario='steady')
. "$PSScriptRoot/common.ps1"
Import-LocalEnvironment
Push-Location $ProjectRoot
try {
    New-Item -ItemType Directory -Force 'work/load' | Out-Null
    docker compose --profile load run --rm -e "SCENARIO=$Scenario" k6 run --summary-export "/results/$Scenario.json" /scripts/risk.js
    Assert-NativeSuccess 'Load checks failed'
} finally { Pop-Location }
