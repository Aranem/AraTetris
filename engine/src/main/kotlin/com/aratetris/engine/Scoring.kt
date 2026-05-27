package com.aratetris.engine

import kotlin.math.pow

/** Modern Tetris Guideline scoring values and the gravity speed curve. */
object Scoring {

    /** Seconds the active piece takes to fall one row at [level] (Guideline formula). */
    fun gravitySecondsPerRow(level: Int): Double {
        val l = level.coerceIn(1, 20)
        return (0.8 - (l - 1) * 0.007).pow((l - 1).toDouble())
    }

    /** Base points (before ×level) for a plain line clear. */
    fun linePoints(lines: Int): Int = when (lines) {
        1 -> 100
        2 -> 300
        3 -> 500
        4 -> 800
        else -> 0
    }

    fun tSpinFullPoints(lines: Int): Int = when (lines) {
        0 -> 400
        1 -> 800
        2 -> 1200
        else -> 1600
    }

    fun tSpinMiniPoints(lines: Int): Int = when (lines) {
        0 -> 100
        1 -> 200
        else -> 400
    }

    fun perfectClearPoints(lines: Int): Int = when (lines) {
        1 -> 800
        2 -> 1200
        3 -> 1800
        4 -> 2000
        else -> 0
    }

    const val SOFT_DROP_PER_CELL = 1
    const val HARD_DROP_PER_CELL = 2
    const val LOCK_DELAY_SECONDS = 0.5
    const val MAX_LOCK_RESETS = 15
}
