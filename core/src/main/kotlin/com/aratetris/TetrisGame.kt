package com.aratetris

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.aratetris.engine.Action
import com.aratetris.engine.TetrisEngine
import com.aratetris.engine.agent.RandomAgent
import com.aratetris.engine.agent.TetrisAgent

/**
 * The libGDX shell. It owns a [TetrisEngine] and is purely a *view + input adapter* over it: it
 * forwards human input (keyboard/touch) as engine [Action]s, and can instead let a [TetrisAgent]
 * drive the very same engine (toggle with B / on-screen). All game logic lives in the engine.
 */
class TetrisGame(providedAgent: TetrisAgent? = null) : ApplicationAdapter() {

    private val botAgent: TetrisAgent = providedAgent ?: RandomAgent(seed = System.nanoTime())

    private lateinit var viewport: FitViewport
    private lateinit var renderer: Renderer
    private lateinit var highScores: HighScores
    private lateinit var menu: StartMenu
    private val gestures = GestureControls()

    private enum class Screen { MENU, CONTROLS, PLAYING, GAME_OVER }
    private var screen = Screen.MENU

    private var engine: TetrisEngine? = null
    private var botPlaying = false
    private var botTimer = 0f
    private var confirmingReset = false
    private var scoreSubmitted = false
    private var lastRank: Int? = null

    // Horizontal / soft-drop auto-repeat (DAS/ARR) state.
    private var moveDir = 0
    private var moveDelay = 0f
    private var softActive = false
    private var softDelay = 0f

    private val tmpTouch = Vector2()

    override fun create() {
        viewport = FitViewport(Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        renderer = Renderer().also { it.create() }
        highScores = HighScores()
        menu = StartMenu()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render() {
        val dt = minOf(Gdx.graphics.deltaTime, 0.05f)
        Gdx.gl.glClearColor(Constants.BACKGROUND.r, Constants.BACKGROUND.g, Constants.BACKGROUND.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        renderer.setProjection(viewport.camera.combined)

        when (screen) {
            Screen.MENU -> renderMenu()
            Screen.CONTROLS -> renderControls()
            Screen.PLAYING -> renderPlaying(dt)
            Screen.GAME_OVER -> renderGameOver()
        }
    }

    // ----------------------------------------------------------------- screens

    private fun renderMenu() {
        if (kbJust(Keys.LEFT) || kbJust(Keys.A)) menu.levelDown()
        if (kbJust(Keys.RIGHT) || kbJust(Keys.D)) menu.levelUp()
        if (kbJust(Keys.T)) menu.toggleProgression()
        if (kbJust(Keys.V)) menu.toggleSwipe()
        if (kbJust(Keys.SPACE) || kbJust(Keys.ENTER)) startGame()
        if (kbJust(Keys.C)) screen = Screen.CONTROLS

        tap()?.let { v ->
            when {
                hit(TouchControls.menuLevelDown, v) -> menu.levelDown()
                hit(TouchControls.menuLevelUp, v) -> menu.levelUp()
                hit(TouchControls.menuToggle, v) -> menu.toggleProgression()
                hit(TouchControls.menuSwipe, v) -> menu.toggleSwipe()
                hit(TouchControls.menuStart, v) -> startGame()
                hit(TouchControls.menuControls, v) -> screen = Screen.CONTROLS
            }
        }
        renderer.drawMenu(menu, highScores.best())
    }

    private fun renderControls() {
        if (Gdx.input.justTouched() || kbJust(Keys.ANY_KEY)) screen = Screen.MENU
        renderer.drawControls()
    }

    private fun renderPlaying(dt: Float) {
        val e = engine ?: return

        if (e.isPaused) {
            gestures.reset() // discard any in-progress gesture so resuming doesn't fire a stray move
            handlePauseMenu(e)
            if (screen != Screen.PLAYING || engine !== e) return // left to menu / restarted
            renderer.drawGame(e.state, showTouch = touchUi, botPlaying = botPlaying, swipeMode = touchUi && menu.swipeControls)
            renderer.drawPauseMenu()
            return
        }

        handlePlayingInput(dt)
        if (engine !== e) return // a restart replaced the engine this frame
        if (e.isPaused) { // just paused this frame
            renderer.drawGame(e.state, showTouch = touchUi, botPlaying = botPlaying, swipeMode = touchUi && menu.swipeControls)
            renderer.drawPauseMenu()
            return
        }

        e.update(dt)
        if (e.isGameOver) {
            if (!scoreSubmitted) {
                lastRank = highScores.submit(e.score)
                scoreSubmitted = true
            }
            screen = Screen.GAME_OVER
        }
        renderer.drawGame(e.state, showTouch = touchUi, botPlaying = botPlaying, swipeMode = touchUi && menu.swipeControls)
    }

    private fun handlePauseMenu(e: TetrisEngine) {
        if (kbJust(Keys.P) || kbJust(Keys.ESCAPE)) { e.applyAction(Action.PAUSE); return } // resume
        if (kbJust(Keys.R)) { startGame(); return }
        if (kbJust(Keys.M)) { screen = Screen.MENU; return }
        tap()?.let { v ->
            when {
                hit(TouchControls.pauseResume, v) -> e.applyAction(Action.PAUSE)
                hit(TouchControls.pauseRestart, v) -> startGame()
                hit(TouchControls.pauseMainMenu, v) -> screen = Screen.MENU
            }
        }
    }

    private fun renderGameOver() {
        val e = engine
        if (confirmingReset) {
            if (kbJust(Keys.Y) || kbJust(Keys.ENTER)) { highScores.reset(); confirmingReset = false }
            if (kbJust(Keys.N) || kbJust(Keys.ESCAPE)) confirmingReset = false
            tap()?.let { v ->
                when {
                    hit(TouchControls.confirmYes, v) -> { highScores.reset(); confirmingReset = false }
                    hit(TouchControls.confirmNo, v) -> confirmingReset = false
                }
            }
        } else {
            if (kbJust(Keys.SPACE) || kbJust(Keys.R) || kbJust(Keys.ENTER)) { startGame(); return }
            if (kbJust(Keys.FORWARD_DEL) || kbJust(Keys.DEL)) confirmingReset = true
            tap()?.let { v ->
                when {
                    hit(TouchControls.gameOverRestart, v) -> { startGame(); return }
                    hit(TouchControls.gameOverReset, v) -> confirmingReset = true
                }
            }
        }
        if (e != null) renderer.drawGameOver(e.state, highScores.list(), lastRank, confirmingReset)
    }

    // ----------------------------------------------------------------- gameplay input

    private fun handlePlayingInput(dt: Float) {
        val e = engine ?: return

        if (kbJust(Keys.P) || kbJust(Keys.ESCAPE)) { e.applyAction(Action.PAUSE); return }
        if (touchUi) {
            tap()?.let { v -> if (hit(TouchControls.gamePause, v)) { e.applyAction(Action.PAUSE); return } }
        }
        if (kbJust(Keys.B)) botPlaying = !botPlaying
        if (kbJust(Keys.R)) { startGame(); return }

        if (botPlaying) {
            botTimer += dt
            if (botTimer >= BOT_INTERVAL) {
                botTimer = 0f
                e.applyAction(botAgent.selectAction(e.state))
            }
            return
        }

        var dir = 0
        if (kb(Keys.A)) dir -= 1
        if (kb(Keys.D)) dir += 1
        var soft = kb(Keys.DOWN) || kb(Keys.S)
        var rotCW = kbJust(Keys.RIGHT) || kbJust(Keys.E)
        var rotCCW = kbJust(Keys.LEFT) || kbJust(Keys.Q)
        var hard = kbJust(Keys.UP) || kbJust(Keys.SPACE) || kbJust(Keys.W)
        var hold = kbJust(Keys.C) || kbJust(Keys.SHIFT_LEFT)

        if (touchUi) {
            if (menu.swipeControls) {
                applySwipeInput(e, dt)
                return
            }
            val held = heldTouchActions()
            if (Action.LEFT in held) dir -= 1
            if (Action.RIGHT in held) dir += 1
            if (Action.SOFT_DROP in held) soft = true
            tappedButton()?.let { b ->
                when (b.action) {
                    Action.ROTATE_CW -> rotCW = true
                    Action.ROTATE_CCW -> rotCCW = true
                    Action.HARD_DROP -> hard = true
                    else -> {}
                }
            }
            tap()?.let { v -> if (hit(TouchControls.gameHold, v)) hold = true }
        }

        horizontalRepeat(dir, dt)
        softRepeat(soft, dt)
        if (rotCW) e.applyAction(Action.ROTATE_CW)
        if (rotCCW) e.applyAction(Action.ROTATE_CCW)
        if (hard) e.applyAction(Action.HARD_DROP)
        if (hold) e.applyAction(Action.HOLD)
    }

    private fun applySwipeInput(e: TetrisEngine, dt: Float) {
        val touched = Gdx.input.isTouched(0)
        val v = if (touched)
            viewport.unproject(tmpTouch.set(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat()))
        else tmpTouch
        val g = gestures.update(
            touched, v.x, v.y, dt, Constants.VIRTUAL_WIDTH / 2f,
            TouchControls.gameHold, TouchControls.gamePause,
        )
        if (g.pause) { e.applyAction(Action.PAUSE); return }
        if (g.moveSteps != 0) {
            val a = if (g.moveSteps < 0) Action.LEFT else Action.RIGHT
            repeat(kotlin.math.abs(g.moveSteps)) { e.applyAction(a) }
        }
        repeat(g.softSteps) { e.applyAction(Action.SOFT_DROP) }
        if (g.hardDrop) e.applyAction(Action.HARD_DROP)
        if (g.rotateCW) e.applyAction(Action.ROTATE_CW)
        if (g.rotateCCW) e.applyAction(Action.ROTATE_CCW)
        if (g.hold) e.applyAction(Action.HOLD)
    }

    private fun horizontalRepeat(dir: Int, dt: Float) {
        val e = engine ?: return
        if (dir == 0) { moveDir = 0; return }
        if (dir != moveDir) {
            moveDir = dir
            moveDelay = DAS
            e.applyAction(if (dir < 0) Action.LEFT else Action.RIGHT)
        } else {
            moveDelay -= dt
            if (moveDelay <= 0f) {
                e.applyAction(if (dir < 0) Action.LEFT else Action.RIGHT)
                moveDelay = ARR
            }
        }
    }

    private fun softRepeat(active: Boolean, dt: Float) {
        val e = engine ?: return
        if (!active) { softActive = false; return }
        if (!softActive) {
            softActive = true
            softDelay = SOFT_ARR
            e.applyAction(Action.SOFT_DROP)
        } else {
            softDelay -= dt
            if (softDelay <= 0f) {
                e.applyAction(Action.SOFT_DROP)
                softDelay = SOFT_ARR
            }
        }
    }

    private fun heldTouchActions(): Set<Action> {
        val out = HashSet<Action>()
        for (p in 0..1) {
            if (Gdx.input.isTouched(p)) {
                val v = viewport.unproject(tmpTouch.set(Gdx.input.getX(p).toFloat(), Gdx.input.getY(p).toFloat()))
                TouchControls.gameplay.forEach { if (it.repeat && it.rect.contains(v.x, v.y)) out.add(it.action) }
            }
        }
        return out
    }

    private fun tappedButton(): TouchButton? {
        val v = tap() ?: return null
        return TouchControls.gameplay.firstOrNull { it.rect.contains(v.x, v.y) }
    }

    // ----------------------------------------------------------------- helpers

    private fun startGame() {
        engine = TetrisEngine(menu.buildConfig())
        screen = Screen.PLAYING
        botPlaying = false
        botTimer = 0f
        confirmingReset = false
        scoreSubmitted = false
        lastRank = null
        moveDir = 0
        softActive = false
        gestures.reset()
    }

    private val touchUi: Boolean get() = Gdx.app.type == Application.ApplicationType.Android

    private fun kb(key: Int) = Gdx.input.isKeyPressed(key)
    private fun kbJust(key: Int) = Gdx.input.isKeyJustPressed(key)

    /** The just-tapped point in virtual coordinates, or null if there was no new touch this frame. */
    private fun tap(): Vector2? {
        if (!Gdx.input.justTouched()) return null
        return viewport.unproject(tmpTouch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
    }

    private fun hit(rect: com.badlogic.gdx.math.Rectangle, v: Vector2) = rect.contains(v.x, v.y)

    override fun dispose() {
        renderer.dispose()
    }

    companion object {
        private const val DAS = 0.15f       // delayed auto-shift before horizontal repeat
        private const val ARR = 0.04f       // auto-repeat rate for horizontal movement
        private const val SOFT_ARR = 0.03f  // soft-drop repeat rate
        private const val BOT_INTERVAL = 0.06f // throttle so a watching human can follow the bot
    }
}
