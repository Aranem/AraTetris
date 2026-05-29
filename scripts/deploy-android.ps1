<#
.SYNOPSIS
  Install AraTetris on a connected Android device and launch it.

.DESCRIPTION
  Builds the debug APK (unless -NoBuild), then installs it over USB with adb and starts the app.
  The USB link can drop mid-install, so a failed install is retried once after restarting the adb
  server. Requires USB debugging enabled on the phone and the on-device "Allow USB debugging" prompt
  accepted.

.PARAMETER NoBuild
  Skip building; install the APK that's already in android\build\outputs\apk\debug.

.PARAMETER NoLaunch
  Install only; don't start the app afterwards.

.PARAMETER JavaHome
  Path to a JDK 17+ to build with. Defaults to Android Studio's bundled JBR.

.EXAMPLE
  .\scripts\deploy-android.ps1            # build, install, launch
  .\scripts\deploy-android.ps1 -NoBuild   # install the existing APK and launch
#>
[CmdletBinding()]
param(
    [switch]$NoBuild,
    [switch]$NoLaunch,
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $RepoRoot

# Prefer the collected artifact in dist/; fall back to gradle's per-module output.
$apkDist  = Join-Path $RepoRoot 'dist\AraTetris-debug.apk'
$apkBuild = Join-Path $RepoRoot 'android\build\outputs\apk\debug\android-debug.apk'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$appId = 'com.aratetris'
$launcher = 'com.aratetris/com.aratetris.android.AndroidLauncher'

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at '$adb'. Install the Android SDK platform-tools (comes with Android Studio)."
}

if (-not $NoBuild) {
    & "$PSScriptRoot\build.ps1" android -JavaHome $JavaHome
    if ($LASTEXITCODE -ne 0) { Write-Error "Build failed." }
}
$apk = if (Test-Path $apkDist) { $apkDist } else { $apkBuild }
if (-not (Test-Path $apk)) {
    Write-Error "APK not found in dist\ or android\build\outputs\apk\debug\. Run without -NoBuild to build it first."
}

# Confirm an authorized device is attached before we try to install.
& $adb start-server | Out-Null
$attached = (& $adb devices) | Select-String '\tdevice$'
if (-not $attached) {
    Write-Host "No authorized device detected. Plug in the phone, enable USB debugging, and accept the on-screen prompt." -ForegroundColor Yellow
    & $adb devices
    exit 1
}

Write-Host "Installing $apk ..." -ForegroundColor Cyan
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed (the USB link can be flaky); restarting adb and retrying once..." -ForegroundColor Yellow
    & $adb kill-server | Out-Null
    & $adb start-server | Out-Null
    & $adb wait-for-device
    & $adb install -r $apk
}
if ($LASTEXITCODE -ne 0) { Write-Error "adb install failed (exit code $LASTEXITCODE). Check the cable/port and that the device stays authorized." }

Write-Host "Installed." -ForegroundColor Green
if (-not $NoLaunch) {
    & $adb shell am start -n $launcher | Out-Null
    Write-Host "Launched $appId on the device." -ForegroundColor Green
}
