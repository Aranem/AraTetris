package com.aratetris.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TetrisEngineTest {

    private val scriptedActions = listOf(
        Action.LEFT, Action.ROTATE_CW, Action.RIGHT, Action.SOFT_DROP,
        Action.ROTATE_CCW, Action.HARD_DROP, Action.LEFT, Action.HARD_DROP,
        Action.HOLD, Action.RIGHT, Action.HARD_DROP, Action.ROTATE_CW, Action.HARD_DROP,
    )

    @Test
    fun sameSeedAndActionsAreReproducible() {
        val a = TetrisEngine(GameConfig(seed = 42L))
        val b = TetrisEngine(GameConfig(seed = 42L))
        repeat(50) {
            for (action in scriptedActions) {
                a.applyAction(action)
                b.applyAction(action)
            }
        }
        assertEquals(a.score, b.score)
        assertEquals(a.lines, b.lines)
        assertEquals(a.isGameOver, b.isGameOver)
        assertTrue(a.state.cells.contentEquals(b.state.cells), "boards must match")
    }

    @Test
    fun differentSeedsDiverge() {
        val a = TetrisEngine(GameConfig(seed = 1L))
        val b = TetrisEngine(GameConfig(seed = 2L))
        // The first pieces from different bags should differ for these seeds.
        assertNotEquals(a.state.next, b.state.next)
    }

    @Test
    fun copyIsIndependent() {
        val original = TetrisEngine(GameConfig(seed = 7L))
        repeat(20) { original.applyAction(scriptedActions[it % scriptedActions.size]) }

        val snapshotScore = original.score
        val snapshotCells = original.state.cells.copyOf()

        val clone = original.copy()
        repeat(100) { clone.applyAction(Action.HARD_DROP) }

        // Mutating the clone must not touch the original.
        assertEquals(snapshotScore, original.score)
        assertTrue(original.state.cells.contentEquals(snapshotCells))
        // And the clone genuinely advanced.
        assertNotEquals(snapshotScore, clone.score)
    }

    @Test
    fun startLevelIsRespected() {
        val engine = TetrisEngine(GameConfig(seed = 1L, startLevel = 7))
        assertEquals(7, engine.level)
    }

    @Test
    fun levelProgressionAdvancesEveryTenLines() {
        assertEquals(2, levelAfterClearingTenLines(progression = true))
    }

    @Test
    fun levelStaysFixedWhenProgressionOff() {
        assertEquals(1, levelAfterClearingTenLines(progression = false))
    }

    private fun levelAfterClearingTenLines(progression: Boolean): Int {
        val engine = TetrisEngine(GameConfig(seed = 3L, startLevel = 1, levelProgression = progression))
        repeat(10) {
            engine.testClearBoard()
            // Fill the bottom row except the rightmost column.
            for (x in 0 until 9) engine.testFill(x, 0, 1)
            // Drop a vertical I into column 9 to complete and clear the row.
            engine.testSetActive(PieceType.I, rot = 1, originX = 7, originY = 20)
            val result = engine.applyAction(Action.HARD_DROP)
            assertEquals(1, result.linesCleared, "iteration $it should clear exactly one line")
        }
        assertEquals(10, engine.lines)
        return engine.level
    }

    @Test
    fun lineClearScoresGuidelinePoints() {
        val engine = TetrisEngine(GameConfig(seed = 5L, startLevel = 1))
        engine.testClearBoard()
        for (x in 0 until 9) engine.testFill(x, 0, 1)
        engine.testSetActive(PieceType.I, rot = 1, originX = 7, originY = 20)
        val before = engine.score
        val result = engine.applyAction(Action.HARD_DROP)
        assertEquals(1, result.linesCleared)
        assertEquals(TSpin.NONE, result.tSpin)
        // 100 (single) * level 1, plus 2 points per hard-dropped row.
        assertTrue(result.scoreDelta >= 100, "single clear should award at least 100")
        assertEquals(result.scoreDelta, engine.score - before)
    }

    @Test
    fun holdSwapsAndIsLimitedToOncePerPiece() {
        val engine = TetrisEngine(GameConfig(seed = 9L, holdEnabled = true))
        val firstType = engine.state.active!!.type
        assertTrue(engine.state.canHold)

        engine.applyAction(Action.HOLD)
        assertEquals(firstType, engine.state.hold, "held piece should be the one we swapped out")
        assertFalse(engine.state.canHold, "cannot hold again until the next piece locks")

        // A second hold before locking is a no-op.
        val held = engine.state.hold
        engine.applyAction(Action.HOLD)
        assertEquals(held, engine.state.hold)
    }

    @Test
    fun detectsFullTSpin() {
        // Build a T-slot at the bottom-left and rotate a T (point-down) into it.
        val engine = TetrisEngine(GameConfig(seed = 11L))
        engine.testClearBoard()
        // Three of the four 3x3 corners around center (1,1) filled: (0,0),(2,0),(0,2).
        engine.testFill(0, 0, 1)
        engine.testFill(2, 0, 1)
        engine.testFill(0, 2, 1)
        // Place the T in its R orientation at origin (0,0), then rotate CW into state 2 (point down).
        engine.testSetActive(PieceType.T, rot = 1, originX = 0, originY = 0)
        engine.applyAction(Action.ROTATE_CW)
        assertEquals(2, engine.testRotation, "T should be point-down")
        val result = engine.applyAction(Action.HARD_DROP)
        assertEquals(TSpin.FULL, result.tSpin)
    }

    @Test
    fun legalPlacementsForFlatPieceCoverAllColumns() {
        val engine = TetrisEngine(GameConfig(seed = 13L))
        engine.testClearBoard()
        engine.testSetActive(PieceType.O, rot = 0, originX = 4, originY = 20)
        // O is 2 wide on a 10-wide board: 9 distinct columns, all rotations identical.
        assertEquals(9, engine.legalPlacements().size)
    }

    @Test
    fun applyPlacementLocksAndAdvances() {
        val engine = TetrisEngine(GameConfig(seed = 17L))
        val placements = engine.legalPlacements()
        assertTrue(placements.isNotEmpty())
        val result = engine.applyPlacement(placements.first())
        assertTrue(result.pieceLocked)
    }

    @Test
    fun randomAgentPlaysToGameOverWithoutCrashing() {
        val engine = TetrisEngine(GameConfig(seed = 99L))
        val agent = com.aratetris.engine.agent.RandomAgent(seed = 99L)
        var steps = 0
        while (!engine.isGameOver && steps < 100_000) {
            engine.applyAction(agent.selectAction(engine.state))
            steps++
        }
        assertTrue(engine.isGameOver, "a random agent biased to hard-drop should eventually top out")
    }
}
