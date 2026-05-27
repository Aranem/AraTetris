# Controls

## Windows (keyboard)

| Action | Keys |
|--------|------|
| Move left / right | `A` / `D` |
| Rotate left / right | `Left` / `Right`  (also `Q` / `E`) |
| Soft drop | `Down`  (also `S`) |
| Hard drop | `Up`  (also `Space` / `W`) |
| Hold | `C`  (also `Shift`) |
| Pause menu | `P`  (also `Esc`) |
| Restart | `R` |
| Toggle bot (watch the built-in agent play) | `B` |

**Start menu:** `Left` / `Right` change the starting level, `T` toggles level progression,
`V` toggles swipe controls, `Space` starts, `C` opens the controls page.

**Pause menu** (`P` / `Esc` while playing): Resume (`P`/`Esc`), Restart (`R`), Main Menu (`M`).

**Game over:** `Space` / `R` restart; `Del` opens the "reset high scores" confirmation.

## Android (touch)

Two modes, switchable from the **SWIPE: ON / OFF** button on the start menu (the choice is
remembered across launches).

### Swipe mode (default)

| Action | Gesture |
|--------|---------|
| Move left / right | Swipe left / right anywhere (one cell per finger-width, so longer swipes move further) |
| Soft drop | Swipe down, then hold — it keeps soft-dropping while your finger stays down; slide left/right to steer the piece as it falls |
| Hard drop | Flick up |
| Rotate left / right | Tap the left / right half of the screen |
| Hold | Tap the **HOLD** button (left) |
| Pause | Tap the **PAUSE** button (right) |

Each swipe commits to one axis as soon as it gets going, so a sideways move can't turn into a hard
drop and vice-versa — an upward flick doesn't have to be perfectly straight. The exception is soft
drop: while you hold a soft drop you can still slide left/right to steer (hard drop stays disabled
for that gesture). After a hard drop you must lift your finger before another gesture registers, so
a lingering touch won't act on the next piece. (This applies to swipe mode only; button mode and the
keyboard are unaffected.)

### Button mode

A 3×2 on-screen grid mirroring the keyboard, plus HOLD and PAUSE buttons:

```
[ ROT L ] [ DROP ] [ ROT R ]     (rotate-left / hard-drop / rotate-right)
[ LEFT  ] [ SOFT ] [ RIGHT ]     (move-left / soft-drop / move-right)
```

LEFT / RIGHT / SOFT repeat while held; the rest fire on tap.

On every screen, menus and the pause/game-over overlays are fully tappable.
