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
     * For cells with candidate masks [cands] that add up to [sum], none of
     * them in [used]: the numbers each cell can still take in some valid
     * filling. Cells [i] and [j] must differ when [differ] says so; by
     * default all must (as in a cage).
     */
    fun possibleDigits(
        cands: List<Int>,
        sum: Int,
        used: Int,
        differ: (Int, Int) -> Boolean = { _, _ -> true },
    ): IntArray {
        val possible = IntArray(cands.size)
        val chosen = IntArray(cands.size)
        fun fill(i: Int, remaining: Int) {
            if (i == cands.size) {
                if (remaining == 0) for (k in chosen.indices) possible[k] = possible[k] or (1 shl (chosen[k] - 1))
                return
            }
            for (d in 1..9) {
                if (d > remaining) break
                val bit = 1 shl (d - 1)
                if (cands[i] and bit == 0 || used and bit != 0) continue
                if ((0 until i).any { j -> chosen[j] == d && differ(j, i) }) continue
                chosen[i] = d
                fill(i + 1, remaining - d)
            }
        }
        fill(0, sum)
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

    /** Gives up on a level after this many attempts and serves the hardest found, labelled honestly. */
    private const val MAX_ATTEMPTS = 400

    /**
     * Killer has its own ladder, because cage logic solves most puzzles
     * before classic techniques are needed:
     * - Easy: singles and cage combinations only
     * - Medium: needs the 45 rule
     * - Hard: needs classic techniques such as pointing and pairs
     * - Expert: needs those too, with no printed numbers at all, or needs
     *   anything harder (rare)
     */
    fun levelOf(hardest: Technique?, printed: Int): Difficulty = when {
        hardest == null || hardest <= Technique.CAGE -> Difficulty.EASY
        hardest == Technique.FORTY_FIVE -> Difficulty.MEDIUM
        hardest <= Technique.NAKED_TRIPLE -> if (printed == 0) Difficulty.EXPERT else Difficulty.HARD
        else -> Difficulty.EXPERT
    }

    /** What each Killer level asks of the player, for the level buttons. */
    fun describe(level: Difficulty): String = when (level) {
        Difficulty.EASY -> "Cage sums"
        Difficulty.MEDIUM -> "The 45 rule"
        Difficulty.HARD -> "Pointing and pairs too"
        Difficulty.EXPERT -> "No printed numbers"
    }

    /** The hardest technique allowed for a Killer [level]. */
    private fun capFor(level: Difficulty): Technique = when (level) {
        Difficulty.EASY -> Technique.CAGE
        Difficulty.MEDIUM -> Technique.FORTY_FIVE
        Difficulty.HARD, Difficulty.EXPERT -> Technique.NAKED_TRIPLE
    }

    /**
     * Makes a Killer puzzle at [difficulty], graded by the hardest technique
     * it needs on the Killer ladder (see [levelOf]).
     *
     * Each attempt grows cages over a filled grid (bigger cages for harder
     * levels), then solves by logic with techniques capped at the level.
     * Wherever logic gets stuck it prints one cell's number, preferring one
     * that lands the puzzle exactly on the level, until it can be solved
     * without guessing. A logic solve uses only sound steps, so it also
     * proves the solution is unique. Attempts that come out easier are
     * thrown away.
     */
    fun generate(difficulty: Difficulty = Difficulty.MEDIUM, random: Random = Random.Default): Puzzle {
        var fallback: Puzzle? = null
        for (i in 0 until MAX_ATTEMPTS) {
            val candidate = attempt(difficulty, random) ?: continue
            if (candidate.difficulty == difficulty) return candidate
            if (fallback == null || candidate.difficulty > fallback.difficulty) fallback = candidate
        }
        // Practically unreachable; fall back to a Hard puzzle, labelled honestly.
        return fallback ?: generate(Difficulty.HARD, random)
    }

    /** One try at [target]: a puzzle solvable by logic, or null if this layout can't make [target]. */
    private fun attempt(target: Difficulty, random: Random): Puzzle? {
        val solution = Solver.randomFilledGrid(random)
        val cages = cagesFor(solution, random, target)
        val givens = IntArray(Grid.CELLS)
        val cap = capFor(target)
        fun solve() = LogicSolver(givens, cages).solve(maxTechnique = cap)
        fun printed() = givens.count { it != 0 }
        fun puzzle(grade: Grade) = Puzzle(givens.copyOf(), solution, levelOf(grade.hardest, printed()), grade.hardest, cages)

        var grade = solve()
        // Expert means no printed numbers, so a layout that needs any is no use for it.
        if (target == Difficulty.EXPERT && !grade.solved) return null
        while (!grade.solved) {
            val stuck = (0 until Grid.CELLS).filter { grade.stuckValues[it] == 0 && givens[it] == 0 }.shuffled(random)
            var keepsItHard = -1
            // Trying every stuck cell is slow for Killer, so try a handful.
            for (cell in stuck.take(12)) {
                givens[cell] = solution[cell]
                val trial = solve()
                if (trial.solved && levelOf(trial.hardest, printed()) == target) return puzzle(trial)
                if (!trial.solved && keepsItHard < 0) keepsItHard = cell
                givens[cell] = 0
            }
            val cell = if (keepsItHard >= 0) keepsItHard else stuck.first()
            givens[cell] = solution[cell]
            grade = solve()
        }
        return puzzle(grade)
    }

    /**
     * Splits the grid into cages of 1 to 5 cells with no repeated number in
     * a cage: mostly 2 to 3 cells for easier levels, 3 to 5 for harder ones.
     */
    fun cagesFor(solution: IntArray, random: Random, difficulty: Difficulty = Difficulty.MEDIUM): List<Cage> {
        // Cumulative chances (out of 100) of a cage aiming for 1, 2, 3 and 4 cells; the rest aim for 5.
        val sizes = when (difficulty) {
            Difficulty.EASY -> intArrayOf(6, 50, 85, 97)
            Difficulty.MEDIUM -> intArrayOf(4, 38, 76, 95)
            Difficulty.HARD -> intArrayOf(2, 25, 60, 88)
            Difficulty.EXPERT -> intArrayOf(1, 18, 50, 82)
        }
        val owner = IntArray(Grid.CELLS) { -1 }
        val cages = ArrayList<MutableList<Int>>()
        for (start in (0 until Grid.CELLS).shuffled(random)) {
            if (owner[start] >= 0) continue
            val roll = random.nextInt(100)
            val target = 1 + sizes.count { roll >= it }
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
