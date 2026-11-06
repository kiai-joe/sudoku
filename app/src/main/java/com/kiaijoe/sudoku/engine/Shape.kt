package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/**
 * Which groups of cells must each hold 1 to 9 once: the rows and columns,
 * the boxes, and for Sudoku X the two long diagonals.
 *
 * [regionOf] gives each cell's box: the 3×3 boxes for classic and X, or nine
 * irregular, connected regions of nine cells for Jigsaw.
 */
class Shape private constructor(val regionOf: IntArray, val diagonals: Boolean) {

    /** True when the boxes are irregular regions. */
    val isJigsaw: Boolean = !regionOf.contentEquals(CLASSIC_REGIONS)

    /** The boxes (or regions), each in reading order. */
    val boxes: List<House> =
        if (isJigsaw) {
            List(9) { r -> House(HouseKind.REGION, r, (0 until Grid.CELLS).filter { regionOf[it] == r }.toIntArray()) }
        } else {
            House.BOXES
        }

    /** The rows, columns and (for X) diagonals: the lines a box can meet. */
    val lines: List<House> = House.ROWS + House.COLUMNS + if (diagonals) House.DIAGONALS else emptyList()

    /** Every group that must hold 1 to 9 once. */
    val houses: List<House> = boxes + lines

    /** Boxes first, then rows, columns and diagonals: the order people look for hidden singles in. */
    val boxesFirst: List<House> get() = houses

    /**
     * Rows, columns, boxes, then diagonals: the order subsets are searched in.
     * For classic it's the order the solver has always used, so grading and
     * stored lesson examples don't change.
     */
    val units: List<House> = House.ROWS + House.COLUMNS + boxes + if (diagonals) House.DIAGONALS else emptyList()

    /** For each cell, the houses it belongs to. */
    val housesOf: Array<List<House>> = Array(Grid.CELLS) { cell -> houses.filter { h -> cell in h } }

    /** For each cell, the indices in [houses] of the houses it belongs to. */
    val houseIndicesOf: Array<IntArray> = Array(Grid.CELLS) { cell -> houses.indices.filter { cell in houses[it] }.toIntArray() }

    /** For each cell, every other cell that shares a house with it. */
    val peers: Array<IntArray> = Array(Grid.CELLS) { cell ->
        housesOf[cell].flatMap { it.cells.toList() }.filter { it != cell }.distinct().sorted().toIntArray()
    }

    private val peerSets: Array<Set<Int>> = Array(Grid.CELLS) { peers[it].toSet() }

    fun arePeers(a: Int, b: Int) = a != b && b in peerSets[a]

    /** [cell]'s box or region. */
    fun boxOf(cell: Int): House = boxes[regionOf[cell]]

    /** True when every house holds each digit 1-9 exactly once. */
    fun isValidSolution(grid: IntArray): Boolean {
        if (grid.size != Grid.CELLS || grid.any { it !in 1..9 }) return false
        return houses.all { h -> h.cells.fold(0) { m, c -> m or (1 shl (grid[c] - 1)) } == Grid.ALL_DIGITS }
    }

    /** "-" for classic, "x" for Sudoku X, or "j" and the 81 region digits for Jigsaw. */
    fun encode(): String = when {
        isJigsaw -> "j" + regionOf.joinToString("")
        diagonals -> "x"
        else -> "-"
    }

    override fun equals(other: Any?) = other is Shape && other.diagonals == diagonals && other.regionOf.contentEquals(regionOf)
    override fun hashCode() = regionOf.contentHashCode() * 31 + diagonals.hashCode()

    companion object {
        private val CLASSIC_REGIONS = IntArray(Grid.CELLS) { Grid.box(it) }

        val CLASSIC = Shape(CLASSIC_REGIONS, diagonals = false)

        /** Sudoku X: both long diagonals hold 1 to 9 too. */
        val X = Shape(CLASSIC_REGIONS, diagonals = true)

        /** A Jigsaw shape with these regions. Each must be nine connected cells. */
        fun jigsaw(regionOf: IntArray): Shape {
            require(isValidLayout(regionOf)) { "Not a jigsaw layout" }
            return Shape(regionOf.copyOf(), diagonals = false)
        }

        /** Reads [encode]'s text back, or null for anything else. */
        fun decode(s: String): Shape? = when {
            s == "-" -> CLASSIC
            s == "x" -> X
            s.startsWith("j") && s.length == 1 + Grid.CELLS ->
                runCatching { jigsaw(IntArray(Grid.CELLS) { s[it + 1].digitToInt() }) }.getOrNull()
            else -> null
        }

        /** True when [regionOf] has nine regions of nine cells, each connected through edges. */
        fun isValidLayout(regionOf: IntArray): Boolean {
            if (regionOf.size != Grid.CELLS || regionOf.any { it !in 0..8 }) return false
            return (0..8).all { r ->
                val cells = (0 until Grid.CELLS).filter { regionOf[it] == r }
                cells.size == 9 && connected(cells.toSet())
            }
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

        private fun connected(cells: Set<Int>): Boolean {
            if (cells.isEmpty()) return false
            val seen = HashSet<Int>()
            val stack = ArrayDeque(listOf(cells.first()))
            while (stack.isNotEmpty()) {
                val c = stack.removeLast()
                if (!seen.add(c)) continue
                for (n in neighbours(c)) if (n in cells && n !in seen) stack.addLast(n)
            }
            return seen.size == cells.size
        }

        /**
         * A random Jigsaw layout: start from the 3×3 boxes and swap pairs of
         * cells across region borders many times, keeping every region
         * connected, so the regions wander into irregular shapes.
         */
        fun randomJigsaw(random: Random, swaps: Int = 400): IntArray {
            val regions = CLASSIC_REGIONS.copyOf()
            var done = 0
            var tries = 0
            while (done < swaps && tries < swaps * 50) {
                tries++
                // Two cells on a border between regions A and B, each swapped into the other.
                val a = random.nextInt(Grid.CELLS)
                val na = neighbours(a).filter { regions[it] != regions[a] }
                if (na.isEmpty()) continue
                val ra = regions[a]
                val rb = regions[na.random(random)]
                val bCandidates = (0 until Grid.CELLS).filter { b ->
                    regions[b] == rb && b != a && neighbours(b).any { regions[it] == ra && it != a }
                }
                if (bCandidates.isEmpty()) continue
                val b = bCandidates.random(random)
                regions[a] = rb
                regions[b] = ra
                val aCells = (0 until Grid.CELLS).filter { regions[it] == ra }.toSet()
                val bCells = (0 until Grid.CELLS).filter { regions[it] == rb }.toSet()
                if (connected(aCells) && connected(bCells)) {
                    done++
                } else {
                    regions[a] = ra
                    regions[b] = rb
                }
            }
            return regions
        }
    }
}
