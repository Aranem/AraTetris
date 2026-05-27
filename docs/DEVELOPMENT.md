# Development

## Toolchain

| Tool | Version |
|------|---------|
| Gradle | 8.14.5 (via the `gradlew` wrapper — no separate install) |
| Kotlin | 2.3.21 |
| libGDX | 1.14.1 |
| Android Gradle Plugin | 8.13.2 |
| JDK | 17+ (Android Studio's bundled JBR / JDK 21 recommended) |
| Android | compileSdk/targetSdk 36, minSdk 24 |

### JDK pinning

The Gradle daemon is pinned to a specific JDK in `gradle.properties`:

```
org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr
```

This makes the build use Android Studio's JDK regardless of whatever `java` is on your `PATH`.
Change this path if Android Studio is installed elsewhere, or point it at any JDK 17+. The Android
SDK location is read from `local.properties` (`sdk.dir=...`), which Android Studio creates for you;
it is git-ignored.

## Common commands

> Windows PowerShell shown; on macOS/Linux use `./gradlew`.

```powershell
.\gradlew.bat engine:test          # run the engine unit tests
.\gradlew.bat headless:run         # run agents over the engine, print stats (no graphics)
.\gradlew.bat headless:run --args="100"   # run 100 games

.\gradlew.bat lwjgl3:run           # run the desktop game from source
.\gradlew.bat lwjgl3:dist          # build lwjgl3\build\libs\AraTetris.jar

.\gradlew.bat android:assembleDebug   # build the debug APK
.\gradlew.bat android:installDebug    # install to a connected device (USB debugging on)
```

## Module layout

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full picture. In short:

- `engine` — pure Kotlin game logic (no libGDX). Unit-tested.
- `core` — libGDX view + input over the engine.
- `lwjgl3` / `android` — platform launchers.
- `headless` — drives agents over the engine with no graphics.

## Writing an algorithm

Depend on `:engine` only. Either implement `TetrisAgent` (sees `GameStateView`, returns an `Action`)
for micro-action play, or drive `TetrisEngine` directly with `legalPlacements()` / `applyPlacement()`
and `copy()` for search. `headless/src/main/kotlin/com/aratetris/headless/Main.kt` shows both,
including a small greedy placement agent you can use as a starting point.

For reproducible runs use a fixed `seed` and turn-based timing (`gravityEnabled = false`), which lets
your code fully control stepping and makes `(seed + actions)` deterministic.
