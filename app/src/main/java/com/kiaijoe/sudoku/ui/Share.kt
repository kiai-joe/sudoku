package com.kiaijoe.sudoku.ui

import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.tr
import com.kiaijoe.sudoku.ui.screens.title
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * A short text about a solved [game], for sharing through Android's share
 * sheet. A daily puzzle names its day and shows that week as a row of
 * squares (red for each day solved) with the streak, so friends playing the
 * same puzzle can compare; [daily] is that daily's record (classic or Killer).
 *
 * ```
 * Sumi · Daily puzzle · Thursday 6 November
 * Hard · 12:04
 * No mistakes, no hints
 * 🟥🟥🟥🟥⬜⬜⬜ 🔥 4
 * ```
 */
fun shareText(game: GameState, daily: DailyUi? = null, locale: Locale = Locale.getDefault(), timed: Boolean = true): String {
    val level = game.title()
    val time = formatTime(game.elapsedSeconds)
    val lines = mutableListOf<String>()
    val day = game.daily
    if (day != null) {
        val date = LocalDate.ofEpochDay(day)
        val kind = if (game.isKiller) tr("Daily Killer", "Killer ya leo") else tr("Daily puzzle", "Fumbo la leo")
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val month = date.month.getDisplayName(TextStyle.FULL, locale)
        lines += "Sumi · $kind · $dayName ${date.dayOfMonth} $month"
        lines += if (timed) "${game.difficulty.label} · $time" else game.difficulty.label
    } else {
        lines += "Sumi · $level"
        lines += if (timed) tr("Solved in $time", "Limetatuliwa kwa $time") else tr("Solved", "Limetatuliwa")
    }
    lines += effort(game.mistakes, game.hintsUsed)
    if (day != null) {
        val solved = daily?.results?.keys.orEmpty() + day
        val date = LocalDate.ofEpochDay(day)
        val monday = date.toEpochDay() - (date.dayOfWeek.value - 1)
        val week = (0 until 7).joinToString("") { if (monday + it in solved) "🟥" else "⬜" }
        val streak = daily?.streak ?: 0
        lines += if (streak > 0) "$week 🔥 $streak" else week
    }
    return lines.joinToString("\n")
}

private fun effort(mistakes: Int, hints: Int): String {
    if (mistakes == 0 && hints == 0) return tr("No mistakes, no hints", "Bila makosa, bila madokezo")
    val m = tr(if (mistakes == 1) "1 mistake" else "$mistakes mistakes", if (mistakes == 1) "kosa 1" else "makosa $mistakes")
    val h = tr(if (hints == 1) "1 hint" else "$hints hints", if (hints == 1) "dokezo 1" else "madokezo $hints")
    return "$m · $h"
}
