param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$BaselineSnapshot,
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$ImprovedSnapshot,
    [ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Campaign = (Get-Date -Format yyyyMMdd)
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
foreach ($round in 1..3) {
    $stages=@('baseline','pooled')
    if ($round % 2 -eq 0) { [array]::Reverse($stages) }
    foreach ($stage in $stages) {
        $snapshot=if ($stage -eq 'baseline') { $BaselineSnapshot } else { $ImprovedSnapshot }
        $name="$stage-round$round-$Campaign"
        $resultDir=Join-Path $projectRoot "docs/performance/web-results/$name"
        if (Test-Path (Join-Path $resultDir 'completed.txt')) {
            $metadata=Get-Content (Join-Path $resultDir 'environment.json') -Raw | ConvertFrom-Json
            if ($metadata.snapshot -ne $snapshot -or $metadata.warmupSeconds -ne 30 -or $metadata.measuredSeconds -ne 60 -or $metadata.repetitions -ne 1 -or @($metadata.clients).Count -ne 3 -or $metadata.poolSize -ne 5) { throw 'Existing result does not match the comparison protocol.' }
            Write-Output "Retaining completed round: $name"
            continue
        }
        $levels=if ($round % 2 -eq 0) { @(10,5,1) } else { @(1,5,10) }
        & (Join-Path $PSScriptRoot 'run-web-performance.ps1') -Snapshot $snapshot -RunName $name -Concurrency $levels -Repetitions 1
    }
}
Write-Output 'All 18 measured trials completed. Run summarize-web-performance.py to validate and compare the evidence.'
