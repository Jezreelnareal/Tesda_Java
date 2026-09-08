param(
    [ValidateRange(1, 10000)]
    [int]$Iterations = 200
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot 'Initialize-Environment.ps1')
$outputDirectory = Join-Path $projectRoot "target\classes"
$resultsDirectory = Join-Path $projectRoot "docs\performance\results"
$classpathFile = Join-Path $projectRoot "target\performance-classpath.txt"
$mavenRepository = Join-Path $projectRoot "target\maven-repository"

. (Join-Path $projectRoot '.mvn/wrapper/Initialize-Java.ps1')
$mavenExecutable = Join-Path $projectRoot 'mvnw.cmd'

New-Item -ItemType Directory -Force -Path $resultsDirectory | Out-Null
& $mavenExecutable "-Dmaven.repo.local=$mavenRepository" -q -DskipTests `
    compile dependency:build-classpath `
    "-Dmdep.outputFile=$classpathFile"
if ($LASTEXITCODE -ne 0) {
    throw "Maven compilation or dependency resolution failed."
}

$dependencies = Get-Content -LiteralPath $classpathFile -Raw
$classPath = "$outputDirectory;$dependencies"
foreach ($mode in @("unpooled", "reused")) {
    $recording = Join-Path $resultsDirectory "$mode.jfr"
    $benchmarkLog = Join-Path $resultsDirectory "$mode-benchmark.txt"
    $summaryLog = Join-Path $resultsDirectory "$mode-jfr-summary.txt"

    $recordingOption = "-XX:StartFlightRecording=filename=$recording,settings=profile,dumponexit=true,jdk.SocketRead#enabled=true,jdk.SocketRead#threshold=0ms,jdk.SocketWrite#enabled=true,jdk.SocketWrite#threshold=0ms"
    $temporaryDirectoryOption = "-Djava.io.tmpdir=$resultsDirectory"
    & java -Xms64m -Xmx64m -XX:+UseG1GC $temporaryDirectoryOption `
        $recordingOption `
        -cp $classPath performance.PerformanceBenchmark $mode $Iterations |
        Tee-Object -FilePath $benchmarkLog

    if ($LASTEXITCODE -ne 0) {
        throw "The $mode benchmark failed."
    }

    & jfr summary $recording | Out-File -FilePath $summaryLog -Encoding utf8
    if ($LASTEXITCODE -ne 0) {
        throw "Could not summarize the $mode JFR recording."
    }
}

Write-Output "Performance evidence saved in $resultsDirectory"
