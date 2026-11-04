package com.kiaijoe.sudoku.engine

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * The daily puzzle. Days are epoch days (days since 1970-01-01, as from
 * LocalDate.toEpochDay), so they store as plain numbers.
 *
 * The puzzle is generated from a seed made from the date, so every player
 * gets the same one with no server, and it works offline.
 */
object Daily {

    /** Easy on Monday, rising through the week to Expert on Sunday. */
    fun difficultyFor(day: Long): Difficulty = when (LocalDate.ofEpochDay(day).dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> Difficulty.EASY
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> Difficulty.MEDIUM
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> Difficulty.HARD
        DayOfWeek.SUNDAY -> Difficulty.EXPERT
    }

    fun puzzleFor(day: Long): Puzzle = Generator.generate(difficultyFor(day), Random(seed(day)))

    /** A fresh game for [day], marked as that day's daily puzzle. */
    fun gameFor(day: Long): GameState = GameState.from(puzzleFor(day)).copy(daily = day)

    private fun seed(day: Long): Long = day * 1_000_003L + 0x5EED_5A1L

    /**
     * Days in a row with a solved daily puzzle, ending today. If today isn't
     * solved yet the streak still counts up to yesterday, so it isn't lost
     * until the day is over.
     */
    fun streak(solved: Set<Long>, today: Long): Int {
        var day = if (today in solved) today else today - 1
        var count = 0
        while (day in solved) {
            count++
            day--
        }
        return count
    }

    /** The longest run of consecutive solved days. */
    fun longestStreak(solved: Set<Long>): Int {
        var best = 0
        for (day in solved) {
            if (day - 1 in solved) continue
            var length = 1
            while (day + length in solved) length++
            if (length > best) best = length
        }
        return best
    }
}
