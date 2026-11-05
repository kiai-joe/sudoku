package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameCodec
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.ui.theme.DarkMode
import com.kiaijoe.sudoku.ui.theme.InkTheme

/**
 * @property instantCheck mark wrong numbers as soon as they're placed. When
 *   off, nothing shows whether a number is right until the player taps Check,
 *   and the mistake limit doesn't apply.
 * @property haptics short vibrations when placing numbers and finishing groups.
 * @property theme the ink palette.
 * @property largeNumbers bigger digits on the board and number pad.
 * @property darkMode light, dark, or following the phone.
 */
data class Settings(
    val mistakeLimit: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val instantCheck: Boolean = true,
    val haptics: Boolean = true,
    val theme: InkTheme = InkTheme.SUMI,
    val largeNumbers: Boolean = false,
    val darkMode: DarkMode = DarkMode.SYSTEM,
)

/**
 * @property timedWins wins recorded with their time and hints, which began
 *   after [won] did, so the average and the no-hint share come from these.
 * @property totalSeconds the solve times of [timedWins] added up.
 * @property noHintWins how many of [timedWins] used no hints.
 */
data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestSeconds: Long? = null,
    val timedWins: Int = 0,
    val totalSeconds: Long = 0,
    val noHintWins: Int = 0,
) {
    val winRate: Int get() = if (played == 0) 0 else (won * 100) / played

    /** Average solve time, or null before any timed win. */
    val averageSeconds: Long? get() = if (timedWins == 0) null else totalSeconds / timedWins

    /** Share of timed wins solved without a hint, in percent, or null before any. */
    val noHintRate: Int? get() = if (timedWins == 0) null else (noHintWins * 100) / timedWins
}

/**
 * Saves games, settings and stats in SharedPreferences. A regular game and
 * the two daily puzzles are kept in separate slots so none replaces another.
 */
class GameRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Everything Sumi keeps, as a backup file's text. See [Backup]. */
    fun exportBackup(): String = Backup.export(prefs)

    /**
     * Replaces everything with the backup in [text], keeping what was there if
     * any of it can't be read back. Returns how many entries were restored.
     */
    fun restoreBackup(text: String): Int = Backup.restore(prefs, text) {
        loadGame()
        loadDailyGame()
        loadDailyResults()
        loadDailyGame(killer = true)
        loadDailyResults(killer = true)
        loadAchievements()
        loadSettings()
        loadStats()
        loadKillerStats()
        loadLessonsDone()
    }

    fun loadGame(): GameState? = prefs.getString(KEY_GAME, null)?.let(GameCodec::decode)

    fun saveGame(state: GameState?) {
        prefs.edit().apply {
            if (state == null) remove(KEY_GAME) else putString(KEY_GAME, GameCodec.encode(state))
        }.apply()
    }

    /** The daily game in progress, classic or (when [killer]) Killer; each has its own slot. */
    fun loadDailyGame(killer: Boolean = false): GameState? =
        prefs.getString(dailyGameKey(killer), null)?.let(GameCodec::decode)

    /** Saves [state] to its daily slot (the Killer one for a Killer game), or clears [killer]'s slot when null. */
    fun saveDailyGame(state: GameState?, killer: Boolean = state?.isKiller == true) {
        val key = dailyGameKey(killer)
        prefs.edit().apply {
            if (state == null) remove(key) else putString(key, GameCodec.encode(state))
        }.apply()
    }

    private fun dailyGameKey(killer: Boolean) = if (killer) KEY_KILLER_DAILY_GAME else KEY_DAILY_GAME

    private fun dailyResultsKey(killer: Boolean) = if (killer) KEY_KILLER_DAILY_RESULTS else KEY_DAILY_RESULTS

    /** Solved daily puzzles, classic or Killer: epoch day to solve time in seconds. */
    fun loadDailyResults(killer: Boolean = false): Map<Long, Long> =
        prefs.getStringSet(dailyResultsKey(killer), emptySet()).orEmpty().mapNotNull { entry ->
            val (day, seconds) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val d = day.toLongOrNull() ?: return@mapNotNull null
            val t = seconds.toLongOrNull() ?: return@mapNotNull null
            d to t
        }.toMap()

    fun recordDailyWin(day: Long, seconds: Long, killer: Boolean = false) {
        val results = loadDailyResults(killer).toMutableMap()
        results[day] = minOf(seconds, results[day] ?: Long.MAX_VALUE)
        prefs.edit().putStringSet(dailyResultsKey(killer), results.map { (d, t) -> "$d:$t" }.toSet()).apply()
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
        largeNumbers = prefs.getBoolean(KEY_LARGE_NUMBERS, false),
        darkMode = prefs.getString(KEY_DARK_MODE, null)
            ?.let { name -> DarkMode.entries.firstOrNull { it.name == name } }
            ?: DarkMode.SYSTEM,
    )

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putBoolean(KEY_MISTAKE_LIMIT, settings.mistakeLimit)
            .putBoolean(KEY_AUTO_NOTES, settings.autoRemoveNotes)
            .putBoolean(KEY_HIGHLIGHT, settings.highlightSameNumbers)
            .putBoolean(KEY_INSTANT_CHECK, settings.instantCheck)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putString(KEY_THEME, settings.theme.name)
            .putBoolean(KEY_LARGE_NUMBERS, settings.largeNumbers)
            .putString(KEY_DARK_MODE, settings.darkMode.name)
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
            timedWins = prefs.getInt(key(name, "timed"), 0),
            totalSeconds = prefs.getLong(key(name, "total"), 0L),
            noHintWins = prefs.getInt(key(name, "nohint"), 0),
        )
    }

    fun recordStart(difficulty: Difficulty) = recordStart(difficulty.name)

    fun recordKillerStart() = recordStart(KILLER)

    private fun recordStart(name: String) {
        val played = prefs.getInt(key(name, "played"), 0)
        prefs.edit().putInt(key(name, "played"), played + 1).apply()
    }

    /** Records a win and returns true when it beats the previous best time. */
    fun recordWin(difficulty: Difficulty, seconds: Long, hints: Int): Boolean = recordWin(difficulty.name, seconds, hints)

    fun recordKillerWin(seconds: Long, hints: Int): Boolean = recordWin(KILLER, seconds, hints)

    private fun recordWin(name: String, seconds: Long, hints: Int): Boolean {
        val s = loadStats(name)
        val isBest = s.bestSeconds == null || seconds < s.bestSeconds
        prefs.edit().apply {
            putInt(key(name, "won"), s.won + 1)
            if (isBest) putLong(key(name, "best"), seconds)
            putInt(key(name, "timed"), s.timedWins + 1)
            putLong(key(name, "total"), s.totalSeconds + seconds)
            putInt(key(name, "nohint"), s.noHintWins + if (hints == 0) 1 else 0)
        }.apply()
        return isBest
    }

    /** Lessons finished, by technique name. */
    fun loadLessonsDone(): Set<String> = prefs.getStringSet(KEY_LESSONS_DONE, emptySet()).orEmpty().toSet()

    fun markLessonDone(technique: String) {
        prefs.edit().putStringSet(KEY_LESSONS_DONE, loadLessonsDone() + technique).apply()
    }

    fun resetStats() {
        prefs.edit().apply {
            (Difficulty.entries.map { it.name } + KILLER).forEach { name ->
                for (field in STAT_FIELDS) remove(key(name, field))
            }
        }.apply()
    }

    private fun key(name: String, field: String) = "stats_${name}_$field"

    companion object {
        /** The one preferences file that holds everything; the backup rules in res/xml name it. */
        const val PREFS_NAME = "sudoku"
        private const val KILLER = "KILLER"
        private val STAT_FIELDS = listOf("played", "won", "best", "timed", "total", "nohint")
        private const val KEY_LESSONS_DONE = "lessons_done"
        private const val KEY_GAME = "current_game"
        private const val KEY_DAILY_GAME = "daily_game"
        private const val KEY_DAILY_RESULTS = "daily_results"
        private const val KEY_KILLER_DAILY_GAME = "killer_daily_game"
        private const val KEY_KILLER_DAILY_RESULTS = "killer_daily_results"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_MISTAKE_LIMIT = "mistake_limit"
        private const val KEY_AUTO_NOTES = "auto_remove_notes"
        private const val KEY_HIGHLIGHT = "highlight_same"
        private const val KEY_INSTANT_CHECK = "instant_check"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_THEME = "theme"
        private const val KEY_LARGE_NUMBERS = "large_numbers"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
