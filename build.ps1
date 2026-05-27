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
  .\build.ps1            # build both the desktop jar and the Android APK
  .\build.ps1 desktop    # just the Windows jar
  .\build.ps1 android    # just the Android APK
#>
[CmdletBinding()]
param(
    [ValidateSet('all', 'desktop', 'android')]
    [string]$Target = 'all',

    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path $JavaHome)) {
    Write-Error "JDK not found at '$JavaHome'. Install Android Studio, or pass -JavaHome <path to a JDK 17+>."
}
$env:JAVA_HOME = $JavaHome

$tasks = switch ($Target) {
    'desktop' { @('lwjgl3:dist') }
    'android' { @('android:assembleDebug') }
    'all'     { @('lwjgl3:dist', 'android:assembleDebug') }
}

Write-Host "Building [$Target] with JAVA_HOME=$JavaHome" -ForegroundColor Cyan
& .\gradlew.bat @tasks
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed (gradle exit code $LASTEXITCODE)." }

Write-Host "`nBuild succeeded. Artifacts:" -ForegroundColor Green
if ($Target -ne 'android') {
    $jar = Join-Path $PSScriptRoot 'lwjgl3\build\libs\AraTetris.jar'
    if (Test-Path $jar) {
        Write-Host "  Windows jar : $jar"
        Write-Host "                run it with  .\run-desktop.ps1"
    }
}
if ($Target -ne 'desktop') {
    $apk = Join-Path $PSScriptRoot 'android\build\outputs\apk\debug\android-debug.apk'
    if (Test-Path $apk) {
        Write-Host "  Android APK : $apk"
        Write-Host "                install it with  .\deploy-android.ps1 -NoBuild"
    }
}
