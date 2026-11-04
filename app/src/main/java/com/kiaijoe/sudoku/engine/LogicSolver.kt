package com.kiaijoe.sudoku.engine

/**
 * Solving techniques a person uses, easiest first. [level] is the
 * difficulty a puzzle gets when this is the hardest technique it needs.
 */
enum class Technique(val label: String, val level: Difficulty) {
    NAKED_SINGLE("Naked single", Difficulty.EASY),
    HIDDEN_SINGLE("Hidden single", Difficulty.EASY),
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

/**
 * One deduction: either a digit placed in a cell, or candidates removed.
 * [eliminations] maps cell to the bitmask of digits removed from it.
 */
class Step(
    val technique: Technique,
    val cell: Int = -1,
    val digit: Int = 0,
    val eliminations: Map<Int, Int> = emptyMap(),
) {
    val isPlacement get() = digit != 0
}

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
class LogicSolver(givens: IntArray) {
    private val values = IntArray(Grid.CELLS)
    private val cands = IntArray(Grid.CELLS) { Grid.ALL_DIGITS }

    init {
        require(givens.size == Grid.CELLS)
        for (cell in 0 until Grid.CELLS) if (givens[cell] != 0) place(cell, givens[cell])
    }

    fun candidates(cell: Int) = cands[cell]
    fun value(cell: Int) = values[cell]

    /** Applies easiest-first deductions until solved or nothing at or below [maxLevel] helps. */
    fun solve(maxLevel: Difficulty = Difficulty.EXPERT): Grade {
        val steps = ArrayList<Step>()
        var hardest: Technique? = null
        while (true) {
            if (values.all { it != 0 }) return Grade(true, hardest, steps, values.copyOf())
            if (hasContradiction()) return Grade(false, hardest, steps, values.copyOf())
            val step = nextStep(maxLevel) ?: return Grade(false, hardest, steps, values.copyOf())
            apply(step)
            steps += step
            if (hardest == null || step.technique.ordinal > hardest.ordinal) hardest = step.technique
        }
    }

    /** The easiest deduction available now, or null. Does not change the grid. */
    fun nextStep(maxLevel: Difficulty = Difficulty.EXPERT): Step? {
        for (t in Technique.entries) {
            if (t.level > maxLevel) break
            val step = when (t) {
                Technique.NAKED_SINGLE -> nakedSingle()
                Technique.HIDDEN_SINGLE -> hiddenSingle()
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
            for ((cell, mask) in step.eliminations) cands[cell] = cands[cell] and mask.inv()
        }
    }

    private fun place(cell: Int, digit: Int) {
        values[cell] = digit
        cands[cell] = 0
        val bit = bit(digit)
        for (peer in Grid.peers[cell]) cands[peer] = cands[peer] and bit.inv()
    }

    private fun hasContradiction(): Boolean {
        for (cell in 0 until Grid.CELLS) if (values[cell] == 0 && cands[cell] == 0) return true
        return false
    }

    // --- Singles ---------------------------------------------------------

    private fun nakedSingle(): Step? {
        for (cell in 0 until Grid.CELLS) {
            if (values[cell] == 0 && Integer.bitCount(cands[cell]) == 1) {
                return Step(Technique.NAKED_SINGLE, cell, digitOf(cands[cell]))
            }
        }
        return null
    }

    private fun hiddenSingle(): Step? {
        // Boxes first: the place people look for these.
        for (unit in BOXES_FIRST) {
            for (d in 1..9) {
                var only = -1
                var count = 0
                for (cell in unit) {
                    if (cands[cell] and bit(d) != 0) {
                        only = cell
                        count++
                    }
                }
                if (count == 1) return Step(Technique.HIDDEN_SINGLE, only, d)
            }
        }
        return null
    }

    // --- Intersections ---------------------------------------------------

    /** A digit confined to one row or column inside a box leaves the rest of that line. */
    private fun pointing(): Step? {
        for (box in BOXES) {
            for (d in 1..9) {
                val cells = box.filter { cands[it] and bit(d) != 0 }
                if (cells.size < 2) continue
                val line = when {
                    cells.all { Grid.row(it) == Grid.row(cells[0]) } -> ROWS[Grid.row(cells[0])]
                    cells.all { Grid.col(it) == Grid.col(cells[0]) } -> COLS[Grid.col(cells[0])]
                    else -> continue
                }
                val elim = eliminate(line.filter { it !in box }, bit(d))
                if (elim.isNotEmpty()) return Step(Technique.POINTING, eliminations = elim)
            }
        }
        return null
    }

    /** A digit confined to one box inside a row or column leaves the rest of that box. */
    private fun claiming(): Step? {
        for (line in ROWS + COLS) {
            for (d in 1..9) {
                val cells = line.filter { cands[it] and bit(d) != 0 }
                if (cells.size < 2) continue
                val b = Grid.box(cells[0])
                if (!cells.all { Grid.box(it) == b }) continue
                val elim = eliminate(BOXES[b].filter { it !in line }, bit(d))
                if (elim.isNotEmpty()) return Step(Technique.CLAIMING, eliminations = elim)
            }
        }
        return null
    }

    // --- Subsets ---------------------------------------------------------

    /** k cells in a unit that between them hold only k digits: those digits leave the unit's other cells. */
    private fun nakedSubset(k: Int, technique: Technique): Step? {
        for (unit in ALL_UNITS) {
            val open = unit.filter { values[it] == 0 && Integer.bitCount(cands[it]) in 2..k }
            if (open.size < k) continue
            for (combo in combinations(open.size, k)) {
                var union = 0
                for (i in combo) union = union or cands[open[i]]
                if (Integer.bitCount(union) != k) continue
                val chosen = combo.map { open[it] }
                val elim = eliminate(unit.filter { values[it] == 0 && it !in chosen }, union)
                if (elim.isNotEmpty()) return Step(technique, eliminations = elim)
            }
        }
        return null
    }

    /** k digits that fit only in the same k cells of a unit: those cells lose every other digit. */
    private fun hiddenSubset(k: Int, technique: Technique): Step? {
        for (unit in ALL_UNITS) {
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
                if (elim.isNotEmpty()) return Step(technique, eliminations = elim)
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
                val bases = if (byRow) ROWS else COLS
                val covers = if (byRow) COLS else ROWS
                val baseIdx = ArrayList<Int>()
                val baseMask = ArrayList<Int>()
                for ((i, line) in bases.withIndex()) {
                    var mask = 0
                    for ((pos, cell) in line.withIndex()) if (cands[cell] and bit(d) != 0) mask = mask or (1 shl pos)
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
                        for ((pos, cell) in covers[c].withIndex()) if (pos !in usedBases) targets += cell
                    }
                    val elim = eliminate(targets, bit(d))
                    if (elim.isNotEmpty()) return Step(technique, eliminations = elim)
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
                if (elim.isNotEmpty()) return Step(Technique.XY_WING, eliminations = elim)
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
                if (elim.isNotEmpty()) return Step(Technique.XYZ_WING, eliminations = elim)
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

        private val ROWS: List<IntArray> = List(9) { r -> IntArray(9) { c -> r * 9 + c } }
        private val COLS: List<IntArray> = List(9) { c -> IntArray(9) { r -> r * 9 + c } }
        private val BOXES: List<IntArray> = List(9) { b ->
            IntArray(9) { i -> ((b / 3) * 3 + i / 3) * 9 + (b % 3) * 3 + i % 3 }
        }
        private val ALL_UNITS: List<IntArray> = ROWS + COLS + BOXES
        private val BOXES_FIRST: List<IntArray> = BOXES + ROWS + COLS

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
        fun grade(givens: IntArray): Grade = LogicSolver(givens).solve()
    }
}
