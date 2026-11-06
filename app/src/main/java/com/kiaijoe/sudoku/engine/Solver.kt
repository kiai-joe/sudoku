package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/**
 * Bitmask backtracking solver. It always branches on the empty cell with the
 * fewest candidates, which keeps uniqueness checks fast enough to run many
 * times while a puzzle is being generated. It works on any [Shape]: classic,
 * Sudoku X or Jigsaw.
 */
object Solver {

    /** Returns the solution, or null when the grid has none. 0 marks an empty cell. */
    fun solve(grid: IntArray, shape: Shape = Shape.CLASSIC): IntArray? {
        val out = IntArray(Grid.CELLS)
        return if (run(grid, shape, limit = 1, random = null, out = out) == 1) out else null
    }

    /** Counts solutions, stopping once [limit] is reached. */
    fun countSolutions(grid: IntArray, limit: Int = 2, shape: Shape = Shape.CLASSIC): Int =
        run(grid, shape, limit, random = null, out = null)

    fun hasUniqueSolution(grid: IntArray, shape: Shape = Shape.CLASSIC): Boolean = countSolutions(grid, limit = 2, shape = shape) == 1

    /**
     * A complete, valid grid with digits chosen at random, or null if
     * [maxNodes] search steps weren't enough (some Jigsaw layouts are slow or
     * impossible to fill; the caller then tries another layout).
     */
    fun randomFilledGrid(random: Random, shape: Shape = Shape.CLASSIC, maxNodes: Long = Long.MAX_VALUE): IntArray? {
        val out = IntArray(Grid.CELLS)
        val found = run(IntArray(Grid.CELLS), shape, limit = 1, random = random, out = out, maxNodes = maxNodes)
        return if (found == 1) out else null
    }

    /** A classic filled grid; always succeeds. */
    fun randomFilledGrid(random: Random): IntArray = randomFilledGrid(random, Shape.CLASSIC)!!

    private class Search(val shape: Shape, val limit: Int, val random: Random?, val out: IntArray?, val maxNodes: Long) {
        /** For each cell, the indices of its houses in [Shape.houses]. */
        val cellHouses: Array<IntArray> = shape.houseIndicesOf
        val used = IntArray(shape.houses.size)
        var nodes = 0L
    }

    private fun run(grid: IntArray, shape: Shape, limit: Int, random: Random?, out: IntArray?, maxNodes: Long = Long.MAX_VALUE): Int {
        require(grid.size == Grid.CELLS) { "Grid must have 81 cells" }
        val g = grid.copyOf()
        val s = Search(shape, limit, random, out, maxNodes)
        for (cell in 0 until Grid.CELLS) {
            val d = g[cell]
            if (d == 0) continue
            if (d !in 1..9) return 0
            val bit = 1 shl (d - 1)
            for (h in s.cellHouses[cell]) {
                if (s.used[h] and bit != 0) return 0
                s.used[h] = s.used[h] or bit
            }
        }
        return search(g, s, limit)
    }

    private fun search(g: IntArray, s: Search, limit: Int): Int {
        if (++s.nodes > s.maxNodes) return 0
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until Grid.CELLS) {
            if (g[cell] != 0) continue
            var used = 0
            for (h in s.cellHouses[cell]) used = used or s.used[h]
            val mask = Grid.ALL_DIGITS and used.inv()
            val count = Integer.bitCount(mask)
            if (count == 0) return 0
            if (count < bestCount) {
                best = cell; bestMask = mask; bestCount = count
                if (count == 1) break
            }
        }
        if (best == -1) {
            s.out?.let { g.copyInto(it) }
            return 1
        }

        val digits = IntArray(bestCount)
        var n = 0
        for (d in 1..9) if (bestMask and (1 shl (d - 1)) != 0) digits[n++] = d
        s.random?.let { digits.shuffle(it) }

        val houses = s.cellHouses[best]
        var total = 0
        for (d in digits) {
            val bit = 1 shl (d - 1)
            g[best] = d
            for (h in houses) s.used[h] = s.used[h] or bit
            total += search(g, s, limit - total)
            g[best] = 0
            for (h in houses) s.used[h] = s.used[h] and bit.inv()
            if (total >= limit || s.nodes > s.maxNodes) break
        }
        return total
    }
}
