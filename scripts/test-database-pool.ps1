param([Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Snapshot)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
. (Join-Path $PSScriptRoot 'Initialize-Environment.ps1')
$database='jcash_pool_test_'+[Guid]::NewGuid().ToString('N')
$previous=$env:JCASH_POOL_TEST_DB_URL
$snapshotDir=Join-Path $projectRoot "target/web-performance/snapshots/$Snapshot"
$output=Join-Path $snapshotDir 'pool-integration-tests.txt'
function Invoke-PoolSql([string]$Sql) {
    $Sql | docker compose -f compose.performance.yaml exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'
    if ($LASTEXITCODE -ne 0) { throw 'Isolated integration database command failed.' }
}
try {
    $schema=[IO.File]::ReadAllText((Join-Path $projectRoot 'database/schema.sql')).Replace('DROP DATABASE IF EXISTS jcash_db;','').Replace('jcash_db',$database)
    $seed=[IO.File]::ReadAllText((Join-Path $projectRoot 'database/seed.sql')).Replace('jcash_db',$database)
    Invoke-PoolSql "$schema`n$seed`nGRANT ALL ON $database.* TO 'jcash'@'%';" | Out-Null
    $env:JCASH_POOL_TEST_DB_URL="jdbc:mysql://localhost:3307/$database"
    $ErrorActionPreference='Continue'
    & (Join-Path $projectRoot 'mvnw.cmd') -B -ntp -f (Join-Path $snapshotDir 'pom.xml') '-Dtest=DatabasePoolTest' '-DfailIfNoTests=true' test *> $output
    $testExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    Get-Content $output -Tail 25
    if ($testExit -ne 0) { throw 'Database pool integration tests failed.' }
} finally {
    if ($database -match '^jcash_pool_test_[a-f0-9]{32}$') {
        Invoke-PoolSql "DROP DATABASE IF EXISTS $database; REVOKE ALL ON $database.* FROM 'jcash'@'%';" | Out-Null
    }
    $env:JCASH_POOL_TEST_DB_URL=$previous
}
