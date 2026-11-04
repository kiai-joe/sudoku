package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GeneratorTest {

    @Test
    fun everyDifficultyProducesAUniqueConsistentPuzzle() {
        for (difficulty in Difficulty.entries) {
            repeat(5) { seed ->
                val puzzle = Generator.generate(difficulty, Random(seed))
                assertTrue("solution valid", Grid.isValidSolution(puzzle.solution))
                assertTrue("unique", Solver.hasUniqueSolution(puzzle.givens))
                for (cell in 0 until 81) {
                    val given = puzzle.givens[cell]
                    assertTrue("givens match solution", given == 0 || given == puzzle.solution[cell])
                }
                assertArrayEquals(puzzle.solution, Solver.solve(puzzle.givens))
            }
        }
    }

    @Test
    fun harderLevelsHaveFewerClues() {
        fun averageClues(d: Difficulty) =
            (0 until 5).map { seed -> Generator.generate(d, Random(seed)).givens.count { it != 0 } }.average()

        val easy = averageClues(Difficulty.EASY)
        val medium = averageClues(Difficulty.MEDIUM)
        val hard = averageClues(Difficulty.HARD)
        val expert = averageClues(Difficulty.EXPERT)
        assertTrue("easy $easy > medium $medium", easy > medium)
        assertTrue("medium $medium > hard $hard", medium > hard)
        assertTrue("hard $hard > expert $expert", hard > expert)
        for (d in Difficulty.entries) {
            val clues = (0 until 5).map { Generator.generate(d, Random(it)).givens.count { c -> c != 0 } }
            assertTrue("$d never goes below target", clues.all { it >= d.targetClues })
        }
    }

    @Test
    fun sameSeedGivesSamePuzzle() {
        val a = Generator.generate(Difficulty.MEDIUM, Random(42))
        val b = Generator.generate(Difficulty.MEDIUM, Random(42))
        assertArrayEquals(a.givens, b.givens)
    }
}
