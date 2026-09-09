param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Name,
    [ValidatePattern('^[a-f0-9]{7,40}$')][string]$BaselineRevision
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$snapshot = Join-Path $projectRoot "target\web-performance\snapshots\$Name"
if (Test-Path -LiteralPath $snapshot) { throw "Snapshot already exists: $Name. Use a new name to preserve evidence." }
New-Item -ItemType Directory -Path $snapshot -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'src') -Destination $snapshot -Recurse
if ($BaselineRevision) {
    # Restore only the compared connection implementation inside the disposable copy.
    # The HTTP workload and profiler remain identical to the current harness.
    $baselineSource = & git -C $projectRoot show "${BaselineRevision}:src/main/java/shared/util/DatabaseConnection.java"
    if ($LASTEXITCODE -ne 0) { throw 'Could not read the requested baseline revision.' }
    [IO.File]::WriteAllText((Join-Path $snapshot 'src/main/java/shared/util/DatabaseConnection.java'),($baselineSource -join "`n")+"`n",(New-Object Text.UTF8Encoding($false)))
}
[xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw
$namespace = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
$namespace.AddNamespace('m','http://maven.apache.org/POM/4.0.0')
# A disposable standalone build compiles the real root src tree even during local IDE/module edits.
foreach ($node in @($pom.SelectNodes('/m:project/m:packaging|/m:project/m:modules',$namespace))) {
    [void]$node.ParentNode.RemoveChild($node)
}
if ($BaselineRevision) {
    foreach ($node in @($pom.SelectNodes('/m:project/m:dependencies/m:dependency[m:groupId="com.zaxxer" and m:artifactId="HikariCP"]',$namespace))) {
        [void]$node.ParentNode.RemoveChild($node)
    }
}
$pom.Save((Join-Path $snapshot 'pom.xml'))
$manifest = Get-ChildItem -LiteralPath (Join-Path $snapshot 'src') -File -Recurse | Sort-Object FullName | ForEach-Object {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash,$_.FullName.Substring($snapshot.Length+1)
}
$manifest | Set-Content -LiteralPath (Join-Path $snapshot 'source-sha256.txt') -Encoding utf8
$ErrorActionPreference = 'Continue' # Windows PowerShell treats native stderr warnings as errors.
& (Join-Path $projectRoot 'mvnw.cmd') -B -ntp -f (Join-Path $snapshot 'pom.xml') test dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt' *> (Join-Path $snapshot 'build.txt')
$buildExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($buildExit -ne 0) { Get-Content (Join-Path $snapshot 'build.txt') -Tail 50; throw 'Snapshot compilation/tests failed.' }
Write-Output $snapshot
