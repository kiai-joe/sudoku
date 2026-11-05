package com.kiaijoe.sudoku.widget

import com.kiaijoe.sudoku.engine.Daily
import com.kiaijoe.sudoku.ui.formatTime
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** How one day of this week shows on the widget. */
enum class DayState { SOLVED, TODAY, UPCOMING, MISSED }

data class WidgetDay(val letter: String, val number: Int, val state: DayState)

/**
 * Everything the home-screen widget shows, worked out from the saved daily
 * record so it can be tested without a launcher.
 *
 * @property action the button: Play, Continue, or once today is done just its
 *   time, shown with a tick (the description says it in full).
 * @property description what a screen reader says for the whole widget.
 */
data class DailyWidgetModel(
    val overline: String,
    val title: String,
    val streak: Int,
    val days: List<WidgetDay>,
    val action: String,
    val solved: Boolean,
    val description: String,
)

/**
 * The widget for [today] (an epoch day), given the solved days in [results]
 * (epoch day to seconds) and whether today's puzzle is [inProgress].
 */
fun dailyWidgetModel(
    today: Long,
    results: Map<Long, Long>,
    inProgress: Boolean,
    locale: Locale = Locale.getDefault(),
): DailyWidgetModel {
    val date = LocalDate.ofEpochDay(today)
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    val days = (0 until 7).map { i ->
        val day = monday.plusDays(i.toLong())
        val epoch = day.toEpochDay()
        WidgetDay(
            letter = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
            number = day.dayOfMonth,
            state = when {
                epoch in results -> DayState.SOLVED
                epoch == today -> DayState.TODAY
                epoch > today -> DayState.UPCOMING
                else -> DayState.MISSED
            },
        )
    }
    val streak = Daily.streak(results.keys, today)
    val solvedIn = results[today]
    val action = when {
        solvedIn != null -> formatTime(solvedIn)
        inProgress -> "Continue"
        else -> "Play"
    }
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val month = date.month.getDisplayName(TextStyle.FULL, locale)
    val label = Daily.label(today)
    val description = buildString {
        append("Daily puzzle, $dayName, $label. ")
        append(
            when {
                solvedIn != null -> "Solved in ${formatTime(solvedIn)}. "
                inProgress -> "In progress. "
                else -> "Not started. "
            },
        )
        append(
            when (streak) {
                0 -> "No streak yet."
                1 -> "1-day streak."
                else -> "$streak-day streak."
            },
        )
    }
    return DailyWidgetModel(
        overline = "Daily · $dayName ${date.dayOfMonth} $month".uppercase(locale),
        title = label,
        streak = streak,
        days = days,
        action = action,
        solved = solvedIn != null,
        description = description,
    )
}
