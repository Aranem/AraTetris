package com.aratetris

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.aratetris.engine.GameStateView
import com.aratetris.engine.PieceType
import com.aratetris.engine.Shapes

/** Draws everything with a [ShapeRenderer] (blocks/panels) and the built-in [BitmapFont] (text). */
class Renderer {

    private lateinit var shape: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var fontTexture: Texture // the binarized glyph atlas (owned here, not by the font)
    private val layout = GlyphLayout()

    fun create() {
        shape = ShapeRenderer()
        batch = SpriteBatch()
        font = buildCrispFont()
    }

    /**
     * The built-in [BitmapFont] is a ~15px Arial atlas with anti-aliasing baked into the pixels, which
     * looks grainy once scaled up. Rebuild it from a copy of that atlas whose alpha is forced fully
     * on/off (no partially-transparent edge pixels) and sampled with nearest filtering, so scaling
     * never introduces in-between shades. Trade-off: hard, blocky edges instead of soft ones.
     */
    private fun buildCrispFont(): BitmapFont {
        val base = BitmapFont()
        val tex = base.region.texture
        if (!tex.textureData.isPrepared) tex.textureData.prepare()
        val src = tex.textureData.consumePixmap()
        val bin = Pixmap(src.width, src.height, Pixmap.Format.RGBA8888)
        bin.blending = Pixmap.Blending.None
        for (y in 0 until src.height) {
            for (x in 0 until src.width) {
                val alpha = src.getPixel(x, y) and 0xFF // getPixel always returns RGBA8888
                bin.drawPixel(x, y, if (alpha >= ALPHA_CUTOFF) -1 else 0) // -1 = opaque white, 0 = clear
            }
        }
        fontTexture = Texture(bin).apply { setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) }
        src.dispose()
        bin.dispose()
        base.region.texture.dispose() // original soft atlas no longer needed; glyph metrics are reused
        val crisp = BitmapFont(base.data, TextureRegion(fontTexture), false)
        crisp.setUseIntegerPositions(false)
        // Widen each glyph's advance so the blocky letters don't visually run into each other.
        crisp.data.glyphs.forEach { row -> row?.forEach { g -> if (g != null) g.xadvance += LETTER_SPACING } }
        return crisp
    }

    fun setProjection(m: Matrix4) {
        shape.projectionMatrix = m
        batch.projectionMatrix = m
    }

    fun dispose() {
        shape.dispose()
        batch.dispose()
        font.dispose()
        fontTexture.dispose() // font was built from a passed-in region, so it doesn't own this
    }

    // ------------------------------------------------------------------ in-game

    fun drawGame(state: GameStateView, showTouch: Boolean, botPlaying: Boolean, swipeMode: Boolean, flash: String? = null) {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Constants.BOARD_BG
        shape.rect(Constants.BOARD_X, Constants.BOARD_Y, Constants.BOARD_W, Constants.BOARD_H)

        // Locked cells.
        for (y in 0 until Constants.BOARD_ROWS) {
            for (x in 0 until Constants.BOARD_COLS) {
                val id = state.cellAt(x, y)
                if (id != 0) cell(x, y, Constants.colorForId(id))
            }
        }
        // Ghost (landing preview).
        state.ghost?.cells?.forEach { c ->
            if (c.y in 0 until Constants.BOARD_ROWS) {
                val sx = Constants.BOARD_X + c.x * Constants.CELL
                val sy = Constants.BOARD_Y + c.y * Constants.CELL
                shape.color = Constants.GHOST
                shape.rect(sx + 2f, sy + 2f, Constants.CELL - 4f, Constants.CELL - 4f)
            }
        }
        // Active piece.
        state.active?.let { p ->
            val color = Constants.colorFor(p.type)
            p.cells.forEach { c -> if (c.y in 0 until Constants.BOARD_ROWS) cell(c.x, c.y, color) }
        }

        // Side panels.
        state.hold?.let { miniPiece(it, 12f, 660f, 18f) }
        state.next.take(5).forEachIndexed { i, t -> miniPiece(t, 396f, 660f - i * 74f, 18f) }

        if (showTouch) {
            if (!swipeMode) {
                TouchControls.gameplay.forEach { b ->
                    shape.color = Constants.BUTTON
                    shape.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height)
                }
            }
            shape.color = Constants.BUTTON
            shape.rect(TouchControls.gameHold.x, TouchControls.gameHold.y,
                TouchControls.gameHold.width, TouchControls.gameHold.height)
            shape.rect(TouchControls.gamePause.x, TouchControls.gamePause.y,
                TouchControls.gamePause.width, TouchControls.gamePause.height)
        }
        shape.end()

        // Grid lines.
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Constants.GRID_LINE
        for (x in 0..Constants.BOARD_COLS) {
            val sx = Constants.BOARD_X + x * Constants.CELL
            shape.line(sx, Constants.BOARD_Y, sx, Constants.BOARD_Y + Constants.BOARD_H)
        }
        for (y in 0..Constants.BOARD_ROWS) {
            val sy = Constants.BOARD_Y + y * Constants.CELL
            shape.line(Constants.BOARD_X, sy, Constants.BOARD_X + Constants.BOARD_W, sy)
        }
        shape.end()

        // Text.
        batch.begin()
        text("SCORE ${state.score}", 12f, 792f, 1.1f, Constants.TEXT)
        text("LV ${state.level}", 12f, 600f, 1f, Constants.TEXT)
        text("LINES ${state.lines}", 12f, 572f, 1f, Constants.TEXT)
        flash?.let { text(it, 12f, 544f, 1f, Constants.ACCENT) } // transient TETRIS / B2B banner
        if (state.combo > 0) text("COMBO ${state.combo}", 12f, 516f, 1f, Constants.ACCENT)
        text("HOLD", 12f, 712f, 1f, Constants.TEXT_DIM)
        text("NEXT", 396f, 712f, 1f, Constants.TEXT_DIM)
        if (botPlaying) text("BOT", 396f, 792f, 1.1f, Constants.ACCENT)

        if (showTouch) {
            if (!swipeMode) {
                TouchControls.gameplay.forEach { b ->
                    centered(b.label, b.rect.x + b.rect.width / 2f, b.rect.y + b.rect.height / 2f, 0.9f, Constants.TEXT)
                }
            } else {
                centered("Swipe to move / soft-drop   flick up = hard drop   tap L/R = rotate",
                    Constants.VIRTUAL_WIDTH / 2f, 36f, 0.6f, Constants.TEXT_DIM)
            }
            centered("HOLD", center(TouchControls.gameHold).first, center(TouchControls.gameHold).second, 0.8f, Constants.TEXT)
            centered("PAUSE", center(TouchControls.gamePause).first, center(TouchControls.gamePause).second, 0.8f, Constants.TEXT)
        } else {
            text("A/D move   Left/Right rotate   Up drop   Down soft   C hold   P pause   B bot   R restart",
                10f, 36f, 0.62f, Constants.TEXT_DIM)
        }
        batch.end()
    }

    // ------------------------------------------------------------------ menu

    fun drawMenu(menu: StartMenu, best: Int) {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Constants.BACKGROUND
        shape.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        panel(TouchControls.menuLevelDown)
        panel(TouchControls.menuLevelUp)
        panel(TouchControls.menuToggle)
        panel(TouchControls.menuSwipe)
        accentPanel(TouchControls.menuStart)
        panel(TouchControls.menuControls)
        shape.end()

        batch.begin()
        centered("ARATETRIS", Constants.VIRTUAL_WIDTH / 2f, 700f, 2.4f, Constants.ACCENT)
        centered("Best: $best", Constants.VIRTUAL_WIDTH / 2f, 636f, 1f, Constants.TEXT_DIM)
        centered("START LEVEL", Constants.VIRTUAL_WIDTH / 2f, 562f, 1.1f, Constants.TEXT_DIM)
        centered("${menu.startLevel}", Constants.VIRTUAL_WIDTH / 2f, 474f, 2f, Constants.TEXT)
        centered("-", center(TouchControls.menuLevelDown).first, center(TouchControls.menuLevelDown).second, 2f, Constants.TEXT)
        centered("+", center(TouchControls.menuLevelUp).first, center(TouchControls.menuLevelUp).second, 2f, Constants.TEXT)
        centered(if (menu.levelProgression) "LEVEL UP: ON" else "LEVEL UP: OFF",
            center(TouchControls.menuToggle).first, center(TouchControls.menuToggle).second, 1f, Constants.TEXT)
        centered(if (menu.swipeControls) "SWIPE: ON" else "SWIPE: OFF",
            center(TouchControls.menuSwipe).first, center(TouchControls.menuSwipe).second, 1f, Constants.TEXT)
        centered("START", center(TouchControls.menuStart).first, center(TouchControls.menuStart).second, 1.2f, Constants.TEXT)
        centered("CONTROLS", center(TouchControls.menuControls).first, center(TouchControls.menuControls).second, 1f, Constants.TEXT)
        centered("Left/Right level   T level-up   V swipe   Space start   C controls",
            Constants.VIRTUAL_WIDTH / 2f, 122f, 0.65f, Constants.TEXT_DIM)
        batch.end()
    }

    // ------------------------------------------------------------------ pause menu

    fun drawPauseMenu() {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Constants.OVERLAY
        shape.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        accentPanel(TouchControls.pauseResume)
        panel(TouchControls.pauseRestart)
        panel(TouchControls.pauseMainMenu)
        shape.end()

        batch.begin()
        centered("PAUSED", Constants.VIRTUAL_WIDTH / 2f, 600f, 2.2f, Constants.TEXT)
        centered("RESUME", center(TouchControls.pauseResume).first, center(TouchControls.pauseResume).second, 1.2f, Constants.TEXT)
        centered("RESTART", center(TouchControls.pauseRestart).first, center(TouchControls.pauseRestart).second, 1.1f, Constants.TEXT)
        centered("MAIN MENU", center(TouchControls.pauseMainMenu).first, center(TouchControls.pauseMainMenu).second, 1.1f, Constants.TEXT)
        centered("P/Esc resume   R restart   M main menu", Constants.VIRTUAL_WIDTH / 2f, 200f, 0.75f, Constants.TEXT_DIM)
        batch.end()
    }

    // ------------------------------------------------------------------ controls page

    fun drawControls() {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Constants.BACKGROUND
        shape.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        accentPanel(TouchControls.controlsBack)
        shape.end()

        batch.begin()
        centered("CONTROLS", Constants.VIRTUAL_WIDTH / 2f, 740f, 2f, Constants.ACCENT)
        val rows = listOf(
            "Move" to "A / D",
            "Rotate left" to "Left  (or Q)",
            "Rotate right" to "Right  (or E)",
            "Soft drop" to "Down  (or S)",
            "Hard drop" to "Up  (or Space / W)",
            "Hold" to "C  (or Shift)",
            "Pause menu" to "P  (or Esc)",
            "Bot on/off" to "B",
            "Restart" to "R",
        )
        var y = 660f
        for ((label, keys) in rows) {
            text(label, 60f, y, 0.95f, Constants.TEXT_DIM)
            text(keys, 230f, y, 0.95f, Constants.TEXT)
            y -= 40f
        }
        text("Android: buttons, or swipe mode (toggle on menu) -", 60f, y - 6f, 0.78f, Constants.TEXT_DIM)
        text("swipe move/soft-drop, flick up hard-drop, tap L/R rotate", 60f, y - 34f, 0.78f, Constants.TEXT_DIM)
        text("Reset high scores: Del, on the game-over screen", 60f, y - 64f, 0.78f, Constants.TEXT_DIM)
        centered("BACK", center(TouchControls.controlsBack).first, center(TouchControls.controlsBack).second, 1.1f, Constants.TEXT)
        centered("press any key or tap to return", Constants.VIRTUAL_WIDTH / 2f, 40f, 0.7f, Constants.TEXT_DIM)
        batch.end()
    }

    // ------------------------------------------------------------------ game over

    fun drawGameOver(state: GameStateView, scores: List<Int>, lastRank: Int?, confirming: Boolean) {
        // Dim the field that is already drawn behind this overlay.
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Constants.OVERLAY
        shape.rect(0f, 0f, Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT)
        if (!confirming) {
            accentPanel(TouchControls.gameOverRestart)
            panel(TouchControls.gameOverMenu)
            panel(TouchControls.gameOverReset)
        } else {
            panel(TouchControls.confirmYes)
            panel(TouchControls.confirmNo)
        }
        shape.end()

        batch.begin()
        centered("GAME OVER", Constants.VIRTUAL_WIDTH / 2f, 720f, 2f, Constants.TEXT)
        centered("Score ${state.score}", Constants.VIRTUAL_WIDTH / 2f, 660f, 1.2f, Constants.ACCENT)
        if (lastRank != null) centered("New high score! #$lastRank", Constants.VIRTUAL_WIDTH / 2f, 628f, 1f, Constants.ACCENT)

        centered("HIGH SCORES", Constants.VIRTUAL_WIDTH / 2f, 580f, 1.1f, Constants.TEXT_DIM)
        if (scores.isEmpty()) {
            centered("- none yet -", Constants.VIRTUAL_WIDTH / 2f, 540f, 1f, Constants.TEXT_DIM)
        } else {
            scores.forEachIndexed { i, s ->
                centered("${i + 1}.  $s", Constants.VIRTUAL_WIDTH / 2f, 545f - i * 28f, 1f, Constants.TEXT)
            }
        }

        if (!confirming) {
            centered("RESTART", center(TouchControls.gameOverRestart).first, center(TouchControls.gameOverRestart).second, 1.2f, Constants.TEXT)
            centered("MAIN MENU", center(TouchControls.gameOverMenu).first, center(TouchControls.gameOverMenu).second, 1.1f, Constants.TEXT)
            centered("RESET SCORES", center(TouchControls.gameOverReset).first, center(TouchControls.gameOverReset).second, 0.95f, Constants.TEXT)
            centered("Space/R restart   M menu   Del reset", Constants.VIRTUAL_WIDTH / 2f, 130f, 0.75f, Constants.TEXT_DIM)
        } else {
            centered("Reset all scores?", Constants.VIRTUAL_WIDTH / 2f, 420f, 1.3f, Constants.TEXT)
            centered("YES", center(TouchControls.confirmYes).first, center(TouchControls.confirmYes).second, 1.2f, Constants.TEXT)
            centered("NO", center(TouchControls.confirmNo).first, center(TouchControls.confirmNo).second, 1.2f, Constants.TEXT)
        }
        batch.end()
    }

    // ------------------------------------------------------------------ helpers

    private fun cell(x: Int, y: Int, color: Color) {
        val sx = Constants.BOARD_X + x * Constants.CELL
        val sy = Constants.BOARD_Y + y * Constants.CELL
        shape.color = color
        shape.rect(sx + 1f, sy + 1f, Constants.CELL - 2f, Constants.CELL - 2f)
    }

    private fun miniPiece(type: PieceType, originX: Float, originY: Float, size: Float) {
        val cells = Shapes.cells(type, 0)
        val minX = cells.minOf { it.first }
        val minY = cells.minOf { it.second }
        shape.color = Constants.colorFor(type)
        cells.forEach { (cx, cy) ->
            shape.rect(originX + (cx - minX) * size + 1f, originY + (cy - minY) * size + 1f, size - 2f, size - 2f)
        }
    }

    private fun panel(r: Rectangle) {
        shape.color = Constants.BUTTON
        shape.rect(r.x, r.y, r.width, r.height)
    }

    private fun accentPanel(r: Rectangle) {
        shape.color = Constants.BUTTON_DOWN
        shape.rect(r.x, r.y, r.width, r.height)
    }

    private fun center(r: Rectangle): Pair<Float, Float> = (r.x + r.width / 2f) to (r.y + r.height / 2f)

    private fun text(s: String, x: Float, y: Float, scale: Float, color: Color) {
        font.data.setScale(scale)
        font.color = color
        font.draw(batch, s, x, y)
        font.data.setScale(1f)
    }

    private fun centered(s: String, cx: Float, cy: Float, scale: Float, color: Color) {
        font.data.setScale(scale)
        font.color = color
        layout.setText(font, s)
        font.draw(batch, layout, cx - layout.width / 2f, cy + layout.height / 2f)
        font.data.setScale(1f)
    }

    private companion object {
        // Glyph pixels at least this opaque become solid; the rest become fully transparent.
        // Higher = thinner strokes (more edge pixels dropped); much above 0x80 erodes thin strokes
        // (e.g. drops a '+' arm), so keep it near 50%.
        const val ALPHA_CUTOFF = 0x80
        // Extra horizontal advance added to every glyph, in unscaled font units (scales with text).
        const val LETTER_SPACING = 1
    }
}
