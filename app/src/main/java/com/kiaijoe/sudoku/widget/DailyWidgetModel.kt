package com.kiaijoe.sudoku.widget

import com.kiaijoe.sudoku.engine.Daily
import com.kiaijoe.sudoku.engine.Lang
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
    killer: Boolean = false,
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
    val sw = Lang.of(locale) == Lang.SW
    fun t(en: String, swahili: String) = if (sw) swahili else en
    val streak = Daily.streak(results.keys, today)
    val solvedIn = results[today]
    val action = when {
        solvedIn != null -> formatTime(solvedIn)
        inProgress -> t("Continue", "Endelea")
        else -> t("Play", "Cheza")
    }
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val month = date.month.getDisplayName(TextStyle.FULL, locale)
    val label = Daily.label(today, killer)
    val description = buildString {
        if (killer) {
            append(t("Daily Killer, $dayName, $label. ", "Killer ya leo, $dayName, $label. "))
        } else {
            append(t("Daily puzzle, $dayName, $label. ", "Fumbo la leo, $dayName, $label. "))
        }
        append(
            when {
                solvedIn != null -> t("Solved in ${formatTime(solvedIn)}. ", "Limetatuliwa kwa ${formatTime(solvedIn)}. ")
                inProgress -> t("In progress. ", "Linaendelea. ")
                else -> t("Not started. ", "Halijaanzwa. ")
            },
        )
        append(
            when (streak) {
                0 -> t("No streak yet.", "Bado hakuna mfululizo.")
                1 -> t("1-day streak.", "Mfululizo wa siku 1.")
                else -> t("$streak-day streak.", "Mfululizo wa siku $streak.")
            },
        )
    }
    return DailyWidgetModel(
        overline = ((if (killer) t("Daily Killer", "Killer ya leo") else t("Daily", "Leo")) + " · $dayName ${date.dayOfMonth} $month")
            .uppercase(locale),
        // The overline already says Daily Killer, so the title is just the level, leaving room for the streak.
        title = Daily.difficultyFor(today, killer).label,
        streak = streak,
        days = days,
        action = action,
        solved = solvedIn != null,
        description = description,
    )
}
