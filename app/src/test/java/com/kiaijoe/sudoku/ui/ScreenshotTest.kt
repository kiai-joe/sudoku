package com.kiaijoe.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.HintPlan
import com.kiaijoe.sudoku.engine.Hints
import com.kiaijoe.sudoku.engine.Killer
import com.kiaijoe.sudoku.engine.Technique
import com.kiaijoe.sudoku.engine.Tutorial
import com.kiaijoe.sudoku.ui.screens.CalendarScreen
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.screens.HomeScreen
import com.kiaijoe.sudoku.ui.screens.LearnScreen
import com.kiaijoe.sudoku.ui.screens.LessonScreen
import com.kiaijoe.sudoku.ui.screens.StatsScreen
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.random.Random

/**
 * Renders the main screens to PNGs in src/test/screenshots.
 *
 * Run `./gradlew recordRoborazziDebug` to update them. CI does this on every
 * pull request and commits any changes, so they show up as image diffs.
 * Each screen is drawn from a fixed, seeded game state so runs match.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(
        name: String,
        dark: Boolean,
        theme: InkTheme = InkTheme.SUMI,
        content: @Composable () -> Unit,
    ) {
        compose.setContent { SudokuTheme(darkTheme = dark, theme = theme) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    // --- Home -------------------------------------------------------------

    @Test fun homeLight() = home(dark = false)

    @Test fun homeDark() = home(dark = true)

    private fun home(dark: Boolean) = capture(if (dark) "home_dark" else "home_light", dark) {
        HomeScreen(
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily, stats = Fixtures.stats) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onStats = {},
            onLearn = {},
            onSettingsChange = {},
        )
    }

    // --- Other ink palettes ----------------------------------------------------

    @Test
    fun homeIndigoNight() = capture("home_indigo_night", dark = false, theme = InkTheme.INDIGO_NIGHT) {
        HomeScreen(
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onStats = {},
            onLearn = {},
            onSettingsChange = {},
        )
    }

    @Test fun gameIndigoNight() = themedGame("game_indigo_night", dark = false, theme = InkTheme.INDIGO_NIGHT)

    @Test fun gameBambooLight() = themedGame("game_bamboo_light", dark = false, theme = InkTheme.BAMBOO)

    @Test fun gameBambooDark() = themedGame("game_bamboo_dark", dark = true, theme = InkTheme.BAMBOO)

    private fun themedGame(name: String, dark: Boolean, theme: InkTheme) = capture(name, dark, theme) {
        val g = Fixtures.midGame()
        GameScreen(
            state = UiState(game = g, selected = Fixtures.selectedCell(g)),
            actions = GameActions.None,
            onExit = {},
        )
    }

    // --- Game in progress --------------------------------------------------

    @Test fun gameLight() = game(dark = false)

    @Test fun gameDark() = game(dark = true)

    private fun game(dark: Boolean) = capture(if (dark) "game_dark" else "game_light", dark) {
        val g = Fixtures.midGame()
        GameScreen(
            state = UiState(game = g, selected = Fixtures.selectedCell(g)),
            actions = GameActions.None,
            onExit = {},
        )
    }

    /** "Show mistakes right away" off, after tapping Check: the wrong number is flagged. */
    @Test
    fun checkModeLight() = capture("game_check_light", dark = false) {
        val g = Fixtures.midGame()
        val wrong = (0 until 81).filter { g.isWrong(it) }.toSet()
        GameScreen(
            state = UiState(
                game = g,
                settings = Settings(instantCheck = false),
                flagged = wrong,
                checks = 1,
                checkMessage = "1 number is wrong. It's marked in red.",
            ),
            actions = GameActions.None,
            onExit = {},
        )
    }

    // --- Killer Sudoku -------------------------------------------------------

    @Test fun killerLight() = killer(dark = false)

    @Test fun killerDark() = killer(dark = true)

    private fun killer(dark: Boolean) = capture(if (dark) "killer_dark" else "killer_light", dark) {
        val g = Fixtures.killerGame()
        GameScreen(
            state = UiState(game = g, selected = (0 until 81).first { g.values[it] == 0 }),
            actions = GameActions.None,
            onExit = {},
        )
    }

    /** A Killer hint explaining a cage. */
    @Test
    fun killerHintLight() = capture("killer_hint_light", dark = false) {
        val (g, plan) = Fixtures.killerCageHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan)), actions = GameActions.None, onExit = {})
    }

    // --- Hints -------------------------------------------------------------

    @Test fun hintLight() = hint(dark = false)

    @Test fun hintDark() = hint(dark = true)

    private fun hint(dark: Boolean) = capture(if (dark) "hint_dark" else "hint_light", dark) {
        val (g, plan) = Fixtures.eliminationHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan)), actions = GameActions.None, onExit = {})
    }

    @Test
    fun hintMistake() = capture("hint_mistake_light", dark = false) {
        val g = Fixtures.midGame()
        GameScreen(state = UiState(game = g, hint = HintUi(Hints.forGame(g))), actions = GameActions.None, onExit = {})
    }

    // --- Win ---------------------------------------------------------------

    @Test fun winLight() = win(dark = false)

    @Test fun winDark() = win(dark = true)

    private fun win(dark: Boolean) = capture(if (dark) "win_dark" else "win_light", dark) {
        GameScreen(
            state = UiState(game = Fixtures.wonGame(), lastWinWasBest = true, stats = Fixtures.stats),
            actions = GameActions.None,
            onExit = {},
        )
    }

    // --- Daily calendar ------------------------------------------------------

    @Test fun calendarLight() = calendar(dark = false)

    @Test fun calendarDark() = calendar(dark = true)

    private fun calendar(dark: Boolean) = capture(if (dark) "calendar_dark" else "calendar_light", dark) {
        CalendarScreen(daily = Fixtures.daily, onBack = {})
    }

    /** The whole daily page, including the weekday chart below the fold, on a tall screen. */
    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun calendarFullLight() = capture("calendar_full_light", dark = false) {
        CalendarScreen(daily = Fixtures.daily, onBack = {})
    }

    // --- Learn ---------------------------------------------------------------

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun learnLight() = capture("learn_light", dark = false) {
        LearnScreen(onOpen = {}, onBack = {})
    }

    /** The X-Wing lesson at its first step, with the example built up front. */
    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun lessonXWingLight() = capture("lesson_xwing_light", dark = false) {
        val index = Tutorial.lessons.indexOfFirst { it.technique == Technique.X_WING }
        LessonScreen(
            index = index,
            example = Tutorial.example(Tutorial.lessons[index]),
            loading = false,
            onNextLesson = {},
            onBack = {},
        )
    }

    /** The 45 rule lesson, opened at its 45-rule step. */
    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun lessonFortyFiveLight() = capture("lesson_45_light", dark = false) {
        val index = Tutorial.lessons.indexOfFirst { it.technique == Technique.FORTY_FIVE }
        LessonScreen(
            index = index,
            example = Tutorial.example(Tutorial.lessons[index]),
            loading = false,
            onNextLesson = null,
            onBack = {},
        )
    }

    /** The whole home screen, including the Killer levels below the fold. */
    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun homeFullLight() = capture("home_full_light", dark = false) {
        HomeScreen(
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onStats = {},
            onLearn = {},
            onSettingsChange = {},
        )
    }

    // --- Stats -------------------------------------------------------------

    @Test
    fun statsLight() = capture("stats_light", dark = false) {
        StatsScreen(stats = Fixtures.stats, onReset = {}, onBack = {})
    }

    /** The whole statistics page with some achievements earned. */
    @Test
    @Config(qualifiers = "w411dp-h1700dp-xxhdpi")
    fun statsFullLight() = capture("stats_full_light", dark = false) {
        StatsScreen(
            stats = Fixtures.stats,
            killerStats = DifficultyStats(played = 3, won = 2, bestSeconds = 640),
            achievements = Fixtures.achievements,
            onReset = {},
            onBack = {},
        )
    }

    /** A win that earns two achievements. */
    @Test
    fun winAchievementLight() = capture("win_achievement_light", dark = false) {
        GameScreen(
            state = UiState(
                game = Fixtures.wonGame(),
                lastWinWasBest = true,
                stats = Fixtures.stats,
                newAchievements = listOf(Achievement.STEADY_HAND, Achievement.ON_YOUR_OWN),
            ),
            actions = GameActions.None,
            onExit = {},
        )
    }
}

/** Fixed game states for screenshots, all from seeded puzzles. */
private object Fixtures {

    val stats = mapOf(
        Difficulty.EASY to DifficultyStats(played = 14, won = 13, bestSeconds = 192),
        Difficulty.MEDIUM to DifficultyStats(played = 9, won = 7, bestSeconds = 431),
        Difficulty.HARD to DifficultyStats(played = 4, won = 2, bestSeconds = 905),
        Difficulty.EXPERT to DifficultyStats(played = 1, won = 0),
    )

    /** Thursday 5 November 2026: Monday to Wednesday solved, plus a run in October. */
    val daily: DailyUi = run {
        val today = LocalDate.parse("2026-11-05").toEpochDay()
        val solved = listOf(1L, 2L, 3L) + (6L..14L) + listOf(18L, 22L, 23L)
        DailyUi(today = today, results = solved.associate { (today - it) to 300L + it * 17 })
    }

    /** A few achievements earned in late October and early November 2026. */
    val achievements: Map<Achievement, Long> = mapOf(
        Achievement.FIRST_STAMP to LocalDate.parse("2026-10-12").toEpochDay(),
        Achievement.STEADY_HAND to LocalDate.parse("2026-10-20").toEpochDay(),
        Achievement.CAGE_BREAKER to LocalDate.parse("2026-11-01").toEpochDay(),
        Achievement.WEEK_OF_INK to LocalDate.parse("2026-10-25").toEpochDay(),
        Achievement.REGULAR to LocalDate.parse("2026-11-03").toEpochDay(),
    )

    /** A Medium game partway through: some numbers placed, pencil marks, and one mistake. */
    fun midGame(): GameState {
        val puzzle = Generator.generate(Difficulty.MEDIUM, Random(7))
        var g = GameState.from(puzzle)
        repeat(14) { g = placeNextHint(g) }
        val empty = (0 until 81).filter { g.values[it] == 0 }
        // Pencil marks in two cells, each including the right answer.
        for (cell in empty.take(2)) {
            val right = puzzle.solution[cell]
            val other = (1..9).first { it != right && it != (right % 9) + 1 }
            g = g.toggleNote(cell, right).toggleNote(cell, other)
        }
        val wrongCell = empty[4]
        g = g.place(wrongCell, (1..9).first { it != puzzle.solution[wrongCell] }, mistakeLimit = 3, autoRemoveNotes = true)
        return g.copy(elapsedSeconds = 252)
    }

    fun selectedCell(g: GameState) = (0 until 81).filter { g.values[it] == 0 }[6]

    /** A Hard game at the point where the next hint rules out candidates, with that hint. */
    fun eliminationHint(): Pair<GameState, HintPlan> {
        var g = GameState.from(Generator.generate(Difficulty.HARD, Random(3)))
        repeat(81) {
            val plan = Hints.forGame(g)
            if (plan is HintPlan.Teach && !plan.cards.first().isPlacement) return g.copy(elapsedSeconds = 517) to plan
            g = placeNextHint(g)
        }
        error("no elimination hint found")
    }

    /** A Killer game a few moves in. */
    fun killerGame(): GameState {
        var g = GameState.from(Killer.generate(Difficulty.MEDIUM, Random(11)))
        repeat(6) { g = placeNextHint(g) }
        return g.copy(elapsedSeconds = 184)
    }

    /** A Killer game at the point where the next hint is a cage combination. */
    fun killerCageHint(): Pair<GameState, HintPlan> {
        var g = GameState.from(Killer.generate(Difficulty.MEDIUM, Random(11)))
        repeat(81) {
            val plan = Hints.forGame(g)
            if (plan is HintPlan.Teach && plan.cards.first().technique == Technique.CAGE && it >= 4) {
                return g.copy(elapsedSeconds = 241) to plan
            }
            g = placeNextHint(g)
        }
        error("no cage hint found")
    }

    fun wonGame(): GameState {
        val puzzle = Generator.generate(Difficulty.HARD, Random(5))
        return GameState.from(puzzle).copy(
            values = puzzle.solution.toList(),
            status = GameStatus.WON,
            elapsedSeconds = 611,
            mistakes = 1,
            hintsUsed = 2,
            hardest = puzzle.hardest ?: Technique.X_WING,
        )
    }

    private fun placeNextHint(g: GameState): GameState {
        val cards = (Hints.forGame(g) as HintPlan.Teach).cards
        val last = cards.last()
        return g.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
    }
}
