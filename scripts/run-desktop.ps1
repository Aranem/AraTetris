<#
.SYNOPSIS
  Build (if needed) and run the AraTetris desktop game on Windows.

.DESCRIPTION
  Launches the standalone jar with a JDK 17+ (default: Android Studio's bundled JBR). Builds the
  jar first if it's missing (or if -Rebuild is given).

.PARAMETER Rebuild
  Force a fresh build of the jar before running.

.PARAMETER JavaHome
  Path to a JDK 17+ to run with. Defaults to Android Studio's bundled JBR.

.EXAMPLE
  .\scripts\run-desktop.ps1            # run (building first only if needed)
  .\scripts\run-desktop.ps1 -Rebuild   # rebuild, then run
#>
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $RepoRoot

$java = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path $java)) {
    Write-Error "java.exe not found under '$JavaHome'. Install Android Studio, or pass -JavaHome <path to a JDK 17+>."
}

# Prefer the collected artifact in dist/; fall back to gradle's per-module output
# (so direct `gradlew lwjgl3:dist` invocations still work).
$jarDist  = Join-Path $RepoRoot 'dist\AraTetris.jar'
$jarBuild = Join-Path $RepoRoot 'lwjgl3\build\libs\AraTetris.jar'
if ($Rebuild -or -not ((Test-Path $jarDist) -or (Test-Path $jarBuild))) {
    & "$PSScriptRoot\build.ps1" desktop -JavaHome $JavaHome
    if ($LASTEXITCODE -ne 0) { Write-Error "Build failed." }
}
$jar = if (Test-Path $jarDist) { $jarDist } else { $jarBuild }

Write-Host "Launching AraTetris (close the window to return)..." -ForegroundColor Cyan
& $java -jar $jar
