$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$testDatabase = 'jcash_web_test_' + [Guid]::NewGuid().ToString('N')
if ($testDatabase -notmatch '^jcash_web_test_[a-f0-9]{32}$') { throw 'Invalid test database name.' }
$previousUrl = $env:JCASH_DB_URL
$previousUser = $env:JCASH_DB_USER
$previousPassword = $env:JCASH_DB_PASSWORD
$created = $false
$granted = $false
try {
    $schema = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'database/schema.sql'))
    $schema = $schema.Replace('DROP DATABASE IF EXISTS jcash_db;', '').Replace('jcash_db', $testDatabase)
    $seed = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'database/seed.sql')).Replace('jcash_db', $testDatabase)
    $sql = "$schema`n$seed`nGRANT ALL ON $testDatabase.* TO 'jcash'@'%';"
    $created = $true
    $sql | docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the isolated MySQL test database.' }
    $granted = $true
    $env:JCASH_DB_URL = "jdbc:mysql://localhost:3306/$testDatabase"
    $env:JCASH_DB_USER = 'jcash'
    if (-not $env:JCASH_DB_PASSWORD) {
        foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot '.env')) {
            if ($line -match '^\s*JCASH_DB_PASSWORD\s*=(.*)$') { $env:JCASH_DB_PASSWORD = $Matches[1].Trim().Trim('"').Trim("'") }
        }
    }
    $maven = Get-Command mvn -ErrorAction SilentlyContinue
    if ($maven) { $executable = $maven.Source }
    else {
        $bundled = Get-ChildItem 'C:\Program Files\JetBrains' -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $bundled) { throw 'Maven not found.' }
        $executable = $bundled.FullName
    }
    & $executable -B -ntp compile dependency:build-classpath '-Dmdep.outputFile=target/web-classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Java build failed.' }
    Set-Location -LiteralPath (Join-Path $projectRoot 'frontend')
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Next.js build failed.' }
    & npm.cmd run test:e2e
    if ($LASTEXITCODE -ne 0) { throw 'Browser tests failed.' }
} finally {
    Set-Location -LiteralPath $projectRoot
    if ($created -and $testDatabase -match '^jcash_web_test_[a-f0-9]{32}$') {
        $cleanup = "DROP DATABASE IF EXISTS $testDatabase;"
        if ($granted) { $cleanup += " REVOKE ALL ON $testDatabase.* FROM 'jcash'@'%';" }
        $cleanup |
            docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'
    }
    $env:JCASH_DB_URL = $previousUrl
    $env:JCASH_DB_USER = $previousUser
    $env:JCASH_DB_PASSWORD = $previousPassword
}
