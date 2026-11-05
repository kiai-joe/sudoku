package com.kiaijoe.sudoku.engine

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/** How a player does on one weekday's puzzles. Times are null until one is solved. */
data class WeekdayStats(
    val day: DayOfWeek,
    val difficulty: Difficulty,
    val killer: Boolean,
    val solved: Int,
    val averageSeconds: Long?,
    val bestSeconds: Long?,
)

/**
 * The daily puzzles: a classic one and a Killer one each day. Days are epoch
 * days (days since 1970-01-01, as from LocalDate.toEpochDay), so they store
 * as plain numbers.
 *
 * Each puzzle is generated from a seed made from the date, so every player
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

    /** The daily Killer's level on [day], on the Killer ladder. It rises through the week the same way. */
    fun killerDifficultyFor(day: Long): Difficulty = difficultyFor(day)

    /** The level of [day]'s classic puzzle, or of its Killer one when [killer]. */
    fun difficultyFor(day: Long, killer: Boolean): Difficulty =
        if (killer) killerDifficultyFor(day) else difficultyFor(day)

    /** "Medium", or "Killer · Medium" for the daily Killer, for showing which puzzle a day has. */
    fun label(day: Long, killer: Boolean = false): String =
        (if (killer) "Killer · " else "") + difficultyFor(day, killer).label

    fun puzzleFor(day: Long): Puzzle = Generator.generate(difficultyFor(day), Random(seed(day)))

    fun killerPuzzleFor(day: Long): Puzzle = Killer.generate(killerDifficultyFor(day), Random(killerSeed(day)))

    /** A fresh game for [day], marked as that day's daily puzzle (the Killer one when [killer]). */
    fun gameFor(day: Long, killer: Boolean = false): GameState =
        GameState.from(if (killer) killerPuzzleFor(day) else puzzleFor(day)).copy(daily = day)

    private fun seed(day: Long): Long = day * 1_000_003L + 0x5EED_5A1L

    private fun killerSeed(day: Long): Long = day * 1_000_033L + 0x0C0FFEEL

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

    /**
     * Solves, average and best time for each weekday, Monday first. [results]
     * maps day to seconds, for the classic daily or, when [killer], the Killer one.
     */
    fun weekdayStats(results: Map<Long, Long>, killer: Boolean = false): List<WeekdayStats> {
        val byWeekday = results.entries.groupBy { LocalDate.ofEpochDay(it.key).dayOfWeek }
        return DayOfWeek.entries.map { dow ->
            val times = byWeekday[dow].orEmpty().map { it.value }
            WeekdayStats(
                day = dow,
                // Any date with this weekday gives its level. Epoch day 4 (1970-01-05)
                // was a Monday, and DayOfWeek.value runs 1 (Monday) to 7 (Sunday).
                difficulty = difficultyFor(3L + dow.value, killer),
                killer = killer,
                solved = times.size,
                averageSeconds = if (times.isEmpty()) null else times.sum() / times.size,
                bestSeconds = times.minOrNull(),
            )
        }
    }
}
