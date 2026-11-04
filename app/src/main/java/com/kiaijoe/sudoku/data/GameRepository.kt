package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameCodec
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.ui.theme.InkTheme

/**
 * @property instantCheck mark wrong numbers as soon as they're placed. When
 *   off, nothing shows whether a number is right until the player taps Check,
 *   and the mistake limit doesn't apply.
 * @property haptics short vibrations when placing numbers and finishing groups.
 * @property theme the ink palette.
 */
data class Settings(
    val mistakeLimit: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val instantCheck: Boolean = true,
    val haptics: Boolean = true,
    val theme: InkTheme = InkTheme.SUMI,
)

data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestSeconds: Long? = null,
) {
    val winRate: Int get() = if (played == 0) 0 else (won * 100) / played
}

/**
 * Saves games, settings and stats in SharedPreferences. A regular game and
 * the daily puzzle are kept in separate slots so neither replaces the other.
 */
class GameRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sudoku", Context.MODE_PRIVATE)

    fun loadGame(): GameState? = prefs.getString(KEY_GAME, null)?.let(GameCodec::decode)

    fun saveGame(state: GameState?) {
        prefs.edit().apply {
            if (state == null) remove(KEY_GAME) else putString(KEY_GAME, GameCodec.encode(state))
        }.apply()
    }

    fun loadDailyGame(): GameState? = prefs.getString(KEY_DAILY_GAME, null)?.let(GameCodec::decode)

    fun saveDailyGame(state: GameState?) {
        prefs.edit().apply {
            if (state == null) remove(KEY_DAILY_GAME) else putString(KEY_DAILY_GAME, GameCodec.encode(state))
        }.apply()
    }

    /** Solved daily puzzles: epoch day to solve time in seconds. */
    fun loadDailyResults(): Map<Long, Long> =
        prefs.getStringSet(KEY_DAILY_RESULTS, emptySet()).orEmpty().mapNotNull { entry ->
            val (day, seconds) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val d = day.toLongOrNull() ?: return@mapNotNull null
            val t = seconds.toLongOrNull() ?: return@mapNotNull null
            d to t
        }.toMap()

    fun recordDailyWin(day: Long, seconds: Long) {
        val results = loadDailyResults().toMutableMap()
        results[day] = minOf(seconds, results[day] ?: Long.MAX_VALUE)
        prefs.edit().putStringSet(KEY_DAILY_RESULTS, results.map { (d, t) -> "$d:$t" }.toSet()).apply()
    }

    /** Earned achievements and the epoch day each was earned. */
    fun loadAchievements(): Map<Achievement, Long> =
        prefs.getStringSet(KEY_ACHIEVEMENTS, emptySet()).orEmpty().mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val achievement = Achievement.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val day = parts[1].toLongOrNull() ?: return@mapNotNull null
            achievement to day
        }.toMap()

    /** Adds [earned] with [day], keeping the first day for any already earned. */
    fun addAchievements(earned: Set<Achievement>, day: Long) {
        val all = loadAchievements().toMutableMap()
        for (a in earned) all.putIfAbsent(a, day)
        prefs.edit().putStringSet(KEY_ACHIEVEMENTS, all.map { (a, d) -> "${a.name}:$d" }.toSet()).apply()
    }

    fun loadSettings() = Settings(
        mistakeLimit = prefs.getBoolean(KEY_MISTAKE_LIMIT, true),
        autoRemoveNotes = prefs.getBoolean(KEY_AUTO_NOTES, true),
        highlightSameNumbers = prefs.getBoolean(KEY_HIGHLIGHT, true),
        instantCheck = prefs.getBoolean(KEY_INSTANT_CHECK, true),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        theme = prefs.getString(KEY_THEME, null)
            ?.let { name -> InkTheme.entries.firstOrNull { it.name == name } }
            ?: InkTheme.SUMI,
    )

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putBoolean(KEY_MISTAKE_LIMIT, settings.mistakeLimit)
            .putBoolean(KEY_AUTO_NOTES, settings.autoRemoveNotes)
            .putBoolean(KEY_HIGHLIGHT, settings.highlightSameNumbers)
            .putBoolean(KEY_INSTANT_CHECK, settings.instantCheck)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putString(KEY_THEME, settings.theme.name)
            .apply()
    }

    fun loadStats(): Map<Difficulty, DifficultyStats> = Difficulty.entries.associateWith { loadStats(it.name) }

    /** Killer games keep their own record, apart from the classic levels. */
    fun loadKillerStats(): DifficultyStats = loadStats(KILLER)

    private fun loadStats(name: String): DifficultyStats {
        val best = prefs.getLong(key(name, "best"), -1L)
        return DifficultyStats(
            played = prefs.getInt(key(name, "played"), 0),
            won = prefs.getInt(key(name, "won"), 0),
            bestSeconds = best.takeIf { it >= 0 },
        )
    }

    fun recordStart(difficulty: Difficulty) = recordStart(difficulty.name)

    fun recordKillerStart() = recordStart(KILLER)

    private fun recordStart(name: String) {
        val played = prefs.getInt(key(name, "played"), 0)
        prefs.edit().putInt(key(name, "played"), played + 1).apply()
    }

    /** Records a win and returns true when it beats the previous best time. */
    fun recordWin(difficulty: Difficulty, seconds: Long): Boolean = recordWin(difficulty.name, seconds)

    fun recordKillerWin(seconds: Long): Boolean = recordWin(KILLER, seconds)

    private fun recordWin(name: String, seconds: Long): Boolean {
        val won = prefs.getInt(key(name, "won"), 0)
        val best = prefs.getLong(key(name, "best"), -1L)
        val isBest = best < 0 || seconds < best
        prefs.edit().apply {
            putInt(key(name, "won"), won + 1)
            if (isBest) putLong(key(name, "best"), seconds)
        }.apply()
        return isBest
    }

    fun resetStats() {
        prefs.edit().apply {
            (Difficulty.entries.map { it.name } + KILLER).forEach { name ->
                remove(key(name, "played")); remove(key(name, "won")); remove(key(name, "best"))
            }
        }.apply()
    }

    private fun key(name: String, field: String) = "stats_${name}_$field"

    private companion object {
        const val KILLER = "KILLER"
        const val KEY_GAME = "current_game"
        const val KEY_DAILY_GAME = "daily_game"
        const val KEY_DAILY_RESULTS = "daily_results"
        const val KEY_ACHIEVEMENTS = "achievements"
        const val KEY_MISTAKE_LIMIT = "mistake_limit"
        const val KEY_AUTO_NOTES = "auto_remove_notes"
        const val KEY_HIGHLIGHT = "highlight_same"
        const val KEY_INSTANT_CHECK = "instant_check"
        const val KEY_HAPTICS = "haptics"
        const val KEY_THEME = "theme"
    }
}
