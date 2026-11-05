package com.kiaijoe.sudoku.engine

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.random.Random

/**
 * Swahili: the phone's language picks it, and every piece of engine text a
 * player can see (levels, techniques, achievements, lessons and every kind
 * of hint) comes out in Swahili, with no English left in.
 */
class LangTest {

    @After
    fun reset() {
        Lang.forced = null
    }

    @Test
    fun phoneLanguagePicksSwahili() {
        assertEquals(Lang.SW, Lang.of(Locale("sw", "TZ")))
        assertEquals(Lang.SW, Lang.of(Locale("sw", "KE")))
        assertEquals(Lang.EN, Lang.of(Locale.UK))
        assertEquals(Lang.EN, Lang.of(Locale.FRANCE))
        Lang.forced = Lang.SW
        assertEquals("Kati", Difficulty.MEDIUM.label)
        Lang.forced = Lang.EN
        assertEquals("Medium", Difficulty.MEDIUM.label)
    }

    /** Words that only English text would have. */
    private val english = Regex("\\b(the|this|that|cell|cells|can't|only|and|or|row|column|box|so|in|is|of|with)\\b", RegexOption.IGNORE_CASE)

    private fun assertSwahili(what: String, text: String) {
        val found = english.find(text)
        assertTrue("$what has English (\"${found?.value}\"): $text", found == null)
        assertTrue("$what is empty", text.isNotBlank())
    }

    @Test
    fun labelsAndLessonsAreInSwahili() {
        Lang.forced = Lang.SW
        for (d in Difficulty.entries) {
            assertSwahili("level $d", d.label)
            assertSwahili("Killer level $d", Killer.describe(d))
        }
        for (a in Achievement.entries) {
            assertSwahili("achievement $a", a.title)
            assertSwahili("achievement $a", a.description)
        }
        for (lesson in Tutorial.lessons) {
            assertSwahili("lesson ${lesson.technique}", lesson.summary)
            assertSwahili("lesson ${lesson.technique}", lesson.intro)
            assertNotEquals(lesson.introEn, lesson.intro)
        }
        assertEquals("Killer · Kati", Daily.label(LocaleDay.THURSDAY, killer = true))
    }

    /** Every technique's hint, from real classic and Killer games, explained in Swahili. */
    @Test
    fun everyKindOfHintIsInSwahili() {
        Lang.forced = Lang.SW
        val seen = HashSet<Technique?>()
        val random = Random(7)
        val puzzles = Difficulty.entries.flatMap { level ->
            List(3) { Generator.generate(level, random) } + List(2) { Killer.generate(level, random) }
        } + Tutorial.lessons.filter { it.seed != null }.map {
            if (it.killer) Killer.generate(it.level, Random(it.seed!!)) else Generator.generate(it.level, Random(it.seed!!))
        }
        for (puzzle in puzzles) {
            var game = GameState.from(puzzle)
            while (true) {
                val plan = Hints.forGame(game) as? HintPlan.Teach ?: break
                for (card in plan.cards) {
                    seen += card.technique
                    assertSwahili("${card.technique} title", card.title)
                    assertSwahili("${card.technique} hint", card.text)
                }
                val last = plan.cards.last()
                game = game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
            }
        }
        // The common techniques all came up, so their wording was checked.
        for (t in listOf(Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE, Technique.POINTING, Technique.CAGE, Technique.FORTY_FIVE, Technique.X_WING)) {
            assertTrue("$t came up", t in seen)
        }
    }

    private object LocaleDay {
        /** Thursday 5 November 2026, a Medium day. */
        val THURSDAY = java.time.LocalDate.parse("2026-11-05").toEpochDay()
    }
}
