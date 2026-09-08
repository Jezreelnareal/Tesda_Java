# Load the backend's saved settings without printing credentials.
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*(JCASH_DB_PASSWORD|JCASH_DB_URL|JCASH_DB_USER|JCASH_API_PORT)\s*=(.*)$') {
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
if (-not $env:JCASH_API_PORT) { $env:JCASH_API_PORT = '8081' }
