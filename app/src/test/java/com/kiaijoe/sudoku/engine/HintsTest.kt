package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HintsTest {

    /** Taking only hints, at every level, solves the puzzle, and every card is sound and readable. */
    @Test
    fun followingHintsSolvesEveryLevel() {
        val seen = HashSet<Technique>()
        for (difficulty in Difficulty.entries) {
            repeat(4) { seed ->
                val puzzle = Generator.generate(difficulty, Random(seed * 13 + difficulty.ordinal))
                var game = GameState.from(puzzle)
                var placed = 0
                while (game.status == GameStatus.PLAYING) {
                    val plan = Hints.forGame(game)
                    assertTrue("expected a teaching hint, got $plan", plan is HintPlan.Teach)
                    val cards = (plan as HintPlan.Teach).cards
                    assertTrue(cards.last().isPlacement)
                    assertTrue("only the last card places a number", cards.dropLast(1).none { it.isPlacement })
                    for (card in cards) {
                        card.technique?.let { seen += it }
                        assertTrue("${card.title} has text", card.text.length > 20)
                        assertFalse("${card.title}: ${card.text}", card.text.contains("null"))
                        assertFalse("${card.title} focus", card.focus.isEmpty())
                        for ((cell, mask) in card.eliminations) {
                            assertEquals("${card.title} rules out the answer", 0, mask and GameState.bit(puzzle.solution[cell]))
                        }
                    }
                    val last = cards.last()
                    assertEquals("hinted digit is right", puzzle.solution[last.placeCell], last.placeDigit)
                    game = game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
                    placed++
                    assertTrue("too many hints", placed <= 81)
                }
                assertEquals(GameStatus.WON, game.status)
            }
        }
        assertTrue(Technique.HIDDEN_SINGLE in seen)
        assertTrue(Technique.POINTING in seen || Technique.CLAIMING in seen)
    }

    @Test
    fun aWrongNumberIsPointedOutFirst() {
        val game = TestGrids.game()
        val cell = (0 until 81).first { game.values[it] == 0 }
        val wrongDigit = (1..9).first { it != TestGrids.SOLUTION[cell] }
        val withMistake = game.place(cell, wrongDigit, mistakeLimit = null, autoRemoveNotes = true)
        assertEquals(HintPlan.FixMistake(cell, wrongDigit), Hints.forGame(withMistake))
    }

    @Test
    fun playerNotesDoNotChangeTheHint() {
        val game = TestGrids.game()
        val cell = (0 until 81).first { game.values[it] == 0 }
        val noted = game.toggleNote(cell, 1).toggleNote(cell, 2)
        assertEquals(Hints.forGame(game), Hints.forGame(noted))
    }

    @Test
    fun wordingReadsNaturally() {
        assertEquals("4", Hints.digitList(GameState.bit(4)))
        assertEquals("3 and 8", Hints.digitList(GameState.bit(3) or GameState.bit(8)))
        assertEquals("2, 5 and 9", Hints.digitList(GameState.bit(2) or GameState.bit(5) or GameState.bit(9)))
        assertEquals("1, 4 or 9", Hints.orList(GameState.bit(1) or GameState.bit(4) or GameState.bit(9)))
        assertEquals("the centre box", Hints.houseName(House.BOXES[4]))
        assertEquals("row 3", Hints.houseName(House.ROWS[2]))
        assertEquals("the cell at row 2, column 3", Hints.cellName(11))
    }
}
