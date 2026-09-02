param(
    [ValidateRange(1, 10000)]
    [int]$Iterations = 200
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot "target\classes"
$resultsDirectory = Join-Path $projectRoot "docs\performance\results"
$classpathFile = Join-Path $projectRoot "target\performance-classpath.txt"
$mavenRepository = Join-Path $projectRoot "target\maven-repository"

$mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -eq $mavenCommand) {
    $bundledMaven = Get-ChildItem "C:\Program Files\JetBrains" -Recurse `
        -Filter mvn.cmd -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $bundledMaven) {
        throw "Maven was not found. Install Maven or run this from IntelliJ's Maven tool window."
    }
    $mavenExecutable = $bundledMaven.FullName
} else {
    $mavenExecutable = $mavenCommand.Source
}

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
