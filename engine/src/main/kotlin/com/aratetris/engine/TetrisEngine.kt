package com.aratetris.engine

/**
 * The whole game as a pure, deterministic state machine — with no graphics, no window and no
 * Android dependency. A human shell and an algorithm both drive it through the same surface:
 *
 *  - micro-actions: [applyAction] (LEFT/RIGHT/ROTATE/SOFT_DROP/HARD_DROP/HOLD)
 *  - timing: [update] (real-time gravity) or [tick] (one turn-based gravity step)
 *  - high-level: [legalPlacements] / [applyPlacement] / [movesFor] for heuristic/search bots
 *  - simulation: [copy] for side-effect-free lookahead, and a seedable [Rng] for reproducibility
 *
 * Read state through the immutable [state] snapshot.
 */
class TetrisEngine(val config: GameConfig = GameConfig(), autoStart: Boolean = true) {

    val width: Int = config.width
    val height: Int = config.height

    private val board = Board(config.width, config.totalHeight)
    private var rng = Rng(config.seed)
    private var bag = Bag(rng)

    // Active piece (bounding-box origin in board coordinates, y-up).
    private var type = PieceType.I
    private var rotation = 0
    private var ox = 0
    private var oy = 0

    private var holdType: PieceType? = null
    private var canHold = true

    private var scoreValue = 0
    private var linesValue = 0
    private var levelValue = config.startLevel
    private var combo = -1
    private var b2bActive = false
    private var gameOver = false
    private var paused = false

    // T-spin bookkeeping.
    private var lastRotation = false
    private var lastKickIndex = 0

    // Real-time gravity / lock-delay bookkeeping (only used when config.gravityEnabled).
    private var gravityAccum = 0.0
    private var lockTimer = 0.0
    private var lockResets = 0

    init {
        if (autoStart) reset(config.seed)
    }

    val isGameOver: Boolean get() = gameOver
    val isPaused: Boolean get() = paused
    val score: Int get() = scoreValue
    val level: Int get() = levelValue
    val lines: Int get() = linesValue

    // ----------------------------------------------------------------- lifecycle

    fun reset(seed: Long = config.seed) {
        board.clear()
        rng = Rng(seed)
        bag = Bag(rng)
        scoreValue = 0
        linesValue = 0
        levelValue = config.startLevel
        combo = -1
        b2bActive = false
        gameOver = false
        paused = false
        gravityAccum = 0.0
        lockTimer = 0.0
        lockResets = 0
        holdType = null
        canHold = true
        spawnFromBag()
    }

    // ----------------------------------------------------------------- micro-actions

    fun applyAction(action: Action): StepResult {
        if (gameOver) return StepResult.NOTHING.copy(gameOver = true)
        if (paused && action != Action.PAUSE) return StepResult.NOTHING

        return when (action) {
            Action.LEFT -> {
                val ok = tryMove(-1, 0)
                if (ok) { lastRotation = false; resetLockTimerOnManipulate() }
                StepResult.NOTHING.copy(moved = ok)
            }
            Action.RIGHT -> {
                val ok = tryMove(1, 0)
                if (ok) { lastRotation = false; resetLockTimerOnManipulate() }
                StepResult.NOTHING.copy(moved = ok)
            }
            Action.ROTATE_CW -> {
                val ok = tryRotate(1)
                if (ok) { lastRotation = true; resetLockTimerOnManipulate() }
                StepResult.NOTHING.copy(moved = ok, rotated = ok)
            }
            Action.ROTATE_CCW -> {
                val ok = tryRotate(-1)
                if (ok) { lastRotation = true; resetLockTimerOnManipulate() }
                StepResult.NOTHING.copy(moved = ok, rotated = ok)
            }
            Action.SOFT_DROP -> {
                val ok = tryMove(0, -1)
                if (ok) scoreValue += Scoring.SOFT_DROP_PER_CELL
                StepResult.NOTHING.copy(moved = ok, scoreDelta = if (ok) Scoring.SOFT_DROP_PER_CELL else 0)
            }
            Action.HARD_DROP -> hardDrop()
            Action.HOLD -> holdAction()
            Action.PAUSE -> { paused = !paused; StepResult.NOTHING }
            Action.NONE -> StepResult.NOTHING
        }
    }

    private fun hardDrop(): StepResult {
        val d = dropDistance()
        oy -= d
        val dropScore = Scoring.HARD_DROP_PER_CELL * d
        scoreValue += dropScore
        return lockPiece(extraScore = dropScore)
    }

    private fun holdAction(): StepResult {
        if (!config.holdEnabled || !canHold) return StepResult.NOTHING
        val current = type
        val swapIn = holdType
        holdType = current
        if (swapIn == null) spawnFromBag() else spawnPiece(swapIn)
        canHold = false
        lastRotation = false
        return StepResult.NOTHING.copy(moved = true, gameOver = gameOver)
    }

    // ----------------------------------------------------------------- timing

    /** Advance real-time gravity by [deltaSeconds]. No-op unless [GameConfig.gravityEnabled]. */
    fun update(deltaSeconds: Float) {
        if (!config.gravityEnabled || gameOver || paused) return
        val step = Scoring.gravitySecondsPerRow(levelValue)
        gravityAccum += deltaSeconds.toDouble()
        while (gravityAccum >= step) {
            gravityAccum -= step
            if (tryMove(0, -1)) {
                lockTimer = 0.0
            } else {
                break
            }
        }
        if (dropDistance() == 0) {
            lockTimer += deltaSeconds.toDouble()
            if (lockTimer >= Scoring.LOCK_DELAY_SECONDS) lockPiece(extraScore = 0)
        } else {
            lockTimer = 0.0
        }
    }

    /** Advance exactly one gravity step (turn-based). Locks the piece if it cannot fall further. */
    fun tick(): StepResult {
        if (gameOver || paused) return StepResult.NOTHING
        return if (tryMove(0, -1)) StepResult.NOTHING.copy(moved = true) else lockPiece(extraScore = 0)
    }

    // ----------------------------------------------------------------- placement (high-level)

    /**
     * Every distinct final resting placement reachable for the current piece by rotating and sliding
     * from the top, then hard-dropping. (Tucks/spins under overhangs are not enumerated; obtain the
     * held piece's options by issuing [Action.HOLD] then calling this again.)
     */
    fun legalPlacements(): List<Placement> {
        if (gameOver) return emptyList()
        val result = mutableListOf<Placement>()
        val seen = HashSet<List<CellPos>>()
        for (rot in 0..3) {
            val local = Shapes.cells(type, rot)
            val minLocalX = local.minOf { it.first }
            val maxLocalX = local.maxOf { it.first }
            val maxLocalY = local.maxOf { it.second }
            val startOy = config.totalHeight - 1 - maxLocalY
            for (oxCand in -minLocalX..(width - 1 - maxLocalX)) {
                if (board.collides(absCells(type, rot, oxCand, startOy))) continue
                var d = 0
                while (!board.collides(absCells(type, rot, oxCand, startOy - (d + 1)))) d++
                val landed = absCells(type, rot, oxCand, startOy - d)
                    .map { CellPos(it.first, it.second) }
                    .sortedWith(compareBy({ it.x }, { it.y }))
                if (seen.add(landed)) result.add(Placement(rot, oxCand))
            }
        }
        return result
    }

    /** Move the current piece to [p]'s orientation/column at the top, then hard-drop and lock. */
    fun applyPlacement(p: Placement): StepResult {
        if (gameOver) return StepResult.NOTHING.copy(gameOver = true)
        rotation = norm(p.rotation)
        ox = p.x
        val local = Shapes.cells(type, rotation)
        oy = config.totalHeight - 1 - local.maxOf { it.second }
        if (board.collides(absCells(type, rotation, ox, oy))) {
            gameOver = true
            return StepResult.NOTHING.copy(gameOver = true)
        }
        // A straight hard-drop placement is never a spin.
        lastRotation = false
        val d = dropDistance()
        oy -= d
        val dropScore = Scoring.HARD_DROP_PER_CELL * d
        scoreValue += dropScore
        return lockPiece(extraScore = dropScore)
    }

    /**
     * The micro-action sequence that reaches [p] from a freshly spawned piece (rotate, slide,
     * hard-drop). Assumes the open spawn area, so rotations do not kick; useful for animating the
     * bot's individual moves in the rendered window.
     */
    fun movesFor(p: Placement): List<Action> {
        val moves = mutableListOf<Action>()
        repeat(norm(p.rotation)) { moves.add(Action.ROTATE_CW) }
        val diff = p.x - spawnX(type)
        when {
            diff < 0 -> repeat(-diff) { moves.add(Action.LEFT) }
            diff > 0 -> repeat(diff) { moves.add(Action.RIGHT) }
        }
        moves.add(Action.HARD_DROP)
        return moves
    }

    // ----------------------------------------------------------------- simulation

    /** A fully independent deep copy (board, RNG, bag, piece, counters) for lookahead/search. */
    fun copy(): TetrisEngine {
        val e = TetrisEngine(config, autoStart = false)
        board.cells.copyInto(e.board.cells)
        e.rng = rng.copy()
        e.bag = bag.copyWith(e.rng)
        e.type = type
        e.rotation = rotation
        e.ox = ox
        e.oy = oy
        e.holdType = holdType
        e.canHold = canHold
        e.scoreValue = scoreValue
        e.linesValue = linesValue
        e.levelValue = levelValue
        e.combo = combo
        e.b2bActive = b2bActive
        e.gameOver = gameOver
        e.paused = paused
        e.lastRotation = lastRotation
        e.lastKickIndex = lastKickIndex
        e.gravityAccum = gravityAccum
        e.lockTimer = lockTimer
        e.lockResets = lockResets
        return e
    }

    // ----------------------------------------------------------------- state snapshot

    val state: GameStateView
        get() {
            val vis = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) vis[x + y * width] = board.cellAt(x, y)
            }
            val activeView = if (gameOver) null else
                PieceView(type, rotation, absCells(type, rotation, ox, oy).map { CellPos(it.first, it.second) })
            val ghostView = if (gameOver) null else {
                val d = dropDistance()
                PieceView(type, rotation, absCells(type, rotation, ox, oy - d).map { CellPos(it.first, it.second) })
            }
            return GameStateView(
                width = width,
                height = height,
                cells = vis,
                active = activeView,
                ghost = ghostView,
                next = bag.peek(config.previewCount),
                hold = holdType,
                canHold = canHold,
                score = scoreValue,
                level = levelValue,
                lines = linesValue,
                combo = combo,
                backToBack = b2bActive,
                gameOver = gameOver,
                paused = paused,
            )
        }

    // ----------------------------------------------------------------- internals

    private fun norm(r: Int) = ((r % 4) + 4) % 4

    private fun spawnX(t: PieceType) = (width - t.boxSize) / 2

    private fun spawnY(t: PieceType): Int {
        val minLocalY = Shapes.cells(t, 0).minOf { it.second }
        return height - minLocalY
    }

    private fun absCells(t: PieceType, rot: Int, originX: Int, originY: Int): List<Cell> =
        Shapes.cells(t, rot).map { (x, y) -> (x + originX) to (y + originY) }

    private fun tryMove(dx: Int, dy: Int): Boolean {
        if (board.collides(absCells(type, rotation, ox + dx, oy + dy))) return false
        ox += dx
        oy += dy
        return true
    }

    private fun tryRotate(dir: Int): Boolean {
        val from = rotation
        val to = norm(rotation + dir)
        for ((i, k) in Kicks.forPiece(type, from, to).withIndex()) {
            val nx = ox + k.first
            val ny = oy + k.second
            if (!board.collides(absCells(type, to, nx, ny))) {
                ox = nx
                oy = ny
                rotation = to
                lastKickIndex = i
                return true
            }
        }
        return false
    }

    private fun dropDistance(): Int {
        var d = 0
        while (!board.collides(absCells(type, rotation, ox, oy - (d + 1)))) d++
        return d
    }

    private fun resetLockTimerOnManipulate() {
        if (dropDistance() == 0 && lockResets < Scoring.MAX_LOCK_RESETS) {
            lockTimer = 0.0
            lockResets++
        }
    }

    private fun spawnFromBag() = spawnPiece(bag.next())

    private fun spawnPiece(t: PieceType) {
        type = t
        rotation = 0
        ox = spawnX(t)
        oy = spawnY(t)
        lastRotation = false
        lastKickIndex = 0
        gravityAccum = 0.0
        lockTimer = 0.0
        lockResets = 0
        if (board.collides(absCells(type, rotation, ox, oy))) gameOver = true
    }

    private fun lockPiece(extraScore: Int): StepResult {
        val cells = absCells(type, rotation, ox, oy)
        val spin = detectTSpin()
        board.lock(cells, type.id)
        val cleared = board.clearFullRows()
        val perfectClear = cleared > 0 && board.isEmpty()
        val lockPoints = computeScore(cleared, spin, perfectClear)
        scoreValue += lockPoints
        linesValue += cleared
        if (config.levelProgression) levelValue = config.startLevel + linesValue / 10

        canHold = true
        spawnFromBag()

        return StepResult(
            moved = true,
            rotated = false,
            pieceLocked = true,
            linesCleared = cleared,
            tSpin = spin,
            perfectClear = perfectClear,
            scoreDelta = lockPoints + extraScore,
            gameOver = gameOver,
        )
    }

    private fun computeScore(cleared: Int, spin: TSpin, perfectClear: Boolean): Int {
        val lvl = levelValue
        val base: Int
        val difficult: Boolean
        when (spin) {
            TSpin.FULL -> { base = Scoring.tSpinFullPoints(cleared); difficult = cleared > 0 }
            TSpin.MINI -> { base = Scoring.tSpinMiniPoints(cleared); difficult = cleared > 0 }
            TSpin.NONE -> { base = Scoring.linePoints(cleared); difficult = cleared == 4 }
        }
        var points = base * lvl
        if (cleared > 0 && difficult && b2bActive) points = points * 3 / 2 // Back-to-Back x1.5

        if (cleared > 0) {
            b2bActive = difficult
            combo += 1
            points += 50 * combo * lvl
        } else {
            combo = -1
            // A spin that clears no lines does not break Back-to-Back: leave b2bActive unchanged.
        }
        if (perfectClear && config.perfectClearBonus) points += Scoring.perfectClearPoints(cleared) * lvl
        return points
    }

    // ----------------------------------------------------------------- test hooks (module-internal)

    internal fun testClearBoard() = board.clear()

    internal fun testFill(x: Int, y: Int, id: Int) {
        board.cells[board.index(x, y)] = id
    }

    internal fun testSetActive(t: PieceType, rot: Int, originX: Int, originY: Int) {
        type = t
        rotation = norm(rot)
        ox = originX
        oy = originY
        lastRotation = false
        lastKickIndex = 0
    }

    internal fun testActiveCells(): List<CellPos> =
        absCells(type, rotation, ox, oy).map { CellPos(it.first, it.second) }

    internal val testRotation: Int get() = rotation

    /** Guideline 3-corner T-spin detection (with last-kick promotion to full). */
    private fun detectTSpin(): TSpin {
        if (type != PieceType.T || !lastRotation) return TSpin.NONE
        val cx = ox + 1
        val cy = oy + 1
        val tl = board.solid(cx - 1, cy + 1)
        val tr = board.solid(cx + 1, cy + 1)
        val bl = board.solid(cx - 1, cy - 1)
        val br = board.solid(cx + 1, cy - 1)
        val filled = listOf(tl, tr, bl, br).count { it }
        if (filled < 3) return TSpin.NONE
        if (lastKickIndex == 4) return TSpin.FULL // the final wall-kick always yields a full T-spin
        val (f1, f2) = when (rotation) {
            0 -> tl to tr   // point up
            1 -> tr to br   // point right
            2 -> bl to br   // point down
            else -> tl to bl // point left
        }
        val frontFilled = (if (f1) 1 else 0) + (if (f2) 1 else 0)
        return if (frontFilled == 2) TSpin.FULL else TSpin.MINI
    }
}
