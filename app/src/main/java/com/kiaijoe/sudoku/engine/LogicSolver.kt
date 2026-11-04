package com.kiaijoe.sudoku.engine

/**
 * Solving techniques a person uses, easiest first. [level] is the
 * difficulty a puzzle gets when this is the hardest technique it needs.
 */
enum class Technique(val label: String, val level: Difficulty, val killerOnly: Boolean = false) {
    NAKED_SINGLE("Naked single", Difficulty.EASY),
    HIDDEN_SINGLE("Hidden single", Difficulty.EASY),
    CAGE("Cage combinations", Difficulty.EASY, killerOnly = true),
    FORTY_FIVE("45 rule", Difficulty.MEDIUM, killerOnly = true),
    POINTING("Pointing", Difficulty.MEDIUM),
    CLAIMING("Box/line reduction", Difficulty.MEDIUM),
    NAKED_PAIR("Naked pair", Difficulty.MEDIUM),
    HIDDEN_PAIR("Hidden pair", Difficulty.MEDIUM),
    NAKED_TRIPLE("Naked triple", Difficulty.MEDIUM),
    HIDDEN_TRIPLE("Hidden triple", Difficulty.HARD),
    X_WING("X-Wing", Difficulty.HARD),
    XY_WING("XY-Wing", Difficulty.HARD),
    NAKED_QUAD("Naked quad", Difficulty.HARD),
    HIDDEN_QUAD("Hidden quad", Difficulty.EXPERT),
    SWORDFISH("Swordfish", Difficulty.EXPERT),
    XYZ_WING("XYZ-Wing", Difficulty.EXPERT),
}

enum class HouseKind { ROW, COLUMN, BOX }

/** A row, column or box. [index] runs 0-8; boxes go left to right, top to bottom. */
data class House(val kind: HouseKind, val index: Int) {
    /** The nine cells, in reading order. Shared; do not modify. */
    val cells: IntArray get() = CELLS[kind.ordinal][index]

    companion object {
        private val CELLS: Array<Array<IntArray>> = arrayOf(
            Array(9) { r -> IntArray(9) { c -> r * 9 + c } },
            Array(9) { c -> IntArray(9) { r -> r * 9 + c } },
            Array(9) { b -> IntArray(9) { i -> ((b / 3) * 3 + i / 3) * 9 + (b % 3) * 3 + i % 3 } },
        )

        val ROWS = List(9) { House(HouseKind.ROW, it) }
        val COLUMNS = List(9) { House(HouseKind.COLUMN, it) }
        val BOXES = List(9) { House(HouseKind.BOX, it) }
    }
}

/**
 * One deduction: either a digit placed in a cell, or candidates removed.
 * [eliminations] maps cell to the bitmask of digits removed from it.
 *
 * The rest describes why, for explaining it: [pattern] holds the cells that
 * form the pattern (in a fixed order per technique), [houses] the rows,
 * columns and boxes involved, and [digits] the bitmask of digits it is about.
 */
class Step(
    val technique: Technique,
    val cell: Int = -1,
    val digit: Int = 0,
    val eliminations: Map<Int, Int> = emptyMap(),
    val pattern: List<Int> = emptyList(),
    val houses: List<House> = emptyList(),
    val digits: Int = 0,
    val cage: Cage? = null,
    val fortyFive: FortyFive? = null,
) {
    val isPlacement get() = digit != 0
}

/**
 * The working behind a 45-rule step. The rows, columns or boxes in [houses]
 * add up to [total] (45 each). Cages entirely inside add up to [insideSum].
 * Cells of the other cages that cross the edge are either inside (innies)
 * or, when [outies], outside; [cells] are the empty ones and must add up to [sum].
 * [crossingSum] is the full sum of the crossing cages, used to explain outies.
 */
data class FortyFive(
    val houses: List<House>,
    val total: Int,
    val insideSum: Int,
    val crossingSum: Int,
    val outies: Boolean,
    val cells: List<Int>,
    val sum: Int,
)

/**
 * The result of solving by logic alone.
 *
 * @property solved true when the techniques up to the allowed level filled the grid.
 * @property hardest the hardest technique used, or null if nothing was needed.
 * @property steps every deduction made, in order.
 * @property stuckValues the grid at the point the solver stopped.
 */
class Grade(
    val solved: Boolean,
    val hardest: Technique?,
    val steps: List<Step>,
    val stuckValues: IntArray,
) {
    /** The difficulty this puzzle deserves, or null if logic could not solve it. */
    val level: Difficulty? get() = if (solved) hardest?.level ?: Difficulty.EASY else null

    fun count(technique: Technique) = steps.count { it.technique == technique }
}

/**
 * Solves a puzzle the way a person does, with pencil-mark candidates and the
 * techniques in [Technique], always trying the easiest one that makes
 * progress. The hardest technique needed sets the puzzle's difficulty.
 */
class LogicSolver(givens: IntArray, private val cages: List<Cage> = emptyList()) {
    private val values = IntArray(Grid.CELLS)
    private val cands = IntArray(Grid.CELLS) { Grid.ALL_DIGITS }

    /**
     * Goes up whenever a cell's value or candidates change. Killer checks
     * remember the total for their cells when they last found nothing, and
     * skip until it moves; cage arithmetic is the slow part of solving.
     */
    private val version = IntArray(Grid.CELLS)

    init {
        require(givens.size == Grid.CELLS)
        for (cell in 0 until Grid.CELLS) if (givens[cell] != 0) place(cell, givens[cell])
    }

    fun candidates(cell: Int) = cands[cell]
    fun value(cell: Int) = values[cell]

    /**
     * Applies easiest-first deductions until solved or nothing at or below
     * [maxLevel] (and, if given, no technique past [maxTechnique]) helps.
     */
    fun solve(maxLevel: Difficulty = Difficulty.EXPERT, maxTechnique: Technique? = null): Grade {
        val steps = ArrayList<Step>()
        var hardest: Technique? = null
        while (true) {
            if (values.all { it != 0 }) return Grade(true, hardest, steps, values.copyOf())
            if (hasContradiction()) return Grade(false, hardest, steps, values.copyOf())
            val step = nextStep(maxLevel, maxTechnique) ?: return Grade(false, hardest, steps, values.copyOf())
            apply(step)
            steps += step
            if (hardest == null || step.technique.ordinal > hardest.ordinal) hardest = step.technique
        }
    }

    /** The easiest deduction available now, or null. Does not change the grid. */
    fun nextStep(maxLevel: Difficulty = Difficulty.EXPERT, maxTechnique: Technique? = null): Step? {
        for (t in Technique.entries) {
            if (t.level > maxLevel || (maxTechnique != null && t > maxTechnique)) break
            val step = when (t) {
                Technique.NAKED_SINGLE -> nakedSingle()
                Technique.HIDDEN_SINGLE -> hiddenSingle()
                Technique.CAGE -> cageCombinations()
                Technique.FORTY_FIVE -> fortyFive()
                Technique.POINTING -> pointing()
                Technique.CLAIMING -> claiming()
                Technique.NAKED_PAIR -> nakedSubset(2, t)
                Technique.HIDDEN_PAIR -> hiddenSubset(2, t)
                Technique.NAKED_TRIPLE -> nakedSubset(3, t)
                Technique.HIDDEN_TRIPLE -> hiddenSubset(3, t)
                Technique.X_WING -> fish(2, t)
                Technique.XY_WING -> xyWing()
                Technique.NAKED_QUAD -> nakedSubset(4, t)
                Technique.HIDDEN_QUAD -> hiddenSubset(4, t)
                Technique.SWORDFISH -> fish(3, t)
                Technique.XYZ_WING -> xyzWing()
            }
            if (step != null) return step
        }
        return null
    }

    fun apply(step: Step) {
        if (step.isPlacement) {
            place(step.cell, step.digit)
        } else {
            for ((cell, mask) in step.eliminations) {
                cands[cell] = cands[cell] and mask.inv()
                version[cell]++
            }
        }
    }

    private fun place(cell: Int, digit: Int) {
        values[cell] = digit
        cands[cell] = 0
        version[cell]++
        val bit = bit(digit)
        for (peer in Grid.peers[cell]) {
            if (cands[peer] and bit != 0) {
                cands[peer] = cands[peer] and bit.inv()
                version[peer]++
            }
        }
    }

    private fun versionOf(cells: List<Int>): Int {
        var total = 0
        for (cell in cells) total += version[cell]
        return total
    }

    private fun hasContradiction(): Boolean {
        for (cell in 0 until Grid.CELLS) if (values[cell] == 0 && cands[cell] == 0) return true
        return false
    }

    // --- Singles ---------------------------------------------------------

    private fun nakedSingle(): Step? {
        for (cell in 0 until Grid.CELLS) {
            if (values[cell] == 0 && Integer.bitCount(cands[cell]) == 1) {
                val d = digitOf(cands[cell])
                return Step(
                    Technique.NAKED_SINGLE, cell, d,
                    pattern = listOf(cell),
                    houses = listOf(House.ROWS[Grid.row(cell)], House.COLUMNS[Grid.col(cell)], House.BOXES[Grid.box(cell)]),
                    digits = bit(d),
                )
            }
        }
        return null
    }

    private fun hiddenSingle(): Step? {
        // Boxes first: the place people look for these.
        for (house in BOXES_FIRST) {
            val unit = house.cells
            for (d in 1..9) {
                var only = -1
                var count = 0
                for (cell in unit) {
                    if (cands[cell] and bit(d) != 0) {
                        only = cell
                        count++
                    }
                }
                if (count == 1) {
                    return Step(Technique.HIDDEN_SINGLE, only, d, pattern = listOf(only), houses = listOf(house), digits = bit(d))
                }
            }
        }
        return null
    }

    // --- Killer cages ------------------------------------------------------

    /**
     * A cage's empty cells must hold different numbers, not already in the
     * cage, that make up the rest of its sum. Any candidate that appears in
     * no such filling goes.
     */
    private val cageChecked = IntArray(cages.size) { -1 }

    private fun cageCombinations(): Step? {
        for ((index, cage) in cages.withIndex()) {
            val seen = versionOf(cage.cells)
            if (seen == cageChecked[index]) continue
            val open = cage.cells.filter { values[it] == 0 }
            if (open.isEmpty()) continue
            var used = 0
            var placed = 0
            for (cell in cage.cells) {
                if (values[cell] != 0) {
                    used = used or bit(values[cell])
                    placed += values[cell]
                }
            }
            val possible = Killer.possibleDigits(open.map { cands[it] }, cage.sum - placed, used)
            val elim = HashMap<Int, Int>()
            for ((i, cell) in open.withIndex()) {
                val extra = cands[cell] and possible[i].inv()
                if (extra != 0) elim[cell] = extra
            }
            if (elim.isNotEmpty()) {
                val digits = elim.values.fold(0) { a, b -> a or b }
                return Step(Technique.CAGE, eliminations = elim, pattern = cage.cells, digits = digits, cage = cage)
            }
            cageChecked[index] = seen
        }
        return null
    }

    /** Which cage each cell is in, for Killer puzzles. */
    private val cageOf: IntArray = IntArray(Grid.CELLS) { -1 }.also { o ->
        cages.forEachIndexed { i, cage -> cage.cells.forEach { o[it] = i } }
    }

    /**
     * The 45 rule. Every row, column and box adds up to 45, so k of them
     * together add up to 45k. Take such an area: the cages entirely inside
     * it add up to a known amount, so the cells of crossing cages that lie
     * inside (innies) must make up the rest. Likewise the crossing cages'
     * cells outside the area (outies) must add up to the crossing cages'
     * total minus what their innies hold. When those cells are few, their sum
     * limits their numbers, like a small cage whose cells only have to differ
     * when they share a row, column, box or cage.
     */
    private val areaChecked by lazy { IntArray(areas.size) { -1 } }

    private fun fortyFive(): Step? {
        for ((index, area) in areas.withIndex()) {
            val seen = versionOf(area.innies) + versionOf(area.outies)
            if (seen == areaChecked[index]) continue
            limitBySum(area.innies, area.innieSum)?.let { (elim, open, rest) ->
                return fortyFiveStep(area, elim, open, rest, outies = false)
            }
            limitBySum(area.outies, area.outieSum)?.let { (elim, open, rest) ->
                return fortyFiveStep(area, elim, open, rest, outies = true)
            }
            areaChecked[index] = seen
        }
        return null
    }

    private fun fortyFiveStep(area: Area, elim: Map<Int, Int>, open: List<Int>, rest: Int, outies: Boolean) = Step(
        Technique.FORTY_FIVE, eliminations = elim, pattern = open, houses = area.houses,
        digits = elim.values.fold(0) { a, b -> a or b },
        fortyFive = FortyFive(area.houses, area.total, area.insideSum, area.crossingSum, outies, open, rest),
    )

    /** An area for the 45 rule, with the parts that only depend on the cages worked out once. */
    private class Area(
        val houses: List<House>,
        val total: Int,
        val insideSum: Int,
        val crossingSum: Int,
        val innies: List<Int>,
        val outies: List<Int>,
    ) {
        val innieSum get() = total - insideSum
        val outieSum get() = crossingSum - innieSum
    }

    /** The areas worth checking: those with a few innies or outies (at most 4 cells either way). */
    private val areas: List<Area> by lazy {
        if (cages.isEmpty()) return@lazy emptyList()
        REGIONS.mapNotNull { region ->
            val inside = BooleanArray(Grid.CELLS)
            for (house in region) for (cell in house.cells) inside[cell] = true
            var insideSum = 0
            var crossingSum = 0
            val innies = ArrayList<Int>()
            val outies = ArrayList<Int>()
            for (cage in cages) {
                val count = cage.cells.count { inside[it] }
                when (count) {
                    0 -> Unit
                    cage.cells.size -> insideSum += cage.sum
                    else -> {
                        crossingSum += cage.sum
                        for (cell in cage.cells) if (inside[cell]) innies += cell else outies += cell
                    }
                }
            }
            if (innies.isEmpty() || (innies.size > 4 && outies.size > 4)) {
                null
            } else {
                Area(region, 45 * region.size, insideSum, crossingSum, innies, outies)
            }
        }
    }

    /**
     * If [cells] must add up to [sum] and at most four of them are empty,
     * the candidates that fit no such filling. Returns the eliminations, the
     * empty cells and what they must add up to, or null if nothing goes.
     */
    private fun limitBySum(cells: List<Int>, sum: Int): Triple<Map<Int, Int>, List<Int>, Int>? {
        val open = cells.filter { values[it] == 0 }
        if (open.isEmpty() || open.size > 4) return null
        val rest = sum - cells.sumOf { values[it] }
        val possible = Killer.possibleDigits(open.map { cands[it] }, rest, used = 0) { i, j ->
            Grid.arePeers(open[i], open[j]) || cageOf[open[i]] == cageOf[open[j]]
        }
        val elim = HashMap<Int, Int>()
        for ((i, cell) in open.withIndex()) {
            val extra = cands[cell] and possible[i].inv()
            if (extra != 0) elim[cell] = extra
        }
        return if (elim.isEmpty()) null else Triple(elim, open, rest)
    }

    // --- Intersections ---------------------------------------------------

    /** A digit confined to one row or column inside a box leaves the rest of that line. */
    private fun pointing(): Step? {
        for (boxHouse in House.BOXES) {
            val box = boxHouse.cells
            for (d in 1..9) {
                val cells = box.filter { cands[it] and bit(d) != 0 }
                if (cells.size < 2) continue
                val line = when {
                    cells.all { Grid.row(it) == Grid.row(cells[0]) } -> House.ROWS[Grid.row(cells[0])]
                    cells.all { Grid.col(it) == Grid.col(cells[0]) } -> House.COLUMNS[Grid.col(cells[0])]
                    else -> continue
                }
                val elim = eliminate(line.cells.filter { it !in box }, bit(d))
                if (elim.isNotEmpty()) {
                    return Step(Technique.POINTING, eliminations = elim, pattern = cells, houses = listOf(boxHouse, line), digits = bit(d))
                }
            }
        }
        return null
    }

    /** A digit confined to one box inside a row or column leaves the rest of that box. */
    private fun claiming(): Step? {
        for (lineHouse in House.ROWS + House.COLUMNS) {
            val line = lineHouse.cells
            for (d in 1..9) {
                val cells = line.filter { cands[it] and bit(d) != 0 }
                if (cells.size < 2) continue
                val b = Grid.box(cells[0])
                if (!cells.all { Grid.box(it) == b }) continue
                val box = House.BOXES[b]
                val elim = eliminate(box.cells.filter { it !in line }, bit(d))
                if (elim.isNotEmpty()) {
                    return Step(Technique.CLAIMING, eliminations = elim, pattern = cells, houses = listOf(lineHouse, box), digits = bit(d))
                }
            }
        }
        return null
    }

    // --- Subsets ---------------------------------------------------------

    /** k cells in a unit that between them hold only k digits: those digits leave the unit's other cells. */
    private fun nakedSubset(k: Int, technique: Technique): Step? {
        for (house in ALL_UNITS) {
            val unit = house.cells
            val open = unit.filter { values[it] == 0 && Integer.bitCount(cands[it]) in 2..k }
            if (open.size < k) continue
            for (combo in combinations(open.size, k)) {
                var union = 0
                for (i in combo) union = union or cands[open[i]]
                if (Integer.bitCount(union) != k) continue
                val chosen = combo.map { open[it] }
                val elim = eliminate(unit.filter { values[it] == 0 && it !in chosen }, union)
                if (elim.isNotEmpty()) {
                    return Step(technique, eliminations = elim, pattern = chosen, houses = listOf(house), digits = union)
                }
            }
        }
        return null
    }

    /** k digits that fit only in the same k cells of a unit: those cells lose every other digit. */
    private fun hiddenSubset(k: Int, technique: Technique): Step? {
        for (house in ALL_UNITS) {
            val unit = house.cells
            // For each open digit, the positions (0-8) in the unit where it can go.
            val digits = ArrayList<Int>()
            val where = ArrayList<Int>()
            for (d in 1..9) {
                var mask = 0
                for ((pos, cell) in unit.withIndex()) if (cands[cell] and bit(d) != 0) mask = mask or (1 shl pos)
                if (Integer.bitCount(mask) in 2..k) {
                    digits += d
                    where += mask
                }
            }
            if (digits.size < k) continue
            for (combo in combinations(digits.size, k)) {
                var cellsMask = 0
                var digitMask = 0
                for (i in combo) {
                    cellsMask = cellsMask or where[i]
                    digitMask = digitMask or bit(digits[i])
                }
                if (Integer.bitCount(cellsMask) != k) continue
                val elim = HashMap<Int, Int>()
                for (pos in 0 until 9) {
                    if (cellsMask and (1 shl pos) == 0) continue
                    val cell = unit[pos]
                    val extra = cands[cell] and digitMask.inv()
                    if (extra != 0) elim[cell] = extra
                }
                if (elim.isNotEmpty()) {
                    val chosen = (0 until 9).filter { cellsMask and (1 shl it) != 0 }.map { unit[it] }
                    return Step(technique, eliminations = elim, pattern = chosen, houses = listOf(house), digits = digitMask)
                }
            }
        }
        return null
    }

    // --- Fish ------------------------------------------------------------

    /**
     * X-Wing (n = 2) and Swordfish (n = 3): when a digit's places in n rows
     * all fall in the same n columns, it leaves those columns everywhere
     * else. The same holds with rows and columns swapped.
     */
    private fun fish(n: Int, technique: Technique): Step? {
        for (d in 1..9) {
            for (byRow in listOf(true, false)) {
                val bases = if (byRow) House.ROWS else House.COLUMNS
                val covers = if (byRow) House.COLUMNS else House.ROWS
                val baseIdx = ArrayList<Int>()
                val baseMask = ArrayList<Int>()
                for ((i, line) in bases.withIndex()) {
                    var mask = 0
                    for ((pos, cell) in line.cells.withIndex()) if (cands[cell] and bit(d) != 0) mask = mask or (1 shl pos)
                    if (Integer.bitCount(mask) in 2..n) {
                        baseIdx += i
                        baseMask += mask
                    }
                }
                if (baseIdx.size < n) continue
                for (combo in combinations(baseIdx.size, n)) {
                    var union = 0
                    for (i in combo) union = union or baseMask[i]
                    if (Integer.bitCount(union) != n) continue
                    val usedBases = combo.map { baseIdx[it] }.toSet()
                    val targets = ArrayList<Int>()
                    for (c in 0 until 9) {
                        if (union and (1 shl c) == 0) continue
                        for ((pos, cell) in covers[c].cells.withIndex()) if (pos !in usedBases) targets += cell
                    }
                    val elim = eliminate(targets, bit(d))
                    if (elim.isNotEmpty()) {
                        val baseHouses = usedBases.sorted().map { bases[it] }
                        val coverHouses = (0 until 9).filter { union and (1 shl it) != 0 }.map { covers[it] }
                        val cells = baseHouses.flatMap { h -> h.cells.filter { cands[it] and bit(d) != 0 } }
                        return Step(technique, eliminations = elim, pattern = cells, houses = baseHouses + coverHouses, digits = bit(d))
                    }
                }
            }
        }
        return null
    }

    // --- Wings -----------------------------------------------------------

    /**
     * A pivot with candidates {a, b} sees one cell with {a, c} and another
     * with {b, c}. Whichever the pivot is, one of them is c, so c leaves
     * every cell that sees both of them.
     */
    private fun xyWing(): Step? {
        for (pivot in 0 until Grid.CELLS) {
            val pc = cands[pivot]
            if (values[pivot] != 0 || Integer.bitCount(pc) != 2) continue
            val pincers = Grid.peers[pivot].filter {
                values[it] == 0 && Integer.bitCount(cands[it]) == 2 && Integer.bitCount(cands[it] and pc) == 1
            }
            for (i in pincers.indices) for (j in i + 1 until pincers.size) {
                val a = pincers[i]
                val b = pincers[j]
                val ca = cands[a]
                val cb = cands[b]
                if ((ca and pc) == (cb and pc)) continue
                val c = ca and cb and pc.inv()
                if (Integer.bitCount(c) != 1) continue
                val targets = (0 until Grid.CELLS).filter {
                    it != a && it != b && it != pivot && Grid.arePeers(it, a) && Grid.arePeers(it, b)
                }
                val elim = eliminate(targets, c)
                if (elim.isNotEmpty()) return Step(Technique.XY_WING, eliminations = elim, pattern = listOf(pivot, a, b), digits = c)
            }
        }
        return null
    }

    /** Like an XY-Wing, but the pivot also holds c, so targets must see the pivot too. */
    private fun xyzWing(): Step? {
        for (pivot in 0 until Grid.CELLS) {
            val pc = cands[pivot]
            if (values[pivot] != 0 || Integer.bitCount(pc) != 3) continue
            val pincers = Grid.peers[pivot].filter {
                values[it] == 0 && Integer.bitCount(cands[it]) == 2 && (cands[it] and pc.inv()) == 0
            }
            for (i in pincers.indices) for (j in i + 1 until pincers.size) {
                val a = pincers[i]
                val b = pincers[j]
                if ((cands[a] or cands[b]) != pc) continue
                val c = cands[a] and cands[b]
                if (Integer.bitCount(c) != 1) continue
                val targets = Grid.peers[pivot].filter {
                    it != a && it != b && Grid.arePeers(it, a) && Grid.arePeers(it, b)
                }
                val elim = eliminate(targets, c)
                if (elim.isNotEmpty()) return Step(Technique.XYZ_WING, eliminations = elim, pattern = listOf(pivot, a, b), digits = c)
            }
        }
        return null
    }

    // --- Helpers ---------------------------------------------------------

    /** Which of [cells] would lose any of [mask], and what they would lose. */
    private fun eliminate(cells: List<Int>, mask: Int): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        for (cell in cells) {
            val hit = cands[cell] and mask
            if (values[cell] == 0 && hit != 0) out[cell] = hit
        }
        return out
    }

    companion object {
        private fun bit(d: Int) = 1 shl (d - 1)
        private fun digitOf(mask: Int) = Integer.numberOfTrailingZeros(mask) + 1

        private val ALL_UNITS: List<House> = House.ROWS + House.COLUMNS + House.BOXES

        /**
         * Areas the 45 rule looks at, smallest first: each box, then runs of
         * one to four whole rows, and of one to four whole columns.
         */
        private val REGIONS: List<List<House>> = buildList {
            for (box in House.BOXES) add(listOf(box))
            for (lines in listOf(House.ROWS, House.COLUMNS)) {
                for (length in 1..4) for (start in 0..9 - length) add(lines.subList(start, start + length))
            }
        }.sortedBy { it.size }
        private val BOXES_FIRST: List<House> = House.BOXES + House.ROWS + House.COLUMNS

        private val comboCache = HashMap<Long, List<IntArray>>()

        /** All k-element index combinations of 0 until n. */
        private fun combinations(n: Int, k: Int): List<IntArray> = synchronized(comboCache) {
            comboCache.getOrPut(n.toLong() * 16 + k) {
                val out = ArrayList<IntArray>()
                val idx = IntArray(k) { it }
                while (true) {
                    out += idx.copyOf()
                    var i = k - 1
                    while (i >= 0 && idx[i] == n - k + i) i--
                    if (i < 0) break
                    idx[i]++
                    for (j in i + 1 until k) idx[j] = idx[j - 1] + 1
                }
                out
            }
        }

        /** Grades a puzzle using every technique. */
        fun grade(givens: IntArray, cages: List<Cage> = emptyList()): Grade = LogicSolver(givens, cages).solve()
    }
}
