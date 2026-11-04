package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LogicSolverTest {

    @Test
    fun classicPuzzleNeedsOnlySingles() {
        val grade = LogicSolver.grade(TestGrids.PUZZLE)
        assertTrue(grade.solved)
        assertEquals(Difficulty.EASY, grade.level)
        assertArrayEquals(TestGrids.SOLUTION, grade.stuckValues)
    }

    @Test
    fun veryHardPuzzleIsBeyondTheseTechniques() {
        // Arto Inkala's 2012 puzzle, which needs far more than wings and fish.
        val grade = LogicSolver.grade(
            TestGrids.parse("800000000003600000070090200050007000000045700000100030001000068008500010090000400"),
        )
        assertFalse(grade.solved)
        assertNull(grade.level)
    }

    /**
     * Every technique must be sound: a placement must match the solution and
     * an elimination must never remove the solution's digit. Checked step by
     * step on many puzzles, including ones logic cannot finish.
     */
    @Test
    fun everyStepAgreesWithTheSolution() {
        val used = HashSet<Technique>()
        repeat(150) { seed ->
            val random = Random(seed)
            val solution = Solver.randomFilledGrid(random)
            val givens = solution.copyOf()
            for (cell in (0 until 81).shuffled(random)) {
                val keep = givens[cell]
                givens[cell] = 0
                if (!Solver.hasUniqueSolution(givens)) givens[cell] = keep
            }
            val grade = LogicSolver.grade(givens)
            for (step in grade.steps) {
                used += step.technique
                if (step.isPlacement) {
                    assertEquals("${step.technique} placement", solution[step.cell], step.digit)
                } else {
                    for ((cell, mask) in step.eliminations) {
                        val truth = 1 shl (solution[cell] - 1)
                        assertEquals("${step.technique} removed the answer from cell $cell", 0, mask and truth)
                    }
                }
            }
            if (grade.solved) assertArrayEquals(solution, grade.stuckValues)
        }
        // The common techniques should all show up across this many puzzles.
        for (t in listOf(Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE, Technique.POINTING, Technique.NAKED_PAIR)) {
            assertTrue("$t was never used", t in used)
        }
    }

    @Test
    fun cappingTheLevelStopsBeforeHarderTechniques() {
        repeat(40) { seed ->
            val puzzle = Generator.generate(Difficulty.HARD, Random(seed))
            val capped = LogicSolver(puzzle.givens).solve(Difficulty.MEDIUM)
            assertFalse("a Hard puzzle can't be finished with Medium techniques", capped.solved)
            assertTrue(capped.steps.all { it.technique.level <= Difficulty.MEDIUM })
        }
    }
}
