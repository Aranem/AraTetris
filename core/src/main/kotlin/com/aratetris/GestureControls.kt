package com.aratetris

import com.badlogic.gdx.math.Rectangle
import kotlin.math.abs
import kotlin.math.max

/**
 * Swipe-mode only: translates raw touch on the primary pointer into Tetris gestures (virtual
 * coordinates, y-up). (Button mode and keyboard go through other input paths and are unaffected.)
 *
 *  - drag horizontally  -> move left/right, one cell per [STEP] of travel
 *  - drag downward      -> soft drop, which then keeps repeating while the finger is held down
 *  - flick upward        -> hard drop (once per gesture)
 *  - quick tap           -> rotate (left half of screen = CCW, right half = CW),
 *                           unless the tap lands on the hold/pause button.
 *
 * Each gesture commits to an axis once the finger has travelled [AXIS_COMMIT] from the touch-down
 * point: whichever axis dominated wins. So a horizontal move can't turn into a hard drop, and an
 * upward flick doesn't have to be perfectly straight — sideways drift neither moves the piece nor
 * blocks the drop. The one relaxation is soft drop: once a downward swipe latches it on, left/right
 * steering is allowed too (but hard drop stays disabled for the rest of that gesture). After a hard
 * drop, the gesture is **consumed** and ignored until the finger lifts, so a lingering touch can't
 * act on the next piece.
 *
 * Stateful across frames; call [update] once per frame while gesture controls are enabled.
 */
class GestureControls {

    private enum class Axis { NONE, HORIZONTAL, VERTICAL }

    private var active = false
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var time = 0f
    private var hadAction = false
    private var axis = Axis.NONE
    private var consumed = false // after a hard drop, ignore the rest of the gesture until release
    private var softDropping = false // a downward swipe latched on; soft-drops while the finger is held

    class Result {
        var moveSteps = 0 // >0 right, <0 left
        var softHeld = false // hold the soft drop down this frame (auto-repeats in TetrisGame)
        var hardDrop = false
        var rotateCW = false
        var rotateCCW = false
        var hold = false
        var pause = false
    }

    fun update(
        touched: Boolean,
        x: Float,
        y: Float,
        dt: Float,
        halfWidth: Float,
        holdRect: Rectangle,
        pauseRect: Rectangle,
    ): Result {
        val r = Result()
        if (touched) {
            if (!active) {
                active = true
                startX = x; startY = y; lastX = x; lastY = y
                time = 0f; hadAction = false; axis = Axis.NONE; consumed = false; softDropping = false
            } else if (!consumed) {
                time += dt
                // Commit to one axis once the finger has travelled far enough from the start, then
                // stay on it for the rest of the gesture so the two can't bleed into each other.
                if (axis == Axis.NONE) {
                    val tdx = x - startX
                    val tdy = y - startY
                    if (max(abs(tdx), abs(tdy)) >= AXIS_COMMIT) {
                        axis = if (abs(tdx) > abs(tdy)) Axis.HORIZONTAL else Axis.VERTICAL
                    }
                }
                when (axis) {
                    Axis.HORIZONTAL -> horizontalSteps(x, r)
                    Axis.VERTICAL -> {
                        val up = y - startY // y-up: positive is up, negative means the finger is below start
                        if (!softDropping && up >= HARD_DROP) {
                            // upward flick: hard drop once, then ignore everything until finger lifts
                            r.hardDrop = true; hadAction = true; consumed = true
                        } else if (up < 0f || softDropping) {
                            // downward swipe latches soft drop on; it stays on (auto-repeating) while held.
                            // Hard drop is now disabled for this gesture, but left/right steering is allowed:
                            // re-anchor horizontal tracking on the first latched frame so the drift from the
                            // downward swipe doesn't count as an instant move.
                            if (!softDropping) { softDropping = true; lastX = x }
                            hadAction = true
                            horizontalSteps(x, r)
                        }
                    }
                    Axis.NONE -> {}
                }
                r.softHeld = softDropping
            }
        } else if (active) {
            active = false
            if (!hadAction && !consumed && time < TAP_TIME) {
                when {
                    holdRect.contains(startX, startY) -> r.hold = true
                    pauseRect.contains(startX, startY) -> r.pause = true
                    startX < halfWidth -> r.rotateCCW = true
                    else -> r.rotateCW = true
                }
            }
        }
        return r
    }

    /** Accumulate horizontal finger travel into left/right move steps, one cell per [STEP]. */
    private fun horizontalSteps(x: Float, r: Result) {
        var dx = x - lastX
        while (dx >= STEP) { r.moveSteps++; lastX += STEP; dx -= STEP; hadAction = true }
        while (dx <= -STEP) { r.moveSteps--; lastX -= STEP; dx += STEP; hadAction = true }
    }

    fun reset() {
        active = false
        hadAction = false
        axis = Axis.NONE
        consumed = false
        softDropping = false
    }

    companion object {
        private const val STEP = 30f       // virtual px of drag per cell (board cell is 30)
        private const val AXIS_COMMIT = 20f // travel before a gesture locks to horizontal or vertical
        private const val HARD_DROP = 60f  // upward swipe distance that triggers a hard drop
        private const val TAP_TIME = 0.3f  // max seconds of contact still counted as a tap (rotate)
    }
}
