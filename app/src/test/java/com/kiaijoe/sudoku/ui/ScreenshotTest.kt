package com.kiaijoe.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Achievements
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.HintPlan
import com.kiaijoe.sudoku.engine.Lang
import com.kiaijoe.sudoku.engine.Hints
import com.kiaijoe.sudoku.engine.Killer
import com.kiaijoe.sudoku.engine.Progress
import com.kiaijoe.sudoku.engine.Technique
import com.kiaijoe.sudoku.engine.Tutorial
import com.kiaijoe.sudoku.engine.Variant
import com.kiaijoe.sudoku.ui.screens.CalendarScreen
import com.kiaijoe.sudoku.ui.screens.CombinationsScreen
import com.kiaijoe.sudoku.ui.screens.EnterPuzzleScreen
import com.kiaijoe.sudoku.ui.screens.ReplayScreen
import com.kiaijoe.sudoku.ui.screens.boardAt
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.screens.HomeScreen
import com.kiaijoe.sudoku.ui.screens.LearnScreen
import com.kiaijoe.sudoku.ui.screens.LessonScreen
import com.kiaijoe.sudoku.ui.screens.SettingsScreen
import com.kiaijoe.sudoku.ui.screens.StatsScreen
import com.kiaijoe.sudoku.ui.theme.DarkMode
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.Locale
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
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, stats = Fixtures.stats) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onSettings = {},
            onTab = {},
        )
    }

    // --- Other ink palettes ----------------------------------------------------

    @Test
    fun homeIndigoNight() = capture("home_indigo_night", dark = false, theme = InkTheme.INDIGO_NIGHT) {
        HomeScreen(
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily, killerDaily = Fixtures.killerDaily) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onSettings = {},
            onTab = {},
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
        val g = Fixtures.midGame().copy(checks = 1)
        val wrong = (0 until 81).filter { g.isWrong(it) }.toSet()
        GameScreen(
            state = UiState(
                game = g,
                settings = Settings(instantCheck = false),
                flagged = wrong,
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

    // --- Sudoku X, Jigsaw, Zen and a typed-in puzzle ------------------------

    @Test
    fun sudokuXLight() = capture("sudoku_x_light", dark = false) {
        val g = Fixtures.variantGame(Variant.X)
        GameScreen(state = UiState(game = g, selected = Fixtures.selectedCell(g)), actions = GameActions.None, onExit = {})
    }

    @Test fun jigsawLight() = jigsaw(dark = false)

    @Test fun jigsawDark() = jigsaw(dark = true)

    private fun jigsaw(dark: Boolean) = capture(if (dark) "jigsaw_dark" else "jigsaw_light", dark) {
        val g = Fixtures.variantGame(Variant.JIGSAW)
        GameScreen(state = UiState(game = g, selected = Fixtures.selectedCell(g)), actions = GameActions.None, onExit = {})
    }

    /** Zen mode: no timer and no mistake count. */
    @Test
    fun zenLight() = capture("game_zen_light", dark = false) {
        val g = Fixtures.midGame()
        GameScreen(
            state = UiState(game = g, selected = Fixtures.selectedCell(g), settings = Settings(zen = true)),
            actions = GameActions.None,
            onExit = {},
        )
    }

    /** Typing in a puzzle, with a repeated number marked. */
    @Test
    fun enterPuzzleLight() = capture("enter_puzzle_light", dark = false) {
        EnterPuzzleScreen(onBack = {}, onPlay = {}, replacesGame = true, initial = Fixtures.typedIn(), initialSelected = 40)
    }

    @Test
    fun enterPuzzleDark() = capture("enter_puzzle_dark", dark = true) {
        EnterPuzzleScreen(onBack = {}, onPlay = {}, replacesGame = false, initial = Fixtures.typedIn(), initialSelected = 40)
    }

    /** A Killer hint explaining a cage. */
    @Test
    fun killerHintLight() = capture("killer_hint_light", dark = false) {
        val (g, plan) = Fixtures.killerCageHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan)), actions = GameActions.None, onExit = {})
    }

    /** Larger numbers on, for low vision. */
    @Test
    fun gameLargeNumbersLight() = capture("game_large_light", dark = false) {
        val g = Fixtures.midGame()
        GameScreen(
            state = UiState(game = g, selected = Fixtures.selectedCell(g), settings = Settings(largeNumbers = true)),
            actions = GameActions.None,
            onExit = {},
        )
    }

    // --- Hints -------------------------------------------------------------

    @Test fun hintLight() = hint(dark = false)

    @Test fun hintDark() = hint(dark = true)

    private fun hint(dark: Boolean) = capture(if (dark) "hint_dark" else "hint_light", dark) {
        val (g, plan) = Fixtures.eliminationHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan)), actions = GameActions.None, onExit = {})
    }

    /** Hints step by step: first a nudge, then the technique. */
    @Test
    fun hintNudge() = capture("hint_nudge_light", dark = false) {
        val (g, plan) = Fixtures.eliminationHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan, level = HintUi.NUDGE)), actions = GameActions.None, onExit = {})
    }

    @Test
    fun hintTechnique() = capture("hint_technique_light", dark = false) {
        val (g, plan) = Fixtures.eliminationHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan, level = HintUi.TECHNIQUE)), actions = GameActions.None, onExit = {})
    }

    /** Colour marks on a few cells, and every place 7 can still go lit up from its key. */
    @Test
    fun marksAndSpotlight() = capture("game_marks_light", dark = false) {
        var g = Fixtures.midGame()
        val empty = (0 until 81).filter { g.values[it] == 0 }
        for (cell in empty.take(3)) g = g.cycleMark(cell)
        for (cell in empty.drop(3).take(2)) g = g.cycleMark(cell).cycleMark(cell)
        GameScreen(state = UiState(game = g, spotlight = 7), actions = GameActions.None, onExit = {})
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
            state = UiState(game = Fixtures.playedGame(), lastWinWasBest = true, stats = Fixtures.stats),
            actions = GameActions.None,
            onExit = {},
            onReplay = {},
        )
    }

    /** Watching a solve back, partway through, with its moments listed below. */
    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun replayLight() = capture("replay_light", dark = false) {
        val game = Fixtures.playedGame()
        ReplayScreen(game, onBack = {}, initialShown = game.log.size / 2)
    }

    // --- Daily calendar ------------------------------------------------------

    @Test fun calendarLight() = calendar(dark = false)

    @Test fun calendarDark() = calendar(dark = true)

    private fun calendar(dark: Boolean) = capture(if (dark) "calendar_dark" else "calendar_light", dark) {
        CalendarScreen(daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, onBack = {}, onTab = {})
    }

    /** The whole daily page, including the weekday chart below the fold, on a tall screen. */
    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun calendarFullLight() = capture("calendar_full_light", dark = false) {
        CalendarScreen(daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, onBack = {}, onTab = {})
    }

    /** The Daily tab switched to the daily Killer. */
    @Test
    fun calendarKillerLight() = capture("calendar_killer_light", dark = false) {
        CalendarScreen(daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, onBack = {}, onTab = {}, showKiller = true)
    }

    // --- Learn ---------------------------------------------------------------

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun learnLight() = capture("learn_light", dark = false) {
        LearnScreen(onOpen = {}, onBack = {}, onTab = {}, done = Fixtures.lessonsDone)
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun learnDark() = capture("learn_dark", dark = true) {
        LearnScreen(onOpen = {}, onBack = {}, onTab = {}, done = Fixtures.lessonsDone)
    }

    /** A lesson in the dark: the step bar, chip and both buttons' icons. */
    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun lessonDark() = capture("lesson_xwing_dark", dark = true) {
        val index = Tutorial.lessons.indexOfFirst { it.technique == Technique.X_WING }
        LessonScreen(
            index = index,
            example = Tutorial.example(Tutorial.lessons[index]),
            loading = false,
            onNextLesson = {},
            onBack = {},
        )
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

    /** The whole home screen, on a tall screen. */
    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun homeFullLight() = capture("home_full_light", dark = false) {
        HomeScreen(
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily, killerDaily = Fixtures.killerDaily) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onSettings = {},
            onTab = {},
        )
    }

    // --- Wide screens --------------------------------------------------------

    /** A phone on its side: the board on the left, the controls beside it. */
    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun gameLandscape() = capture("game_landscape_light", dark = false) {
        val g = Fixtures.midGame()
        GameScreen(state = UiState(game = g, selected = Fixtures.selectedCell(g)), actions = GameActions.None, onExit = {})
    }

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun hintLandscape() = capture("hint_landscape_dark", dark = true) {
        val (g, plan) = Fixtures.eliminationHint()
        GameScreen(state = UiState(game = g, hint = HintUi(plan)), actions = GameActions.None, onExit = {})
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun gameTablet() = capture("game_tablet_light", dark = false) {
        val g = Fixtures.killerGame()
        GameScreen(
            state = UiState(game = g, selected = (0 until 81).first { g.values[it] == 0 }),
            actions = GameActions.None,
            onExit = {},
        )
    }

    /** Notes mode on a tablet: the 3×3 pad of big keys in its soft indigo. */
    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun notesTabletDark() = capture("notes_tablet_dark", dark = true) {
        val g = Fixtures.midGame()
        GameScreen(
            state = UiState(game = g, selected = Fixtures.selectedCell(g), notesMode = true),
            actions = GameActions.None,
            onExit = {},
        )
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun winTablet() = capture("win_tablet_light", dark = false) {
        GameScreen(
            state = UiState(game = Fixtures.wonGame(), lastWinWasBest = true, stats = Fixtures.stats),
            actions = GameActions.None,
            onExit = {},
        )
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun homeTablet() = capture("home_tablet_light", dark = false) {
        HomeScreen(
            state = Fixtures.midGame().let { UiState(game = it, savedGame = it, daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, stats = Fixtures.stats) },
            onContinue = {},
            onPlayDaily = {},
            onCalendar = {},
            onNewGame = {},
            onNewKiller = {},
            onSettings = {},
            onTab = {},
        )
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun calendarTablet() = capture("calendar_tablet_dark", dark = true) {
        CalendarScreen(daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, onBack = {}, onTab = {})
    }

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun lessonLandscape() = capture("lesson_landscape_light", dark = false) {
        val index = Tutorial.lessons.indexOfFirst { it.technique == Technique.X_WING }
        LessonScreen(
            index = index,
            example = Tutorial.example(Tutorial.lessons[index]),
            loading = false,
            onNextLesson = {},
            onBack = {},
        )
    }

    // --- Settings ----------------------------------------------------------

    @Test fun settingsLight() = settings(dark = false)

    @Test fun settingsDark() = settings(dark = true)

    /** Dark chosen in Settings, with Indigo night, which greys the toggle out because it's always dark. */
    @Test
    fun settingsForcedDark() = capture("settings_forced_dark", dark = true, theme = InkTheme.INDIGO_NIGHT) {
        SettingsScreen(settings = Settings(theme = InkTheme.INDIGO_NIGHT, darkMode = DarkMode.DARK), onChange = {}, onBack = {})
    }

    private fun settings(dark: Boolean) = capture(if (dark) "settings_dark" else "settings_light", dark) {
        SettingsScreen(settings = Settings(), onChange = {}, onBack = {})
    }

    // --- Killer course -------------------------------------------------------

    /** The combinations cheat sheet, on three-cell cages. */
    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun combinationsLight() = capture("combinations_light", dark = false) {
        CombinationsScreen(onBack = {}, initialSize = 3)
    }

    @Test
    fun combinationsDark() = capture("combinations_dark", dark = true) {
        CombinationsScreen(onBack = {}, initialSize = 2)
    }

    /** The whole Learn list, with the Killer course and the cheat sheet row. */
    @Test
    @Config(qualifiers = "w411dp-h2600dp-xxhdpi")
    fun learnFullLight() = capture("learn_full_light", dark = false) {
        LearnScreen(onOpen = {}, onBack = {}, onTab = {}, done = Fixtures.lessonsDone, onCombinations = {})
    }

    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun lessonOutiesLight() = killerLesson("KILLER_OUTIES", "lesson_outies_light")

    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun lessonOneAnswerLight() = killerLesson("KILLER_ONE_ANSWER", "lesson_one_answer_light")

    private fun killerLesson(id: String, name: String) = capture(name, dark = false) {
        val index = Tutorial.lessons.indexOfFirst { it.id == id }
        LessonScreen(
            index = index,
            example = Tutorial.example(Tutorial.lessons[index]),
            loading = false,
            onNextLesson = {},
            onBack = {},
            onPractice = {},
        )
    }

    // --- Swahili -------------------------------------------------------------

    /** Runs [block] with the app in Swahili, as on a phone set to Kiswahili (Tanzania). */
    private fun swahili(block: () -> Unit) {
        val before = Locale.getDefault()
        Lang.forced = Lang.SW
        Locale.setDefault(Locale("sw", "TZ"))
        try {
            block()
        } finally {
            Lang.forced = null
            Locale.setDefault(before)
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun homeSwahili() = swahili {
        capture("home_sw_light", dark = false) {
            HomeScreen(
                state = Fixtures.midGame().let {
                    UiState(game = it, savedGame = it, daily = Fixtures.daily, killerDaily = Fixtures.killerDaily)
                },
                onContinue = {},
                onPlayDaily = {},
                onCalendar = {},
                onNewGame = {},
                onNewKiller = {},
                onSettings = {},
                onTab = {},
            )
        }
    }

    @Test
    fun hintSwahili() = swahili {
        capture("hint_sw_light", dark = false) {
            val (g, plan) = Fixtures.eliminationHint()
            GameScreen(state = UiState(game = g, hint = HintUi(plan)), actions = GameActions.None, onExit = {})
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1300dp-xxhdpi")
    fun lessonSwahili() = swahili {
        capture("lesson_xwing_sw_light", dark = false) {
            val index = Tutorial.lessons.indexOfFirst { it.technique == Technique.X_WING }
            LessonScreen(
                index = index,
                example = Tutorial.example(Tutorial.lessons[index]),
                loading = false,
                onNextLesson = {},
                onBack = {},
                onPractice = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun calendarSwahili() = swahili {
        capture("calendar_sw_light", dark = false) {
            CalendarScreen(daily = Fixtures.daily, killerDaily = Fixtures.killerDaily, onBack = {}, onTab = {})
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun settingsSwahili() = swahili {
        capture("settings_sw_light", dark = false) {
            SettingsScreen(settings = Settings(), onChange = {}, onBack = {})
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun statsSwahili() = swahili {
        capture("stats_sw_light", dark = false) {
            StatsScreen(
                stats = Fixtures.stats,
                killerStats = DifficultyStats(played = 3, won = 2, bestSeconds = 1210),
                achievements = Fixtures.achievements,
                onReset = {},
                onBack = {},
                onTab = {},
            )
        }
    }

    /** Progress in the dark palette, with the floating bar. */
    @Test
    fun statsDark() = capture("stats_dark", dark = true) {
        StatsScreen(
            stats = Fixtures.stats,
            achievements = Fixtures.achievements,
            onReset = {},
            onBack = {},
            onTab = {},
        )
    }

    // --- Stats -------------------------------------------------------------

    @Test
    fun statsLight() = capture("stats_light", dark = false) {
        StatsScreen(stats = Fixtures.stats, onReset = {}, onBack = {}, onTab = {})
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
            onTab = {},
            recentGames = listOf(Fixtures.lostGame(), Fixtures.playedGame()),
        )
    }

    /** Out of mistakes, with Replay beside Try again and New puzzle. */
    @Test
    fun lostLight() = capture("lost_light", dark = false) {
        GameScreen(state = UiState(game = Fixtures.lostGame()), actions = GameActions.None, onExit = {}, onReplay = {})
    }

    /** A win that earns two achievements. */
    @Test
    fun winAchievementLight() = capture("win_achievement_light", dark = false) {
        GameScreen(
            state = UiState(
                game = Fixtures.wonGame(),
                lastWinWasBest = true,
                stats = Fixtures.stats,
                // What this game (Hard, 1 mistake, 2 hints) would really earn as a first win.
                newAchievements = Achievement.entries.filter {
                    it in Achievements.earned(Progress(), Fixtures.wonGame())
                },
            ),
            actions = GameActions.None,
            onExit = {},
        )
    }
}

/** A Pixel 7 on its side. */
private const val PHONE_LANDSCAPE = "w915dp-h412dp-land-xxhdpi"

/** A 10-inch tablet, both ways round. */
private const val TABLET_LANDSCAPE = "w1280dp-h800dp-land-xhdpi"
private const val TABLET_PORTRAIT = "w800dp-h1280dp-port-xhdpi"

/** Fixed game states for screenshots, all from seeded puzzles. */
private object Fixtures {

    val stats = mapOf(
        Difficulty.EASY to DifficultyStats(played = 14, won = 13, bestSeconds = 192, timedWins = 13, totalSeconds = 13 * 251L, noHintWins = 12),
        Difficulty.MEDIUM to DifficultyStats(played = 9, won = 7, bestSeconds = 431, timedWins = 5, totalSeconds = 5 * 545L, noHintWins = 3),
        Difficulty.HARD to DifficultyStats(played = 4, won = 2, bestSeconds = 905),
        Difficulty.EXPERT to DifficultyStats(played = 1, won = 0),
    )

    /** Thursday 5 November 2026: Monday to Wednesday solved, plus a run in October. */
    val daily: DailyUi = run {
        val today = LocalDate.parse("2026-11-05").toEpochDay()
        val solved = listOf(1L, 2L, 3L) + (6L..14L) + listOf(18L, 22L, 23L)
        DailyUi(today = today, results = solved.associate { (today - it) to 300L + it * 17 })
    }

    /** The daily Killer on the same Thursday: Tuesday and Wednesday solved. */
    val killerDaily: DailyUi = run {
        val today = LocalDate.parse("2026-11-05").toEpochDay()
        DailyUi(today = today, results = mapOf(today - 2 to 640L, today - 1 to 815L), killer = true)
    }

    /** The first five lessons finished. */
    val lessonsDone: Set<String> = Tutorial.lessons.take(5).map { it.id }.toSet()

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

    /** A Medium Sudoku X or Jigsaw game a few moves in. */
    fun variantGame(variant: Variant): GameState {
        var g = GameState.from(Generator.generate(Difficulty.MEDIUM, Random(5), variant))
        repeat(8) { g = placeNextHint(g) }
        return g.copy(elapsedSeconds = 131)
    }

    /** A puzzle half typed in from a book: the first five rows, with one 7 typed twice in row 5. */
    fun typedIn(): String {
        val givens = Generator.generate(Difficulty.EASY, Random(2)).givens.copyOf()
        for (i in 45 until 81) givens[i] = 0
        val row = 36 until 45
        val d = row.map { givens[it] }.first { it != 0 }
        givens[row.first { givens[it] == 0 }] = d
        return givens.joinToString("")
    }

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

    /**
     * A Medium game played to the end by following hints, with a wrong number
     * early on, one number placed by a hint and two long pauses, so its log
     * has something to replay.
     */
    /** A game lost halfway through: the first half of [playedGame]'s moves, then out of mistakes. */
    fun lostGame(): GameState {
        val g = playedGame()
        val half = g.log.size / 2
        val log = g.log.take(half)
        return g.copy(
            values = boardAt(g, half),
            log = log,
            status = GameStatus.LOST,
            mistakes = 3,
            elapsedSeconds = log.last().second,
        )
    }

    fun playedGame(): GameState {
        val start = GameState.from(Generator.generate(Difficulty.MEDIUM, Random(2)))
        var game = start
        var clock = 0L
        var move = 0
        while (true) {
            val cards = (Hints.forGame(game) as? HintPlan.Teach)?.cards ?: break
            val card = cards.last()
            move++
            clock += when (move) {
                9 -> 154L
                30 -> 97L
                else -> 11L + move % 7
            }
            if (move == 5) {
                // A wrong number first, then the right one.
                val wrong = (1..9).first { it != game.solution[card.placeCell] }
                val before = game.copy(elapsedSeconds = clock)
                game = before.place(card.placeCell, wrong, mistakeLimit = null, autoRemoveNotes = true).logged(before)
                clock += 6
                val fixed = game.copy(elapsedSeconds = clock).erase(card.placeCell)
                game = fixed.logged(game)
            }
            val before = game.copy(elapsedSeconds = clock)
            game = before.place(card.placeCell, card.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
                .logged(before, hint = move == 20)
        }
        return game.copy(status = GameStatus.WON, hintsUsed = 1)
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
