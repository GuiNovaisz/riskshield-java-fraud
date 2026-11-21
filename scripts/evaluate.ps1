param([long]$Seed=20261121,[string]$Output='evaluation/results',[string]$Configuration='')
. "$PSScriptRoot/common.ps1"
Push-Location $ProjectRoot
try {
    & "$ProjectRoot/mvnw.cmd" -B -ntp compile dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt'
    Assert-NativeSuccess 'Evaluation build failed'
    $separator=[IO.Path]::PathSeparator
    $cp='target/classes'+$separator+(Get-Content 'target/classpath.txt' -Raw).Trim()
    $commit=(git rev-parse HEAD)
    $at=[DateTime]::UtcNow.ToString('o')
    $argsList=@("-Dapplication.commit=$commit","-Devaluation.at=$at",'-cp',$cp,'io.riskshield.evaluation.EvaluationMain',$Seed,$Output)
    if($Configuration) { $argsList+=$Configuration }
    & "$env:JAVA_HOME/bin/java" @argsList
    Assert-NativeSuccess 'Evaluation failed'
} finally { Pop-Location }
