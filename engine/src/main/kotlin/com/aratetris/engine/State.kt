package com.aratetris.engine

/** An absolute board cell occupied by a piece (y-up board coordinates). */
data class CellPos(val x: Int, val y: Int)

/** A snapshot of a piece on the board (absolute coordinates; cells may sit above the visible top). */
data class PieceView(
    val type: PieceType,
    val rotation: Int,
    val cells: List<CellPos>,
)

/** Classification of a lock for scoring. */
enum class TSpin { NONE, MINI, FULL }

/**
 * Immutable read-only view of the whole game, returned by [TetrisEngine.state]. This is what a
 * renderer draws and what a [com.aratetris.engine.agent.TetrisAgent] reads to choose its move.
 */
data class GameStateView(
    val width: Int,
    val height: Int,
    /** Visible occupancy, row-major, size width*height; 0 = empty else [PieceType.id]. */
    val cells: IntArray,
    val active: PieceView?,
    /** Where [active] would land if hard-dropped (for the ghost piece and bot heuristics). */
    val ghost: PieceView?,
    val next: List<PieceType>,
    val hold: PieceType?,
    val canHold: Boolean,
    val score: Int,
    val level: Int,
    val lines: Int,
    val combo: Int,
    /** True once a difficult clear (Tetris / T-spin-with-lines) has primed Back-to-Back. */
    val backToBack: Boolean,
    /** Consecutive difficult clears: 0 none, 1 primed, >=2 means the x1.5 bonus is being earned. */
    val backToBackStreak: Int,
    val gameOver: Boolean,
    val paused: Boolean,
) {
    fun cellAt(x: Int, y: Int): Int =
        if (x in 0 until width && y in 0 until height) cells[x + y * width] else 0

    // Identity-based equals/hashCode are fine here; this is a transient snapshot, not a key.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * What a single [TetrisEngine.applyAction] / [TetrisEngine.tick] / placement produced. Doubles as a
 * reward signal for a learning agent.
 */
data class StepResult(
    val moved: Boolean,
    val rotated: Boolean,
    val pieceLocked: Boolean,
    val linesCleared: Int,
    val tSpin: TSpin,
    val perfectClear: Boolean,
    val scoreDelta: Int,
    val gameOver: Boolean,
) {
    companion object {
        val NOTHING = StepResult(
            moved = false, rotated = false, pieceLocked = false, linesCleared = 0,
            tSpin = TSpin.NONE, perfectClear = false, scoreDelta = 0, gameOver = false,
        )
    }
}

/**
 * A high-level target for the current piece: its [rotation] (0..3) and the [x] of its bounding-box
 * origin. Produced by [TetrisEngine.legalPlacements] and consumed by [TetrisEngine.applyPlacement] /
 * [TetrisEngine.movesFor]. Convenient for heuristic / search bots.
 */
data class Placement(val rotation: Int, val x: Int)
