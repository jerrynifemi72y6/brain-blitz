package com.jerrynifemi.brainblitz

import android.content.Context

/** Tiny SharedPreferences wrapper for high scores. */
object Scores {

    private const val PREFS = "brain_blitz_scores"

    fun best(context: Context, key: String): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("best_$key", 0)

    /** Returns true when this run set a new personal best. */
    fun record(context: Context, key: String, score: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt("best_$key", 0)
        if (score > current) {
            prefs.edit().putInt("best_$key", score).apply()
            return true
        }
        return false
    }
}
