package com.kiaijoe.sudoku.engine

import kotlin.random.Random

/**
 * One technique in the Learn section.
 *
 * The worked example is a position from a real generated puzzle: generate
 * [level] with seed [seed], then make [moves] hinted moves. The next hint
 * then starts with [technique]. The generator is deterministic, so this is
 * the same position on every device; TutorialTest checks it still holds.
 * [seed] is null when the technique has no example (see HIDDEN_QUAD).
 * Killer lessons use a Killer puzzle at [level] instead.
 */
data class Lesson(
    val technique: Technique,
    val summaryEn: String,
    val introEn: String,
    val level: Difficulty,
    val seed: Int?,
    val moves: Int,
    val killer: Boolean = false,
    val summarySw: String = summaryEn,
    val introSw: String = introEn,
) {
    /** A one-line description, in the phone's language. */
    val summary: String get() = tr(summaryEn, summarySw)

    /** How the technique works, in the phone's language. */
    val intro: String get() = tr(introEn, introSw)
}

/**
 * A lesson's worked example: the board, and the explained steps from it to
 * the next number. [start] is the first step using the lesson's technique,
 * where the lesson opens; earlier steps are there to step back through.
 */
data class Example(val game: GameState, val cards: List<HintCard>, val start: Int = 0)

/** A practice puzzle for a lesson, and whether solving it by logic needs the lesson's technique. */
class Practice(val puzzle: Puzzle, val usesTechnique: Boolean)

object Tutorial {

    val lessons: List<Lesson> = listOf(
        Lesson(
            Technique.NAKED_SINGLE,
            summaryEn = "A cell with only one number left",
            introEn = "Look at one empty cell and list the numbers already in its row, its column and its box. " +
                "If eight different numbers are there, the ninth is the only one the cell can take.",
            level = Difficulty.EASY, seed = 0, moves = 3,
            summarySw = "Kisanduku chenye namba moja tu iliyobaki",
            introSw = "Angalia kisanduku kimoja kitupu na uorodheshe namba ambazo tayari zipo katika mstari wake, safu yake na sanduku lake. Ikiwa namba nane tofauti zipo, ya tisa ndiyo pekee kisanduku kinaweza kuchukua.",
        ),
        Lesson(
            Technique.HIDDEN_SINGLE,
            summaryEn = "A number with only one place left",
            introEn = "Pick a number and a box, row or column that still needs it. Check each empty cell there: " +
                "most will already see that number somewhere in their row, column or box. " +
                "If only one cell is left, the number goes there.",
            level = Difficulty.EASY, seed = 4, moves = 16,
            summarySw = "Namba yenye nafasi moja tu iliyobaki",
            introSw = "Chagua namba na sanduku, mstari au safu ambayo bado inaihitaji. Kagua kila kisanduku kitupu humo: vingi tayari vitaona namba hiyo mahali fulani katika mstari, safu au sanduku lake. Kikibaki kisanduku kimoja tu, namba inaenda hapo.",
        ),
        Lesson(
            Technique.POINTING,
            summaryEn = "A box that pins a number to one line",
            introEn = "Sometimes a number's possible places inside a box all lie in one row or one column. " +
                "The box must put it somewhere on that line, so the rest of that row or column can't have it.",
            level = Difficulty.MEDIUM, seed = 0, moves = 9,
            summarySw = "Sanduku linalobana namba kwenye mstari mmoja",
            introSw = "Wakati mwingine nafasi zote zinazowezekana za namba ndani ya sanduku ziko katika mstari mmoja au safu moja. Sanduku lazima liweke namba hiyo mahali fulani kwenye mstari huo, kwa hiyo sehemu nyingine ya mstari au safu hiyo haiwezi kuwa nayo.",
        ),
        Lesson(
            Technique.CLAIMING,
            summaryEn = "A line that pins a number to one box",
            introEn = "The reverse of pointing. If a number's places in a row or column all sit inside one box, " +
                "that line takes the box's copy of the number, so the box's other cells can't have it.",
            level = Difficulty.MEDIUM, seed = 25, moves = 15,
            summarySw = "Mstari unaobana namba kwenye sanduku moja",
            introSw = "Kinyume cha kuelekeza. Ikiwa nafasi za namba katika mstari au safu zote ziko ndani ya sanduku moja, mstari huo unachukua nakala ya sanduku ya namba hiyo, kwa hiyo visanduku vingine vya sanduku haviwezi kuwa nayo.",
        ),
        Lesson(
            Technique.NAKED_PAIR,
            summaryEn = "Two cells that share the same two numbers",
            introEn = "If two cells in a row, column or box can each only be the same two numbers, " +
                "those two numbers go in those two cells, one each. No other cell in that group can have them.",
            level = Difficulty.MEDIUM, seed = 89, moves = 28,
            summarySw = "Visanduku viwili vyenye namba mbili zilezile",
            introSw = "Ikiwa visanduku viwili katika mstari, safu au sanduku vinaweza kuwa namba mbili zilezile tu, namba hizo mbili zinaenda katika visanduku hivyo viwili, moja kila kimoja. Hakuna kisanduku kingine katika kundi hilo kinachoweza kuwa nazo.",
        ),
        Lesson(
            Technique.HIDDEN_PAIR,
            summaryEn = "Two numbers that fit only two cells",
            introEn = "If two numbers can each only go in the same two cells of a group, those cells must hold " +
                "exactly those two numbers. Any other candidates in the two cells can be crossed out.",
            level = Difficulty.HARD, seed = 9, moves = 12,
            summarySw = "Namba mbili zinazotoshea visanduku viwili tu",
            introSw = "Ikiwa namba mbili kila moja inaweza kwenda tu katika visanduku viwili vilevile vya kundi, visanduku hivyo lazima viwe na namba hizo mbili hasa. Namba nyingine zozote zinazowezekana katika visanduku hivyo viwili zinaweza kukatwa.",
        ),
        Lesson(
            Technique.NAKED_TRIPLE,
            summaryEn = "Three cells limited to three numbers",
            introEn = "Like a naked pair, but with three cells whose candidates together use only three numbers. " +
                "A cell doesn't need all three: {1,2}, {2,3} and {1,3} still form a triple.",
            level = Difficulty.HARD, seed = 25, moves = 31,
            summarySw = "Visanduku vitatu vilivyobanwa kwa namba tatu",
            introSw = "Kama jozi wazi, lakini kwa visanduku vitatu ambavyo namba zao zinazowezekana kwa pamoja ni namba tatu tu. Kisanduku hakihitaji kuwa na zote tatu: {1,2}, {2,3} na {1,3} bado ni tatu wazi.",
        ),
        Lesson(
            Technique.HIDDEN_TRIPLE,
            summaryEn = "Three numbers that fit only three cells",
            introEn = "If three numbers can only go in the same three cells of a group, those cells hold those " +
                "numbers and nothing else. It's often hard to spot, because the cells have other candidates too.",
            level = Difficulty.HARD, seed = 10549, moves = 28,
            summarySw = "Namba tatu zinazotoshea visanduku vitatu tu",
            introSw = "Ikiwa namba tatu zinaweza kwenda tu katika visanduku vitatu vilevile vya kundi, visanduku hivyo vinashika namba hizo na si nyingine. Mara nyingi ni vigumu kuziona, kwa sababu visanduku hivyo vina namba nyingine zinazowezekana pia.",
        ),
        Lesson(
            Technique.X_WING,
            summaryEn = "A rectangle that locks a number in two columns",
            introEn = "Find a number that has exactly two possible places in each of two rows, and those places " +
                "line up in the same two columns. Each row puts it in one of those columns, so together they " +
                "fill both. No other cell in those two columns can have that number. It works the same with rows " +
                "and columns swapped.",
            level = Difficulty.HARD, seed = 1, moves = 33,
            summarySw = "Mstatili unaofunga namba katika safu mbili",
            introSw = "Tafuta namba yenye nafasi mbili hasa zinazowezekana katika kila mmoja wa mistari miwili, na nafasi hizo ziko katika safu mbili zilezile. Kila mstari unaiweka katika mojawapo ya safu hizo, kwa hiyo kwa pamoja zinajaza zote mbili. Hakuna kisanduku kingine katika safu hizo mbili kinachoweza kuwa na namba hiyo. Inafanya kazi vivyo hivyo ukibadilishana mistari na safu.",
        ),
        Lesson(
            Technique.XY_WING,
            summaryEn = "Three two-number cells that force a third number",
            introEn = "A pivot cell can be A or B. It sees one cell that is A or C, and another that is B or C. " +
                "Whichever the pivot is, one of those two must be C. So any cell that sees both of them can't be C.",
            level = Difficulty.HARD, seed = 0, moves = 30,
            summarySw = "Visanduku vitatu vya namba mbili vinavyolazimisha namba ya tatu",
            introSw = "Kisanduku cha mhimili kinaweza kuwa A au B. Kinaona kisanduku kimoja ambacho ni A au C, na kingine ambacho ni B au C. Mhimili ukiwa chochote, kimoja kati ya hivyo viwili lazima kiwe C. Kwa hiyo kisanduku chochote kinachoviona vyote viwili hakiwezi kuwa C.",
        ),
        Lesson(
            Technique.NAKED_QUAD,
            summaryEn = "Four cells limited to four numbers",
            introEn = "The same idea as pairs and triples, with four cells in a group that between them can only " +
                "hold four numbers. Those numbers can't go anywhere else in the group.",
            level = Difficulty.EXPERT, seed = 12505, moves = 18,
            summarySw = "Visanduku vinne vilivyobanwa kwa namba nne",
            introSw = "Wazo lilelile la jozi na tatu, kwa visanduku vinne katika kundi ambavyo kwa pamoja vinaweza kushika namba nne tu. Namba hizo haziwezi kwenda kwingine popote katika kundi.",
        ),
        Lesson(
            Technique.HIDDEN_QUAD,
            summaryEn = "Four numbers that fit only four cells",
            introEn = "Four numbers that can only go in the same four cells of a group, so those cells hold nothing " +
                "else. In practice it's almost never needed: when a hidden quad exists, the other cells of the " +
                "group form a naked pair, triple or quad that gives the same result more easily. That's why " +
                "there's no example for it here.",
            level = Difficulty.EXPERT, seed = null, moves = 0,
            summarySw = "Namba nne zinazotoshea visanduku vinne tu",
            introSw = "Namba nne zinazoweza kwenda tu katika visanduku vinne vilevile vya kundi, kwa hiyo visanduku hivyo havishiki kitu kingine. Kiutendaji karibu haihitajiki kamwe: nne fiche ikiwepo, visanduku vingine vya kundi huunda jozi, tatu au nne wazi inayotoa matokeo yaleyale kwa urahisi zaidi. Ndiyo sababu hakuna mfano wake hapa.",
        ),
        Lesson(
            Technique.SWORDFISH,
            summaryEn = "An X-Wing across three rows",
            introEn = "An X-Wing with three rows and three columns. In each of three rows, a number can only go " +
                "in some of the same three columns. Those rows fill the number in all three columns, so it can't " +
                "go anywhere else in them.",
            level = Difficulty.EXPERT, seed = 59, moves = 28,
            summarySw = "X-Wing katika mistari mitatu",
            introSw = "X-Wing yenye mistari mitatu na safu tatu. Katika kila mmoja wa mistari mitatu, namba inaweza kwenda tu katika baadhi ya safu tatu zilezile. Mistari hiyo inajaza namba hiyo katika safu zote tatu, kwa hiyo haiwezi kwenda kwingine popote ndani yake.",
        ),
        Lesson(
            Technique.XYZ_WING,
            summaryEn = "An XY-Wing whose pivot has three numbers",
            introEn = "The pivot can be A, B or C. It sees one cell that is A or C and another that is B or C. " +
                "One of the three must be C, so a cell that sees all three can't be C.",
            level = Difficulty.EXPERT, seed = 10, moves = 22,
            summarySw = "XY-Wing ambayo mhimili wake una namba tatu",
            introSw = "Mhimili unaweza kuwa A, B au C. Unaona kisanduku kimoja ambacho ni A au C na kingine ambacho ni B au C. Kimoja kati ya hivyo vitatu lazima kiwe C, kwa hiyo kisanduku kinachoviona vyote vitatu hakiwezi kuwa C.",
        ),
        Lesson(
            Technique.CAGE,
            summaryEn = "Killer: what a cage's sum allows",
            introEn = "In Killer Sudoku each dashed cage shows the sum of its cells, and a number can't repeat " +
                "inside a cage. Small sums and large sums leave few choices: two cells adding to 3 can only be " +
                "1 and 2, and two adding to 17 can only be 8 and 9. Check which sets of numbers make the sum " +
                "with what each cell can still hold, and cross out the rest.",
            level = Difficulty.EASY, seed = 0, moves = 13, killer = true,
            summarySw = "Killer: kile jumla ya kizimba inaruhusu",
            introSw = "Katika Killer Sudoku kila kizimba cha mistari ya vitone kinaonyesha jumla ya visanduku vyake, na namba haiwezi kujirudia ndani ya kizimba. Jumla ndogo na jumla kubwa huacha machaguo machache: visanduku viwili vyenye jumla ya 3 vinaweza kuwa 1 na 2 tu, na viwili vyenye jumla ya 17 vinaweza kuwa 8 na 9 tu. Angalia ni seti zipi za namba zinazotoa jumla hiyo kwa kile ambacho kila kisanduku bado kinaweza kushika, na ukate zilizobaki.",
        ),
        Lesson(
            Technique.FORTY_FIVE,
            summaryEn = "Killer: every row, column and box adds up to 45",
            introEn = "The numbers 1 to 9 add up to 45, so every row, column and box does too, and k of them " +
                "together add up to 45 × k. Add up the cages that sit entirely inside such an area. Whatever is " +
                "missing must come from the few cells of other cages that poke in (innies). Likewise, cells of " +
                "crossing cages that stick out (outies) must make up the rest of those cages' sums. When there " +
                "are only one or two such cells, this pins them down.",
            level = Difficulty.MEDIUM, seed = 0, moves = 6, killer = true,
            summarySw = "Killer: kila mstari, safu na sanduku lina jumla ya 45",
            introSw = "Namba 1 hadi 9 zina jumla ya 45, kwa hiyo kila mstari, safu na sanduku lina jumla hiyo pia, na k kati yake kwa pamoja zina jumla ya 45 × k. Jumlisha vizimba vilivyo ndani kabisa ya eneo kama hilo. Kinachokosekana lazima kitoke kwenye visanduku vichache vya vizimba vingine vinavyoingia ndani. Vivyo hivyo, visanduku vya vizimba vinavyovuka vinavyotokeza nje lazima vijaze sehemu iliyobaki ya jumla za vizimba hivyo. Vikiwa kimoja au viwili tu, hii huvibainisha.",
        ),
    )

    /** Whether [lesson] offers practice: every lesson with a worked example does. */
    fun hasPractice(lesson: Lesson): Boolean = lesson.seed != null

    /**
     * A fresh puzzle at [lesson]'s level whose logical solution uses the
     * lesson's technique at least once, for practising it. Tries up to
     * [attempts] puzzles; if none needs the technique (rare techniques can
     * take a while), returns the last one, which is still at the right level,
     * with [Practice.usesTechnique] false.
     */
    fun practice(lesson: Lesson, random: Random = Random.Default, attempts: Int = PRACTICE_ATTEMPTS): Practice {
        var last: Puzzle? = null
        repeat(attempts) {
            val puzzle = if (lesson.killer) Killer.generate(lesson.level, random) else Generator.generate(lesson.level, random)
            if (LogicSolver.grade(puzzle.givens, puzzle.cages).count(lesson.technique) > 0) return Practice(puzzle, true)
            last = puzzle
        }
        return Practice(last!!, false)
    }

    private const val PRACTICE_ATTEMPTS = 30

    /** Builds [lesson]'s worked example, or null if it has none. Takes up to about a tenth of a second. */
    fun example(lesson: Lesson): Example? {
        val seed = lesson.seed ?: return null
        val puzzle = if (lesson.killer) Killer.generate(lesson.level, Random(seed)) else Generator.generate(lesson.level, Random(seed))
        var game = GameState.from(puzzle)
        repeat(lesson.moves) {
            val cards = (Hints.forGame(game) as? HintPlan.Teach)?.cards ?: return null
            val last = cards.last()
            game = game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
        }
        val cards = (Hints.forGame(game) as? HintPlan.Teach)?.cards ?: return null
        return Example(game, cards, start = cards.indexOfFirst { it.technique == lesson.technique }.coerceAtLeast(0))
    }
}
