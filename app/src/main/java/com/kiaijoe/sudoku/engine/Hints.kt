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
    // Killer hints can take many routine cage steps before a number is placed (they are condensed afterwards).
    private const val MAX_STEPS = 200

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
            if (step.isPlacement) return HintPlan.Teach(condense(cards))
        }
        // Logic ran out (only possible with puzzles from before grading): reveal a cell.
        val cell = (0 until Grid.CELLS).first { known[it] == 0 }
        val digit = game.solution[cell]
        return HintPlan.Teach(
            listOf(
                HintCard(
                    technique = null,
                    title = tr("Reveal", "Funua"),
                    text = tr(
                        "There's no simple step from here, so here is one answer: ${cellName(cell)} is $digit.",
                        "Hakuna hatua rahisi kutoka hapa, kwa hiyo hili ni jibu moja: ${cellName(cell)} ni $digit.",
                    ),
                    focus = setOf(cell),
                    area = emptySet(),
                    placeCell = cell,
                    placeDigit = digit,
                ),
            ),
        )
    }

    /**
     * In Killer, a hint re-derives many routine cage eliminations before the
     * step that matters. Runs of two or more such cards become one, so the
     * player sees the bookkeeping once and the interesting steps in full.
     */
    private fun condense(cards: List<HintCard>): List<HintCard> {
        val out = ArrayList<HintCard>()
        var i = 0
        while (i < cards.size) {
            var j = i
            while (j < cards.size && cards[j].technique == Technique.CAGE && !cards[j].isPlacement) j++
            if (j - i >= 2) {
                out += mergeCages(cards.subList(i, j))
                i = j
            } else {
                out += cards[i]
                i++
            }
        }
        return out
    }

    private fun mergeCages(run: List<HintCard>): HintCard {
        val eliminations = HashMap<Int, Int>()
        val candidates = HashMap<Int, Int>()
        for (card in run) {
            for ((cell, m) in card.eliminations) eliminations[cell] = (eliminations[cell] ?: 0) or m
            for ((cell, m) in card.candidates) candidates.putIfAbsent(cell, m)
        }
        return HintCard(
            technique = Technique.CAGE,
            title = Technique.CAGE.label,
            text = tr(
                "First, the cage sums narrow down ${run.size} cages. For example: ${run.first().text} " +
                    "The others work the same way, and every number they rule out is crossed out.",
                "Kwanza, jumla za vizimba zinapunguza uwezekano katika vizimba ${run.size}. Kwa mfano: ${run.first().text} " +
                    "Vingine vinafanya kazi kwa njia hiyo hiyo, na kila namba vinayoiondoa imekatwa.",
            ),
            focus = run.flatMap { it.focus }.toSet(),
            area = emptySet(),
            eliminations = eliminations,
            candidates = candidates,
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
            Technique.NAKED_SINGLE -> tr(
                "This cell's row, column and box already hold every digit except ${step.digit}, so it must be ${step.digit}.",
                "Mstari, safu na sanduku la kisanduku hiki tayari vina kila namba isipokuwa ${step.digit}, " +
                    "kwa hiyo lazima kiwe ${step.digit}.",
            )

            Technique.HIDDEN_SINGLE -> tr(
                "${step.digit} has only one place left in ${houseName(h[0])}: every other empty cell there " +
                    "already sees ${article(step.digit)} ${step.digit}. So this cell is ${step.digit}.",
                "${step.digit} ina nafasi moja tu iliyobaki katika ${houseName(h[0])}: kila kisanduku kingine kitupu " +
                    "humo tayari kinaona ${step.digit}. Kwa hiyo kisanduku hiki ni ${step.digit}.",
            )

            Technique.POINTING -> tr(
                "In ${houseName(h[0])}, $d can only go in ${houseName(h[1])}. Whichever cell it takes, " +
                    "${houseName(h[1])} gets its $d from this box, so $d can't go anywhere else in ${houseName(h[1])}.",
                "Katika ${houseName(h[0])}, $d ${can(step.digits)} kwenda tu katika ${houseName(h[1])}. " +
                    "Kisanduku chochote itakachochukua, $d ya ${houseName(h[1])} inatoka kwenye sanduku hili, " +
                    "kwa hiyo $d ${cant(step.digits)} kwenda kwingine popote katika ${houseName(h[1])}.",
            )

            Technique.CLAIMING -> tr(
                "In ${houseName(h[0])}, $d can only go inside ${houseName(h[1])}. So ${houseName(h[1])} gets its " +
                    "$d from ${houseName(h[0])}, and $d can't go anywhere else in that box.",
                "Katika ${houseName(h[0])}, $d ${can(step.digits)} kwenda tu ndani ya ${houseName(h[1])}. " +
                    "Kwa hiyo $d ya ${houseName(h[0])} lazima iwe ndani ya ${houseName(h[1])}, " +
                    "na $d ${cant(step.digits)} kwenda kwingine popote katika sanduku hilo.",
            )

            Technique.CAGE -> cageText(step, cands, value)

            Technique.FORTY_FIVE -> fortyFiveText(step)

            Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.NAKED_QUAD -> tr(
                "These ${countWord(k)} cells in ${houseName(h[0])} can only hold $d. Between them they must take " +
                    "${if (k == 2) "both" else "all ${countWord(k)}"}, so $d can't go anywhere else in ${houseName(h[0])}.",
                "Visanduku hivi ${cellCountSw(k)} katika ${houseName(h[0])} vinaweza kuwa na $d tu. Kwa pamoja " +
                    "lazima vichukue zote ${numberSw(k)}, kwa hiyo $d haziwezi kwenda kwingine popote katika ${houseName(h[0])}.",
            )

            Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE, Technique.HIDDEN_QUAD -> tr(
                "In ${houseName(h[0])}, $d can only go in these ${countWord(k)} cells. So these cells must hold " +
                    "$d, and any other candidates in them can go.",
                "Katika ${houseName(h[0])}, $d zinaweza kwenda tu katika visanduku hivi ${cellCountSw(k)}. Kwa hiyo " +
                    "visanduku hivi lazima viwe na $d, na namba nyingine zinazowezekana ndani yake zinaweza kuondolewa.",
            )

            Technique.X_WING, Technique.SWORDFISH -> {
                val n = h.size / 2
                val bases = linesName(h.take(n))
                val covers = linesName(h.drop(n))
                tr(
                    "In $bases, $d can only go in $covers. Each of those ${plural(h[n])} gets its $d " +
                        "from one of these ${plural(h[0])}, so $d can't go anywhere else in $covers.",
                    "Katika $bases, $d ${can(step.digits)} kwenda tu katika $covers. Kila moja ya $bases inaweka $d " +
                        "yake katika mojawapo ya $covers, hivyo $covers zinapata $d kutoka hapo tu. " +
                        "Kwa hiyo $d ${cant(step.digits)} kwenda kwingine popote katika $covers.",
                )
            }

            Technique.XY_WING -> {
                val (pivot, a, b) = step.pattern
                val x = digitList(cands(pivot) and cands(a))
                val y = digitList(cands(pivot) and cands(b))
                tr(
                    "${cellName(pivot).replaceFirstChar { it.uppercase() }} is $x or $y. If it's $x, " +
                        "${cellName(a)} is $d; if it's $y, ${cellName(b)} is $d. Either way one of those two is $d, " +
                        "so no cell that sees both of them can be $d.",
                    "${cellName(pivot).replaceFirstChar { it.uppercase() }} ni $x au $y. Ikiwa ni $x, " +
                        "${cellName(a)} ni $d; ikiwa ni $y, ${cellName(b)} ni $d. Kwa vyovyote vile, kimoja kati ya " +
                        "hivyo viwili ni $d, kwa hiyo hakuna kisanduku kinachoviona vyote viwili kinachoweza kuwa $d.",
                )
            }

            Technique.XYZ_WING -> {
                val (pivot, a, b) = step.pattern
                tr(
                    "${cellName(pivot).replaceFirstChar { it.uppercase() }} is ${orList(cands(pivot))}. It sees " +
                        "${cellName(a)}, which is ${orList(cands(a))}, and ${cellName(b)}, which is ${orList(cands(b))}. " +
                        "One of these three must be $d, so no cell that sees all three can be $d.",
                    "${cellName(pivot).replaceFirstChar { it.uppercase() }} ni ${orList(cands(pivot))}. Kinaona " +
                        "${cellName(a)}, ambacho ni ${orList(cands(a))}, na ${cellName(b)}, ambacho ni ${orList(cands(b))}. " +
                        "Kimoja kati ya hivi vitatu lazima kiwe $d, kwa hiyo hakuna kisanduku kinachoviona vyote " +
                        "vitatu kinachoweza kuwa $d.",
                )
            }
        }
    }

    /**
     * "This cage adds up to 15. Its 2 empty cells need 9 more, and with the
     * numbers still possible there the only ways are 1+8 or 4+5. So ..."
     */
    private fun cageText(step: Step, cands: (Int) -> Int, value: (Int) -> Int): String {
        val cage = step.cage ?: return tr(
            "The cage's sum rules out the crossed-out numbers.",
            "Jumla ya kizimba inaondoa namba zilizokatwa.",
        )
        val open = cage.cells.filter { value(it) == 0 }
        val placed = cage.cells.map(value).filter { it != 0 }
        val remaining = cage.sum - placed.sum()
        val used = placed.fold(0) { m, d -> m or (1 shl (d - 1)) }
        val combos = Killer.combinations(open.map(cands), remaining, used)
        val ways = combos.take(4).joinToString(tr(" or ", " au ")) { mask ->
            (1..9).filter { mask and (1 shl (it - 1)) != 0 }.joinToString("+")
        } + if (combos.size > 4) tr(", among others", ", miongoni mwa nyingine") else ""
        val has = joinAnd(placed.map { it.toString() })
        if (open.size == 1) {
            return tr(
                "This cage adds up to ${cage.sum} and already has $has, so its last empty cell must be $remaining.",
                "Jumla ya kizimba hiki ni ${cage.sum} na tayari kina $has, kwa hiyo kisanduku chake kitupu " +
                    "cha mwisho lazima kiwe $remaining.",
            )
        }
        val start = if (placed.isEmpty()) {
            tr(
                "This cage of ${open.size} cells adds up to ${cage.sum}.",
                "Jumla ya kizimba hiki cha visanduku ${open.size} ni ${cage.sum}.",
            )
        } else {
            tr(
                "This cage adds up to ${cage.sum} and already has $has, so its ${open.size} empty cells need $remaining more.",
                "Jumla ya kizimba hiki ni ${cage.sum} na tayari kina $has, kwa hiyo visanduku vyake ${open.size} " +
                    "vitupu vinahitaji $remaining zaidi.",
            )
        }
        val combosText = if (combos.size == 1) {
            tr(
                " Using the numbers still possible in its cells, with none repeated, the only way to make $remaining is $ways.",
                " Kwa namba ambazo bado zinawezekana katika visanduku vyake, bila kurudia yoyote, njia pekee " +
                    "ya kupata $remaining ni $ways.",
            )
        } else {
            tr(
                " Using the numbers still possible in its cells, with none repeated, $remaining can only be made as $ways.",
                " Kwa namba ambazo bado zinawezekana katika visanduku vyake, bila kurudia yoyote, $remaining " +
                    "inaweza kupatikana tu kama $ways.",
            )
        }
        return start + combosText + crossedOut(step.digits)
    }

    /** " So 3 and 8 can't go where they're crossed out." */
    private fun crossedOut(digits: Int): String {
        val d = digitList(digits)
        return tr(
            " So $d can't go where they're crossed out.",
            if (Integer.bitCount(digits) == 1) {
                " Kwa hiyo $d haiwezi kwenda ilipokatwa."
            } else {
                " Kwa hiyo $d haziwezi kwenda zilipokatwa."
            },
        )
    }

    /**
     * "Rows 1 to 3 add up to 135 (45 each). The cages entirely inside add up
     * to 121, so the 2 cells of other cages that poke in must make up the
     * other 14. ..."
     */
    private fun fortyFiveText(step: Step): String {
        val f = step.fortyFive ?: return tr(
            "The 45 rule rules out the crossed-out numbers.",
            "Kanuni ya 45 inaondoa namba zilizokatwa.",
        )
        val single = f.houses.size == 1
        val area = areaName(f.houses)
        val n = f.cells.size
        val inside = f.total - f.insideSum
        val reason = if (Lang.current == Lang.SW) {
            val each = if (single) "" else " (45 kwa kila moja)"
            if (!f.outies) {
                "Jumla ya $area ni ${f.total}$each. Vizimba vilivyo ndani kabisa vina jumla ya ${f.insideSum}, " +
                    "kwa hiyo ${if (n == 1) "kisanduku kitupu kimoja" else "visanduku $n vitupu"} vya vizimba " +
                    "vingine vinavyoingia lazima vijaze kilichobaki" +
                    if (f.sum == inside) ", yaani $inside." else ": ukihesabu vilivyojazwa, vinahitaji ${f.sum}."
            } else {
                "Jumla ya $area ni ${f.total}$each, na vizimba vilivyo ndani kabisa vina jumla ya ${f.insideSum}, " +
                    "kwa hiyo vizimba vinavyovuka mpaka wake vinaweka $inside ndani yake. Vizimba hivyo vina jumla " +
                    "ya ${f.crossingSum} kwa ujumla, kwa hiyo " +
                    if (n == 1) {
                        "kisanduku chake kitupu kinachotokeza nje lazima kiwe ${f.sum}."
                    } else {
                        "visanduku vyake $n vitupu vinavyotokeza nje lazima vijumlishe ${f.sum}."
                    }
            }
        } else {
            val capital = area.replaceFirstChar { it.uppercase() }
            val verb = if (single) "adds" else "add"
            val each = if (single) "" else " (45 each)"
            val cellWord = if (n == 1) "cell" else "$n cells"
            if (!f.outies) {
                "$capital $verb up to ${f.total}$each. The cages entirely inside add up to ${f.insideSum}, " +
                    "so the ${if (n == 1) "one empty cell" else "$n empty cells"} of other cages poking in must make up " +
                    "the rest" + if (f.sum == inside) ", $inside." else ": with the filled ones counted, they need ${f.sum}."
            } else {
                "$capital $verb up to ${f.total}$each, and the cages entirely inside add up to ${f.insideSum}, so the cages " +
                    "crossing its edge put $inside inside it. Those cages add up to ${f.crossingSum} in all, so their " +
                    "empty $cellWord sticking out must add up to ${f.sum}."
            }
        }
        val conclusion = if (n == 1) {
            tr(" So that cell is ${f.sum}.", " Kwa hiyo kisanduku hicho ni ${f.sum}.")
        } else {
            crossedOut(step.digits)
        }
        return reason + conclusion
    }

    /** "row 3", "rows 1 to 3", "columns 4 and 5", "the top-left box". */
    private fun areaName(houses: List<House>): String {
        if (houses.size == 1) return houseName(houses[0])
        val word = linesWord(houses[0])
        val first = houses.first().index + 1
        val last = houses.last().index + 1
        return if (houses.size == 2) {
            "$word $first ${tr("and", "na")} $last"
        } else {
            "$word $first ${tr("to", "hadi")} $last"
        }
    }

    // --- Wording ----------------------------------------------------------

    private val Technique.showsAllCandidates: Boolean
        get() = when (this) {
            Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.NAKED_QUAD,
            Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE, Technique.HIDDEN_QUAD,
            Technique.XY_WING, Technique.XYZ_WING, Technique.CAGE, Technique.FORTY_FIVE,
            -> true
            else -> false
        }

    private val BOX_NAMES = listOf(
        "the top-left box", "the top-middle box", "the top-right box",
        "the middle-left box", "the centre box", "the middle-right box",
        "the bottom-left box", "the bottom-middle box", "the bottom-right box",
    )

    private val BOX_NAMES_SW = listOf(
        "sanduku la juu kushoto", "sanduku la juu katikati", "sanduku la juu kulia",
        "sanduku la kati kushoto", "sanduku la katikati", "sanduku la kati kulia",
        "sanduku la chini kushoto", "sanduku la chini katikati", "sanduku la chini kulia",
    )

    fun houseName(house: House): String = when (house.kind) {
        HouseKind.ROW -> tr("row ${house.index + 1}", "mstari wa ${house.index + 1}")
        HouseKind.COLUMN -> tr("column ${house.index + 1}", "safu ya ${house.index + 1}")
        HouseKind.BOX -> tr(BOX_NAMES[house.index], BOX_NAMES_SW[house.index])
    }

    fun cellName(cell: Int) = tr(
        "the cell at row ${Grid.row(cell) + 1}, column ${Grid.col(cell) + 1}",
        "kisanduku cha mstari wa ${Grid.row(cell) + 1}, safu ya ${Grid.col(cell) + 1}",
    )

    /** "rows" or "columns", for [house]'s kind. */
    private fun linesWord(house: House) =
        if (house.kind == HouseKind.ROW) tr("rows", "mistari") else tr("columns", "safu")

    /** "rows 2 and 7", "columns 1, 4 and 8". */
    private fun linesName(lines: List<House>): String =
        "${linesWord(lines[0])} ${joinAnd(lines.map { (it.index + 1).toString() })}"

    private fun plural(h: House) = if (h.kind == HouseKind.ROW) "rows" else "columns"

    /** "4", "3 and 8", "2, 5 and 9". */
    fun digitList(mask: Int): String = joinAnd((1..9).filter { mask and (1 shl (it - 1)) != 0 }.map { it.toString() })

    /** "1 or 9", "1, 4 or 9". */
    fun orList(mask: Int): String =
        joinWith((1..9).filter { mask and (1 shl (it - 1)) != 0 }.map { it.toString() }, tr("or", "au"))

    private fun joinAnd(items: List<String>) = joinWith(items, tr("and", "na"))

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

    // Swahili agrees verbs and numbers with the noun: namba (numbers) take
    // i-/zi-, visanduku (cells) take vi-.

    /** "inaweza" for one number, "zinaweza" for several. */
    private fun can(digits: Int) = if (Integer.bitCount(digits) == 1) "inaweza" else "zinaweza"

    /** "haiwezi" for one number, "haziwezi" for several. */
    private fun cant(digits: Int) = if (Integer.bitCount(digits) == 1) "haiwezi" else "haziwezi"

    /** "viwili", "vitatu", "vinne": a count of cells. */
    private fun cellCountSw(n: Int) = when (n) {
        2 -> "viwili"
        3 -> "vitatu"
        4 -> "vinne"
        else -> n.toString()
    }

    /** "mbili", "tatu", "nne": a count of numbers. */
    private fun numberSw(n: Int) = when (n) {
        2 -> "mbili"
        3 -> "tatu"
        4 -> "nne"
        else -> n.toString()
    }
}
