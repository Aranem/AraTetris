<#
.SYNOPSIS
  Package AraTetris as a self-contained Windows app you can hand to someone else.

.DESCRIPTION
  Uses jpackage (JDK 14+) to bundle the desktop jar with a stripped-down JRE into a portable
  app-image folder, then zips it. The recipient unzips the folder and double-clicks AraTetris.exe;
  no Java install required on their machine.

  The Gradle build JDK (JBR via gradle.properties) doesn't ship jpackage, so we point at a real JDK
  separately. Defaults to Microsoft OpenJDK 25 if installed.

.PARAMETER NoBuild
  Skip building; package the jar already in dist\ (or lwjgl3\build\libs\).

.PARAMETER JpackageJdk
  Path to a JDK 14+ install whose bin\ contains jpackage.exe. Default: Microsoft OpenJDK 25.

.PARAMETER JavaHome
  JDK 17+ used to build the jar (passed through to build.ps1). Default: Android Studio's JBR.

.PARAMETER AppVersion
  Version stamped into the app image. Must look like 1, 1.0, or 1.0.0. Default: 1.0.0.

.PARAMETER NoZip
  Produce the app-image folder but skip zipping it.

.EXAMPLE
  .\scripts\package-windows.ps1            # build, package, zip -> dist\AraTetris-windows.zip
  .\scripts\package-windows.ps1 -NoBuild   # repackage the existing jar
#>
[CmdletBinding()]
param(
    [switch]$NoBuild,
    [string]$JpackageJdk = 'C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot',
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$AppVersion = '1.0.0',
    [switch]$NoZip
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $RepoRoot

$jpackage = Join-Path $JpackageJdk 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) {
    Write-Error "jpackage not found at '$jpackage'. Install a JDK 14+ (e.g. winget install Microsoft.OpenJDK.25) and pass -JpackageJdk if it lives elsewhere."
}

if (-not $NoBuild) {
    & "$PSScriptRoot\build.ps1" desktop -JavaHome $JavaHome
    if ($LASTEXITCODE -ne 0) { Write-Error "Build failed." }
}

$jarDist  = Join-Path $RepoRoot 'dist\AraTetris.jar'
$jarBuild = Join-Path $RepoRoot 'lwjgl3\build\libs\AraTetris.jar'
$jar = if (Test-Path $jarDist) { $jarDist } else { $jarBuild }
if (-not (Test-Path $jar)) {
    Write-Error "AraTetris.jar not found. Run without -NoBuild to build it first."
}

# jpackage's --input wants a directory of jars, not a single jar path. Stage just our fat jar
# (the dist\ folder can also contain the APK, which we don't want bundled).
$Dist = Join-Path $RepoRoot 'dist'
if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Path $Dist | Out-Null }
$stage = Join-Path $Dist '.jpackage-input'
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item -Path $jar -Destination (Join-Path $stage 'AraTetris.jar') -Force

# jpackage refuses to overwrite an existing app-image directory.
$appImageParent = $Dist
$appImageDir = Join-Path $appImageParent 'AraTetris'
if (Test-Path $appImageDir) { Remove-Item -Recurse -Force $appImageDir }

Write-Host "Packaging AraTetris with bundled JRE..." -ForegroundColor Cyan
& $jpackage `
    --type app-image `
    --name AraTetris `
    --app-version $AppVersion `
    --vendor 'AraTetris' `
    --input $stage `
    --main-jar 'AraTetris.jar' `
    --main-class 'com.aratetris.lwjgl3.Lwjgl3Launcher' `
    --dest $appImageParent
if ($LASTEXITCODE -ne 0) { Write-Error "jpackage failed (exit $LASTEXITCODE)." }

Remove-Item -Recurse -Force $stage

$exe = Join-Path $appImageDir 'AraTetris.exe'
if (-not (Test-Path $exe)) { Write-Error "jpackage finished but AraTetris.exe is missing at '$exe'." }

Write-Host "`nPackaged. App image:" -ForegroundColor Green
Write-Host "  Folder : $appImageDir"
Write-Host "  Run    : $exe"

if (-not $NoZip) {
    $zip = Join-Path $Dist 'AraTetris-windows.zip'
    if (Test-Path $zip) { Remove-Item -Force $zip }
    Write-Host "`nZipping for distribution..." -ForegroundColor Cyan
    Compress-Archive -Path $appImageDir -DestinationPath $zip
    $sizeMb = [math]::Round((Get-Item $zip).Length / 1MB, 1)
    Write-Host "  Zip    : $zip  ($sizeMb MB)" -ForegroundColor Green
    Write-Host "           share this; recipient unzips and double-clicks AraTetris\AraTetris.exe"
}
