package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/**
 * Levels are graded by the hardest technique a puzzle needs (see
 * [Technique]), not by how many clues it has.
 */
enum class Difficulty(val label: String, val techniques: String) {
    EASY("Easy", "Singles only"),
    MEDIUM("Medium", "Pointing and pairs"),
    HARD("Hard", "X-Wing and XY-Wing"),
    EXPERT("Expert", "Swordfish and XYZ-Wing"),
}

/**
 * @property hardest the hardest technique the puzzle needs, as graded by [LogicSolver].
 * @property cages Killer Sudoku cages, or empty for a classic puzzle.
 */
class Puzzle(
    val givens: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
    val hardest: Technique? = null,
    val cages: List<Cage> = emptyList(),
)

/**
 * Makes puzzles whose difficulty matches the hardest technique needed.
 *
 * Each attempt fills a random grid and removes clues while the solution
 * stays unique. It then grades the puzzle by logic, allowing techniques up
 * to the requested level. If logic gets stuck, it adds back one clue where
 * it got stuck, preferring a clue that makes the puzzle land exactly on the
 * requested level, and repeats. Attempts that end up easier than requested
 * are thrown away. Easy puzzles keep at least [EASY_CLUE_FLOOR] clues so they
 * also look easy.
 */
object Generator {
    const val EASY_CLUE_FLOOR = 34

    /** Expert hits about 7% of attempts; this many makes a miss vanishingly rare. */
    private const val MAX_ATTEMPTS = 400

    fun generate(difficulty: Difficulty, random: Random = Random.Default): Puzzle {
        var fallback: Puzzle? = null
        for (i in 0 until MAX_ATTEMPTS) {
            val candidate = attempt(difficulty, random)
            if (candidate.difficulty == difficulty) return candidate
            if (fallback == null || candidate.difficulty > fallback.difficulty) fallback = candidate
        }
        // Practically unreachable; serve the hardest puzzle found, labelled honestly.
        return fallback!!
    }

    /** One try at [target]. Returns a logic-solvable puzzle at or below [target]. */
    private fun attempt(target: Difficulty, random: Random): Puzzle {
        val solution = Solver.randomFilledGrid(random)
        val givens = dig(solution, if (target == Difficulty.EASY) EASY_CLUE_FLOOR else 0, random)

        var grade = LogicSolver(givens).solve(target)
        while (!grade.solved) {
            val stuck = (0 until Grid.CELLS).filter { grade.stuckValues[it] == 0 }.shuffled(random)
            var keepsItHard = -1
            for (cell in stuck) {
                givens[cell] = solution[cell]
                val trial = LogicSolver(givens).solve(target)
                if (trial.solved && trial.level == target) return puzzle(givens, solution, trial)
                if (!trial.solved && keepsItHard < 0) keepsItHard = cell
                givens[cell] = 0
            }
            // No single clue lands on target. Add one that keeps it too hard and try
            // again, or if every clue makes it too easy, accept the easier puzzle.
            val cell = if (keepsItHard >= 0) keepsItHard else stuck.first()
            givens[cell] = solution[cell]
            grade = LogicSolver(givens).solve(target)
        }
        return puzzle(givens, solution, grade)
    }

    private fun puzzle(givens: IntArray, solution: IntArray, grade: Grade) =
        Puzzle(givens.copyOf(), solution, grade.level ?: Difficulty.EXPERT, grade.hardest)

    /**
     * Removes clues while the solution stays unique, down to [floor] clues:
     * rotationally symmetric pairs first, which looks nicer, then single cells.
     */
    private fun dig(solution: IntArray, floor: Int, random: Random): IntArray {
        val givens = solution.copyOf()
        var clues = Grid.CELLS

        for (cell in (0 until Grid.CELLS).shuffled(random)) {
            if (clues <= floor) break
            val mirror = Grid.CELLS - 1 - cell
            if (givens[cell] == 0) continue
            val removed = if (cell == mirror) 1 else 2
            if (clues - removed < floor) continue

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
            if (clues <= floor) break
            if (givens[cell] == 0) continue
            val keep = givens[cell]
            givens[cell] = 0
            if (Solver.hasUniqueSolution(givens)) clues-- else givens[cell] = keep
        }
        return givens
    }
}
