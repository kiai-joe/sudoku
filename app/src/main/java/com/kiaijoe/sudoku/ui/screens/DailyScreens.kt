package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Daily
import com.kiaijoe.sudoku.ui.DailyUi
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.CardShape
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.IconBadge
import com.kiaijoe.sudoku.ui.components.Segment
import com.kiaijoe.sudoku.ui.components.SegmentedControl
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.components.IconTile
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.ScreenTitle
import com.kiaijoe.sudoku.ui.components.SumiCard
import com.kiaijoe.sudoku.ui.components.SumiChip
import com.kiaijoe.sudoku.ui.components.Tab
import com.kiaijoe.sudoku.ui.components.TabPage
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun dayName(day: LocalDate) = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun monthName(day: LocalDate) = day.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

/** The home screen's card for today's puzzle, with this week's progress. */
@Composable
fun DailyCard(daily: DailyUi, onPlay: () -> Unit, onCalendar: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    val today = LocalDate.ofEpochDay(daily.today)
    SumiCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("Daily · ${dayName(today)} ${today.dayOfMonth} ${monthName(today)}")
                Text(daily.label, style = MaterialTheme.typography.headlineSmall, color = c.ink)
            }
            val solvedIn = daily.todaySeconds
            if (solvedIn != null) {
                SumiChip(formatTime(solvedIn), tone = ChipTone.Matcha, icon = Lucide.CheckCheck)
            } else {
                PillButton(
                    if (daily.inProgress != null) "Continue" else "Play",
                    onPlay,
                    icon = Lucide.Play,
                    height = 44.dp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        WeekStrip(daily, onCalendar)
    }
}

/**
 * The home screen's smaller card for today's Killer: its level, the Killer
 * streak, and Play, Continue or the solved time. Tapping the card opens the
 * Daily tab on the Killer calendar.
 */
@Composable
fun KillerDailyCard(daily: DailyUi, onPlay: () -> Unit, onCalendar: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    Row(
        modifier
            .fillMaxWidth()
            .card(c)
            .clip(CardShape)
            .clickable(role = Role.Button, onClickLabel = "Open the Killer calendar", onClick = onCalendar)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Lucide.SquareDashed, tint = c.shu, background = c.shuSoft)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Overline("Daily Killer")
            Text(daily.difficulty.label, style = MaterialTheme.typography.titleLarge, color = c.ink)
            Text(
                when (val streak = daily.streak) {
                    0 -> "Its own streak, apart from the classic one"
                    1 -> "1-day Killer streak"
                    else -> "$streak-day Killer streak"
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        val solvedIn = daily.todaySeconds
        if (solvedIn != null) {
            SumiChip(formatTime(solvedIn), tone = ChipTone.Matcha, icon = Lucide.CheckCheck)
        } else {
            PillButton(
                if (daily.inProgress != null) "Continue" else "Play",
                onPlay,
                icon = Lucide.Play,
                height = 44.dp,
                tone = ButtonTone.Seal,
            )
        }
    }
}

/** Monday to Sunday of this week: vermilion on solved days, a ring on today. */
@Composable
private fun WeekStrip(daily: DailyUi, onCalendar: () -> Unit) {
    val c = Sumi.colors
    val today = LocalDate.ofEpochDay(daily.today)
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Open the calendar", onClick = onCalendar),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (i in 0 until 7) {
            val day = monday.plusDays(i.toLong())
            val epoch = day.toEpochDay()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkMuted,
                )
                Spacer(Modifier.height(4.dp))
                val solved = epoch in daily.results
                DayDot(
                    label = if (solved) null else day.dayOfMonth.toString(),
                    solved = solved,
                    today = epoch == daily.today,
                    future = epoch > daily.today,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = dayDescription(daily, epoch, dayName(day)) },
                )
            }
        }
    }
}

private fun dayDescription(daily: DailyUi, day: Long, label: String): String = "$label, " + when {
    day in daily.results -> "solved"
    day == daily.today -> "today"
    day > daily.today -> "upcoming"
    else -> "not solved"
}

/**
 * The Daily tab: the classic daily or the daily Killer, switched at the top,
 * each with its streak, a month at a time with every solved day, and how
 * each weekday goes.
 *
 * @param onPlayDaily plays today's puzzle; true for the Killer one.
 */
@Composable
fun CalendarScreen(
    daily: DailyUi,
    onBack: () -> Unit,
    onPlayDaily: (killer: Boolean) -> Unit = {},
    onTab: ((Tab) -> Unit)? = null,
    killerDaily: DailyUi = DailyUi(today = daily.today, killer = true),
    showKiller: Boolean = false,
    onShowKiller: (Boolean) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val today = LocalDate.ofEpochDay(daily.today)
    var monthIndex by rememberSaveable { mutableStateOf(0L) }
    val month = YearMonth.from(today).plusMonths(monthIndex)

    TabPage(Tab.Daily, onTab, maxContentWidth = 1040.dp) {
        ScreenTitle("Daily puzzles", "A classic and a Killer puzzle every day, the same for everyone.")
        Spacer(Modifier.height(14.dp))
        SegmentedControl(
            segments = listOf(Segment("Classic", Lucide.Grid3x3), Segment("Killer", Lucide.SquareDashed)),
            selected = if (showKiller) 1 else 0,
            onSelect = { onShowKiller(it == 1) },
        )
        Spacer(Modifier.height(14.dp))
        @Suppress("NAME_SHADOWING")
        val daily = if (showKiller) killerDaily else daily

        SumiCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(c.shu),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        Lucide.Flame,
                        contentDescription = null,
                        tint = c.onShu,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            daily.killer && daily.streak == 1 -> "1-day Killer streak"
                            daily.killer -> "${daily.streak}-day Killer streak"
                            daily.streak == 1 -> "1-day streak"
                            else -> "${daily.streak}-day streak"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = c.ink,
                    )
                    Text(
                        "Best: ${daily.longestStreak} days · ${daily.results.size} solved",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                val solvedIn = daily.todaySeconds
                if (solvedIn != null) {
                    SumiChip(formatTime(solvedIn), tone = ChipTone.Matcha, icon = Lucide.CheckCheck)
                } else {
                    PillButton(
                        if (daily.inProgress != null) "Continue" else "Play",
                        { onPlayDaily(daily.killer) },
                        icon = Lucide.Play,
                        height = 44.dp,
                    )
                }
            }
            if (daily.streak == 0 && daily.todaySeconds == null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Solve today's ${daily.label} puzzle to start a streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        val calendar: @Composable () -> Unit = {
            SumiCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Lucide.ChevronLeft, "Previous month", onClick = { monthIndex-- }, size = 38.dp)
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    if (monthIndex < 0) {
                        IconTile(Lucide.ChevronRight, "Next month", onClick = { monthIndex++ }, size = 38.dp)
                    } else {
                        Spacer(Modifier.size(38.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                MonthGrid(daily, month)
            }
        }
        val weekdays: @Composable () -> Unit = {
            Overline("By weekday")
            Spacer(Modifier.height(10.dp))
            WeekdayCard(daily)
        }
        BoxWithConstraints {
            if (maxWidth >= 720.dp) {
                // Side by side on a tablet or a phone on its side.
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f)) { calendar() }
                    Column(Modifier.weight(1f)) { weekdays() }
                }
            } else {
                Column {
                    calendar()
                    Spacer(Modifier.height(20.dp))
                    weekdays()
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(daily: DailyUi, month: YearMonth) {
    val c = Sumi.colors
    Row(Modifier.fillMaxWidth()) {
        for (dow in DayOfWeek.entries) {
            Text(
                dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    for (r in 0 until rows) {
        Row(Modifier.fillMaxWidth()) {
            for (col in 0 until 7) {
                val index = r * 7 + col - leading
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (index in 0 until month.lengthOfMonth()) {
                        val day = first.plusDays(index.toLong())
                        val epoch = day.toEpochDay()
                        DayDot(
                            label = (index + 1).toString(),
                            solved = epoch in daily.results,
                            today = epoch == daily.today,
                            future = epoch > daily.today,
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics { contentDescription = dayDescription(daily, epoch, (index + 1).toString()) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Average solve time for each weekday as a bar, with the number solved and
 * the best time, so a player can see how the rising levels treat them.
 */
@Composable
private fun WeekdayCard(daily: DailyUi) {
    val c = Sumi.colors
    val stats = Daily.weekdayStats(daily.results, daily.killer)
    val longest = stats.maxOf { it.averageSeconds ?: 0L }.coerceAtLeast(1L)
    SumiCard(Modifier.fillMaxWidth()) {
        Text(
            if (daily.killer) {
                "Average solve time. Killer levels rise from Easy on Monday to Expert, with no printed numbers, on Sunday."
            } else {
                "Average solve time. Levels rise from Easy on Monday to Expert on Sunday."
            },
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
        )
        Spacer(Modifier.height(10.dp))
        for (row in stats) {
            val spoken = buildString {
                append(row.day.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                append(", ").append(row.difficulty.label)
                if (row.solved == 0) {
                    append(", none solved yet")
                } else {
                    append(", average ${formatTime(row.averageSeconds!!)}, ${row.solved} solved, best ${formatTime(row.bestSeconds!!)}")
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clearAndSetSemantics { contentDescription = spoken },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(72.dp)) {
                    Text(
                        row.day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.ink,
                    )
                    Text(
                        row.difficulty.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(c.surfaceSunk),
                ) {
                    val average = row.averageSeconds
                    if (average != null) {
                        Box(
                            Modifier
                                .fillMaxWidth(average.toFloat() / longest)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(c.ai),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.width(100.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        row.averageSeconds?.let(::formatTime) ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                    )
                    Text(
                        if (row.solved == 0) "none yet" else "${row.solved}× · best ${formatTime(row.bestSeconds!!)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
