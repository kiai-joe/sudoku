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

    /** Generated puzzles solve by logic alone, every step agrees with the solution, and hints finish them. */
    @Test
    fun generatedPuzzlesSolveByLogicAndHints() {
        repeat(8) { seed ->
            val puzzle = Killer.generate(Random(seed))
            val grade = LogicSolver.grade(puzzle.givens, puzzle.cages)
            assertTrue(grade.solved)
            assertArrayEquals(puzzle.solution, grade.stuckValues)
            for (step in grade.steps) {
                if (step.isPlacement) {
                    assertEquals(puzzle.solution[step.cell], step.digit)
                } else {
                    for ((cell, m) in step.eliminations) assertEquals(0, m and (1 shl (puzzle.solution[cell] - 1)))
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

    @Test
    fun cagesSurviveSaving() {
        val puzzle = Killer.generate(Random(3))
        val game = GameState.from(puzzle)
        assertEquals(puzzle.cages, GameCodec.decode(GameCodec.encode(game))?.cages)
    }
}
