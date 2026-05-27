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

> Commands below use Windows PowerShell (`.\gradlew.bat`). On macOS/Linux use `./gradlew`.

## Build & run on Windows

Run from source:

```powershell
.\gradlew.bat lwjgl3:run
```

Or build a standalone, double-clickable jar:

```powershell
.\gradlew.bat lwjgl3:dist
java -jar lwjgl3\build\libs\AraTetris.jar
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
