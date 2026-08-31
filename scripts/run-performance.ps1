param(
    [ValidateRange(1, 10000)]
    [int]$Iterations = 200,

    [string]$Driver = "C:\Users\jezre\Downloads\mysql-connector-j-26.7.0\mysql-connector-j-26.7.0.jar"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot "out"
$resultsDirectory = Join-Path $projectRoot "docs\performance\results"

if (-not (Test-Path -LiteralPath $Driver)) {
    throw "MySQL Connector/J was not found at: $Driver"
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $resultsDirectory | Out-Null

$sources = Get-ChildItem (
    Join-Path $projectRoot "src\main\java"
) -Recurse -Filter *.java

& javac -d $outputDirectory $sources.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed."
}

$classPath = "$outputDirectory;$Driver"
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
