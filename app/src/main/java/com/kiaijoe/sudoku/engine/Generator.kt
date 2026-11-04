package com.kiaijoe.sudoku.engine

import kotlin.random.Random

enum class Difficulty(val label: String, val targetClues: Int) {
    EASY("Easy", 40),
    MEDIUM("Medium", 32),
    HARD("Hard", 27),
    EXPERT("Expert", 23),
}

class Puzzle(val givens: IntArray, val solution: IntArray, val difficulty: Difficulty)

/**
 * Builds a random full grid, then removes clues while the puzzle keeps
 * exactly one solution and the clue count is above the difficulty's target.
 *
 * Clues go in rotationally symmetric pairs first, which looks nicer, then
 * one at a time to reach the lower targets. Low targets are hard to hit in
 * one go, so harder levels keep the best of a few attempts.
 */
object Generator {

    fun generate(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        val attempts = when (difficulty) {
            Difficulty.EASY, Difficulty.MEDIUM -> 1
            Difficulty.HARD -> 4
            Difficulty.EXPERT -> 12
        }
        var best: Puzzle? = null
        var bestClues = Int.MAX_VALUE
        repeat(attempts) {
            val candidate = attempt(difficulty, random)
            val clues = candidate.givens.count { it != 0 }
            if (clues < bestClues) {
                best = candidate
                bestClues = clues
            }
            if (bestClues <= difficulty.targetClues) return candidate
        }
        return best!!
    }

    private fun attempt(difficulty: Difficulty, random: Random): Puzzle {
        val solution = Solver.randomFilledGrid(random)
        val givens = solution.copyOf()
        var clues = Grid.CELLS
        val target = difficulty.targetClues

        for (cell in (0 until Grid.CELLS).shuffled(random)) {
            if (clues <= target) break
            val mirror = Grid.CELLS - 1 - cell
            if (givens[cell] == 0) continue
            val removed = if (cell == mirror) 1 else 2
            if (clues - removed < target) continue

            val keepA = givens[cell]
            val keepB = givens[mirror]
            givens[cell] = 0
            givens[mirror] = 0
            if (Solver.hasUniqueSolution(givens)) {
                clues -= removed
            } else {
                givens[cell] = keepA
                givens[mirror] = keepB
            }
        }

        for (cell in (0 until Grid.CELLS).shuffled(random)) {
            if (clues <= target) break
            if (givens[cell] == 0) continue
            val keep = givens[cell]
            givens[cell] = 0
            if (Solver.hasUniqueSolution(givens)) clues-- else givens[cell] = keep
        }
        return Puzzle(givens, solution, difficulty)
    }
}
