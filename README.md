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
> `powershell -ExecutionPolicy Bypass -File .\build.ps1`.

## Quick start (helper scripts)

Three PowerShell scripts wrap the build into a single step each, selecting a suitable JDK
automatically:

```powershell
.\build.ps1            # build everything: the Windows jar AND the Android APK
.\run-desktop.ps1      # build (if needed) and play on Windows
.\deploy-android.ps1   # build, install on a USB-connected phone, and launch it
```

Useful flags: `.\build.ps1 desktop` / `.\build.ps1 android` build just one target;
`.\deploy-android.ps1 -NoBuild` installs the existing APK without rebuilding. `deploy-android.ps1`
needs **USB debugging** enabled on the phone (accept the on-screen prompt), and retries once if the
USB link drops mid-install.

The rest of this section shows the underlying Gradle commands if you prefer them.

## Build & run on Windows

Run from source:

```powershell
.\gradlew.bat lwjgl3:run
```

Or build a standalone jar:

```powershell
.\gradlew.bat lwjgl3:dist
# -> lwjgl3\build\libs\AraTetris.jar   (run with .\run-desktop.ps1, or java -jar using a JDK 17+)
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
