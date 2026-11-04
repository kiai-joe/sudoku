package com.kiaijoe.sudoku.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsSelected
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.Killer
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** What a screen reader hears on the game screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class AccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(state: UiState) = compose.setContent {
        SudokuTheme { GameScreen(state = state, actions = GameActions.None, onExit = {}) }
    }

    /** A Medium game with one wrong number and pencil marks in one cell. */
    private fun game(): Triple<GameState, Int, Int> {
        val puzzle = Generator.generate(Difficulty.MEDIUM, Random(7))
        var g = GameState.from(puzzle)
        val empty = (0 until 81).filter { g.values[it] == 0 }
        val wrongCell = empty[0]
        val wrongDigit = (1..9).first { it != puzzle.solution[wrongCell] }
        g = g.place(wrongCell, wrongDigit, mistakeLimit = 3, autoRemoveNotes = true)
        val notesCell = empty[1]
        g = g.toggleNote(notesCell, 2).toggleNote(notesCell, 7)
        return Triple(g, wrongCell, notesCell)
    }

    @Test
    fun everyCellIsDescribedOnce() {
        show(UiState(game = game().first))
        for (r in 1..9) for (c in 1..9) {
            compose.onAllNodesWithContentDescription("Row $r, column $c,", substring = true).assertCountEquals(1)
        }
    }

    @Test
    fun aWrongNumberIsSaidToBeIncorrect() {
        val (g, wrongCell, _) = game()
        show(UiState(game = g))
        val r = wrongCell / 9 + 1
        val c = wrongCell % 9 + 1
        compose.onNodeWithContentDescription("Row $r, column $c, ${g.values[wrongCell]}, incorrect").assertExists()
    }

    @Test
    fun hiddenMistakesAreNotGivenAwayByTheScreenReader() {
        val (g, _, _) = game()
        show(UiState(game = g, settings = Settings(instantCheck = false)))
        compose.onAllNodes(hasContentDescription("incorrect", substring = true)).assertCountEquals(0)
    }

    @Test
    fun pencilMarksAndSelectionAreRead() {
        val (g, _, notesCell) = game()
        show(UiState(game = g, selected = notesCell))
        val r = notesCell / 9 + 1
        val c = notesCell % 9 + 1
        compose.onNodeWithContentDescription("Row $r, column $c, empty, notes 2, 7").assertIsSelected()
    }

    @Test
    fun killerCagesAreRead() {
        val g = GameState.from(Killer.generate(Difficulty.EASY, Random(1)))
        show(UiState(game = g))
        val cage = g.cages.first { it.cells.size >= 2 }
        val cell = cage.cells.first()
        val nodes = compose.onAllNodes(
            hasContentDescription("Row ${cell / 9 + 1}, column ${cell % 9 + 1},", substring = true),
        ).fetchSemanticsNodes()
        val spoken = nodes.single().config.toString()
        assertTrue(spoken, spoken.contains("cage of ${cage.cells.size} adding to ${cage.sum}"))
    }
}
