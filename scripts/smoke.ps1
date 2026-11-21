param([string]$BaseUrl='http://localhost:28080')
. "$PSScriptRoot/common.ps1"
Import-LocalEnvironment
$ready=$false
for($i=0;$i -lt 60;$i++) {
    try { $r=Invoke-RestMethod "$BaseUrl/actuator/health/readiness"; if($r.status -eq 'UP') { $ready=$true;break } } catch { }
    Start-Sleep -Seconds 1
}
if(!$ready) { throw 'RiskShield readiness failed' }
$basic=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('engine:'+$env:ENGINE_PASSWORD))
$headers=@{Authorization='Basic '+$basic}
$payload=@{transactionId=[Guid]::NewGuid().ToString();customerId='syn_smoke';accountId='syn_smoke';amountCents=1000;currency='BRL';merchantId='syn_smoke';deviceId='syn_smoke';country='BR';occurredAt=[DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ss.fffZ');channel='WEB'} | ConvertTo-Json
$first=Invoke-RestMethod "$BaseUrl/risk/evaluations" -Method Post -Headers $headers -ContentType application/json -Body $payload
$duplicate=Invoke-RestMethod "$BaseUrl/risk/evaluations" -Method Post -Headers $headers -ContentType application/json -Body $payload
if($first.decisionId -ne $duplicate.decisionId) { throw 'Idempotent smoke failed' }
Write-Output ('SMOKE PASS: '+$first.result.decision+'; duplicate returns same durable decision')
