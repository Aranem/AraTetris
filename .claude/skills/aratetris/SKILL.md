---
name: aratetris
description: Everything about the AraTetris project — a cross-platform (Windows + Android) Tetris in Kotlin/libGDX with an engine-first design meant to also be played by an algorithm. Use when building, running, or debugging the game on either platform; installing on an Android device; modifying gameplay, rendering, controls (keyboard/touch/swipe), menus, or scoring; or developing/evaluating a playing algorithm against the engine.
---

# AraTetris

Cross-platform Tetris (Windows desktop + Android) from one Kotlin/[libGDX](https://libgdx.com/)
codebase. Built **engine-first**: all game logic is in a pure-Kotlin `engine` module with no graphics
dependency, so the same engine that the human plays can be driven programmatically by an algorithm.
Strictly offline. End goal of the project: write an algorithm that plays the game.

## Environment & toolchain

- The default `java` on PATH is **JDK 8** — too old to build. Use **Android Studio's JBR (JDK 21)**.
  The Gradle daemon is already pinned to it in `gradle.properties`
  (`org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr`). Also export it when invoking
  the wrapper: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.
- Android SDK at `%LOCALAPPDATA%\Android\Sdk` (platform 36, build-tools 36/37, license accepted).
  `local.properties` holds `sdk.dir` and is git-ignored.
- `gradle`/`kotlin` are NOT on PATH — use the `gradlew` wrapper (already committed).
- Versions: Gradle 8.14.5 · Kotlin 2.3.21 · libGDX 1.14.1 · AGP 8.13.2 · compileSdk/targetSdk 36 ·
  minSdk 24 · jvmTarget 17.

## Commands (Windows PowerShell; use `./gradlew` on macOS/Linux)

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat engine:test                  # engine unit tests (20 tests, no graphics)
.\gradlew.bat headless:run --args="50"     # run agents over the engine, print stats
.\gradlew.bat lwjgl3:run                    # run desktop game (blocks)
.\gradlew.bat lwjgl3:dist                   # -> lwjgl3\build\libs\AraTetris.jar
.\gradlew.bat android:assembleDebug         # -> android\build\outputs\apk\debug\android-debug.apk
.\gradlew.bat android:installDebug          # install to connected device
```

To launch the desktop app without blocking the session, run the jar detached:
`Start-Process javaw -ArgumentList '-jar','lwjgl3\build\libs\AraTetris.jar' -PassThru` (then
`Stop-Process` by PID). A process that stays alive a few seconds is a good UI smoke test.

### Android device flow

adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

1. `adb devices` — phone must show `device` (not `unauthorized`; if unauthorized, tap "Allow USB
   debugging" on the phone, optionally "Always allow").
2. `adb install -r android\build\outputs\apk\debug\android-debug.apk` (or `gradlew android:installDebug`).
3. Launch: `adb shell am start -n com.aratetris/com.aratetris.android.AndroidLauncher`.

Debug APK is unsigned; Play Protect may prompt on first install — normal.

## Modules

| Module | Depends on | Role |
|--------|-----------|------|
| `engine` | — | Pure-Kotlin deterministic game logic. **No libGDX/Android.** Algorithm depends on this alone. |
| `core` | `engine` | libGDX view + input adapter (rendering, keyboard/touch/swipe, menus, high scores). |
| `lwjgl3` | `core` | Windows/desktop launcher (`Lwjgl3Launcher`). |
| `android` | `core` | Android launcher (`AndroidLauncher`); portrait; **no INTERNET permission**. |
| `headless` | `engine` | Runs agents over the engine, no graphics; for algorithm dev/eval. |

`engine` not declaring libGDX means accidental graphics imports there fail to compile — the headless
guarantee is build-enforced.

## File map

Engine (`engine/src/main/kotlin/com/aratetris/engine/`):
- `TetrisEngine.kt` — the state machine. `applyAction`, `tick`, `update(dt)`, `legalPlacements`,
  `applyPlacement`, `movesFor`, `copy`, `reset`, `state`. Has `internal` test hooks (testFill/etc.).
- `Board.kt` (grid/collision/line clear, y-up), `Tetromino.kt` (SRS shapes via box rotation + kick
  tables), `Bag.kt` (seeded 7-bag), `Rng.kt` (SplitMix64, copyable), `Scoring.kt` (Guideline values +
  gravity curve), `GameConfig.kt`, `Action.kt`, `State.kt` (GameStateView/StepResult/Placement/
  PieceView/TSpin), `agent/TetrisAgent.kt`, `agent/RandomAgent.kt`.

UI (`core/src/main/kotlin/com/aratetris/`):
- `TetrisGame.kt` — `ApplicationAdapter`: screen state machine (MENU/CONTROLS/PLAYING/GAME_OVER),
  input handling (keyboard + touch buttons + swipe gestures + bot), owns the engine.
- `Renderer.kt` — draws board/pieces/HUD/buttons/overlays with `ShapeRenderer` + `BitmapFont`.
- `TouchControls.kt` — all on-screen rectangles (gameplay grid, hold/pause, menu/pause/gameover).
- `GestureControls.kt` — swipe/tap gesture detection.
- `StartMenu.kt` — start level, level-progression, swipe-on/off (persisted via Preferences).
- `HighScores.kt` — offline top-5 (libGDX Preferences) with reset. `Constants.kt` — layout/colors.

> A control change usually edits `TouchControls` (layout) + `Renderer` (drawing) + `TetrisGame`
> (input) together. The virtual canvas is 480×800 (portrait), board 10×20 at 30px cells.

## Gameplay

10×20 board, 7 pieces, ghost, 5-piece preview, **full SRS** rotation (official kick tables → T-spins),
**hold** (one swap/drop), **7-bag**. **Guideline scoring** (×level): 100/300/500/800 for 1–4 lines;
T-spins incl. minis; Back-to-Back ×1.5; combo 50×count×level; soft 1/cell, hard 2/cell; Perfect-Clear
behind a config flag (off). Per-game options: starting level, level progression on/off.

## Controls

- **Keyboard:** `A`/`D` move; `Left`/`Right` (or `Q`/`E`) rotate; `Down`/`S` soft; `Up`/`Space`/`W`
  hard; `C`/`Shift` hold; `P`/`Esc` pause menu; `R` restart; `B` bot toggle. Menu: `Left`/`Right`
  level, `T` progression, `V` swipe toggle, `Space` start, `C` controls page.
- **Android — swipe mode (default):** swipe L/R move, swipe down soft, flick up hard, tap left/right
  half to rotate; on-screen HOLD (left) and PAUSE (right). **Button mode:** 3×2 grid (rotate-L/hard/
  rotate-R; move-L/soft/move-R) + HOLD/PAUSE. Toggle on the menu (`SWIPE: ON/OFF`), persisted.

## Programmatic interface (for the algorithm)

```kotlin
val e = TetrisEngine(GameConfig(seed = 42L, gravityEnabled = false)) // turn-based, deterministic
e.applyAction(Action.LEFT); e.tick()                 // micro-actions / one gravity step
for (p in e.legalPlacements()) {                     // high-level placement search
    val sim = e.copy(); val r = sim.applyPlacement(p) // copy = side-effect-free lookahead
    // score sim.state (cells/heights/holes), r.linesCleared, r.scoreDelta ...
}
val view = e.state                                   // immutable snapshot
```

- Deterministic for fixed `(seed + actions)`; use `gravityEnabled=false` for reproducible bot runs.
- `StepResult` (lines, scoreDelta, lock, tSpin, perfectClear, gameOver) is a reward signal.
- Agent seam: `TetrisAgent { selectAction(state): Action }`. The window bot toggle and `headless`
  both drive through it. `headless/.../Main.kt` has a `RandomAgent` and a greedy placement agent
  (El-Tetris weights) as starting points.

## Conventions / guardrails

- Keep `engine` free of libGDX/Android. Put logic in `engine`, keep `core` a thin view.
- Strictly offline forever: no INTERNET permission, no network/analytics/ads, no downloaded assets.
- Preserve determinism and `copy()` independence; don't couple logic to wall-clock time.
- Verify with `engine:test` + `headless:run`, then build the jar/APK.

## Git workflow (observed)

Feature branch → push → PR → merge on GitHub → local `git checkout main && git fetch --prune &&
git merge --ff-only origin/main` → delete branch. Don't push directly to `main` (a guard blocks it).
Commit trailer: `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>`. The repo git identity is
`Aranem <ryan.kwandary@gmail.com>` (set locally).

More detail in `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT.md`, `docs/CONTROLS.md`, and `CLAUDE.md`.
