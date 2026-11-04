package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/**
 * Bitmask backtracking solver. It always branches on the empty cell with the
 * fewest candidates, which keeps uniqueness checks fast enough to run many
 * times while a puzzle is being generated.
 */
object Solver {

    /** Returns the solution, or null when the grid has none. 0 marks an empty cell. */
    fun solve(grid: IntArray): IntArray? {
        val out = IntArray(Grid.CELLS)
        return if (run(grid, limit = 1, random = null, out = out) == 1) out else null
    }

    /** Counts solutions, stopping once [limit] is reached. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int = run(grid, limit, random = null, out = null)

    fun hasUniqueSolution(grid: IntArray): Boolean = countSolutions(grid, limit = 2) == 1

    /** A complete, valid grid with digits chosen at random. */
    fun randomFilledGrid(random: Random): IntArray {
        val out = IntArray(Grid.CELLS)
        run(IntArray(Grid.CELLS), limit = 1, random = random, out = out)
        return out
    }

    private fun run(grid: IntArray, limit: Int, random: Random?, out: IntArray?): Int {
        require(grid.size == Grid.CELLS) { "Grid must have 81 cells" }
        val g = grid.copyOf()
        val rows = IntArray(Grid.SIZE)
        val cols = IntArray(Grid.SIZE)
        val boxes = IntArray(Grid.SIZE)
        for (cell in 0 until Grid.CELLS) {
            val d = g[cell]
            if (d == 0) continue
            if (d !in 1..9) return 0
            val bit = 1 shl (d - 1)
            val r = Grid.row(cell); val c = Grid.col(cell); val b = Grid.box(cell)
            if ((rows[r] or cols[c] or boxes[b]) and bit != 0) return 0
            rows[r] = rows[r] or bit
            cols[c] = cols[c] or bit
            boxes[b] = boxes[b] or bit
        }
        return search(g, rows, cols, boxes, limit, random, out)
    }

    private fun search(
        g: IntArray,
        rows: IntArray,
        cols: IntArray,
        boxes: IntArray,
        limit: Int,
        random: Random?,
        out: IntArray?,
    ): Int {
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until Grid.CELLS) {
            if (g[cell] != 0) continue
            val used = rows[Grid.row(cell)] or cols[Grid.col(cell)] or boxes[Grid.box(cell)]
            val mask = Grid.ALL_DIGITS and used.inv()
            val count = Integer.bitCount(mask)
            if (count == 0) return 0
            if (count < bestCount) {
                best = cell; bestMask = mask; bestCount = count
                if (count == 1) break
            }
        }
        if (best == -1) {
            out?.let { g.copyInto(it) }
            return 1
        }

        val digits = IntArray(bestCount)
        var n = 0
        for (d in 1..9) if (bestMask and (1 shl (d - 1)) != 0) digits[n++] = d
        random?.let { digits.shuffle(it) }

        val r = Grid.row(best); val c = Grid.col(best); val b = Grid.box(best)
        var total = 0
        for (d in digits) {
            val bit = 1 shl (d - 1)
            g[best] = d
            rows[r] = rows[r] or bit
            cols[c] = cols[c] or bit
            boxes[b] = boxes[b] or bit
            total += search(g, rows, cols, boxes, limit - total, random, out)
            g[best] = 0
            rows[r] = rows[r] and bit.inv()
            cols[c] = cols[c] and bit.inv()
            boxes[b] = boxes[b] and bit.inv()
            if (total >= limit) break
        }
        return total
    }
}
