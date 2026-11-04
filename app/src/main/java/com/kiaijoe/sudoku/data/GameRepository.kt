package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameCodec
import com.kiaijoe.sudoku.engine.GameState

data class Settings(
    val mistakeLimit: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val highlightSameNumbers: Boolean = true,
)

data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestSeconds: Long? = null,
) {
    val winRate: Int get() = if (played == 0) 0 else (won * 100) / played
}

/** Saves the current game, settings and stats in SharedPreferences. */
class GameRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sudoku", Context.MODE_PRIVATE)

    fun loadGame(): GameState? = prefs.getString(KEY_GAME, null)?.let(GameCodec::decode)

    fun saveGame(state: GameState?) {
        prefs.edit().apply {
            if (state == null) remove(KEY_GAME) else putString(KEY_GAME, GameCodec.encode(state))
        }.apply()
    }

    fun loadSettings() = Settings(
        mistakeLimit = prefs.getBoolean(KEY_MISTAKE_LIMIT, true),
        autoRemoveNotes = prefs.getBoolean(KEY_AUTO_NOTES, true),
        highlightSameNumbers = prefs.getBoolean(KEY_HIGHLIGHT, true),
    )

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putBoolean(KEY_MISTAKE_LIMIT, settings.mistakeLimit)
            .putBoolean(KEY_AUTO_NOTES, settings.autoRemoveNotes)
            .putBoolean(KEY_HIGHLIGHT, settings.highlightSameNumbers)
            .apply()
    }

    fun loadStats(): Map<Difficulty, DifficultyStats> = Difficulty.entries.associateWith { d ->
        val best = prefs.getLong(key(d, "best"), -1L)
        DifficultyStats(
            played = prefs.getInt(key(d, "played"), 0),
            won = prefs.getInt(key(d, "won"), 0),
            bestSeconds = best.takeIf { it >= 0 },
        )
    }

    fun recordStart(difficulty: Difficulty) {
        val played = prefs.getInt(key(difficulty, "played"), 0)
        prefs.edit().putInt(key(difficulty, "played"), played + 1).apply()
    }

    /** Records a win and returns true when it beats the previous best time. */
    fun recordWin(difficulty: Difficulty, seconds: Long): Boolean {
        val won = prefs.getInt(key(difficulty, "won"), 0)
        val best = prefs.getLong(key(difficulty, "best"), -1L)
        val isBest = best < 0 || seconds < best
        prefs.edit().apply {
            putInt(key(difficulty, "won"), won + 1)
            if (isBest) putLong(key(difficulty, "best"), seconds)
        }.apply()
        return isBest
    }

    fun resetStats() {
        prefs.edit().apply {
            Difficulty.entries.forEach { d ->
                remove(key(d, "played")); remove(key(d, "won")); remove(key(d, "best"))
            }
        }.apply()
    }

    private fun key(d: Difficulty, field: String) = "stats_${d.name}_$field"

    private companion object {
        const val KEY_GAME = "current_game"
        const val KEY_MISTAKE_LIMIT = "mistake_limit"
        const val KEY_AUTO_NOTES = "auto_remove_notes"
        const val KEY_HIGHLIGHT = "highlight_same"
    }
}
