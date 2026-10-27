# Builds a native Windows app-image with a bundled Java 21 runtime.
# Run from PowerShell on Windows; jpackage app-images are platform-native.
[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenBin = "mvn",
    [string]$OutputDirectory = "portable-out",
    [string]$AppName = "KylGisPOS"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
if ([string]::IsNullOrWhiteSpace($JavaHome)) { throw "Configure JAVA_HOME with JDK 21." }
$Java = Join-Path $JavaHome "bin\java.exe"
$JLink = Join-Path $JavaHome "bin\jlink.exe"
$JPackage = Join-Path $JavaHome "bin\jpackage.exe"
if (!(Test-Path $Java) -or !(Test-Path $JLink) -or !(Test-Path $JPackage)) {
    throw "JAVA_HOME must point to a complete JDK 21 with jlink and jpackage."
}
$Version = (& $Java -version 2>&1 | Select-Object -First 1)
if ($Version -notmatch 'version "21([."]|-)') { throw "JDK 21 required; detected $Version" }
$Dirty = (git -c safe.directory=$Root status --porcelain)
if ($LASTEXITCODE -ne 0 -or $Dirty) { throw "Worktree must be clean for a reproducible package." }
& $MavenBin "-DskipTests" "clean" "package"
if ($LASTEXITCODE -ne 0) { throw "Maven package failed." }
$Jar = Get-ChildItem -Path (Join-Path $Root "target") -Filter "*.jar" -File | Sort-Object Length -Descending | Select-Object -First 1
if ($null -eq $Jar) { throw "Main JAR not found in target." }
$Out = Join-Path $Root $OutputDirectory
if ([IO.Path]::GetFullPath($Out) -eq [IO.Path]::GetPathRoot([IO.Path]::GetFullPath($Out))) { throw "OutputDirectory cannot be a filesystem root." }
if (Test-Path $Out) { Remove-Item -LiteralPath $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null
$Runtime = Join-Path $Out "runtime"
$JLinkArgs = @("--add-modules", "java.base,java.desktop,java.logging,java.sql,java.naming,java.management,java.net.http,jdk.crypto.ec", "--output", $Runtime, "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2")
& $JLink @JLinkArgs
if ($LASTEXITCODE -ne 0) { throw "jlink failed." }
$JPackageArgs = @("--type", "app-image", "--name", $AppName, "--app-version", "1.3", "--input", $Jar.DirectoryName, "--main-jar", $Jar.Name, "--dest", $Out, "--runtime-image", $Runtime, "--java-options", "-Dfile.encoding=UTF-8", "--java-options", "--enable-native-access=ALL-UNNAMED")
& $JPackage @JPackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }
$AppDir = Join-Path $Out $AppName
$Manifest = Join-Path $Out "$AppName.sha256"
Get-ChildItem -LiteralPath $AppDir -File -Recurse | Sort-Object FullName | ForEach-Object {
    $Hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $Relative = $_.FullName.Substring($Out.Length).TrimStart('\\','/')
    "$Hash  $Relative"
} | Set-Content -LiteralPath $Manifest -Encoding utf8
Write-Output "PORTABLE_PACKAGE=$AppDir"
Write-Output ("PORTABLE_SHA256=" + (Get-FileHash -LiteralPath $Manifest -Algorithm SHA256).Hash.ToLowerInvariant())