package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KillerTest {

    private fun mask(vararg digits: Int) = digits.fold(0) { m, d -> m or (1 shl (d - 1)) }

    @Test
    fun cageCombinationsFollowTheSum() {
        val all = Grid.ALL_DIGITS
        // Two cells adding to 3 can only be 1 and 2.
        assertArrayEquals(intArrayOf(mask(1, 2), mask(1, 2)), Killer.possibleDigits(listOf(all, all), 3, 0))
        // Two cells adding to 17 can only be 8 and 9.
        assertEquals(listOf(mask(8, 9)), Killer.combinations(listOf(all, all), 17, 0))
        // Ten in two cells, with 1 already used in the cage: 2+8, 3+7, 4+6 (not 5+5, not 1+9).
        assertEquals(listOf(mask(2, 8), mask(3, 7), mask(4, 6)), Killer.combinations(listOf(all, all), 10, mask(1)))
    }

    @Test
    fun cagesCoverTheGridWithoutRepeats() {
        repeat(20) { seed ->
            val solution = Solver.randomFilledGrid(Random(seed))
            val cages = Killer.cagesFor(solution, Random(seed))
            val cells = cages.flatMap { it.cells }
            assertEquals(81, cells.size)
            assertEquals((0 until 81).toList(), cells.sorted())
            for (cage in cages) {
                assertTrue(cage.cells.size in 1..5)
                assertEquals(cage.sum, cage.cells.sumOf { solution[it] })
                assertEquals("no repeats", cage.cells.size, cage.cells.map { solution[it] }.toSet().size)
            }
        }
    }

    /**
     * At every level, generated puzzles solve by logic, every step (cage
     * combinations and the 45 rule included) agrees with the solution, the
     * level matches the Killer ladder, and hints alone finish the game.
     */
    @Test
    fun generatedPuzzlesSolveByLogicAndHints() {
        for (level in Difficulty.entries) {
            repeat(3) { seed ->
                val puzzle = Killer.generate(level, Random(seed * 31 + level.ordinal))
                assertEquals("$level", level, puzzle.difficulty)
                val grade = LogicSolver.grade(puzzle.givens, puzzle.cages)
                assertTrue(grade.solved)
                assertArrayEquals(puzzle.solution, grade.stuckValues)
                assertEquals(level, Killer.levelOf(grade.hardest, puzzle.givens.count { it != 0 }))
                if (level == Difficulty.EXPERT) assertEquals("Expert has no printed numbers", 0, puzzle.givens.count { it != 0 })
                for (step in grade.steps) {
                    if (step.isPlacement) {
                        assertEquals(puzzle.solution[step.cell], step.digit)
                    } else {
                        for ((cell, m) in step.eliminations) {
                            assertEquals("${step.technique} removed the answer", 0, m and (1 shl (puzzle.solution[cell] - 1)))
                        }
                    }
                }
                var game = GameState.from(puzzle)
                assertTrue(game.isKiller)
                var moves = 0
                while (game.status == GameStatus.PLAYING) {
                    val cards = (Hints.forGame(game) as HintPlan.Teach).cards
                    assertTrue("Killer hints use logic, not reveals", cards.all { it.technique != null })
                    val last = cards.last()
                    game = game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
                    assertTrue(++moves <= 81)
                }
            }
        }
    }

    /** The 45 rule is sound on many random cage layouts, not just generated puzzles. */
    @Test
    fun fortyFiveRuleNeverRulesOutTheAnswer() {
        var used = 0
        repeat(40) { seed ->
            val solution = Solver.randomFilledGrid(Random(seed))
            val cages = Killer.cagesFor(solution, Random(seed))
            for (step in LogicSolver.grade(IntArray(81), cages).steps) {
                if (step.technique != Technique.FORTY_FIVE) continue
                used++
                for ((cell, m) in step.eliminations) assertEquals(0, m and (1 shl (solution[cell] - 1)))
                assertTrue(Hints.explain(step, { Grid.ALL_DIGITS }).contains("45"))
            }
        }
        assertTrue("the 45 rule was used", used > 0)
    }

    @Test
    fun killerLevelsFollowTheLadder() {
        assertEquals(Difficulty.EASY, Killer.levelOf(Technique.CAGE, 2))
        assertEquals(Difficulty.MEDIUM, Killer.levelOf(Technique.FORTY_FIVE, 1))
        assertEquals(Difficulty.HARD, Killer.levelOf(Technique.POINTING, 1))
        assertEquals(Difficulty.EXPERT, Killer.levelOf(Technique.POINTING, 0))
        assertEquals(Difficulty.EXPERT, Killer.levelOf(Technique.X_WING, 3))
    }

    @Test
    fun cagesSurviveSaving() {
        val puzzle = Killer.generate(Difficulty.MEDIUM, Random(3))
        val game = GameState.from(puzzle)
        assertEquals(puzzle.cages, GameCodec.decode(GameCodec.encode(game))?.cages)
    }
}
