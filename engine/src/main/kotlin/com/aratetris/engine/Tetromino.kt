package com.aratetris.engine

/** A cell offset within a piece's bounding box, y-up (matching the SRS literature). */
typealias Cell = Pair<Int, Int>

/**
 * Piece geometry. Spawn-orientation cells are stored once per piece; the other three orientations
 * are produced by rotating within the bounding box. Coordinates are y-up (origin at the box's
 * bottom-left), so the published SRS wall-kick tables can be used verbatim.
 */
object Shapes {

    // Spawn-state (orientation 0) local cells, y-up, inside a boxSize x boxSize box.
    private val spawn: Map<PieceType, List<Cell>> = mapOf(
        PieceType.I to listOf(0 to 2, 1 to 2, 2 to 2, 3 to 2),
        PieceType.O to listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
        PieceType.T to listOf(0 to 1, 1 to 1, 2 to 1, 1 to 2),
        PieceType.S to listOf(0 to 1, 1 to 1, 1 to 2, 2 to 2),
        PieceType.Z to listOf(0 to 2, 1 to 2, 1 to 1, 2 to 1),
        PieceType.J to listOf(0 to 2, 0 to 1, 1 to 1, 2 to 1),
        PieceType.L to listOf(2 to 2, 0 to 1, 1 to 1, 2 to 1),
    )

    /** Cells for orientation [rotation] (0..3), produced by rotating the spawn cells clockwise. */
    fun cells(type: PieceType, rotation: Int): List<Cell> {
        val n = type.boxSize
        var cs = spawn.getValue(type)
        val r = ((rotation % 4) + 4) % 4
        // Clockwise rotation within an n x n box (y-up): (x, y) -> (y, n - 1 - x).
        repeat(r) { cs = cs.map { (x, y) -> y to (n - 1 - x) } }
        return cs
    }
}

/**
 * SRS wall-kick tables. Each entry maps a (fromState, toState) transition to the ordered list of
 * (dx, dy) offsets to try; the first that fits is taken, otherwise the rotation fails. y-up.
 */
object Kicks {

    private val JLSTZ: Map<Pair<Int, Int>, List<Cell>> = mapOf(
        (0 to 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        (1 to 0) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        (1 to 2) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        (2 to 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        (2 to 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
        (3 to 2) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        (3 to 0) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        (0 to 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
    )

    private val I: Map<Pair<Int, Int>, List<Cell>> = mapOf(
        (0 to 1) to listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        (1 to 0) to listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        (1 to 2) to listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
        (2 to 1) to listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        (2 to 3) to listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        (3 to 2) to listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        (3 to 0) to listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        (0 to 3) to listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
    )

    /** Ordered kick offsets to attempt for a rotation of [type] from [from] to [to]. */
    fun forPiece(type: PieceType, from: Int, to: Int): List<Cell> {
        if (type == PieceType.O) return listOf(0 to 0)
        val table = if (type == PieceType.I) I else JLSTZ
        return table[from to to] ?: listOf(0 to 0)
    }
}
