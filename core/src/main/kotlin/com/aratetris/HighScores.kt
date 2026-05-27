package com.aratetris

import com.badlogic.gdx.Gdx

/**
 * Offline top-5 high-score list, persisted locally via libGDX [com.badlogic.gdx.Preferences]
 * (a small private key-value file on the device — never synced anywhere). Strictly offline.
 */
class HighScores(private val capacity: Int = 5) {

    private val prefs = Gdx.app.getPreferences("aratetris_scores")
    private val scores = ArrayList<Int>()

    init {
        load()
    }

    private fun load() {
        scores.clear()
        for (i in 0 until capacity) {
            val v = prefs.getInteger("score_$i", -1)
            if (v >= 0) scores.add(v)
        }
        scores.sortDescending()
    }

    private fun save() {
        for (i in 0 until capacity) {
            if (i < scores.size) prefs.putInteger("score_$i", scores[i]) else prefs.remove("score_$i")
        }
        prefs.flush()
    }

    fun list(): List<Int> = scores.toList()

    fun best(): Int = scores.firstOrNull() ?: 0

    /** Insert a finished run's score; returns the 1-based rank if it placed in the top list, else null. */
    fun submit(score: Int): Int? {
        scores.add(score)
        scores.sortDescending()
        while (scores.size > capacity) scores.removeAt(scores.size - 1)
        save()
        val rank = scores.indexOf(score)
        return if (rank in 0 until capacity && scores[rank] == score) rank + 1 else null
    }

    /** Clears the entire list (called only after the user confirms). */
    fun reset() {
        scores.clear()
        save()
    }
}
