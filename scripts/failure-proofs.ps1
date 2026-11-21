param([string]$BaseUrl='http://localhost:28080')
. "$PSScriptRoot/common.ps1"
Import-LocalEnvironment
Push-Location $ProjectRoot
try {
    $headers=@{Authorization='Basic '+[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('engine:'+$env:ENGINE_PASSWORD))}
    function New-ProofPayload {
        @{transactionId=[Guid]::NewGuid().ToString();customerId='syn_failure_proof';accountId='syn_failure';amountCents=1000;currency='BRL';merchantId='syn_failure';deviceId='syn_failure';country='BR';occurredAt=[DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ');channel='WEB'} | ConvertTo-Json
    }
    docker compose stop redis | Out-Null
    Assert-NativeSuccess 'Could not stop task-owned Redis'
    try {
        $r=Invoke-RestMethod "$BaseUrl/risk/evaluations" -Method Post -Headers $headers -ContentType application/json -Body (New-ProofPayload)
        if(!$r.result.degraded -or $r.result.decision -ne 'REVIEW' -or $r.features.degradedComponents -notcontains 'VELOCITY_STORE') { throw 'Redis failure contract violated' }
        $redisProof=@{decision=$r.result.decision;degraded=$r.result.degraded;liveness=(Invoke-RestMethod "$BaseUrl/actuator/health/liveness").status;readiness=(Invoke-RestMethod "$BaseUrl/actuator/health/readiness").status}
    } finally { docker compose start redis | Out-Null }
    docker compose stop postgres | Out-Null
    Assert-NativeSuccess 'Could not stop task-owned PostgreSQL'
    try {
        $r=Invoke-WebRequest "$BaseUrl/risk/evaluations" -Method Post -Headers $headers -ContentType application/json -Body (New-ProofPayload) -SkipHttpErrorCheck
        if($r.StatusCode -ne 503) { throw ('DB failure contract violated: '+$r.StatusCode) }
        $dbProof=@{status=$r.StatusCode;liveness=(Invoke-RestMethod "$BaseUrl/actuator/health/liveness").status}
    } finally { docker compose start postgres | Out-Null }
    New-Item -ItemType Directory -Force (Join-Path $ProjectRoot 'docs/artifacts') | Out-Null
    @{observedAt=[DateTime]::UtcNow.ToString('o');redis=$redisProof;postgres=$dbProof} | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $ProjectRoot 'docs/artifacts/failure-proofs.json')
    Write-Output 'LIVE REDIS/POSTGRES FAILURE PROOFS PASS'
} finally { Pop-Location }
