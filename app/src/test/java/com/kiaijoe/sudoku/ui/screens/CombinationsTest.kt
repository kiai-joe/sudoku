package com.kiaijoe.sudoku.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** The cheat sheet's numbers, checked against the well-known Killer tables. */
class CombinationsTest {

    @Test
    fun sumsRunFromSmallestToLargest() {
        assertEquals(3..17, cageSums(2))
        assertEquals(6..24, cageSums(3))
        assertEquals(10..30, cageSums(4))
        assertEquals(15..35, cageSums(5))
    }

    @Test
    fun oneAnswerSumsAreTheKnownOnes() {
        fun oneAnswer(size: Int) = cageSums(size).filter { cageSets(size, it).size == 1 }
        assertEquals(listOf(3, 4, 16, 17), oneAnswer(2))
        assertEquals(listOf(6, 7, 23, 24), oneAnswer(3))
        assertEquals(listOf(10, 11, 29, 30), oneAnswer(4))
        assertEquals(listOf(15, 16, 34, 35), oneAnswer(5))
    }

    @Test
    fun setsAreListedInOrder() {
        assertEquals(listOf(listOf(1, 9), listOf(2, 8), listOf(3, 7), listOf(4, 6)), cageSets(2, 10))
        assertEquals(listOf(listOf(5, 8, 9), listOf(6, 7, 9)), cageSets(3, 22))
        // Every set of n different numbers adds up to its sum.
        for (size in 2..5) for (sum in cageSums(size)) for (set in cageSets(size, sum)) {
            assertEquals(sum, set.sum())
            assertEquals(size, set.toSet().size)
        }
    }
}
