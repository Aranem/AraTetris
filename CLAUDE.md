# CLAUDE.md

Guidance for working in this repo. AraTetris is a cross-platform Tetris (Windows + Android) in
Kotlin/libGDX, built **engine-first** so an algorithm can play it. See `docs/` for full details.

## Build & run (IMPORTANT: JDK)

The default `java` on PATH here is **JDK 8**, which is too old for the build. The Gradle daemon is
pinned to **Android Studio's JBR (JDK 21)** via `org.gradle.java.home` in `gradle.properties`. Always
run the wrapper with that JDK as `JAVA_HOME` too, e.g. in PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat <task>
```

Common tasks:

```powershell
.\gradlew.bat engine:test          # engine unit tests (no graphics)
.\gradlew.bat headless:run         # run agents over the engine, print stats
.\gradlew.bat lwjgl3:run           # run the desktop game
.\gradlew.bat lwjgl3:dist          # -> lwjgl3\build\libs\AraTetris.jar
.\gradlew.bat android:assembleDebug   # -> android\build\outputs\apk\debug\android-debug.apk
.\gradlew.bat android:installDebug    # install to a connected device
```

adb lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. Launch on device:
`adb shell am start -n com.aratetris/com.aratetris.android.AndroidLauncher`.

To launch the desktop app without blocking the session, run the jar detached (e.g. PowerShell
`Start-Process javaw -ArgumentList '-jar','lwjgl3\build\libs\AraTetris.jar'`); `lwjgl3:run` blocks.

## Architecture & conventions

Five modules: **`engine`** (pure Kotlin, the game logic — NO libGDX/Android), **`core`** (libGDX view
+ input over the engine), **`lwjgl3`** / **`android`** (launchers), **`headless`** (runs agents, no
graphics).

- **Keep `engine` graphics-free.** Game logic goes in `engine`; `core` is only a view + input adapter.
  An algorithm depends on `:engine` alone.
- **Strictly offline.** Never add an `INTERNET` permission or any network/analytics/ads. No external
  assets — draw with `ShapeRenderer` + the built-in `BitmapFont`.
- **Determinism matters.** The engine must stay reproducible for a fixed `(seed + actions)`; preserve
  `copy()` independence. Don't tie game logic to wall-clock time (use `tick()` / `update(dt)`).
- Correctness targets: full SRS rotation (official kick tables), modern Guideline scoring.

## Where things live

- Engine: `engine/src/main/kotlin/com/aratetris/engine/` (`TetrisEngine`, `Board`, `Tetromino`,
  `Bag`, `Scoring`, `Action`, `GameConfig`, `State`, `agent/`).
- UI: `core/src/main/kotlin/com/aratetris/` — `TetrisGame` (loop/screens/input), `Renderer`,
  `TouchControls` (button/gesture layout), `GestureControls`, `StartMenu`, `HighScores`, `Constants`.
- Touch/keyboard changes usually touch `TouchControls` (rects/layout) + `Renderer` (drawing) +
  `TetrisGame` (input handling) together.

## Verify changes

Run `engine:test` and `headless:run`; build `lwjgl3:dist` and/or `android:assembleDebug`. For the
desktop UI, the jar launching and staying alive a few seconds is a good smoke test.

## Git workflow (observed)

Work on a feature branch, push it, open a PR, merge on GitHub, then fast-forward local `main`
(`git merge --ff-only origin/main`) and delete the branch.

## Versions

Gradle 8.14.5 · Kotlin 2.3.21 · libGDX 1.14.1 · AGP 8.13.2 · compileSdk/targetSdk 36 · minSdk 24.
