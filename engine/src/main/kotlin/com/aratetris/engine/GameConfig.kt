package com.aratetris.engine

/**
 * All per-game options. An algorithm sets these to create controlled environments (fixed seed,
 * fixed level, gravity off for fast turn-based play); the human shell builds one from the start menu.
 */
data class GameConfig(
    /** Visible playfield width in cells. */
    val width: Int = 10,
    /** Visible playfield height in cells. */
    val height: Int = 20,
    /** RNG seed for the 7-bag. Fixing it makes (seed + action sequence) fully reproducible. */
    val seed: Long = System.nanoTime(),
    /**
     * When true, [TetrisEngine.update] applies real-time gravity (human / real-time bot).
     * When false (default for bots), the piece only falls via [TetrisEngine.tick], soft/hard drop,
     * or placements — i.e. the caller fully controls timing, deterministically.
     */
    val gravityEnabled: Boolean = false,
    /** Number of upcoming pieces exposed in [GameStateView.next]. */
    val previewCount: Int = 5,
    /** Whether the hold mechanic is active (one swap per piece). */
    val holdEnabled: Boolean = true,
    /** Level the game begins at; sets the initial gravity speed. */
    val startLevel: Int = 1,
    /** When false the level never increases — it stays fixed at [startLevel] for the whole game. */
    val levelProgression: Boolean = true,
    /** Guideline Perfect-Clear bonuses; off by default. */
    val perfectClearBonus: Boolean = false,
) {
    /** Buffer rows above the visible field where pieces spawn. */
    val bufferRows: Int get() = 4
    val totalHeight: Int get() = height + bufferRows
}
