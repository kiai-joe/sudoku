package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Sudoku X, Jigsaw and typed-in puzzles. */
class VariantTest {

    @Test
    fun xPuzzlesKeepTheDiagonals() {
        for (seed in 0 until 3) {
            val p = Generator.generate(Difficulty.MEDIUM, Random(seed), Variant.X)
            assertTrue(p.shape.diagonals)
            assertTrue(Solver.hasUniqueSolution(p.givens, p.shape))
            for (diagonal in House.DIAGONALS) {
                assertEquals((1..9).toSet(), diagonal.cells.map { p.solution[it] }.toSet())
            }
            assertTrue(Shape.X.isValidSolution(p.solution))
        }
    }

    @Test
    fun jigsawLayoutsAreNineConnectedRegions() {
        for (seed in 0 until 20) {
            val regions = Shape.randomJigsaw(Random(seed))
            assertTrue("seed $seed", Shape.isValidLayout(regions))
            assertFalse("it moved off the 3×3 boxes", regions.contentEquals(IntArray(81) { Grid.box(it) }))
        }
    }

    @Test
    fun jigsawPuzzlesAreUniqueOnTheirShape() {
        for (seed in 0 until 3) {
            val p = Generator.generate(Difficulty.MEDIUM, Random(seed), Variant.JIGSAW)
            assertTrue(p.shape.isJigsaw)
            assertTrue(p.shape.isValidSolution(p.solution))
            assertTrue(Solver.hasUniqueSolution(p.givens, p.shape))
            for (region in p.shape.boxes) assertEquals(9, region.cells.size)
        }
    }

    @Test
    fun shapesAreSaved() {
        val jig = Generator.generate(Difficulty.EASY, Random(4), Variant.JIGSAW)
        val back = GameCodec.decode(GameCodec.encode(GameState.from(jig)))!!
        assertEquals(jig.shape, back.shape)
        assertEquals(Variant.JIGSAW, back.variant)
        val x = GameCodec.decode(GameCodec.encode(GameState.from(Generator.generate(Difficulty.EASY, Random(1), Variant.X))))!!
        assertEquals(Variant.X, x.variant)
        val old = GameCodec.encode(TestGrids.game()).split("|").take(18).joinToString("|")
        assertEquals(Shape.CLASSIC, GameCodec.decode(old)!!.shape)
    }

    /** Following hints alone solves X and Jigsaw puzzles, and every step is right. */
    @Test
    fun hintsSolveVariants() {
        for (variant in listOf(Variant.X, Variant.JIGSAW)) for (level in listOf(Difficulty.EASY, Difficulty.HARD)) {
            var game = GameState.from(Generator.generate(level, Random(7), variant))
            var guard = 0
            while (game.status == GameStatus.PLAYING && guard++ < 100) {
                val plan = Hints.forGame(game) as HintPlan.Teach
                for (card in plan.cards) {
                    for ((cell, mask) in card.eliminations) {
                        assertEquals("$variant $level ruled out the answer", 0, mask and GameState.bit(game.solution[cell]))
                    }
                    assertNotEquals("$variant hint wording", "", card.text)
                }
                val last = plan.cards.last()
                assertEquals(game.solution[last.placeCell], last.placeDigit)
                game = game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
            }
            assertEquals("$variant $level solved", GameStatus.WON, game.status)
        }
    }

    @Test
    fun typedInPuzzlesAreChecked() {
        val p = Generator.generate(Difficulty.HARD, Random(3))
        val ok = CustomPuzzle.check(p.givens) as CustomCheck.Ok
        assertEquals(p.solution.toList(), ok.puzzle.solution.toList())
        assertEquals(Difficulty.HARD, ok.puzzle.difficulty)
        assertTrue(ok.byLogic)

        assertTrue(CustomPuzzle.check(IntArray(81).also { it[0] = 5 }) is CustomCheck.TooFew)
        val clash = p.givens.copyOf().also { g -> val c = (0 until 81).first { g[it] == 0 && Grid.col(it) != 0 }; g[c] = g[(c / 9) * 9 + (0 until 9).first { g[(c / 9) * 9 + it] != 0 }] }
        assertTrue(CustomPuzzle.check(clash) is CustomCheck.Clash)
        // The first 20 cells of a solved grid leave the bottom rows wide open.
        val open = IntArray(81) { if (it < 20) p.solution[it] else 0 }
        assertEquals(CustomCheck.ManySolutions, CustomPuzzle.check(open))
    }
}
