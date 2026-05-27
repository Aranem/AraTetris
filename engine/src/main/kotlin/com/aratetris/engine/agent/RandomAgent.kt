package com.aratetris.engine.agent

import com.aratetris.engine.Action
import com.aratetris.engine.GameStateView
import kotlin.random.Random

/**
 * Trivial reference agent: picks a random legal-ish micro-action, biased toward hard drops so games
 * actually progress. Not meant to play well — it exists to exercise and demonstrate the programmatic
 * interface. Replace with the real algorithm later.
 */
class RandomAgent(seed: Long = 0L) : TetrisAgent {
    private val rng = Random(seed)
    private val choices = listOf(
        Action.LEFT, Action.RIGHT, Action.ROTATE_CW,
        Action.SOFT_DROP, Action.HARD_DROP, Action.HARD_DROP,
    )

    override fun selectAction(state: GameStateView): Action = choices[rng.nextInt(choices.size)]
}
