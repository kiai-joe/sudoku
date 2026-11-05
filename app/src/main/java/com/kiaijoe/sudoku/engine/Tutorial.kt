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
    val summary: String,
    val intro: String,
    val level: Difficulty,
    val seed: Int?,
    val moves: Int,
    val killer: Boolean = false,
)

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
            summary = "A cell with only one number left",
            intro = "Look at one empty cell and list the numbers already in its row, its column and its box. " +
                "If eight different numbers are there, the ninth is the only one the cell can take.",
            level = Difficulty.EASY, seed = 0, moves = 3,
        ),
        Lesson(
            Technique.HIDDEN_SINGLE,
            summary = "A number with only one place left",
            intro = "Pick a number and a box, row or column that still needs it. Check each empty cell there: " +
                "most will already see that number somewhere in their row, column or box. " +
                "If only one cell is left, the number goes there.",
            level = Difficulty.EASY, seed = 4, moves = 16,
        ),
        Lesson(
            Technique.POINTING,
            summary = "A box that pins a number to one line",
            intro = "Sometimes a number's possible places inside a box all lie in one row or one column. " +
                "The box must put it somewhere on that line, so the rest of that row or column can't have it.",
            level = Difficulty.MEDIUM, seed = 0, moves = 9,
        ),
        Lesson(
            Technique.CLAIMING,
            summary = "A line that pins a number to one box",
            intro = "The reverse of pointing. If a number's places in a row or column all sit inside one box, " +
                "that line takes the box's copy of the number, so the box's other cells can't have it.",
            level = Difficulty.MEDIUM, seed = 25, moves = 15,
        ),
        Lesson(
            Technique.NAKED_PAIR,
            summary = "Two cells that share the same two numbers",
            intro = "If two cells in a row, column or box can each only be the same two numbers, " +
                "those two numbers go in those two cells, one each. No other cell in that group can have them.",
            level = Difficulty.MEDIUM, seed = 89, moves = 28,
        ),
        Lesson(
            Technique.HIDDEN_PAIR,
            summary = "Two numbers that fit only two cells",
            intro = "If two numbers can each only go in the same two cells of a group, those cells must hold " +
                "exactly those two numbers. Any other candidates in the two cells can be crossed out.",
            level = Difficulty.HARD, seed = 9, moves = 12,
        ),
        Lesson(
            Technique.NAKED_TRIPLE,
            summary = "Three cells limited to three numbers",
            intro = "Like a naked pair, but with three cells whose candidates together use only three numbers. " +
                "A cell doesn't need all three: {1,2}, {2,3} and {1,3} still form a triple.",
            level = Difficulty.HARD, seed = 25, moves = 31,
        ),
        Lesson(
            Technique.HIDDEN_TRIPLE,
            summary = "Three numbers that fit only three cells",
            intro = "If three numbers can only go in the same three cells of a group, those cells hold those " +
                "numbers and nothing else. It's often hard to spot, because the cells have other candidates too.",
            level = Difficulty.HARD, seed = 10549, moves = 28,
        ),
        Lesson(
            Technique.X_WING,
            summary = "A rectangle that locks a number in two columns",
            intro = "Find a number that has exactly two possible places in each of two rows, and those places " +
                "line up in the same two columns. Each row puts it in one of those columns, so together they " +
                "fill both. No other cell in those two columns can have that number. It works the same with rows " +
                "and columns swapped.",
            level = Difficulty.HARD, seed = 1, moves = 33,
        ),
        Lesson(
            Technique.XY_WING,
            summary = "Three two-number cells that force a third number",
            intro = "A pivot cell can be A or B. It sees one cell that is A or C, and another that is B or C. " +
                "Whichever the pivot is, one of those two must be C. So any cell that sees both of them can't be C.",
            level = Difficulty.HARD, seed = 0, moves = 30,
        ),
        Lesson(
            Technique.NAKED_QUAD,
            summary = "Four cells limited to four numbers",
            intro = "The same idea as pairs and triples, with four cells in a group that between them can only " +
                "hold four numbers. Those numbers can't go anywhere else in the group.",
            level = Difficulty.EXPERT, seed = 12505, moves = 18,
        ),
        Lesson(
            Technique.HIDDEN_QUAD,
            summary = "Four numbers that fit only four cells",
            intro = "Four numbers that can only go in the same four cells of a group, so those cells hold nothing " +
                "else. In practice it's almost never needed: when a hidden quad exists, the other cells of the " +
                "group form a naked pair, triple or quad that gives the same result more easily. That's why " +
                "there's no example for it here.",
            level = Difficulty.EXPERT, seed = null, moves = 0,
        ),
        Lesson(
            Technique.SWORDFISH,
            summary = "An X-Wing across three rows",
            intro = "An X-Wing with three rows and three columns. In each of three rows, a number can only go " +
                "in some of the same three columns. Those rows fill the number in all three columns, so it can't " +
                "go anywhere else in them.",
            level = Difficulty.EXPERT, seed = 59, moves = 28,
        ),
        Lesson(
            Technique.XYZ_WING,
            summary = "An XY-Wing whose pivot has three numbers",
            intro = "The pivot can be A, B or C. It sees one cell that is A or C and another that is B or C. " +
                "One of the three must be C, so a cell that sees all three can't be C.",
            level = Difficulty.EXPERT, seed = 10, moves = 22,
        ),
        Lesson(
            Technique.CAGE,
            summary = "Killer: what a cage's sum allows",
            intro = "In Killer Sudoku each dashed cage shows the sum of its cells, and a number can't repeat " +
                "inside a cage. Small sums and large sums leave few choices: two cells adding to 3 can only be " +
                "1 and 2, and two adding to 17 can only be 8 and 9. Check which sets of numbers make the sum " +
                "with what each cell can still hold, and cross out the rest.",
            level = Difficulty.EASY, seed = 0, moves = 13, killer = true,
        ),
        Lesson(
            Technique.FORTY_FIVE,
            summary = "Killer: every row, column and box adds up to 45",
            intro = "The numbers 1 to 9 add up to 45, so every row, column and box does too, and k of them " +
                "together add up to 45 × k. Add up the cages that sit entirely inside such an area. Whatever is " +
                "missing must come from the few cells of other cages that poke in (innies). Likewise, cells of " +
                "crossing cages that stick out (outies) must make up the rest of those cages' sums. When there " +
                "are only one or two such cells, this pins them down.",
            level = Difficulty.MEDIUM, seed = 0, moves = 6, killer = true,
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
