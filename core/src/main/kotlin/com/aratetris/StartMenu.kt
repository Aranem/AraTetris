package com.aratetris

import com.aratetris.engine.GameConfig

/**
 * Pre-game options: the chosen starting level and whether the level progresses. Produces the
 * [GameConfig] handed to a fresh [com.aratetris.engine.TetrisEngine].
 */
class StartMenu {
    var startLevel: Int = 1
        private set
    var levelProgression: Boolean = true
        private set

    fun levelUp() { if (startLevel < MAX_LEVEL) startLevel++ }
    fun levelDown() { if (startLevel > MIN_LEVEL) startLevel-- }
    fun toggleProgression() { levelProgression = !levelProgression }

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
