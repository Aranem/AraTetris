package com.aratetris

import com.badlogic.gdx.Gdx
import com.aratetris.engine.GameConfig

/**
 * Pre-game options: starting level, level progression, and (Android) swipe vs button controls.
 * Produces the [GameConfig] handed to a fresh [com.aratetris.engine.TetrisEngine]. Construct after
 * libGDX is initialized (it reads/writes a persisted setting). The swipe preference is remembered
 * across launches via libGDX Preferences (local, offline).
 */
class StartMenu {
    private val prefs = Gdx.app.getPreferences("aratetris_settings")

    var startLevel: Int = 1
        private set
    var levelProgression: Boolean = true
        private set
    var swipeControls: Boolean = prefs.getBoolean("swipeControls", true)
        private set

    fun levelUp() { if (startLevel < MAX_LEVEL) startLevel++ }
    fun levelDown() { if (startLevel > MIN_LEVEL) startLevel-- }
    fun toggleProgression() { levelProgression = !levelProgression }
    fun toggleSwipe() {
        swipeControls = !swipeControls
        prefs.putBoolean("swipeControls", swipeControls)
        prefs.flush()
    }

    fun buildConfig(): GameConfig = GameConfig(
        width = Constants.BOARD_COLS,
        height = Constants.BOARD_ROWS,
        seed = System.nanoTime(),
        gravityEnabled = true,     // real-time falling for interactive play
        previewCount = 5,
        holdEnabled = true,
        startLevel = startLevel,
        levelProgression = levelProgression,
    )

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 20
    }
}
