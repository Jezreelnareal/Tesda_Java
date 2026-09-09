# Leave Maven switches in $args so PowerShell does not interpret -o, -f, or -D.
$MavenArgs = $args

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Initialize-Java.ps1')
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$propertiesPath = Join-Path $PSScriptRoot 'maven-wrapper.properties'
$distributionLine = Get-Content -LiteralPath $propertiesPath | Where-Object { $_ -like 'distributionUrl=*' } | Select-Object -First 1
if (-not $distributionLine) {
    throw 'distributionUrl is missing from maven-wrapper.properties'
}

$distributionUrl = $distributionLine.Substring('distributionUrl='.Length)
$archiveName = Split-Path -Leaf $distributionUrl
$distributionName = $archiveName -replace '-bin\.zip$', ''
$wrapperRoot = Join-Path $env:USERPROFILE '.m2\wrapper\dists\jcash'
$mavenHome = Join-Path $wrapperRoot $distributionName
$mavenCommand = Join-Path $mavenHome 'bin\mvn.cmd'

if (-not (Test-Path -LiteralPath $mavenCommand)) {
    New-Item -ItemType Directory -Force -Path $wrapperRoot | Out-Null
    $archivePath = Join-Path $wrapperRoot $archiveName
    Invoke-WebRequest -Uri $distributionUrl -OutFile $archivePath
    Expand-Archive -LiteralPath $archivePath -DestinationPath $wrapperRoot -Force
    Remove-Item -LiteralPath $archivePath
}

Push-Location $projectRoot
try {
    & $mavenCommand @MavenArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
