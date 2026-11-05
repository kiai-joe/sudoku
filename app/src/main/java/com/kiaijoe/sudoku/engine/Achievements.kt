package com.kiaijoe.sudoku.engine

/** Milestones a player can earn. The order is the order they're listed in. */
enum class Achievement(
    private val titleEn: String,
    private val titleSw: String,
    private val descriptionEn: String,
    private val descriptionSw: String,
) {
    FIRST_STAMP("First stamp", "Muhuri wa kwanza", "Solve any puzzle", "Tatua fumbo lolote"),
    STEADY_HAND("Steady hand", "Mkono thabiti", "Solve a Hard puzzle", "Tatua fumbo la kiwango Ngumu"),
    MASTER("Master", "Gwiji", "Solve an Expert puzzle", "Tatua fumbo la kiwango Bingwa"),
    FULL_LADDER(
        "Full ladder", "Ngazi kamili",
        "Solve a puzzle at every level, Easy to Expert", "Tatua fumbo katika kila kiwango, kuanzia Rahisi hadi Bingwa",
    ),
    ON_YOUR_OWN(
        "On your own", "Peke yako",
        "Solve a Medium or harder puzzle without hints", "Tatua fumbo la Kati au gumu zaidi bila madokezo",
    ),
    CLEAN_INK(
        "Clean ink", "Wino safi",
        "Solve a puzzle with no mistakes and no hints, with mistakes shown",
        "Tatua fumbo bila makosa wala madokezo, makosa yakionyeshwa",
    ),
    SWIFT_BRUSH(
        "Swift brush", "Brashi ya haraka",
        "Solve a Medium or harder puzzle in under 8 minutes", "Tatua fumbo la Kati au gumu zaidi chini ya dakika 8",
    ),
    CAGE_BREAKER("Cage breaker", "Mvunja vizimba", "Solve a Killer Sudoku", "Tatua Killer Sudoku"),
    BLANK_PAGE(
        "Blank page", "Ukurasa mtupu",
        "Solve an Expert Killer, which has no printed numbers", "Tatua Killer ya Bingwa, isiyo na namba zilizochapishwa",
    ),
    REGULAR("Regular", "Mgeni wa kawaida", "Solve 10 daily puzzles", "Tatua mafumbo 10 ya kila siku"),
    WEEK_OF_INK(
        "A week of ink", "Wiki ya wino",
        "Solve the daily puzzle 7 days in a row", "Tatua fumbo la kila siku kwa siku 7 mfululizo",
    ),
    MOON_CYCLE(
        "Moon cycle", "Mzunguko wa mwezi",
        "Solve the daily puzzle 30 days in a row", "Tatua fumbo la kila siku kwa siku 30 mfululizo",
    ),
    ;

    val title: String get() = tr(titleEn, titleSw)

    val description: String get() = tr(descriptionEn, descriptionSw)
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
