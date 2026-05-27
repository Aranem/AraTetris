package com.aratetris.engine.agent

import com.aratetris.engine.Action
import com.aratetris.engine.GameStateView

/**
 * A programmatic player. Given the current [GameStateView], return the next [Action] to apply.
 * This is the seam the user's future algorithm plugs into: the rendered game and the headless
 * runner both drive the engine through an agent exactly as they would through human input.
 */
fun interface TetrisAgent {
    fun selectAction(state: GameStateView): Action
}
