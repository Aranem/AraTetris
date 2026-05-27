package com.aratetris.headless

import com.aratetris.engine.Action
import com.aratetris.engine.GameConfig
import com.aratetris.engine.TetrisEngine
import com.aratetris.engine.agent.RandomAgent

/**
 * Headless driver — proves the engine is fully playable programmatically with no graphics. It runs
 * agents over many games as fast as the CPU allows and prints aggregate stats. This is the entry
 * point the user's future algorithm plugs into.
 *
 * Run: `./gradlew headless:run`            (defaults to 20 games)
 *      `./gradlew headless:run --args 100` (run 100 games)
 */
fun main(args: Array<String>) {
    val games = args.firstOrNull()?.toIntOrNull() ?: 20

    println("AraTetris headless runner")
    println("=========================")

    verifyDeterminism()
    println()
    runAgent("RandomAgent (micro-actions)", games) { engine, stepBudget ->
        val agent = RandomAgent(seed = engine.config.seed)
        var steps = 0
        while (!engine.isGameOver && steps < stepBudget) {
            engine.applyAction(agent.selectAction(engine.state))
            steps++
        }
    }
    println()
    runAgent("GreedyPlacementAgent (placement API)", games) { engine, pieceBudget ->
        var pieces = 0
        while (!engine.isGameOver && pieces < pieceBudget) {
            val best = bestPlacement(engine) ?: break
            engine.applyPlacement(best)
            pieces++
        }
    }
}

/** Confirms (seed + action sequence) reproducibility — essential for training/evaluation. */
private fun verifyDeterminism() {
    fun playOut(): Triple<Int, Int, Boolean> {
        val e = TetrisEngine(GameConfig(seed = 123456L))
        val a = RandomAgent(seed = 123456L)
        var steps = 0
        while (!e.isGameOver && steps < 100_000) { e.applyAction(a.selectAction(e.state)); steps++ }
        return Triple(e.score, e.lines, e.isGameOver)
    }
    val first = playOut()
    val second = playOut()
    val ok = first == second
    println("Determinism (same seed -> same game): ${if (ok) "OK" else "FAILED"}  [$first]")
}

private inline fun runAgent(name: String, games: Int, play: (TetrisEngine, Int) -> Unit) {
    var totalScore = 0L
    var maxScore = 0
    var totalLines = 0L
    var maxLines = 0
    val start = System.nanoTime()
    for (g in 0 until games) {
        val engine = TetrisEngine(GameConfig(seed = 1000L + g, gravityEnabled = false))
        play(engine, 2000)
        totalScore += engine.score
        totalLines += engine.lines
        maxScore = maxOf(maxScore, engine.score)
        maxLines = maxOf(maxLines, engine.lines)
    }
    val ms = (System.nanoTime() - start) / 1_000_000.0
    println(name)
    println("  games=$games  avgScore=${totalScore / games}  maxScore=$maxScore  " +
        "avgLines=${totalLines / games}  maxLines=$maxLines  (${"%.0f".format(ms)} ms)")
}

/**
 * A tiny example heuristic that uses [TetrisEngine.copy] + [TetrisEngine.legalPlacements] +
 * [TetrisEngine.applyPlacement] to pick a landing spot (the classic El-Tetris feature weights).
 * It both validates the placement API and gives the user a working starting point.
 */
private fun bestPlacement(engine: TetrisEngine): com.aratetris.engine.Placement? {
    var best: com.aratetris.engine.Placement? = null
    var bestScore = Double.NEGATIVE_INFINITY
    for (p in engine.legalPlacements()) {
        val sim = engine.copy()
        val result = sim.applyPlacement(p)
        val s = sim.state
        val (aggHeight, holes, bumpiness) = boardFeatures(s.cells, s.width, s.height)
        val value = 0.76 * result.linesCleared - 0.51 * aggHeight - 0.36 * holes - 0.18 * bumpiness
        if (value > bestScore) { bestScore = value; best = p }
    }
    return best
}

private fun boardFeatures(cells: IntArray, width: Int, height: Int): Triple<Int, Int, Int> {
    val heights = IntArray(width)
    var holes = 0
    for (x in 0 until width) {
        var colTop = 0
        for (y in height - 1 downTo 0) {
            if (cells[x + y * width] != 0) { colTop = y + 1; break }
        }
        heights[x] = colTop
        var seenBlock = false
        for (y in height - 1 downTo 0) {
            if (cells[x + y * width] != 0) seenBlock = true
            else if (seenBlock) holes++
        }
    }
    val aggHeight = heights.sum()
    var bumpiness = 0
    for (x in 0 until width - 1) bumpiness += kotlin.math.abs(heights[x] - heights[x + 1])
    return Triple(aggHeight, holes, bumpiness)
}
