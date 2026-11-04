package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Daily
import com.kiaijoe.sudoku.ui.DailyUi
import com.kiaijoe.sudoku.ui.components.HanamaruStamp
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.theme.SealRed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun dayName(day: LocalDate) = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun streakText(days: Int) = when (days) {
    0 -> "Solve today's puzzle to start a streak"
    1 -> "Streak: 1 day"
    else -> "Streak: $days days"
}

/** The home screen's card for today's puzzle, with this week's progress. */
@Composable
fun DailyCard(daily: DailyUi, onPlay: () -> Unit, onCalendar: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val today = LocalDate.ofEpochDay(daily.today)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily puzzle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${dayName(today)} · ${daily.difficulty.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                val solvedIn = daily.todaySeconds
                if (solvedIn != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HanamaruStamp(SealRed, Modifier.size(30.dp), animate = false)
                        Spacer(Modifier.size(6.dp))
                        Text(formatTime(solvedIn), style = MaterialTheme.typography.titleSmall)
                    }
                } else {
                    Button(onClick = onPlay, shape = RoundedCornerShape(12.dp)) {
                        Text(if (daily.inProgress != null) "Continue" else "Play")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            WeekStrip(daily)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    streakText(daily.streak),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCalendar) { Text("Calendar") }
            }
        }
    }
}

/** Monday to Sunday of this week: a hanamaru on solved days, a ring on today. */
@Composable
private fun WeekStrip(daily: DailyUi) {
    val today = LocalDate.ofEpochDay(daily.today)
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val day = monday.plusDays(i.toLong())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                DayMark(daily, day.toEpochDay(), Modifier.size(30.dp))
            }
        }
    }
}

/** One day's mark: hanamaru if solved, ring if today, faint outline otherwise. */
@Composable
private fun DayMark(daily: DailyUi, day: Long, modifier: Modifier, label: String? = null) {
    val scheme = MaterialTheme.colorScheme
    val solved = day in daily.results
    val description = when {
        solved -> "solved"
        day == daily.today -> "today"
        day > daily.today -> "upcoming"
        else -> "not solved"
    }
    Box(
        modifier
            .semantics { contentDescription = listOfNotNull(label, description).joinToString(", ") }
            .then(
                when {
                    solved -> Modifier
                    day == daily.today -> Modifier.border(2.dp, scheme.primary, CircleShape)
                    else -> Modifier.border(1.dp, scheme.outlineVariant, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // With a day number on top, a faded stamp keeps the number readable.
        if (solved) HanamaruStamp(SealRed.copy(alpha = if (label != null) 0.35f else 1f), Modifier.fillMaxSize(), animate = false)
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (day == daily.today || solved) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    day > daily.today -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> scheme.onSurface
                },
            )
        }
    }
}

/** A month at a time, with a hanamaru on every solved day and the streaks so far. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(daily: DailyUi, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val today = LocalDate.ofEpochDay(daily.today)
    var month by remember { mutableStateOf(YearMonth.from(today)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Daily puzzles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            ) {
                Row(Modifier.padding(18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Figure("Streak", daily.streak.toString())
                    Figure("Longest", daily.longestStreak.toString())
                    Figure("Solved", daily.results.size.toString())
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(
                            onClick = { month = month.plusMonths(1) },
                            enabled = month < YearMonth.from(today),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MonthGrid(daily, month)
                }
            }
            Spacer(Modifier.height(16.dp))
            WeekdayCard(daily)
        }
    }
}

@Composable
private fun MonthGrid(daily: DailyUi, month: YearMonth) {
    Row(Modifier.fillMaxWidth()) {
        for (dow in DayOfWeek.entries) {
            Text(
                dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
            for (c in 0 until 7) {
                val index = r * 7 + c - leading
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (index in 0 until month.lengthOfMonth()) {
                        val day = first.plusDays(index.toLong())
                        DayMark(daily, day.toEpochDay(), Modifier.fillMaxSize(), label = (index + 1).toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Average solve time for each weekday as a bar, with the number solved and
 * the best time, so a player can see how the rising levels treat them.
 */
@Composable
private fun WeekdayCard(daily: DailyUi) {
    val scheme = MaterialTheme.colorScheme
    val stats = Daily.weekdayStats(daily.results)
    val longest = stats.maxOf { it.averageSeconds ?: 0L }.coerceAtLeast(1L)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("By weekday", style = MaterialTheme.typography.titleMedium)
            Text(
                "Average solve time. Levels rise from Easy on Monday to Expert on Sunday.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            for (row in stats) {
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(76.dp)) {
                        Text(
                            row.day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(row.difficulty.label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(scheme.surfaceVariant),
                    ) {
                        val average = row.averageSeconds
                        if (average != null) {
                            Box(
                                Modifier
                                    .fillMaxWidth(average.toFloat() / longest)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(scheme.primary),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.width(104.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            row.averageSeconds?.let(::formatTime) ?: "—",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (row.solved == 0) "none yet" else "${row.solved}× · best ${formatTime(row.bestSeconds!!)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
