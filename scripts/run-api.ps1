param([int]$Port = 8080)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

# Load only application settings; never print local credentials.
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*(JCASH_DB_PASSWORD|JCASH_DB_URL|JCASH_DB_USER)\s*=(.*)$') {
            $setting = $Matches[1]
            $value = $Matches[2].Trim().Trim('"').Trim("'")
            if (-not [Environment]::GetEnvironmentVariable($setting, 'Process')) {
                [Environment]::SetEnvironmentVariable($setting, $value, 'Process')
            }
        }
    }
}
if (-not $env:JCASH_DB_URL) { $env:JCASH_DB_URL = 'jdbc:mysql://localhost:3306/jcash_db' }
if (-not $env:JCASH_DB_USER) { $env:JCASH_DB_USER = 'jcash' }
$env:JCASH_API_PORT = "$Port"

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($maven) { $executable = $maven.Source }
else {
    $bundled = Get-ChildItem 'C:\Program Files\JetBrains' -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $bundled) { throw 'Maven not found. Install Maven or use the IntelliJ Maven tool window.' }
    $executable = $bundled.FullName
}
& $executable -B -ntp compile exec:java
if ($LASTEXITCODE -ne 0) { throw 'The Java API failed to start.' }
