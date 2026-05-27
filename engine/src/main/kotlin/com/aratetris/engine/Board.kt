package com.aratetris.engine

/**
 * The playfield grid (including the hidden buffer rows above the visible area). Holds occupancy
 * only — piece movement, scoring and flow live in [TetrisEngine]. Coordinates are y-up: y=0 is the
 * bottom row, y increases upward, x increases rightward.
 */
class Board(val width: Int, val totalHeight: Int) {

    /** Row-major occupancy; 0 = empty, otherwise a [PieceType.id]. */
    val cells = IntArray(width * totalHeight)

    fun index(x: Int, y: Int) = x + y * width

    fun cellAt(x: Int, y: Int): Int =
        if (x in 0 until width && y in 0 until totalHeight) cells[index(x, y)] else 0

    /** True if (x, y) cannot hold part of a piece: outside the walls/floor/ceiling or already filled. */
    fun blocked(x: Int, y: Int): Boolean {
        if (x < 0 || x >= width || y < 0 || y >= totalHeight) return true
        return cells[index(x, y)] != 0
    }

    /** For T-spin corner tests: walls and floor are solid, but open sky above the field is not. */
    fun solid(x: Int, y: Int): Boolean {
        if (x < 0 || x >= width || y < 0) return true
        if (y >= totalHeight) return false
        return cells[index(x, y)] != 0
    }

    fun collides(absCells: List<Cell>): Boolean = absCells.any { (x, y) -> blocked(x, y) }

    fun lock(absCells: List<Cell>, id: Int) {
        for ((x, y) in absCells) {
            if (x in 0 until width && y in 0 until totalHeight) cells[index(x, y)] = id
        }
    }

    /** Removes every full row, shifting the rows above down. Returns the number of rows cleared. */
    fun clearFullRows(): Int {
        var cleared = 0
        var writeY = 0
        for (readY in 0 until totalHeight) {
            val full = (0 until width).all { cells[index(it, readY)] != 0 }
            if (full) {
                cleared++
                continue
            }
            if (writeY != readY) {
                for (x in 0 until width) cells[index(x, writeY)] = cells[index(x, readY)]
            }
            writeY++
        }
        // Blank out the rows left at the top after shifting.
        for (y in writeY until totalHeight) {
            for (x in 0 until width) cells[index(x, y)] = 0
        }
        return cleared
    }

    /** True when the entire field is empty (used for Perfect Clear detection). */
    fun isEmpty(): Boolean = cells.all { it == 0 }

    fun clear() = cells.fill(0)

    fun copy(): Board {
        val b = Board(width, totalHeight)
        cells.copyInto(b.cells)
        return b
    }
}
