package com.aratetris.engine

/**
 * Standard 7-bag randomizer: each "bag" is a shuffled permutation of all seven pieces, so every
 * piece appears exactly once per seven spawns. Deterministic given its [Rng].
 */
class Bag(private val rng: Rng) {

    private val queue = ArrayDeque<PieceType>()

    private fun refill() {
        val pieces = PieceType.entries.toMutableList()
        // Fisher-Yates using the engine RNG so the order is reproducible.
        for (i in pieces.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = pieces[i]; pieces[i] = pieces[j]; pieces[j] = tmp
        }
        queue.addAll(pieces)
    }

    /** The next [n] upcoming pieces without consuming them. */
    fun peek(n: Int): List<PieceType> {
        while (queue.size < n) refill()
        return queue.take(n)
    }

    /** Consume and return the next piece. */
    fun next(): PieceType {
        if (queue.isEmpty()) refill()
        return queue.removeFirst()
    }

    /** Deep copy sharing the supplied (already-copied) RNG, preserving the pending queue. */
    fun copyWith(rngCopy: Rng): Bag {
        val b = Bag(rngCopy)
        b.queue.addAll(queue)
        return b
    }
}
