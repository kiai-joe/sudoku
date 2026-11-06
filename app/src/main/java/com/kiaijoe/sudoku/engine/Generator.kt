package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/**
 * Levels are graded by the hardest technique a puzzle needs (see
 * [Technique]), not by how many clues it has.
 */
enum class Difficulty(
    private val en: String,
    private val sw: String,
    private val techniquesEn: String,
    private val techniquesSw: String,
) {
    EASY("Easy", "Rahisi", "Singles only", "Pekee tu"),
    MEDIUM("Medium", "Kati", "Pointing and pairs", "Kuelekeza na jozi"),
    HARD("Hard", "Ngumu", "X-Wing and XY-Wing", "X-Wing na XY-Wing"),
    EXPERT("Expert", "Bingwa", "Swordfish and XYZ-Wing", "Swordfish na XYZ-Wing"),
    ;

    /** The level's name in the phone's language. */
    val label: String get() = tr(en, sw)

    /** What the level asks of the player, for the level buttons. */
    val techniques: String get() = tr(techniquesEn, techniquesSw)
}

/**
 * @property hardest the hardest technique the puzzle needs, as graded by [LogicSolver].
 * @property cages Killer Sudoku cages, or empty for a classic puzzle.
 * @property shape which groups must hold 1 to 9: classic, Sudoku X or Jigsaw.
 */
class Puzzle(
    val givens: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
    val hardest: Technique? = null,
    val cages: List<Cage> = emptyList(),
    val shape: Shape = Shape.CLASSIC,
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
/** The kinds of puzzle Sumi makes besides Killer: classic, Sudoku X (with diagonals) and Jigsaw. */
enum class Variant(private val en: String, private val sw: String) {
    CLASSIC("Classic", "Kawaida"),
    X("Sudoku X", "Sudoku X"),
    JIGSAW("Jigsaw", "Jigsaw"),
    ;

    val label: String get() = tr(en, sw)
}

object Generator {
    const val EASY_CLUE_FLOOR = 34

    /** Expert hits about 7% of attempts; this many makes a miss vanishingly rare. */
    private const val MAX_ATTEMPTS = 400

    /**
     * A puzzle at [difficulty]. [variant] picks the shape: classic, Sudoku X,
     * or Jigsaw (a fresh random layout for each puzzle).
     */
    fun generate(difficulty: Difficulty, random: Random = Random.Default, variant: Variant = Variant.CLASSIC): Puzzle {
        var fallback: Puzzle? = null
        // One Jigsaw layout per puzzle; only its numbers change between attempts.
        val jigsaw = if (variant == Variant.JIGSAW) fillableJigsaw(random) else null
        for (i in 0 until MAX_ATTEMPTS) {
            val candidate = attempt(difficulty, random, variant, jigsaw)
            if (candidate.difficulty == difficulty) return candidate
            if (fallback == null || candidate.difficulty > fallback.difficulty) fallback = candidate
        }
        // Practically unreachable; serve the hardest puzzle found, labelled honestly.
        return fallback!!
    }

    /** One try at [target]. Returns a logic-solvable puzzle at or below [target]. */
    private fun attempt(target: Difficulty, random: Random, variant: Variant, jigsaw: Shape?): Puzzle {
        val (shape, solution) = filledGrid(variant, random, jigsaw)
        val givens = dig(solution, if (target == Difficulty.EASY) EASY_CLUE_FLOOR else 0, random, shape)

        var grade = LogicSolver(givens, shape = shape).solve(target)
        while (!grade.solved) {
            val stuck = (0 until Grid.CELLS).filter { grade.stuckValues[it] == 0 }.shuffled(random)
            var keepsItHard = -1
            for (cell in stuck) {
                givens[cell] = solution[cell]
                val trial = LogicSolver(givens, shape = shape).solve(target)
                if (trial.solved && trial.level == target) return puzzle(givens, solution, trial, shape)
                if (!trial.solved && keepsItHard < 0) keepsItHard = cell
                givens[cell] = 0
            }
            // No single clue lands on target. Add one that keeps it too hard and try
            // again, or if every clue makes it too easy, accept the easier puzzle.
            val cell = if (keepsItHard >= 0) keepsItHard else stuck.first()
            givens[cell] = solution[cell]
            grade = LogicSolver(givens, shape = shape).solve(target)
        }
        return puzzle(givens, solution, grade, shape)
    }

    /**
     * A shape for [variant] and a random filled grid for it. A Jigsaw layout
     * that's slow to fill (or can't be) is swapped for another.
     */
    private fun filledGrid(variant: Variant, random: Random, jigsaw: Shape?): Pair<Shape, IntArray> = when (variant) {
        Variant.CLASSIC -> Shape.CLASSIC to Solver.randomFilledGrid(random)
        Variant.X -> Shape.X to Solver.randomFilledGrid(random, Shape.X)!!
        Variant.JIGSAW -> {
            val shape = jigsaw ?: fillableJigsaw(random)
            // A layout that filled once fills again; allow more steps before giving up on it.
            val grid = Solver.randomFilledGrid(random, shape, maxNodes = JIGSAW_FILL_NODES * 10)
                ?: return filledGrid(variant, random, fillableJigsaw(random))
            shape to grid
        }
    }

    /** A random Jigsaw layout that a quick search can fill. */
    private fun fillableJigsaw(random: Random): Shape {
        while (true) {
            val shape = Shape.jigsaw(Shape.randomJigsaw(random))
            if (Solver.randomFilledGrid(random, shape, maxNodes = JIGSAW_FILL_NODES) != null) return shape
        }
    }

    /** Search steps allowed to fill a Jigsaw layout before trying another. */
    private const val JIGSAW_FILL_NODES = 20_000L

    private fun puzzle(givens: IntArray, solution: IntArray, grade: Grade, shape: Shape) =
        Puzzle(givens.copyOf(), solution, grade.level ?: Difficulty.EXPERT, grade.hardest, shape = shape)

    /**
     * Removes clues while the solution stays unique, down to [floor] clues:
     * rotationally symmetric pairs first, which looks nicer, then single cells.
     */
    private fun dig(solution: IntArray, floor: Int, random: Random, shape: Shape = Shape.CLASSIC): IntArray {
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
            if (Solver.hasUniqueSolution(givens, shape)) {
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
            if (Solver.hasUniqueSolution(givens, shape)) clues-- else givens[cell] = keep
        }
        return givens
    }
}
