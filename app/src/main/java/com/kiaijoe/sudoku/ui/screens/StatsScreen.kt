package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.ScreenTitle
import com.kiaijoe.sudoku.ui.components.SumiCard
import com.kiaijoe.sudoku.ui.components.SumiChip
import com.kiaijoe.sudoku.ui.components.Tab
import com.kiaijoe.sudoku.ui.components.TabPage
import com.kiaijoe.sudoku.ui.components.icon
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Progress: statistics for each level, then every achievement as a seal. */
@Composable
fun StatsScreen(
    stats: Map<Difficulty, DifficultyStats>,
    onReset: () -> Unit,
    onBack: () -> Unit,
    killerStats: DifficultyStats = DifficultyStats(),
    achievements: Map<Achievement, Long> = emptyMap(),
    onTab: ((Tab) -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    var confirmReset by remember { mutableStateOf(false) }
    // Which level the figures are for: 0..3 the classic levels, 4 Killer. Opens on the one played most.
    var level by rememberSaveable {
        val played = Difficulty.entries.map { stats[it]?.played ?: 0 } + killerStats.played
        mutableStateOf(played.indices.maxBy { played[it] })
    }
    var open by remember { mutableStateOf<Achievement?>(null) }
    val c = Sumi.colors

    TabPage(Tab.Progress, onTab) {
        ScreenTitle("Progress") {
            PillButton("Reset", { confirmReset = true }, tone = ButtonTone.Ghost, height = 40.dp)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val labels = Difficulty.entries.map { it.label } + "Killer"
            labels.forEachIndexed { i, label ->
                SumiChip(
                    label,
                    tone = if (i == level) ChipTone.Ai else ChipTone.Neutral,
                    onClick = { level = i },
                    modifier = Modifier.semantics { selected = i == level },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        val s = if (level < Difficulty.entries.size) stats[Difficulty.entries[level]] ?: DifficultyStats() else killerStats
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(s.won.toString(), "Solved of ${s.played}", Modifier.weight(1f).fillMaxHeight())
                StatTile(s.bestSeconds?.let(::formatTime) ?: "—", "Best time", Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${s.winRate}%", "Win rate", Modifier.weight(1f).fillMaxHeight())
                StatTile(s.played.toString(), "Played", Modifier.weight(1f).fillMaxHeight())
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Achievements", Modifier.weight(1f))
            Text(
                "${achievements.size} of ${Achievement.entries.size}",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Achievement.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { a ->
                    Seal(a, achievements[a], onClick = { open = a }, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    open?.let { a ->
        val day = achievements[a]
        AlertDialog(
            onDismissRequest = { open = null },
            containerColor = c.surface,
            icon = { SealMark(a, earned = day != null, size = 64) },
            title = { Text(a.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            text = {
                Text(
                    a.description + "." + if (day != null) "\nEarned ${earnedDate(day)}." else "\nNot earned yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { PillButton("Done", { open = null }, tone = ButtonTone.Soft, height = 44.dp) },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.surface,
            title = { Text("Reset statistics?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Games played, wins and best times will be cleared. Achievements you've earned stay.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { PillButton("Reset", { confirmReset = false; onReset() }, tone = ButtonTone.Seal, height = 44.dp) },
            dismissButton = { PillButton("Cancel", { confirmReset = false }, tone = ButtonTone.Ghost, height = 44.dp) },
        )
    }
}

private fun earnedDate(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    SumiCard(modifier.clearAndSetSemantics { contentDescription = "$label: $value" }) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = c.ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
    }
}

/** An achievement's round seal: vermilion and stamped when earned, a lock on the sunk well when not. */
@Composable
private fun SealMark(a: Achievement, earned: Boolean, size: Int) {
    val c = Sumi.colors
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (earned) c.shu else c.surfaceSunk)
            .then(if (earned) Modifier.padding(3.dp).border(2.dp, c.onShu, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (earned) a.icon else Lucide.Lock,
            contentDescription = null,
            tint = if (earned) c.onShu else c.inkMuted,
            modifier = Modifier.size((size * 0.42f).dp),
        )
    }
}

@Composable
private fun Seal(a: Achievement, day: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    val tap = onClick
    val spoken = a.title + ". " + a.description + ". " + if (day != null) "Earned ${earnedDate(day)}" else "Not earned yet"
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                this.onClick { tap(); true }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SealMark(a, earned = day != null, size = 54)
        Spacer(Modifier.height(6.dp))
        Text(
            a.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (day != null) c.ink else c.inkMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
