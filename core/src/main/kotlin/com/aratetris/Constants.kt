package com.aratetris

import com.badlogic.gdx.graphics.Color
import com.aratetris.engine.PieceType

/** Virtual-coordinate layout and color palette. The world is a fixed portrait canvas scaled to fit. */
object Constants {
    const val VIRTUAL_WIDTH = 480f
    const val VIRTUAL_HEIGHT = 800f

    const val CELL = 30f
    const val BOARD_COLS = 10
    const val BOARD_ROWS = 20
    const val BOARD_X = 90f          // (480 - 10*30) / 2
    const val BOARD_Y = 150f         // leaves room for on-screen touch controls below
    const val BOARD_W = BOARD_COLS * CELL
    const val BOARD_H = BOARD_ROWS * CELL

    val BACKGROUND = rgb(0x10, 0x15, 0x22)
    val BOARD_BG = rgb(0x06, 0x09, 0x12)
    val GRID_LINE = Color(0.16f, 0.19f, 0.27f, 1f)
    val GHOST = Color(1f, 1f, 1f, 0.16f)
    val TEXT = Color(0.92f, 0.94f, 0.98f, 1f)
    val TEXT_DIM = Color(0.6f, 0.64f, 0.72f, 1f)
    val ACCENT = rgb(0x46, 0xC8, 0xB4)
    val BUTTON = Color(0.16f, 0.2f, 0.3f, 1f)
    val BUTTON_DOWN = Color(0.26f, 0.34f, 0.48f, 1f)
    val OVERLAY = Color(0f, 0f, 0f, 0.72f)

    private fun rgb(r: Int, g: Int, b: Int) = Color(r / 255f, g / 255f, b / 255f, 1f)

    fun colorFor(type: PieceType): Color = colorFromHex(type.rgb)

    fun colorForId(id: Int): Color {
        val type = PieceType.fromId(id) ?: return Color.GRAY
        return colorFor(type)
    }

    fun colorFromHex(hex: Int): Color =
        Color(((hex shr 16) and 0xFF) / 255f, ((hex shr 8) and 0xFF) / 255f, (hex and 0xFF) / 255f, 1f)
}
