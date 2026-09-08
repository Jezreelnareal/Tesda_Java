$ErrorActionPreference = 'Stop'

function Test-JCashJdk([string]$Directory) {
    if (-not $Directory -or -not (Test-Path -LiteralPath (Join-Path $Directory 'bin/javac.exe'))) { return $false }
    $releaseFile = Join-Path $Directory 'release'
    if (-not (Test-Path -LiteralPath $releaseFile)) { return $false }
    $version = Get-Content -LiteralPath $releaseFile | Where-Object { $_ -match '^JAVA_VERSION="26(?:\.|\")' }
    return [bool]$version
}

$jdkDirectory = $env:JAVA_HOME
if (-not (Test-JCashJdk $jdkDirectory)) {
    $jdkDirectory = $null
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidate = Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
        if (Test-JCashJdk $candidate) { $jdkDirectory = $candidate }
    }
    if (-not $jdkDirectory) {
        $roots = @((Join-Path $env:USERPROFILE '.jdks'), (Join-Path $env:ProgramFiles 'Eclipse Adoptium'), (Join-Path $env:ProgramFiles 'Java'))
        foreach ($root in $roots) {
            if (-not (Test-Path -LiteralPath $root)) { continue }
            foreach ($candidate in Get-ChildItem -LiteralPath $root -Directory | Sort-Object Name -Descending) {
                if (Test-JCashJdk $candidate.FullName) { $jdkDirectory = $candidate.FullName; break }
            }
            if ($jdkDirectory) { break }
        }
    }
}
if (-not $jdkDirectory) { throw 'JCash requires JDK 26. Install it and set JAVA_HOME to its directory.' }
$env:JAVA_HOME = $jdkDirectory
$env:PATH = (Join-Path $jdkDirectory 'bin') + [IO.Path]::PathSeparator + $env:PATH
