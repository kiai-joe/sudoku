package com.kiaijoe.sudoku.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DailyWidgetModelTest {

    /** Thursday 5 November 2026. */
    private val thursday = LocalDate.parse("2026-11-05").toEpochDay()
    private val mondayToWednesday = mapOf(thursday - 3 to 300L, thursday - 2 to 320L, thursday - 1 to 290L)

    @Test
    fun aWeekWithThreeDaysSolved() {
        val m = dailyWidgetModel(thursday, mondayToWednesday, inProgress = false, locale = Locale.UK)
        assertEquals("DAILY · THURSDAY 5 NOVEMBER", m.overline)
        assertEquals("Medium", m.title)
        assertEquals(3, m.streak)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), m.days.map { it.letter })
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8), m.days.map { it.number })
        assertEquals(
            listOf(DayState.SOLVED, DayState.SOLVED, DayState.SOLVED, DayState.TODAY, DayState.UPCOMING, DayState.UPCOMING, DayState.UPCOMING),
            m.days.map { it.state },
        )
        assertEquals("Play", m.action)
        assertFalse(m.solved)
        assertEquals("Daily puzzle, Thursday, Medium. Not started. 3-day streak.", m.description)
    }

    @Test
    fun aStartedPuzzleSaysContinue() {
        assertEquals("Continue", dailyWidgetModel(thursday, emptyMap(), inProgress = true, locale = Locale.UK).action)
    }

    @Test
    fun aSolvedDayShowsItsTime() {
        val m = dailyWidgetModel(thursday, mondayToWednesday + (thursday to 312L), inProgress = false, locale = Locale.UK)
        assertTrue(m.solved)
        assertEquals("05:12", m.action)
        assertTrue(m.description, m.description.contains("Solved in 05:12"))
        assertEquals(4, m.streak)
        assertEquals(DayState.SOLVED, m.days[3].state)
    }

    @Test
    fun aMissedDayEndsTheStreak() {
        val m = dailyWidgetModel(thursday, mapOf(thursday - 3 to 300L), inProgress = false, locale = Locale.UK)
        assertEquals(0, m.streak)
        assertEquals(listOf(DayState.SOLVED, DayState.MISSED, DayState.MISSED), m.days.take(3).map { it.state })
    }

    @Test
    fun showsTheClassicDailyEveryDay() {
        // Saturday's widget shows the classic Hard puzzle; the daily Killer lives in the app.
        val m = dailyWidgetModel(thursday + 2, emptyMap(), inProgress = false, locale = Locale.UK)
        assertEquals("Hard", m.title)
    }
}
