package com.aratetris

import com.badlogic.gdx.math.Rectangle
import com.aratetris.engine.Action

/** A tappable on-screen control. [repeat] buttons fire continuously while held (move/soft-drop). */
data class TouchButton(val action: Action, val rect: Rectangle, val label: String, val repeat: Boolean)

/** Fixed on-screen control layout (virtual coordinates, y-up) shared by the renderer and input. */
object TouchControls {

    // 3x2 grid mirroring the keyboard: top row = Q/W/E (rotate-L, hard-drop, rotate-R),
    // bottom row = A/S/D (move-L, soft-drop, move-R).
    val gameplay: List<TouchButton> = listOf(
        grid(Action.ROTATE_CCW, col = 0, row = 1, label = "ROT L", repeat = false),
        grid(Action.HARD_DROP, col = 1, row = 1, label = "DROP", repeat = false),
        grid(Action.ROTATE_CW, col = 2, row = 1, label = "ROT R", repeat = false),
        grid(Action.LEFT, col = 0, row = 0, label = "LEFT", repeat = true),
        grid(Action.SOFT_DROP, col = 1, row = 0, label = "SOFT", repeat = true),
        grid(Action.RIGHT, col = 2, row = 0, label = "RIGHT", repeat = true),
    )

    private fun grid(action: Action, col: Int, row: Int, label: String, repeat: Boolean): TouchButton {
        val x = 6f + col * 158f
        val y = if (row == 0) 8f else 80f
        return TouchButton(action, Rectangle(x, y, 152f, 66f), label, repeat)
    }

    // Hold button on the left column; pause button on the right column (touch play only).
    val gameHold = Rectangle(6f, 300f, 76f, 46f)
    val gamePause = Rectangle(398f, 240f, 76f, 46f)

    // Menu screen.
    val menuLevelDown = Rectangle(110f, 430f, 56f, 56f)
    val menuLevelUp = Rectangle(314f, 430f, 56f, 56f)
    val menuToggle = Rectangle(140f, 330f, 200f, 56f)
    val menuStart = Rectangle(140f, 242f, 200f, 60f)
    val menuControls = Rectangle(140f, 170f, 200f, 50f)

    // Controls page.
    val controlsBack = Rectangle(140f, 70f, 200f, 52f)

    // Pause menu.
    val pauseResume = Rectangle(140f, 440f, 200f, 56f)
    val pauseRestart = Rectangle(140f, 360f, 200f, 56f)
    val pauseMainMenu = Rectangle(140f, 280f, 200f, 56f)

    // Game-over screen.
    val gameOverRestart = Rectangle(140f, 250f, 200f, 58f)
    val gameOverReset = Rectangle(140f, 175f, 200f, 52f)

    // Reset confirmation prompt.
    val confirmYes = Rectangle(80f, 320f, 130f, 58f)
    val confirmNo = Rectangle(270f, 320f, 130f, 58f)

    fun hit(rect: Rectangle, x: Float, y: Float): Boolean = rect.contains(x, y)
}
