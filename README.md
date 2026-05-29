# AraTetris

A cross-platform Tetris for **Windows** and **Android** from a single Kotlin / [libGDX](https://libgdx.com/)
codebase. Full SRS rotation, hold, ghost piece, and modern Guideline scoring. **Strictly offline** —
no network, accounts, ads, or telemetry.

📖 **Documentation:** [Controls](docs/CONTROLS.md) · [Architecture](docs/ARCHITECTURE.md) · [Development](docs/DEVELOPMENT.md)

## Requirements

- **JDK 17+** — the JDK bundled with **Android Studio** works well. The build is already pinned to it
  in `gradle.properties` (`org.gradle.java.home`); adjust that path if needed.
- **Android builds only:** the **Android SDK** (installed with Android Studio).
- No Gradle install needed — the included `gradlew` wrapper handles it.

> Commands below use Windows PowerShell. If scripts are blocked, run them as
> `powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1`.

## Quick start (helper scripts)

Three PowerShell scripts in `scripts/` wrap the build into a single step each, selecting a suitable
JDK automatically:

```powershell
.\scripts\build.ps1            # build everything: the Windows jar AND the Android APK
.\scripts\run-desktop.ps1      # build (if needed) and play on Windows
.\scripts\deploy-android.ps1   # build, install on a USB-connected phone, and launch it
```

Useful flags: `.\scripts\build.ps1 desktop` / `.\scripts\build.ps1 android` build just one target;
`.\scripts\deploy-android.ps1 -NoBuild` installs the existing APK without rebuilding.
`deploy-android.ps1` needs **USB debugging** enabled on the phone (accept the on-screen prompt), and
retries once if the USB link drops mid-install.

Final artifacts are collected into `dist/` for a single predictable location:
`dist\AraTetris.jar` and `dist\AraTetris-debug.apk`. (Gradle still writes its originals under each
subproject's `build/` — `dist/` is just a copy.)

To hand the game to someone who **doesn't have Java installed**, run:

```powershell
.\scripts\package-windows.ps1
```

This uses `jpackage` (from a real JDK 14+; defaults to Microsoft OpenJDK 25 at
`C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot` — pass `-JpackageJdk <path>` to point elsewhere)
to bundle the jar with a stripped-down JRE and produces `dist\AraTetris-windows.zip` (~60 MB). The
recipient unzips and double-clicks `AraTetris\AraTetris.exe`.

The rest of this section shows the underlying Gradle commands if you prefer them.

## Build & run on Windows

Run from source:

```powershell
.\gradlew.bat lwjgl3:run
```

Or build a standalone jar:

```powershell
.\gradlew.bat lwjgl3:dist
# -> lwjgl3\build\libs\AraTetris.jar   (the helper scripts also copy this to dist\AraTetris.jar;
#                                       run with .\scripts\run-desktop.ps1, or java -jar using a JDK 17+)
```

## Build & run on Android

Build the installable APK:

```powershell
.\gradlew.bat android:assembleDebug
# -> android\build\outputs\apk\debug\android-debug.apk
```

Install it on a phone — copy the APK across and tap it (allow "install from unknown sources"), or with
**USB debugging** enabled and the phone plugged in:

```powershell
.\gradlew.bat android:installDebug
```

You can also open the project in **Android Studio** and press **Run**.

---

See [docs/CONTROLS.md](docs/CONTROLS.md) for how to play, and [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)
for the module layout, tests, and the headless runner.
