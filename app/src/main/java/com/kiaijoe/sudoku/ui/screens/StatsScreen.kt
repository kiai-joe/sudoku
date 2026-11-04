package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.ui.components.HanamaruStamp
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.theme.SealRed
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: Map<Difficulty, DifficultyStats>,
    onReset: () -> Unit,
    onBack: () -> Unit,
    killerStats: DifficultyStats = DifficultyStats(),
    achievements: Map<Achievement, Long> = emptyMap(),
) {
    BackHandler(onBack = onBack)
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = { confirmReset = true }) { Text("Reset") } },
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
            Difficulty.entries.forEach { d ->
                StatCard(d.label, stats[d] ?: DifficultyStats())
                Spacer(Modifier.height(12.dp))
            }
            if (killerStats.played > 0) {
                StatCard("Killer Sudoku", killerStats)
                Spacer(Modifier.height(12.dp))
            }
            AchievementsCard(achievements)
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset statistics?") },
            text = { Text("Games played, wins and best times will be cleared. Achievements you've earned stay.") },
            confirmButton = { Button(onClick = { confirmReset = false; onReset() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatCard(title: String, stats: DifficultyStats) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Played", stats.played.toString())
                Stat("Won", stats.won.toString())
                Stat("Win rate", "${stats.winRate}%")
                Stat("Best", stats.bestSeconds?.let(::formatTime) ?: "—")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Every achievement: earned ones stamped with the date, the rest faded with what they need. */
@Composable
private fun AchievementsCard(earned: Map<Achievement, Long>) {
    val scheme = MaterialTheme.colorScheme
    val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Achievements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${earned.size} of ${Achievement.entries.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            for (achievement in Achievement.entries) {
                val day = earned[achievement]
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            HanamaruStamp(SealRed, Modifier.fillMaxSize(), animate = false)
                        } else {
                            Box(Modifier.fillMaxSize().border(1.dp, scheme.outlineVariant, CircleShape))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            achievement.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (day != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (day != null) scheme.onSurface else scheme.onSurfaceVariant,
                        )
                        Text(
                            achievement.description +
                                if (day != null) " · ${LocalDate.ofEpochDay(day).format(dates)}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
