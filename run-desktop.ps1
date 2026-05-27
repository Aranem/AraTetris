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
  .\run-desktop.ps1            # run (building first only if needed)
  .\run-desktop.ps1 -Rebuild   # rebuild, then run
#>
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$java = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path $java)) {
    Write-Error "java.exe not found under '$JavaHome'. Install Android Studio, or pass -JavaHome <path to a JDK 17+>."
}

$jar = Join-Path $PSScriptRoot 'lwjgl3\build\libs\AraTetris.jar'
if ($Rebuild -or -not (Test-Path $jar)) {
    & "$PSScriptRoot\build.ps1" desktop -JavaHome $JavaHome
    if ($LASTEXITCODE -ne 0) { Write-Error "Build failed." }
}

Write-Host "Launching AraTetris (close the window to return)..." -ForegroundColor Cyan
& $java -jar $jar
