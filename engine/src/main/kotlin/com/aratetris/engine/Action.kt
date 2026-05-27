package com.aratetris.engine

/**
 * Every input the engine understands. A human (keyboard/touch) and an algorithm are interchangeable
 * sources of these — they are the single, low-level vocabulary both drive the game through.
 */
enum class Action {
    LEFT,
    RIGHT,
    ROTATE_CW,
    ROTATE_CCW,
    SOFT_DROP,
    HARD_DROP,
    HOLD,
    PAUSE,
    NONE,
}
