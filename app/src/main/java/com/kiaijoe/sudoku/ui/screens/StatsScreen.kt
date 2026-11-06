package com.kiaijoe.sudoku.ui.screens

import com.kiaijoe.sudoku.engine.tr
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
import com.kiaijoe.sudoku.engine.Variant
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.ListRow
import com.kiaijoe.sudoku.ui.components.RowChevron
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.ScreenTitle
import com.kiaijoe.sudoku.ui.components.SealMark
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
    variantStats: Map<Variant, DifficultyStats> = emptyMap(),
    achievements: Map<Achievement, Long> = emptyMap(),
    onTab: ((Tab) -> Unit)? = null,
    recentGames: List<GameState> = emptyList(),
    onReplay: (Int) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    var confirmReset by remember { mutableStateOf(false) }
    // The figures for each chip: the four classic levels, then Killer, Sudoku X and Jigsaw.
    val others = listOf(Variant.X, Variant.JIGSAW)
    val all = Difficulty.entries.map { stats[it] ?: DifficultyStats() } + killerStats +
        others.map { variantStats[it] ?: DifficultyStats() }
    // Which chip is picked. Opens on the one played most.
    var level by rememberSaveable {
        val played = all.map { it.played }
        mutableStateOf(played.indices.maxBy { played[it] })
    }
    var open by remember { mutableStateOf<Achievement?>(null) }
    val c = Sumi.colors

    TabPage(Tab.Progress, onTab) {
        ScreenTitle(tr("Progress", "Maendeleo")) {
            PillButton(tr("Reset", "Futa"), { confirmReset = true }, tone = ButtonTone.Outline, height = 40.dp)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val labels = Difficulty.entries.map { it.label } + "Killer" + others.map { it.label }
            labels.forEachIndexed { i, label ->
                SumiChip(
                    label,
                    tone = if (i == level) ChipTone.Ai else ChipTone.Neutral,
                    onClick = { level = i },
                    modifier = Modifier.semantics { selected = i == level },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val s = all.getOrElse(level) { DifficultyStats() }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    s.won.toString(),
                    if (s.played == 0) tr("Solved", "Yametatuliwa") else tr("Solved of ${s.played} · ${s.winRate}%", "Yametatuliwa kati ya ${s.played} · ${s.winRate}%"),
                    Modifier.weight(1f).fillMaxHeight(),
                )
                StatTile(s.bestSeconds?.let(::formatTime) ?: "—", tr("Best time", "Muda bora"), Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(s.averageSeconds?.let(::formatTime) ?: "—", tr("Average", "Wastani"), Modifier.weight(1f).fillMaxHeight())
                StatTile(s.noHintRate?.let { "$it%" } ?: "—", tr("Without hints", "Bila madokezo"), Modifier.weight(1f).fillMaxHeight())
            }
        }
        // Wins from before these were kept count in "Solved" but not in the average.
        if (s.timedWins < s.won) {
            Spacer(Modifier.height(10.dp))
            Text(
                tr(
                    "Average and hints count timed wins only, ${s.timedWins} of ${s.won}. Zen wins and older ones count as solved.",
                    "Wastani na madokezo vinahesabu ushindi uliopimwa muda tu, ${s.timedWins} kati ya ${s.won}. " +
                        "Ushindi wa hali ya utulivu na wa zamani unahesabiwa kama yaliyotatuliwa.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
        }

        // The last two finished games, to watch back.
        val replayable = recentGames.withIndex().filter { it.value.log.isNotEmpty() }
        if (replayable.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Overline(tr("Replay a recent game", "Rudia mchezo wa karibuni"))
            Spacer(Modifier.height(8.dp))
            ListCard(
                rows = replayable.map { (i, g) ->
                    @Composable {
                        val won = g.status == GameStatus.WON
                        ListRow(
                            icon = if (won) Lucide.CircleCheckBig else Lucide.X,
                            title = g.title(),
                            subtitle = (if (won) tr("Solved", "Limetatuliwa") else tr("Out of mistakes", "Makosa yamekwisha")) +
                                " · ${formatTime(g.elapsedSeconds)} · " +
                                tr(if (g.log.size == 1) "1 move" else "${g.log.size} moves", if (g.log.size == 1) "hatua 1" else "hatua ${g.log.size}"),
                            iconColor = if (won) c.ai else c.shu,
                            iconBackground = if (won) c.aiSoft else c.shuSoft,
                            onClick = { onReplay(i) },
                            trailing = { RowChevron() },
                        )
                    }
                },
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline(tr("Achievements", "Tuzo"), Modifier.weight(1f))
            Text(
                tr("${achievements.size} of ${Achievement.entries.size}", "${achievements.size} kati ya ${Achievement.entries.size}"),
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        Achievement.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { a ->
                    Seal(a, achievements[a], onClick = { open = a }, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(16.dp))
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
                    a.description + "." + if (day != null) {
                        tr("\nEarned ${earnedDate(day)}.", "\nIlipatikana ${earnedDate(day)}.")
                    } else {
                        tr("\nNot earned yet.", "\nBado haijapatikana.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { PillButton(tr("Done", "Sawa"), { open = null }, tone = ButtonTone.Soft, height = 44.dp) },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.surface,
            title = { Text(tr("Reset statistics?", "Futa takwimu?"), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    tr("Games played, wins and best times will be cleared. Achievements you've earned stay.", "Michezo iliyochezwa, ushindi na muda bora vitafutwa. Tuzo ulizopata zitabaki."),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { PillButton(tr("Reset", "Futa"), { confirmReset = false; onReset() }, tone = ButtonTone.Seal, height = 44.dp) },
            dismissButton = { PillButton(tr("Cancel", "Ghairi"), { confirmReset = false }, tone = ButtonTone.Ghost, height = 44.dp) },
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
private fun Seal(a: Achievement, day: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    val tap = onClick
    val spoken = a.title + ". " + a.description + ". " + if (day != null) tr("Earned ${earnedDate(day)}", "Ilipatikana ${earnedDate(day)}") else tr("Not earned yet", "Bado haijapatikana")
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
            // A margin each side, so a long name wraps instead of meeting its neighbour's.
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}
