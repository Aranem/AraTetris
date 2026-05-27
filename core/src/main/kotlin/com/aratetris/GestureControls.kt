package com.aratetris

import com.badlogic.gdx.math.Rectangle

/**
 * Translates raw touch on the primary pointer into Tetris gestures (virtual coordinates, y-up):
 *  - drag horizontally  -> move left/right, one cell per [STEP] of travel
 *  - drag downward      -> soft drop, one row per [STEP] of travel
 *  - flick upward        -> hard drop (once per gesture)
 *  - quick tap           -> rotate (left half of screen = CCW, right half = CW),
 *                           unless the tap lands on the hold/pause button.
 *
 * Stateful across frames; call [update] once per frame while gesture controls are enabled.
 */
class GestureControls {

    private var active = false
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var time = 0f
    private var hadAction = false
    private var hardDropped = false

    class Result {
        var moveSteps = 0 // >0 right, <0 left
        var softSteps = 0
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
                time = 0f; hadAction = false; hardDropped = false
            } else {
                time += dt
                var dx = x - lastX
                while (dx >= STEP) { r.moveSteps++; lastX += STEP; dx -= STEP; hadAction = true }
                while (dx <= -STEP) { r.moveSteps--; lastX -= STEP; dx += STEP; hadAction = true }
                var down = lastY - y // y-up, so a decrease means the finger moved down
                while (down >= STEP) { r.softSteps++; lastY -= STEP; down -= STEP; hadAction = true }
                if (!hardDropped && (y - startY) >= HARD_DROP) { r.hardDrop = true; hardDropped = true; hadAction = true }
            }
        } else if (active) {
            active = false
            if (!hadAction && time < TAP_TIME) {
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

    fun reset() {
        active = false
        hadAction = false
        hardDropped = false
    }

    companion object {
        private const val STEP = 30f      // virtual px of drag per cell (board cell is 30)
        private const val HARD_DROP = 80f // upward swipe distance that triggers a hard drop
        private const val TAP_TIME = 0.3f // max seconds of contact still counted as a tap (rotate)
    }
}
