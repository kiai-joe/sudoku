package com.kiaijoe.sudoku.engine

/** What checking a typed-in puzzle found. */
sealed interface CustomCheck {
    /** Fewer than 17 numbers: no classic puzzle with so few has a single solution. */
    data class TooFew(val clues: Int) : CustomCheck

    /** Two of the same number in a row, column or box. */
    data class Clash(val cells: Set<Int>) : CustomCheck

    data object NoSolution : CustomCheck

    data object ManySolutions : CustomCheck

    /**
     * A proper puzzle. [puzzle] carries its level from the technique grader;
     * [byLogic] is false when logic alone can't finish it (it would need
     * guessing), in which case it's labelled Expert.
     */
    data class Ok(val puzzle: Puzzle, val byLogic: Boolean) : CustomCheck
}

/** Checks and grades a classic puzzle typed in by the player. */
object CustomPuzzle {
    /** The fewest clues a classic Sudoku with one solution can have. */
    const val MIN_CLUES = 17

    fun check(givens: IntArray): CustomCheck {
        require(givens.size == Grid.CELLS)
        val clash = clashes(givens)
        if (clash.isNotEmpty()) return CustomCheck.Clash(clash)
        val clues = givens.count { it != 0 }
        if (clues < MIN_CLUES) return CustomCheck.TooFew(clues)
        return when (Solver.countSolutions(givens, limit = 2)) {
            0 -> CustomCheck.NoSolution
            1 -> {
                val solution = Solver.solve(givens)!!
                val grade = LogicSolver.grade(givens)
                CustomCheck.Ok(
                    Puzzle(givens.copyOf(), solution, grade.level ?: Difficulty.EXPERT, grade.hardest),
                    byLogic = grade.solved,
                )
            }
            else -> CustomCheck.ManySolutions
        }
    }

    /** Cells whose number repeats in a row, column or box. */
    fun clashes(givens: IntArray): Set<Int> {
        val out = HashSet<Int>()
        for (house in Shape.CLASSIC.houses) {
            for (d in 1..9) {
                val cells = house.cells.filter { givens[it] == d }
                if (cells.size > 1) out += cells
            }
        }
        return out
    }
}
