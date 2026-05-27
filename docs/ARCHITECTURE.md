# Architecture

AraTetris is **engine-first**: all game logic lives in a pure-Kotlin module with no graphics
dependency, and the rendered game is a thin view over it. This keeps the logic testable, keeps the
app strictly offline, and lets an algorithm drive the exact same game a human plays.

## Modules

| Module | Depends on | What it is |
|--------|-----------|-----------|
| `engine` | — | Pure-Kotlin, deterministic game engine. **No libGDX, no Android.** The only module an algorithm needs. |
| `core` | `engine` | libGDX view + input shell (rendering, keyboard/touch, menus, high scores). |
| `lwjgl3` | `core` | Windows/desktop launcher. |
| `android` | `core` | Android launcher (portrait, no `INTERNET` permission). |
| `headless` | `engine` | Runs agents over the engine with no graphics — for algorithm development and batch evaluation. |

Because `engine` doesn't declare a libGDX dependency, any accidental graphics import there fails to
compile — the headless guarantee is enforced by the build, not by discipline.

## Strictly offline

- No runtime network of any kind. The Android manifest declares **no `INTERNET` permission**.
- No external assets: blocks are drawn with `ShapeRenderer` and text with the built-in font, so
  everything is compiled into the artifact.
- High scores are stored locally via libGDX `Preferences` (a small private file), never synced.
- Internet is needed **once at build time** to download Gradle/libGDX/Kotlin from Maven; after that,
  builds work offline.

## Gameplay

- Standard 10×20 board, 7 tetrominoes, ghost piece, 5-piece next preview.
- **Full SRS** rotation with the official wall-kick tables (enables T-spins).
- **Hold** piece (one swap per drop), **7-bag** randomizer.
- **Guideline scoring** (all values × level): single 100 / double 300 / triple 500 / Tetris 800;
  T-spins (incl. minis); Back-to-Back ×1.5; combo 50 × count × level; soft drop 1/cell, hard drop
  2/cell. Perfect-Clear bonuses are available behind a config flag (off by default).
- Configurable per game: **starting level** and **level progression on/off** (chosen on the start menu).

## Programmatic interface

A human and an algorithm are interchangeable drivers of the same `TetrisEngine`, through the same
`Action`s. The engine supports both **micro-actions** and **high-level placements**, and both
**turn-based** (default for bots) and **real-time** timing.

```kotlin
enum class Action { LEFT, RIGHT, ROTATE_CW, ROTATE_CCW, SOFT_DROP, HARD_DROP, HOLD, PAUSE, NONE }

val engine = TetrisEngine(GameConfig(seed = 42L, gravityEnabled = false))

// Micro-actions (human-equivalent; good for RL):
engine.applyAction(Action.LEFT)
engine.tick()                       // advance one gravity step (turn-based)
// engine.update(dt)                // real-time gravity instead

// High-level placements (good for heuristic / search bots):
for (p in engine.legalPlacements()) {
    val sim = engine.copy()         // independent deep copy for lookahead
    val result = sim.applyPlacement(p)
    // ...evaluate sim.state, result.linesCleared, result.scoreDelta...
}

val view = engine.state             // immutable snapshot: board, active/ghost, queue, score...
```

Key properties for algorithm work:

- **Deterministic:** a fixed `(seed + action sequence)` always reproduces the same game.
- **Clonable:** `engine.copy()` gives a side-effect-free copy for search/lookahead.
- **Reward signal:** each `applyAction`/`applyPlacement` returns a `StepResult`
  (lines cleared, score delta, lock, T-spin, perfect clear, game over).
- **Agent seam:** implement `TetrisAgent { selectAction(state): Action }`. The rendered game (bot
  toggle) and the `headless` runner both drive the engine through this same interface. A
  `RandomAgent` ships as a reference; `headless` also includes a small greedy placement agent.

See [DEVELOPMENT.md](DEVELOPMENT.md) for how to run the engine tests and the headless runner.
