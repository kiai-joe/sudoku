package com.kiaijoe.sudoku.ui

import com.kiaijoe.sudoku.engine.Cage
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

class ShareTest {

    @After
    fun reset() {
        Lang.forced = null
    }

    private val thursday = LocalDate.parse("2026-11-05").toEpochDay()

    private fun won(seconds: Long, mistakes: Int = 0, hints: Int = 0) =
        GameState.from(Generator.generate(Difficulty.HARD, Random(1))).copy(
            status = GameStatus.WON,
            elapsedSeconds = seconds,
            mistakes = mistakes,
            hintsUsed = hints,
        )

    @Test
    fun aDailyShowsItsDayAndWeek() {
        Lang.forced = Lang.EN
        val daily = DailyUi(today = thursday, results = mapOf(thursday - 3 to 300L, thursday - 2 to 400L, thursday - 1 to 500L))
        val text = shareText(won(724).copy(daily = thursday), daily.copy(results = daily.results + (thursday to 724L)), Locale.UK)
        assertEquals(
            "Sumi · Daily puzzle · Thursday 5 November\nHard · 12:04\nNo mistakes, no hints\n🟥🟥🟥🟥⬜⬜⬜ 🔥 4",
            text,
        )
    }

    @Test
    fun aRegularGameSaysItsLevelAndEffort() {
        Lang.forced = Lang.EN
        assertEquals("Sumi · Hard\nSolved in 08:00\n1 mistake · 2 hints", shareText(won(480, mistakes = 1, hints = 2)))
        val killer = won(1000).copy(cages = listOf(Cage(45, (0 until 9).toList())))
        assertEquals("Sumi · Killer · Hard", shareText(killer).lines().first())
    }

    @Test
    fun itSpeaksSwahili() {
        Lang.forced = Lang.SW
        assertEquals("Sumi · Ngumu\nLimetatuliwa kwa 08:00\nBila makosa, bila madokezo", shareText(won(480)))
    }
}
