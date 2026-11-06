package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialTest {

    @Test
    fun everyTechniqueHasALessonAndIdsAreUnique() {
        assertEquals(Technique.entries.toSet(), Tutorial.lessons.map { it.technique }.toSet())
        assertEquals(Tutorial.lessons.size, Tutorial.lessons.map { it.id }.toSet().size)
        // Lessons that teach a whole technique keep its name as their id, so saved progress still counts.
        for (lesson in Tutorial.lessons.filter { it.focus == null }) assertEquals(lesson.technique.name, lesson.id)
    }

    @Test
    fun theKillerCourseIsInOrder() {
        val course = Tutorial.lessons.filter { it.killer }.map { it.id }
        assertEquals(
            listOf(
                "KILLER_RULES", "KILLER_ONE_ANSWER", "KILLER_LAST_CELL", "CAGE", "KILLER_GRID",
                "FORTY_FIVE", "KILLER_OUTIES", "KILLER_MANY_LINES", "KILLER_CLASSIC",
            ),
            course,
        )
    }

    /**
     * Each stored example must still lead to its technique. If the generator
     * or solver changes, this fails and the seeds need finding again.
     */
    @Test
    fun everyExampleStartsWithItsTechnique() {
        for (lesson in Tutorial.lessons) {
            val example = Tutorial.example(lesson)
            if (lesson.seed == null) {
                assertNull(example)
                continue
            }
            assertNotNull("${lesson.technique} example", example)
            val card = example!!.cards[example.start]
            assertTrue("${lesson.id} opens on what it teaches", card.step != null && lesson.teaches(card.step!!, example.game))
            assertTrue("${lesson.id} opens within the first few steps", example.start <= 3)
            assertTrue(example.cards.last().isPlacement)
            assertEquals(lesson.level, example.game.difficulty)
        }
    }

    /** Every stored practice seed must still give a puzzle at the lesson's level that needs its technique. */
    @Test
    fun practiceSeedsStillNeedTheirTechnique() {
        for ((technique, seeds) in Tutorial.PRACTICE_SEEDS) {
            val lesson = Tutorial.lessons.first { it.technique == technique }
            for (seed in seeds) {
                val puzzle = Generator.generate(lesson.level, kotlin.random.Random(seed))
                assertEquals("$technique seed $seed level", lesson.level, puzzle.difficulty)
                assertTrue(
                    "$technique seed $seed needs it",
                    LogicSolver.grade(puzzle.givens).count(technique) > 0,
                )
            }
        }
    }

    /** Practice finds a puzzle that needs the technique, for every lesson with an example. */
    @Test
    fun practiceNeedsTheLessonsTechnique() {
        for (lesson in Tutorial.lessons.filter { Tutorial.hasPractice(it) }) {
            val practice = Tutorial.practice(lesson, kotlin.random.Random(lesson.technique.ordinal))
            assertTrue("${lesson.id} practice needs it", practice.usesTechnique)
            assertEquals(lesson.killer, practice.puzzle.cages.isNotEmpty())
            val grade = LogicSolver.grade(practice.puzzle.givens, practice.puzzle.cages)
            assertTrue("${lesson.id} practice is used", grade.steps.any { lesson.teaches(it) })
        }
    }
}
