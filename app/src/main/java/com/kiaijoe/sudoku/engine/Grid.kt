package com.kiaijoe.sudoku.engine

/** Index helpers for a 9x9 grid stored row-major in 81 cells. */
object Grid {
    const val SIZE = 9
    const val CELLS = 81
    internal const val ALL_DIGITS = 0x1FF

    fun row(cell: Int) = cell / SIZE
    fun col(cell: Int) = cell % SIZE
    fun box(cell: Int) = (row(cell) / 3) * 3 + col(cell) / 3

    /** For each cell, the 20 other cells that share its row, column or box. */
    val peers: Array<IntArray> = Array(CELLS) { cell ->
        (0 until CELLS).filter { other ->
            other != cell &&
                (row(other) == row(cell) || col(other) == col(cell) || box(other) == box(cell))
        }.toIntArray()
    }

    fun arePeers(a: Int, b: Int) = a != b && (row(a) == row(b) || col(a) == col(b) || box(a) == box(b))

    /** True when every row, column and box holds each digit 1-9 exactly once. */
    fun isValidSolution(grid: IntArray): Boolean {
        if (grid.size != CELLS) return false
        val rows = IntArray(SIZE)
        val cols = IntArray(SIZE)
        val boxes = IntArray(SIZE)
        for (cell in 0 until CELLS) {
            val d = grid[cell]
            if (d !in 1..9) return false
            val bit = 1 shl (d - 1)
            val r = row(cell); val c = col(cell); val b = box(cell)
            if (rows[r] and bit != 0 || cols[c] and bit != 0 || boxes[b] and bit != 0) return false
            rows[r] = rows[r] or bit
            cols[c] = cols[c] or bit
            boxes[b] = boxes[b] or bit
        }
        return true
    }
}
