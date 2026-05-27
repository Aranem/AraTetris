package com.aratetris.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for the pure data pieces: shapes/rotation geometry, the board, scoring, and the 7-bag. */
class DataTest {

    @Test
    fun spawnGeometryMatchesSrs() {
        assertEquals(setOf(0 to 1, 1 to 1, 2 to 1, 1 to 2), Shapes.cells(PieceType.T, 0).toSet())
        assertEquals(setOf(0 to 2, 1 to 2, 2 to 2, 3 to 2), Shapes.cells(PieceType.I, 0).toSet())
    }

    @Test
    fun clockwiseRotationMatchesSrs() {
        // T spawn -> R (point right): vertical bar in middle column + bump on right-middle.
        assertEquals(setOf(1 to 0, 1 to 1, 1 to 2, 2 to 1), Shapes.cells(PieceType.T, 1).toSet())
        // I spawn -> R: vertical bar in column 2.
        assertEquals(setOf(2 to 0, 2 to 1, 2 to 2, 2 to 3), Shapes.cells(PieceType.I, 1).toSet())
    }

    @Test
    fun fourRotationsReturnToStart() {
        for (type in PieceType.entries) {
            assertEquals(
                Shapes.cells(type, 0).toSet(),
                Shapes.cells(type, 4).toSet(),
                "piece $type should return to spawn after 4 rotations",
            )
        }
    }

    @Test
    fun boardClearsFullRowsAndShiftsDown() {
        val b = Board(10, 24)
        // Fill row 0 completely, and put a single block at (3, 1).
        for (x in 0 until 10) b.cells[b.index(x, 0)] = 1
        b.cells[b.index(3, 1)] = 2
        val cleared = b.clearFullRows()
        assertEquals(1, cleared)
        // The lone block shifts from y=1 down to y=0.
        assertEquals(2, b.cellAt(3, 0))
        assertEquals(0, b.cellAt(0, 0))
    }

    @Test
    fun boardClearsMultipleRows() {
        val b = Board(10, 24)
        for (y in 0 until 4) for (x in 0 until 10) b.cells[b.index(x, y)] = 1
        assertEquals(4, b.clearFullRows())
        assertTrue(b.isEmpty())
    }

    @Test
    fun guidelineScoringValues() {
        assertEquals(100, Scoring.linePoints(1))
        assertEquals(300, Scoring.linePoints(2))
        assertEquals(500, Scoring.linePoints(3))
        assertEquals(800, Scoring.linePoints(4))
        assertEquals(400, Scoring.tSpinFullPoints(0))
        assertEquals(800, Scoring.tSpinFullPoints(1))
        assertEquals(1200, Scoring.tSpinFullPoints(2))
        assertEquals(1600, Scoring.tSpinFullPoints(3))
        assertEquals(100, Scoring.tSpinMiniPoints(0))
        assertEquals(200, Scoring.tSpinMiniPoints(1))
    }

    @Test
    fun gravityCurveDecreasesWithLevel() {
        assertEquals(1.0, Scoring.gravitySecondsPerRow(1), 1e-9) // 0.8^0 = 1.0s per row at level 1
        assertTrue(Scoring.gravitySecondsPerRow(5) < Scoring.gravitySecondsPerRow(1))
        assertTrue(Scoring.gravitySecondsPerRow(10) < Scoring.gravitySecondsPerRow(5))
    }

    @Test
    fun sevenBagContainsEachPieceOnce() {
        val bag = Bag(Rng(12345))
        val first = bag.peek(7)
        assertEquals(7, first.toSet().size, "first 7 should be all distinct piece types")
        val fourteen = bag.peek(14)
        assertEquals(7, fourteen.take(7).toSet().size)
        assertEquals(7, fourteen.drop(7).toSet().size, "second bag should also contain each piece once")
    }
}
