package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialTest {

    @Test
    fun everyTechniqueHasOneLessonInOrder() {
        assertEquals(Technique.entries, Tutorial.lessons.map { it.technique })
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
            assertEquals("${lesson.technique} example", lesson.technique, example!!.cards.first().technique)
            assertTrue(example.cards.last().isPlacement)
            assertEquals(lesson.level, example.game.difficulty)
        }
    }
}
