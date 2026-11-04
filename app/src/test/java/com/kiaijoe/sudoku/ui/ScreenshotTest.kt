package com.kiaijoe.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.HintPlan
import com.kiaijoe.sudoku.engine.Hints
import com.kiaijoe.sudoku.engine.Technique
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.screens.HomeScreen
import com.kiaijoe.sudoku.ui.screens.StatsScreen
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
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

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent { SudokuTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    // --- Home -------------------------------------------------------------

    @Test fun homeLight() = home(dark = false)

    @Test fun homeDark() = home(dark = true)

    private fun home(dark: Boolean) = capture(if (dark) "home_dark" else "home_light", dark) {
        HomeScreen(
            state = UiState(game = Fixtures.midGame(), stats = Fixtures.stats),
            onContinue = {},
            onNewGame = {},
            onStats = {},
            onSettingsChange = {},
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

    // --- Stats -------------------------------------------------------------

    @Test
    fun statsLight() = capture("stats_light", dark = false) {
        StatsScreen(stats = Fixtures.stats, onReset = {}, onBack = {})
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
