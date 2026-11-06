package com.kiaijoe.sudoku.engine

/** Milestones a player can earn. The order is the order they're listed in. */
enum class Achievement(
    private val titleEn: String,
    private val titleSw: String,
    private val descriptionEn: String,
    private val descriptionSw: String,
) {
    FIRST_STAMP("First stamp", "Muhuri wa kwanza", "Solve any puzzle", "Tatua fumbo lolote"),
    LIGHTNING("Lightning", "Umeme", "Solve an Easy puzzle in under 3 minutes", "Tatua fumbo Rahisi chini ya dakika 3"),
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
    IRON_WILL(
        "Iron will", "Nia ya chuma",
        "Solve an Expert puzzle with no mistakes and no hints, with mistakes shown",
        "Tatua fumbo la Bingwa bila makosa wala madokezo, makosa yakionyeshwa",
    ),
    BLIND_INK(
        "Blind ink", "Wino gizani",
        "Solve an Expert puzzle with mistakes hidden, without Check or hints",
        "Tatua fumbo la Bingwa makosa yakiwa yamefichwa, bila Kagua wala madokezo",
    ),
    SWIFT_BRUSH(
        "Swift brush", "Brashi ya haraka",
        "Solve a Medium or harder puzzle in under 8 minutes", "Tatua fumbo la Kati au gumu zaidi chini ya dakika 8",
    ),
    SWORDSMITH(
        "Swordsmith", "Mhunzi wa panga",
        "Solve a puzzle that needs a Swordfish or XYZ-Wing", "Tatua fumbo linalohitaji Swordfish au XYZ-Wing",
    ),
    SECOND_WIND(
        "Second wind", "Pumzi mpya",
        "Solve a puzzle after restarting it when you ran out of mistakes",
        "Tatua fumbo baada ya kulianza upya makosa yalipokwisha",
    ),
    HUNDRED_STAMPS("Hundred stamps", "Mihuri mia", "Solve 100 puzzles of any kind", "Tatua mafumbo 100 ya aina yoyote"),
    CAGE_BREAKER("Cage breaker", "Mvunja vizimba", "Solve a Killer Sudoku", "Tatua Killer Sudoku"),
    BLANK_PAGE(
        "Blank page", "Ukurasa mtupu",
        "Solve an Expert Killer, which has no printed numbers", "Tatua Killer ya Bingwa, isiyo na namba zilizochapishwa",
    ),
    FIRST_LESSON("First lesson", "Somo la kwanza", "Finish any lesson", "Maliza somo lolote"),
    PUT_TO_PRACTICE(
        "Put to practice", "Kwa vitendo",
        "Solve a practice puzzle started from a lesson", "Tatua fumbo la mazoezi lililoanzishwa kutoka kwenye somo",
    ),
    SCHOLAR("Scholar", "Msomi", "Finish every lesson", "Maliza masomo yote"),
    REGULAR("Regular", "Mgeni wa kawaida", "Solve 10 daily puzzles", "Tatua mafumbo 10 ya kila siku"),
    WEEK_OF_INK(
        "A week of ink", "Wiki ya wino",
        "Solve the daily puzzle 7 days in a row", "Tatua fumbo la kila siku kwa siku 7 mfululizo",
    ),
    FULL_WEEK(
        "Full week", "Wiki kamili",
        "Solve every daily puzzle from Monday to Sunday in one week",
        "Tatua kila fumbo la kila siku kuanzia Jumatatu hadi Jumapili katika wiki moja",
    ),
    MOON_CYCLE(
        "Moon cycle", "Mzunguko wa mwezi",
        "Solve the daily puzzle 30 days in a row", "Tatua fumbo la kila siku kwa siku 30 mfululizo",
    ),
    KILLER_REGULAR("Killer regular", "Mzoefu wa Killer", "Solve 10 daily Killers", "Tatua Killer 10 za kila siku"),
    CAGED_WEEK(
        "Caged week", "Wiki ya vizimba",
        "Solve the daily Killer 7 days in a row", "Tatua Killer ya kila siku kwa siku 7 mfululizo",
    ),
    TWO_BRUSHES(
        "Two brushes", "Brashi mbili",
        "Solve the daily puzzle and the daily Killer on the same day", "Tatua fumbo la leo na Killer ya leo siku moja",
    ),
    ;

    val title: String get() = tr(titleEn, titleSw)

    val description: String get() = tr(descriptionEn, descriptionSw)
}

/**
 * What a player has done so far, from their saved statistics.
 *
 * @property won classic wins per level (daily puzzles not included).
 * @property killerWon Killer wins, outside the daily puzzles.
 * @property dailySolved daily puzzles solved.
 * @property longestStreak the longest run of daily puzzles solved on consecutive days.
 * @property dailyDays the days whose classic daily was solved.
 * @property killerDailyDays the days whose daily Killer was solved.
 * @property lessonsDone how many lessons are finished.
 * @property variantWon Sudoku X and Jigsaw wins.
 * @property customWon wins on puzzles the player typed in.
 */
data class Progress(
    val won: Map<Difficulty, Int> = emptyMap(),
    val killerWon: Int = 0,
    val dailySolved: Int = 0,
    val longestStreak: Int = 0,
    val dailyDays: Set<Long> = emptySet(),
    val killerDailyDays: Set<Long> = emptySet(),
    val lessonsDone: Int = 0,
    val variantWon: Int = 0,
    val customWon: Int = 0,
)

object Achievements {
    private const val SWIFT_SECONDS = 8 * 60
    private const val LIGHTNING_SECONDS = 3 * 60
    private val EXPERT_TECHNIQUES = setOf(Technique.SWORDFISH, Technique.XYZ_WING, Technique.HIDDEN_QUAD)

    /**
     * Every achievement [progress] and, if given, the game just [won] earn.
     * [mistakesShown] is whether mistakes were marked as they happened;
     * without it a mistake-free game proves nothing, so Clean ink needs it.
     */
    fun earned(progress: Progress, won: GameState? = null, mistakesShown: Boolean = true): Set<Achievement> {
        val out = HashSet<Achievement>()
        val classicLevels = progress.won.filterValues { it > 0 }.keys.toMutableSet()

        val anySolved = classicLevels.isNotEmpty() || progress.killerWon > 0 || progress.dailySolved > 0 ||
            progress.killerDailyDays.isNotEmpty() || progress.variantWon > 0 || progress.customWon > 0
        if (anySolved) out += Achievement.FIRST_STAMP
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
            if (level == Difficulty.EASY && !won.isKiller && won.elapsedSeconds < LIGHTNING_SECONDS) out += Achievement.LIGHTNING
            if (level == Difficulty.EXPERT && won.hintsUsed == 0) {
                if (mistakesShown && won.mistakes == 0) out += Achievement.IRON_WILL
                if (!mistakesShown && won.checks == 0) out += Achievement.BLIND_INK
            }
            if (won.hardest in EXPERT_TECHNIQUES) out += Achievement.SWORDSMITH
            if (won.afterLoss) out += Achievement.SECOND_WIND
            if (won.practice != null) out += Achievement.PUT_TO_PRACTICE
        }

        if (Difficulty.HARD in classicLevels) out += Achievement.STEADY_HAND
        if (Difficulty.EXPERT in classicLevels) out += Achievement.MASTER
        if (classicLevels.containsAll(Difficulty.entries)) out += Achievement.FULL_LADDER
        if (progress.dailySolved >= 10) out += Achievement.REGULAR
        if (progress.longestStreak >= 7) out += Achievement.WEEK_OF_INK
        if (progress.longestStreak >= 30) out += Achievement.MOON_CYCLE

        val solved = progress.won.values.sum() + progress.killerWon + progress.variantWon + progress.customWon +
            progress.dailyDays.size + progress.killerDailyDays.size
        if (solved >= 100) out += Achievement.HUNDRED_STAMPS
        if (progress.lessonsDone >= 1) out += Achievement.FIRST_LESSON
        if (progress.lessonsDone >= Tutorial.lessons.size) out += Achievement.SCHOLAR
        if (progress.killerDailyDays.size >= 10) out += Achievement.KILLER_REGULAR
        if (Daily.longestStreak(progress.killerDailyDays) >= 7) out += Achievement.CAGED_WEEK
        if (progress.dailyDays.any { it in progress.killerDailyDays }) out += Achievement.TWO_BRUSHES
        if (hasFullWeek(progress.dailyDays)) out += Achievement.FULL_WEEK
        return out
    }

    /** Whether [days] holds every day of some Monday-to-Sunday week. */
    private fun hasFullWeek(days: Set<Long>): Boolean = days.any { day ->
        java.time.LocalDate.ofEpochDay(day).dayOfWeek == java.time.DayOfWeek.MONDAY && (1L..6L).all { day + it in days }
    }
}
