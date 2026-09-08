param([ValidateRange(1, 65535)][int]$Port)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Initialize-Environment.ps1')
Set-Location -LiteralPath $projectRoot
if ($PSBoundParameters.ContainsKey('Port')) { $env:JCASH_API_PORT = "$Port" }

& (Join-Path $projectRoot 'mvnw.cmd') -B -ntp spring-boot:run
if ($LASTEXITCODE -ne 0) { throw 'The Java API failed to start.' }
