package com.aratetris

import com.badlogic.gdx.math.Rectangle
import com.aratetris.engine.Action

/** A tappable on-screen control. [repeat] buttons fire continuously while held (move/soft-drop). */
data class TouchButton(val action: Action, val rect: Rectangle, val label: String, val repeat: Boolean)

/** Fixed on-screen control layout (virtual coordinates, y-up) shared by the renderer and input. */
object TouchControls {

    val gameplay: List<TouchButton> = listOf(
        button(Action.LEFT, 0, "<", repeat = true),
        button(Action.RIGHT, 1, ">", repeat = true),
        button(Action.ROTATE_CW, 2, "ROT", repeat = false),
        button(Action.SOFT_DROP, 3, "DOWN", repeat = true),
        button(Action.HARD_DROP, 4, "DROP", repeat = false),
        button(Action.HOLD, 5, "HOLD", repeat = false),
    )

    private fun button(action: Action, slot: Int, label: String, repeat: Boolean) =
        TouchButton(action, Rectangle(3f + slot * 80f, 25f, 74f, 95f), label, repeat)

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
