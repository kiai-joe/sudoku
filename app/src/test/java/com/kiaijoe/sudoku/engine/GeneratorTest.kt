package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
    fun everyPuzzleIsGradedAtItsLevel() {
        for (difficulty in Difficulty.entries) {
            repeat(8) { seed ->
                val puzzle = Generator.generate(difficulty, Random(seed * 101 + difficulty.ordinal))
                val grade = LogicSolver.grade(puzzle.givens)
                assertTrue("$difficulty solvable by logic", grade.solved)
                assertEquals("$difficulty graded level", difficulty, grade.level)
                assertEquals(difficulty, puzzle.difficulty)
                assertEquals(grade.hardest, puzzle.hardest)
            }
        }
    }

    @Test
    fun easyPuzzlesKeepEnoughClues() {
        repeat(10) { seed ->
            val clues = Generator.generate(Difficulty.EASY, Random(seed)).givens.count { it != 0 }
            assertTrue("easy has $clues clues", clues >= Generator.EASY_CLUE_FLOOR)
        }
    }

    @Test
    fun sameSeedGivesSamePuzzle() {
        val a = Generator.generate(Difficulty.MEDIUM, Random(42))
        val b = Generator.generate(Difficulty.MEDIUM, Random(42))
        assertArrayEquals(a.givens, b.givens)
    }
}
