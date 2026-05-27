package com.aratetris.engine

/**
 * Tiny deterministic SplitMix64 RNG. Holds all of its state in a single Long, so it is trivially
 * copyable by value — essential for `TetrisEngine.copy()` based lookahead/search and for
 * reproducible (seed + action sequence) games.
 */
class Rng(var state: Long) {

    fun copy(): Rng = Rng(state)

    fun nextLong(): Long {
        state += 0x9E3779B97F4A7C15uL.toLong()
        var z = state
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return z xor (z ushr 31)
    }

    /** Uniform-ish int in [0, bound). bound is tiny here (<= 7) so modulo bias is negligible. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        val r = nextLong() ushr 1 // force non-negative
        return (r % bound).toInt()
    }
}
