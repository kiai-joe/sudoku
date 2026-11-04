package com.kiaijoe.sudoku.engine

/**
 * One explained step of a hint, ready to show.
 *
 * @property focus the cells that make up the pattern, drawn strongest.
 * @property area the rows, columns and boxes the step reasons about.
 * @property eliminations cell to the digits this step rules out there.
 * @property candidates cell to the candidates worth showing in pattern cells.
 * @property placeCell where a digit is placed, or -1 for an elimination step.
 */
data class HintCard(
    val technique: Technique?,
    val title: String,
    val text: String,
    val focus: Set<Int>,
    val area: Set<Int>,
    val eliminations: Map<Int, Int> = emptyMap(),
    val candidates: Map<Int, Int> = emptyMap(),
    val placeCell: Int = -1,
    val placeDigit: Int = 0,
) {
    val isPlacement get() = placeCell >= 0
}

sealed interface HintPlan {
    /** A wrong number is on the board; deal with that before anything else. */
    data class FixMistake(val cell: Int, val digit: Int) : HintPlan

    /** Steps that lead to the next number, the last one placing it. */
    data class Teach(val cards: List<HintCard>) : HintPlan

    data object NothingLeft : HintPlan
}

/**
 * Hints that teach. From the player's board (ignoring wrong numbers and their
 * pencil marks), finds the easiest next deduction and explains it, continuing
 * through any candidate eliminations until a number can be placed.
 */
object Hints {
    private const val MAX_STEPS = 40

    fun forGame(game: GameState): HintPlan {
        if (game.status != GameStatus.PLAYING) return HintPlan.NothingLeft
        val wrong = (0 until Grid.CELLS).firstOrNull { game.isWrong(it) }
        if (wrong != null) return HintPlan.FixMistake(wrong, game.values[wrong])
        if (game.isSolved) return HintPlan.NothingLeft

        val known = IntArray(Grid.CELLS) { if (game.isGiven(it) || game.isCorrect(it)) game.values[it] else 0 }
        val solver = LogicSolver(known, game.cages)
        val cards = ArrayList<HintCard>()
        while (cards.size < MAX_STEPS) {
            val step = solver.nextStep() ?: break
            cards += card(step, solver)
            solver.apply(step)
            if (step.isPlacement) return HintPlan.Teach(cards)
        }
        // Logic ran out (only possible with puzzles from before grading): reveal a cell.
        val cell = (0 until Grid.CELLS).first { known[it] == 0 }
        val digit = game.solution[cell]
        return HintPlan.Teach(
            listOf(
                HintCard(
                    technique = null,
                    title = "Reveal",
                    text = "There's no simple step from here, so here is one answer: ${cellName(cell)} is $digit.",
                    focus = setOf(cell),
                    area = emptySet(),
                    placeCell = cell,
                    placeDigit = digit,
                ),
            ),
        )
    }

    /** Builds the card for [step], reading candidates from [solver] before the step is applied. */
    fun card(step: Step, solver: LogicSolver): HintCard {
        val shown = HashMap<Int, Int>()
        if (!step.isPlacement) {
            val mask = if (step.technique.showsAllCandidates) Grid.ALL_DIGITS else step.digits
            for (cell in step.pattern) {
                val c = solver.candidates(cell) and mask
                if (c != 0) shown[cell] = c
            }
        }
        val area = step.houses.flatMap { it.cells.toList() }.toSet()
        return HintCard(
            technique = step.technique,
            title = step.technique.label,
            text = explain(step, { solver.candidates(it) }, { solver.value(it) }),
            focus = step.pattern.toSet(),
            area = area,
            eliminations = step.eliminations,
            candidates = shown,
            placeCell = step.cell,
            placeDigit = step.digit,
        )
    }

    /** Plain-language explanation of [step]. [cands] gives a cell's candidates before the step. */
    fun explain(step: Step, cands: (Int) -> Int, value: (Int) -> Int = { 0 }): String {
        val d = digitList(step.digits)
        val h = step.houses
        val k = step.pattern.size
        return when (step.technique) {
            Technique.NAKED_SINGLE ->
                "This cell's row, column and box already hold every digit except ${step.digit}, so it must be ${step.digit}."

            Technique.HIDDEN_SINGLE ->
                "${step.digit} has only one place left in ${houseName(h[0])}: every other empty cell there " +
                    "already sees ${article(step.digit)} ${step.digit}. So this cell is ${step.digit}."

            Technique.POINTING ->
                "In ${houseName(h[0])}, $d can only go in ${houseName(h[1])}. Whichever cell it takes, " +
                    "${houseName(h[1])} gets its $d from this box, so $d can't go anywhere else in ${houseName(h[1])}."

            Technique.CLAIMING ->
                "In ${houseName(h[0])}, $d can only go inside ${houseName(h[1])}. So ${houseName(h[1])} gets its " +
                    "$d from ${houseName(h[0])}, and $d can't go anywhere else in that box."

            Technique.CAGE -> cageText(step, cands, value)

            Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.NAKED_QUAD ->
                "These ${countWord(k)} cells in ${houseName(h[0])} can only hold $d. Between them they must take " +
                    "${if (k == 2) "both" else "all ${countWord(k)}"}, so $d can't go anywhere else in ${houseName(h[0])}."

            Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE, Technique.HIDDEN_QUAD ->
                "In ${houseName(h[0])}, $d can only go in these ${countWord(k)} cells. So these cells must hold " +
                    "$d, and any other candidates in them can go."

            Technique.X_WING, Technique.SWORDFISH -> {
                val n = h.size / 2
                val bases = linesName(h.take(n))
                val covers = linesName(h.drop(n))
                "In $bases, $d can only go in $covers. Each of those ${plural(h[n])} gets its $d " +
                    "from one of these ${plural(h[0])}, so $d can't go anywhere else in $covers."
            }

            Technique.XY_WING -> {
                val (pivot, a, b) = step.pattern
                val x = digitList(cands(pivot) and cands(a))
                val y = digitList(cands(pivot) and cands(b))
                "${cellName(pivot).replaceFirstChar { it.uppercase() }} is $x or $y. If it's $x, " +
                    "${cellName(a)} is $d; if it's $y, ${cellName(b)} is $d. Either way one of those two is $d, " +
                    "so no cell that sees both of them can be $d."
            }

            Technique.XYZ_WING -> {
                val (pivot, a, b) = step.pattern
                "${cellName(pivot).replaceFirstChar { it.uppercase() }} is ${orList(cands(pivot))}. It sees " +
                    "${cellName(a)}, which is ${orList(cands(a))}, and ${cellName(b)}, which is ${orList(cands(b))}. " +
                    "One of these three must be $d, so no cell that sees all three can be $d."
            }
        }
    }

    /**
     * "This cage adds up to 15. Its 2 empty cells need 9 more, and with the
     * numbers still possible there the only ways are 1+8 or 4+5. So ..."
     */
    private fun cageText(step: Step, cands: (Int) -> Int, value: (Int) -> Int): String {
        val cage = step.cage ?: return "The cage's sum rules out the crossed-out numbers."
        val open = cage.cells.filter { value(it) == 0 }
        val placed = cage.cells.map(value).filter { it != 0 }
        val remaining = cage.sum - placed.sum()
        val used = placed.fold(0) { m, d -> m or (1 shl (d - 1)) }
        val combos = Killer.combinations(open.map(cands), remaining, used)
        val ways = combos.take(4).joinToString(" or ") { mask ->
            (1..9).filter { mask and (1 shl (it - 1)) != 0 }.joinToString("+")
        } + if (combos.size > 4) ", among others" else ""
        if (open.size == 1) {
            return "This cage adds up to ${cage.sum} and already has ${joinAnd(placed.map { it.toString() })}, " +
                "so its last empty cell must be $remaining."
        }
        val start = if (placed.isEmpty()) {
            "This cage of ${open.size} cells adds up to ${cage.sum}."
        } else {
            "This cage adds up to ${cage.sum} and already has ${joinAnd(placed.map { it.toString() })}, " +
                "so its ${open.size} empty cells need $remaining more."
        }
        val combosText = when {
            combos.size == 1 ->
                " Using the numbers still possible in its cells, with none repeated, the only way to make $remaining is $ways."
            else ->
                " Using the numbers still possible in its cells, with none repeated, $remaining can only be made as $ways."
        }
        return "$start$combosText So ${digitList(step.digits)} can't go where they're crossed out."
    }

    // --- Wording ----------------------------------------------------------

    private val Technique.showsAllCandidates: Boolean
        get() = when (this) {
            Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.NAKED_QUAD,
            Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE, Technique.HIDDEN_QUAD,
            Technique.XY_WING, Technique.XYZ_WING, Technique.CAGE,
            -> true
            else -> false
        }

    private val BOX_NAMES = listOf(
        "the top-left box", "the top-middle box", "the top-right box",
        "the middle-left box", "the centre box", "the middle-right box",
        "the bottom-left box", "the bottom-middle box", "the bottom-right box",
    )

    fun houseName(house: House): String = when (house.kind) {
        HouseKind.ROW -> "row ${house.index + 1}"
        HouseKind.COLUMN -> "column ${house.index + 1}"
        HouseKind.BOX -> BOX_NAMES[house.index]
    }

    fun cellName(cell: Int) = "the cell at row ${Grid.row(cell) + 1}, column ${Grid.col(cell) + 1}"

    /** "rows 2 and 7", "columns 1, 4 and 8". */
    private fun linesName(lines: List<House>): String {
        val word = if (lines[0].kind == HouseKind.ROW) "rows" else "columns"
        return "$word ${joinAnd(lines.map { (it.index + 1).toString() })}"
    }

    private fun plural(h: House) = if (h.kind == HouseKind.ROW) "rows" else "columns"

    /** "4", "3 and 8", "2, 5 and 9". */
    fun digitList(mask: Int): String = joinAnd((1..9).filter { mask and (1 shl (it - 1)) != 0 }.map { it.toString() })

    /** "1 or 9", "1, 4 or 9". */
    fun orList(mask: Int): String = joinWith((1..9).filter { mask and (1 shl (it - 1)) != 0 }.map { it.toString() }, "or")

    private fun joinAnd(items: List<String>) = joinWith(items, "and")

    private fun joinWith(items: List<String>, word: String): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " $word " + items.last()
    }

    private fun article(digit: Int) = if (digit == 8) "an" else "a"

    private fun countWord(n: Int) = when (n) {
        2 -> "two"
        3 -> "three"
        4 -> "four"
        else -> n.toString()
    }
}
