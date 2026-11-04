package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SolverTest {

    @Test
    fun solvesKnownPuzzle() {
        val solved = Solver.solve(TestGrids.PUZZLE)
        assertArrayEquals(TestGrids.SOLUTION, solved)
    }

    @Test
    fun knownPuzzleHasUniqueSolution() {
        assertTrue(Solver.hasUniqueSolution(TestGrids.PUZZLE))
    }

    @Test
    fun emptyGridHasManySolutions() {
        assertEquals(2, Solver.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun contradictoryGridHasNoSolution() {
        val grid = IntArray(81).also { it[0] = 5; it[1] = 5 }
        assertNull(Solver.solve(grid))
        assertEquals(0, Solver.countSolutions(grid))
    }

    @Test
    fun randomFilledGridIsValid() {
        repeat(20) { seed ->
            assertTrue(Grid.isValidSolution(Solver.randomFilledGrid(Random(seed))))
        }
    }

    @Test
    fun peersAreTwentyCells() {
        Grid.peers.forEach { assertEquals(20, it.size) }
    }
}
