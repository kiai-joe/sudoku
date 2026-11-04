package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/** A Killer Sudoku cage: its [cells] hold different numbers that add up to [sum]. */
data class Cage(val sum: Int, val cells: List<Int>)

/**
 * Killer Sudoku: the grid is split into cages, each showing the sum of its
 * cells, and a number can't repeat within a cage.
 */
object Killer {

    /**
     * For cells with candidate masks [cands] that must hold different
     * numbers adding up to [sum], none of them in [used]: the numbers each
     * cell can still take in some valid filling.
     */
    fun possibleDigits(cands: List<Int>, sum: Int, used: Int): IntArray {
        val possible = IntArray(cands.size)
        val chosen = IntArray(cands.size)
        fun fill(i: Int, remaining: Int, taken: Int) {
            if (i == cands.size) {
                if (remaining == 0) for (k in chosen.indices) possible[k] = possible[k] or (1 shl (chosen[k] - 1))
                return
            }
            for (d in 1..9) {
                if (d > remaining) break
                val bit = 1 shl (d - 1)
                if (cands[i] and bit == 0 || taken and bit != 0) continue
                chosen[i] = d
                fill(i + 1, remaining - d, taken or bit)
            }
        }
        fill(0, sum, used)
        return possible
    }

    /**
     * The different sets of numbers (as bitmasks) that can fill those cells,
     * ordered by their smallest number, so they read 2+8, 3+7, 4+6.
     */
    fun combinations(cands: List<Int>, sum: Int, used: Int): List<Int> {
        val sets = HashSet<Int>()
        val chosen = IntArray(cands.size)
        fun fill(i: Int, remaining: Int, taken: Int) {
            if (i == cands.size) {
                if (remaining == 0) sets += chosen.fold(0) { m, d -> m or (1 shl (d - 1)) }
                return
            }
            for (d in 1..9) {
                if (d > remaining) break
                val bit = 1 shl (d - 1)
                if (cands[i] and bit == 0 || taken and bit != 0) continue
                chosen[i] = d
                fill(i + 1, remaining - d, taken or bit)
            }
        }
        fill(0, sum, used)
        return sets.sortedWith(compareBy({ it and -it }, { it }))
    }

    /**
     * Makes a Killer puzzle. Cages are grown at random over a filled grid.
     * Then, like the classic generator, it solves by logic (now including
     * cage combinations) and wherever logic gets stuck it prints that
     * cell's number, until the puzzle can be solved without guessing. A logic
     * solve uses only sound steps, so it also proves the solution is unique.
     */
    fun generate(random: Random = Random.Default): Puzzle {
        val solution = Solver.randomFilledGrid(random)
        val cages = cagesFor(solution, random)
        val givens = IntArray(Grid.CELLS)
        while (true) {
            val grade = LogicSolver(givens, cages).solve()
            if (grade.solved) {
                return Puzzle(givens, solution, grade.level ?: Difficulty.EXPERT, grade.hardest, cages)
            }
            val stuck = (0 until Grid.CELLS).filter { grade.stuckValues[it] == 0 && givens[it] == 0 }
            val cell = stuck.random(random)
            givens[cell] = solution[cell]
        }
    }

    /** Splits the grid into cages of 1 to 5 cells, mostly 2 to 4, with no repeated number in a cage. */
    fun cagesFor(solution: IntArray, random: Random): List<Cage> {
        val owner = IntArray(Grid.CELLS) { -1 }
        val cages = ArrayList<MutableList<Int>>()
        for (start in (0 until Grid.CELLS).shuffled(random)) {
            if (owner[start] >= 0) continue
            val target = when (random.nextInt(100)) {
                in 0 until 4 -> 1
                in 4 until 38 -> 2
                in 38 until 76 -> 3
                in 76 until 95 -> 4
                else -> 5
            }
            val cage = mutableListOf(start)
            owner[start] = cages.size
            var digits = 1 shl (solution[start] - 1)
            while (cage.size < target) {
                val next = cage.flatMap { neighbours(it) }
                    .filter { owner[it] < 0 && digits and (1 shl (solution[it] - 1)) == 0 }
                    .distinct()
                if (next.isEmpty()) break
                val cell = next.random(random)
                cage += cell
                owner[cell] = cages.size
                digits = digits or (1 shl (solution[cell] - 1))
            }
            cages += cage
        }
        return cages.map { cells -> Cage(cells.sumOf { solution[it] }, cells.sorted()) }
    }

    private fun neighbours(cell: Int): List<Int> {
        val r = Grid.row(cell)
        val c = Grid.col(cell)
        return buildList {
            if (r > 0) add(cell - 9)
            if (r < 8) add(cell + 9)
            if (c > 0) add(cell - 1)
            if (c < 8) add(cell + 1)
        }
    }
}
