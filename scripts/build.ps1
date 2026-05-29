<#
.SYNOPSIS
  Build AraTetris into runnable artifacts: a Windows jar and/or an Android APK.

.DESCRIPTION
  Wraps the Gradle wrapper to produce runnable artifacts in one step. Uses a JDK 17+
  (default: Android Studio's bundled JBR) so the build runs on a supported Java version.

.PARAMETER Target
  What to build: 'all' (default), 'desktop' (Windows jar only), or 'android' (APK only).

.PARAMETER JavaHome
  Path to a JDK 17+ to build with. Defaults to Android Studio's bundled JBR.

.EXAMPLE
  .\scripts\build.ps1            # build both the desktop jar and the Android APK
  .\scripts\build.ps1 desktop    # just the Windows jar
  .\scripts\build.ps1 android    # just the Android APK
#>
[CmdletBinding()]
param(
    [ValidateSet('all', 'desktop', 'android')]
    [string]$Target = 'all',

    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $RepoRoot

if (-not (Test-Path $JavaHome)) {
    Write-Error "JDK not found at '$JavaHome'. Install Android Studio, or pass -JavaHome <path to a JDK 17+>."
}
$env:JAVA_HOME = $JavaHome

$tasks = @(switch ($Target) {
    'desktop' { 'lwjgl3:dist' }
    'android' { 'android:assembleDebug' }
    'all'     { 'lwjgl3:dist'; 'android:assembleDebug' }
})

Write-Host "Building [$Target] with JAVA_HOME=$JavaHome" -ForegroundColor Cyan
& .\gradlew.bat @tasks
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed (gradle exit code $LASTEXITCODE)." }

# Collect final artifacts into dist/ so there's one predictable place to look.
$Dist = Join-Path $RepoRoot 'dist'
if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Path $Dist | Out-Null }

Write-Host "`nBuild succeeded. Artifacts:" -ForegroundColor Green
if ($Target -ne 'android') {
    $jarSrc = Join-Path $RepoRoot 'lwjgl3\build\libs\AraTetris.jar'
    $jarDst = Join-Path $Dist 'AraTetris.jar'
    if (Test-Path $jarSrc) {
        Copy-Item -Path $jarSrc -Destination $jarDst -Force
        Write-Host "  Windows jar : $jarDst"
        Write-Host "                run it with  .\scripts\run-desktop.ps1"
    }
}
if ($Target -ne 'desktop') {
    $apkSrc = Join-Path $RepoRoot 'android\build\outputs\apk\debug\android-debug.apk'
    $apkDst = Join-Path $Dist 'AraTetris-debug.apk'
    if (Test-Path $apkSrc) {
        Copy-Item -Path $apkSrc -Destination $apkDst -Force
        Write-Host "  Android APK : $apkDst"
        Write-Host "                install it with  .\scripts\deploy-android.ps1 -NoBuild"
    }
}
