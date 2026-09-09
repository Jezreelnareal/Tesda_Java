param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Snapshot,
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$RunName,
    [ValidateSet(1,5,10)][int[]]$Concurrency = @(1,5,10),
    [ValidateRange(1,10)][int]$Repetitions = 3,
    [ValidateRange(1,120)][int]$WarmupSeconds = 30,
    [ValidateRange(1,300)][int]$MeasuredSeconds = 60,
    [ValidateRange(1,20)][int]$PoolSize = 5,
    [ValidateRange(128,2048)][int]$HeapMb = 512,
    [ValidateRange(1024,65535)][int]$Port = 8281
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
. (Join-Path $PSScriptRoot 'Initialize-Environment.ps1')
. (Join-Path $projectRoot '.mvn/wrapper/Initialize-Java.ps1')
$snapshotDir = Join-Path $projectRoot "target\web-performance\snapshots\$Snapshot"
$results = Join-Path $projectRoot "docs\performance\web-results\$RunName"
if (!(Test-Path (Join-Path $snapshotDir 'target/classes/performance/WebPerformanceBenchmark.class'))) { throw 'Build the named snapshot first.' }
foreach ($line in Get-Content (Join-Path $snapshotDir 'source-sha256.txt')) {
    if ($line -match '^([A-Fa-f0-9]{64})  (.+)$') {
        $expectedHash=$Matches[1]; $relativePath=$Matches[2]
        if ((Get-FileHash -LiteralPath (Join-Path $snapshotDir $relativePath) -Algorithm SHA256).Hash -ne $expectedHash) { throw 'Snapshot source changed after it was built.' }
    }
}
if (Test-Path -LiteralPath $results) { throw 'Results directory already exists; use a unique RunName.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw 'Test API port is already occupied.' }
New-Item -ItemType Directory -Force -Path $results | Out-Null
$classPath = (Join-Path $snapshotDir 'target/classes') + ';' + (Get-Content (Join-Path $snapshotDir 'target/classpath.txt') -Raw).Trim()
Copy-Item (Join-Path $snapshotDir 'source-sha256.txt'),(Join-Path $snapshotDir 'pom.xml'),(Join-Path $snapshotDir 'build.txt') -Destination $results
$testDatabase = 'jcash_perf_' + [Guid]::NewGuid().ToString('N')
$previousUrl = $env:JCASH_DB_URL
$previousUser = $env:JCASH_DB_USER
$previousPort = $env:JCASH_API_PORT
$previousPool = $env:JCASH_DB_POOL_SIZE
$created = $false
$api = $null
$driver = $null
function Invoke-TestSql([string]$Sql) {
    $Sql | docker compose -f compose.performance.yaml exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names'
    if ($LASTEXITCODE -ne 0) { throw 'Test database command failed.' }
}
function Start-TestJava([string[]]$JavaArguments,[string]$Directory,[string]$Label) {
    # ArgumentList is one Windows command line: quote paths explicitly. No shell interpolation.
    $argumentLine = ($JavaArguments | ForEach-Object { '"' + $_.Replace('"','\"') + '"' }) -join ' '
    $launched = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList $argumentLine -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $Directory "$Label-stdout.txt") -RedirectStandardError (Join-Path $Directory "$Label-stderr.txt")
    # Keep the native handle open: Windows PowerShell can otherwise lose ExitCode
    # after HasExited notices completion and reopens the process by PID.
    $null = $launched.Handle
    return $launched
}
try {
    $metadata = [ordered]@{
        startedUtc = [DateTime]::UtcNow.ToString('o'); snapshot = $Snapshot; runName = $RunName;
        gitCommit = (& git rev-parse HEAD); database = $testDatabase; clients = $Concurrency;
        repetitions = $Repetitions; warmupSeconds = $WarmupSeconds; measuredSeconds = $MeasuredSeconds;
        jvmFlags = @('-Xms256m',"-Xmx${HeapMb}m",'-XX:+UseG1GC'); poolSize = $PoolSize;
        endpoint = "http://127.0.0.1:$Port/api/user/dashboard"; workload = '100 customers, 100 cash-in records each; independent sessions; direct Java API';
        computer = (Get-CimInstance Win32_ComputerSystem | Select-Object NumberOfLogicalProcessors,TotalPhysicalMemory);
        cpu = (Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name);
        os = (Get-CimInstance Win32_OperatingSystem | Select-Object Caption,Version);
        java = (& java --version | ForEach-Object { "$_" });
        docker = (& docker version --format '{{.Server.Version}}');
        mysqlContainer = (& docker inspect jcash-performance-mysql --format '{{.Image}} memory={{.HostConfig.Memory}} nanocpus={{.HostConfig.NanoCpus}}');
        buildNote = 'Frozen copy of src; snapshot POM removes root aggregator packaging/modules only. Root IDE/module edits are preserved.'
    }
    $metadata | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $results 'environment.json') -Encoding utf8
    $schema = [IO.File]::ReadAllText((Join-Path $projectRoot 'database/schema.sql')).Replace('DROP DATABASE IF EXISTS jcash_db;','').Replace('jcash_db',$testDatabase)
    $created = $true
    Invoke-TestSql "$schema`nGRANT ALL ON $testDatabase.* TO 'jcash'@'%';" | Out-Null
    $env:JCASH_DB_URL = "jdbc:mysql://localhost:3307/$testDatabase"
    $env:JCASH_DB_USER = 'jcash'
    $env:JCASH_API_PORT = "$Port"
    $env:JCASH_DB_POOL_SIZE = "$PoolSize"
    & java -cp $classPath performance.WebPerformanceBenchmark seed > (Join-Path $results 'dataset-before.json')
    if ($LASTEXITCODE -ne 0) { throw 'Fixture seeding failed.' }
    Invoke-TestSql "SELECT VERSION(); SHOW VARIABLES WHERE Variable_name IN ('innodb_buffer_pool_size','max_connections','innodb_flush_log_at_trx_commit'); USE $testDatabase; EXPLAIN SELECT id,transaction_type,amount,details,transaction_date_time,sender_mobile_number,receiver_mobile_number,admin_username FROM transactions WHERE sender_mobile_number='09900000001' OR receiver_mobile_number='09900000001' ORDER BY transaction_date_time DESC,id DESC;" > (Join-Path $results 'mysql-environment-and-explain.txt')
    $jfc = Join-Path $results 'measurement.jfc'
    & jfr configure --input profile --output $jfc 'jdk.InitialEnvironmentVariable#enabled=false' 'jdk.InitialSystemProperty#enabled=false' 'jdk.JavaMonitorEnter#enabled=true' 'jdk.JavaMonitorEnter#threshold=1ms' 'jdk.SocketRead#enabled=true' 'jdk.SocketRead#threshold=1ms' 'jdk.SocketWrite#enabled=true' 'jdk.SocketWrite#threshold=1ms' > (Join-Path $results 'jfr-configuration.txt')
    if ($LASTEXITCODE -ne 0) { throw 'JFR configuration failed.' }
    foreach ($repeat in 1..$Repetitions) {
        # Reverse concurrency order on alternate repetitions to reduce ordering effects.
        $levels = @($Concurrency)
        if ($repeat % 2 -eq 0) { [array]::Reverse($levels) }
        foreach ($clients in $levels) {
            $trial = Join-Path $results "c$clients-r$repeat"
            New-Item -ItemType Directory -Path $trial | Out-Null
            $api = Start-TestJava @('-Xms256m',"-Xmx${HeapMb}m",'-XX:+UseG1GC','-cp',$classPath,'Main',"--server.port=$Port",'--logging.level.root=WARN') $trial 'backend'
            $ready=$false
            for ($attempt=0;$attempt -lt 60;$attempt++) {
                if ($api.HasExited) { throw 'Backend exited during startup.' }
                try { $health=Invoke-RestMethod "http://127.0.0.1:$Port/api/health" -TimeoutSec 2; if ($health.status -eq 'connected') {$ready=$true;break} } catch { }
                Start-Sleep -Milliseconds 500
            }
            if (!$ready) { throw 'Backend did not become ready.' }
            & jcmd $api.Id VM.flags > (Join-Path $trial 'jvm-flags.txt')
            $recording=Join-Path $trial 'backend.jfr'
            & jcmd $api.Id JFR.start name=issue11 "settings=$jfc" "filename=$recording" dumponexit=true > (Join-Path $trial 'jfr-start.txt')
            if ($LASTEXITCODE -ne 0) { throw 'Could not start profiling.' }
            $driver=Start-TestJava @('-Xms128m','-Xmx512m','-cp',$classPath,'performance.WebPerformanceBenchmark','run',"http://127.0.0.1:$Port","$clients","$WarmupSeconds","$MeasuredSeconds",$trial) $trial 'driver'
            Write-Output "Warm-up: $RunName / c$clients-r$repeat"
            while (!(Test-Path (Join-Path $trial 'warmup-complete.txt'))) {
                if ($driver.HasExited) { Get-Content (Join-Path $trial 'driver-stderr.txt'); throw 'Load generator failed during warm-up.' }
                Start-Sleep -Milliseconds 500
            }
            [IO.File]::WriteAllText((Join-Path $trial 'start-measurement'),'start')
            Write-Output "Measuring: $RunName / c$clients-r$repeat ($MeasuredSeconds seconds)"
            $samples = New-Object System.Collections.Generic.List[object]
            while (!$driver.HasExited) {
                $api.Refresh(); $driver.Refresh()
                $samples.Add([pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');backendCpuSeconds=$api.TotalProcessorTime.TotalSeconds;backendWorkingSet=$api.WorkingSet64;driverCpuSeconds=$driver.TotalProcessorTime.TotalSeconds;driverWorkingSet=$driver.WorkingSet64})
                & docker stats jcash-performance-mysql --no-stream --format '{{json .}}' | Add-Content (Join-Path $trial 'docker-stats.jsonl')
                if ($LASTEXITCODE -ne 0) { throw 'Container resource monitoring failed.' }
                Start-Sleep -Seconds 2
            }
            $driver.WaitForExit()
            if ($driver.ExitCode -ne 0) { throw 'Load generator exited unsuccessfully.' }
            $samples | Export-Csv (Join-Path $trial 'process-samples.csv') -NoTypeInformation
            & jcmd $api.Id JFR.stop name=issue11 "filename=$recording" > (Join-Path $trial 'jfr-stop.txt')
            if ($LASTEXITCODE -ne 0) { throw 'Could not save profiling recording.' }
            & jfr summary $recording > (Join-Path $trial 'jfr-summary.txt')
            & java -cp $classPath performance.JfrEvidence $recording (Join-Path $trial 'jfr-analysis.json') (Join-Path $trial 'result.json')
            if ($LASTEXITCODE -ne 0) { throw 'Could not analyze profiling recording.' }
            Stop-Process -Id $api.Id -ErrorAction SilentlyContinue; $api=$null
            if (!(Test-Path (Join-Path $trial 'result.json'))) { throw 'No measured result was produced.' }
            $result=Get-Content (Join-Path $trial 'result.json') -Raw | ConvertFrom-Json
            if ($result.errors -ne 0) { throw 'Measured errors require investigation.' }
            Write-Output ("Completed c{0}-r{1}: {2:N1} req/s, p95 {3:N2} ms, errors {4}" -f $clients,$repeat,$result.successfulRequestsPerSecond,$result.p95Ms,$result.errors)
            $driver=$null
        }
    }
    & java -cp $classPath performance.WebPerformanceBenchmark verify-data > (Join-Path $results 'dataset-after.json')
    if ($LASTEXITCODE -ne 0) { throw 'Post-test fixture verification failed.' }
    if ((Get-Content (Join-Path $results 'dataset-before.json') -Raw).Trim() -ne (Get-Content (Join-Path $results 'dataset-after.json') -Raw).Trim()) { throw 'Test data changed during read-only benchmark.' }
    [IO.File]::WriteAllText((Join-Path $results 'completed.txt'),[DateTime]::UtcNow.ToString('o'))
} finally {
    foreach ($process in @($driver,$api)) { if ($process -and !$process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue } }
    if ($created -and $testDatabase -match '^jcash_perf_[a-f0-9]{32}$') {
        Invoke-TestSql "DROP DATABASE IF EXISTS $testDatabase; REVOKE ALL ON $testDatabase.* FROM 'jcash'@'%';" | Out-Null
        [IO.File]::WriteAllText((Join-Path $results 'cleanup.txt'),"Removed only isolated database $testDatabase")
    }
    $env:JCASH_DB_URL=$previousUrl; $env:JCASH_DB_USER=$previousUser; $env:JCASH_API_PORT=$previousPort; $env:JCASH_DB_POOL_SIZE=$previousPool
}
