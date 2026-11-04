package com.kiaijoe.sudoku.engine

/** Milestones a player can earn. The order is the order they're listed in. */
enum class Achievement(val title: String, val description: String) {
    FIRST_STAMP("First stamp", "Solve any puzzle"),
    STEADY_HAND("Steady hand", "Solve a Hard puzzle"),
    MASTER("Master", "Solve an Expert puzzle"),
    FULL_LADDER("Full ladder", "Solve a puzzle at every level, Easy to Expert"),
    ON_YOUR_OWN("On your own", "Solve a Medium or harder puzzle without hints"),
    CLEAN_INK("Clean ink", "Solve a puzzle with no mistakes and no hints, with mistakes shown"),
    SWIFT_BRUSH("Swift brush", "Solve a Medium or harder puzzle in under 8 minutes"),
    CAGE_BREAKER("Cage breaker", "Solve a Killer Sudoku"),
    BLANK_PAGE("Blank page", "Solve an Expert Killer, which has no printed numbers"),
    REGULAR("Regular", "Solve 10 daily puzzles"),
    WEEK_OF_INK("A week of ink", "Solve the daily puzzle 7 days in a row"),
    MOON_CYCLE("Moon cycle", "Solve the daily puzzle 30 days in a row"),
}

/**
 * What a player has done so far, from their saved statistics.
 *
 * @property won classic wins per level (daily puzzles not included).
 * @property killerWon Killer wins, outside the daily puzzle.
 * @property dailySolved daily puzzles solved.
 * @property longestStreak the longest run of daily puzzles solved on consecutive days.
 */
data class Progress(
    val won: Map<Difficulty, Int> = emptyMap(),
    val killerWon: Int = 0,
    val dailySolved: Int = 0,
    val longestStreak: Int = 0,
)

object Achievements {
    private const val SWIFT_SECONDS = 8 * 60

    /**
     * Every achievement [progress] and, if given, the game just [won] earn.
     * [mistakesShown] is whether mistakes were marked as they happened;
     * without it a mistake-free game proves nothing, so Clean ink needs it.
     */
    fun earned(progress: Progress, won: GameState? = null, mistakesShown: Boolean = true): Set<Achievement> {
        val out = HashSet<Achievement>()
        val classicLevels = progress.won.filterValues { it > 0 }.keys.toMutableSet()

        if (classicLevels.isNotEmpty() || progress.killerWon > 0 || progress.dailySolved > 0) out += Achievement.FIRST_STAMP
        if (progress.killerWon > 0) out += Achievement.CAGE_BREAKER

        if (won != null && won.status == GameStatus.WON) {
            out += Achievement.FIRST_STAMP
            val level = won.difficulty
            if (won.isKiller) {
                out += Achievement.CAGE_BREAKER
                if (level == Difficulty.EXPERT) out += Achievement.BLANK_PAGE
            } else {
                classicLevels += level
            }
            if (level >= Difficulty.MEDIUM && won.hintsUsed == 0) out += Achievement.ON_YOUR_OWN
            if (mistakesShown && won.mistakes == 0 && won.hintsUsed == 0) out += Achievement.CLEAN_INK
            if (level >= Difficulty.MEDIUM && won.elapsedSeconds < SWIFT_SECONDS) out += Achievement.SWIFT_BRUSH
        }

        if (Difficulty.HARD in classicLevels) out += Achievement.STEADY_HAND
        if (Difficulty.EXPERT in classicLevels) out += Achievement.MASTER
        if (classicLevels.containsAll(Difficulty.entries)) out += Achievement.FULL_LADDER
        if (progress.dailySolved >= 10) out += Achievement.REGULAR
        if (progress.longestStreak >= 7) out += Achievement.WEEK_OF_INK
        if (progress.longestStreak >= 30) out += Achievement.MOON_CYCLE
        return out
    }
}
