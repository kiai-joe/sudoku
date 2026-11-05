package com.kiaijoe.sudoku.ui.screens

import com.kiaijoe.sudoku.engine.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.Killer
import com.kiaijoe.sudoku.ui.UiState
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.CardShape
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.IconBadge
import com.kiaijoe.sudoku.ui.components.IconTile
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.SealLogo
import com.kiaijoe.sudoku.ui.components.SumiChip
import com.kiaijoe.sudoku.ui.components.Tab
import com.kiaijoe.sudoku.ui.components.TabPage
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.components.icon
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi

@Composable
fun HomeScreen(
    state: UiState,
    onContinue: () -> Unit,
    onPlayDaily: () -> Unit,
    onCalendar: () -> Unit,
    onPlayKillerDaily: () -> Unit = {},
    onKillerCalendar: () -> Unit = {},
    onNewGame: (Difficulty) -> Unit,
    onNewKiller: (Difficulty) -> Unit,
    onSettings: () -> Unit,
    onTab: ((Tab) -> Unit)? = null,
) {
    var pending by remember { mutableStateOf<Difficulty?>(null) }
    var pendingKiller by remember { mutableStateOf<Difficulty?>(null) }
    val c = Sumi.colors

    TabPage(Tab.Home, onTab, maxContentWidth = 1040.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SealLogo(size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Sumi",
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            val streak = state.daily.streak
            if (streak > 0) {
                SumiChip(
                    streak.toString(),
                    tone = ChipTone.Shu,
                    icon = Lucide.Flame,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = tr(
                            if (streak == 1) "1-day streak" else "$streak-day streak",
                            "Mfululizo wa siku $streak",
                        )
                    },
                )
                Spacer(Modifier.width(8.dp))
            }
            IconTile(Lucide.Settings, tr("Settings", "Mipangilio"), onClick = onSettings)
        }
        Spacer(Modifier.height(16.dp))

        // Today's puzzle and the game in progress.
        val today: @Composable ColumnScope.() -> Unit = {
            DailyCard(state.daily, onPlay = onPlayDaily, onCalendar = onCalendar)
            Spacer(Modifier.height(12.dp))
            KillerDailyCard(state.killerDaily, onPlay = onPlayKillerDaily, onCalendar = onKillerCalendar)
            val game = state.savedGame
            if (state.canContinue && game != null) {
                Spacer(Modifier.height(12.dp))
                ContinueCard(
                    title = "${if (game.isKiller) "Killer · " else ""}${game.difficulty.label}",
                    detail = formatTime(game.elapsedSeconds) + " · " + game.values.count { it != 0 }.let { filled ->
                        tr("$filled of 81 filled", "$filled kati ya 81 vimejazwa")
                    },
                    onClick = onContinue,
                )
            }
        }
        // The four levels and Killer.
        val newGame: @Composable ColumnScope.() -> Unit = {
            Overline(tr("New game", "Mchezo mpya"))
            Spacer(Modifier.height(8.dp))
            Difficulty.entries.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { d ->
                        LevelTile(
                            name = d.label,
                            detail = d.techniques,
                            icon = d.icon,
                            onClick = { if (state.canContinue) pending = d else onNewGame(d) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            KillerCard(onPick = { d -> if (state.canContinue) pendingKiller = d else onNewKiller(d) })
        }
        BoxWithConstraints {
            if (maxWidth >= 720.dp) {
                // Side by side on a tablet or a phone on its side.
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f)) { today() }
                    Column(Modifier.weight(1f)) { newGame() }
                }
            } else {
                Column {
                    today()
                    Spacer(Modifier.height(24.dp))
                    newGame()
                }
            }
        }
    }

    pending?.let { d ->
        ConfirmDialog(
            title = tr("Start a new ${d.label} game?", "Anza mchezo mpya wa ${d.label}?"),
            onConfirm = { pending = null; onNewGame(d) },
            onDismiss = { pending = null },
        )
    }
    pendingKiller?.let { d ->
        ConfirmDialog(
            title = tr("Start a ${d.label} Killer game?", "Anza mchezo wa Killer wa ${d.label}?"),
            onConfirm = { pendingKiller = null; onNewKiller(d) },
            onDismiss = { pendingKiller = null },
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sumi.colors.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(tr("Your game in progress will be replaced.", "Mchezo wako unaoendelea utabadilishwa."), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { PillButton(tr("Start", "Anza"), onConfirm, height = 44.dp) },
        dismissButton = { PillButton(tr("Cancel", "Ghairi"), onDismiss, tone = ButtonTone.Ghost, height = 44.dp) },
    )
}

/** The indigo card that picks up the game in progress. */
@Composable
private fun ContinueCard(title: String, detail: String, onClick: () -> Unit) {
    val c = Sumi.colors
    Row(
        Modifier
            .fillMaxWidth()
            .card(c, color = c.ai)
            .clip(CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(c.onAi.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.Play, contentDescription = null, tint = c.onAi, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Continue", "Endelea"), style = MaterialTheme.typography.titleLarge, color = c.onAi)
            Text("$title · $detail", style = MaterialTheme.typography.bodySmall, color = c.onAi.copy(alpha = 0.85f))
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = c.onAi, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun LevelTile(
    name: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Sumi.colors
    Row(
        modifier
            .card(c)
            .clip(CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint = c.ai, background = c.aiSoft, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
    }
}

/** Killer Sudoku: one card that opens to its four levels. */
@Composable
private fun KillerCard(onPick: (Difficulty) -> Unit) {
    val c = Sumi.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .card(c)
            .clip(CardShape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = !open }
                .semantics(mergeDescendants = true) {
                    stateDescription = if (open) tr("Expanded", "Imefunguliwa") else tr("Collapsed", "Imefungwa")
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Lucide.SquareDashed, tint = c.shu, background = c.shuSoft, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Killer Sudoku", style = MaterialTheme.typography.titleMedium, color = c.ink)
                Text(tr("Four levels · cages that add up", "Viwango vinne · vizimba vyenye jumla"), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
            }
            Icon(
                Lucide.ChevronRight,
                contentDescription = null,
                tint = c.inkMuted,
                modifier = Modifier.size(20.dp).rotate(if (open) 90f else 0f),
            )
        }
        if (open) {
            for (d in Difficulty.entries) {
                HorizontalDivider(thickness = 1.dp, color = c.line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onPick(d) }
                        .semantics(mergeDescendants = true) {}
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(d.icon, contentDescription = null, tint = c.shu, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Killer · ${d.label}", style = MaterialTheme.typography.labelLarge, color = c.ink)
                        Text(Killer.describe(d), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                    }
                    Icon(Lucide.Play, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** A day in a week strip or calendar: vermilion when solved, ringed in indigo when it's today. */
@Composable
internal fun DayDot(
    label: String?,
    solved: Boolean,
    today: Boolean,
    future: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Sumi.colors
    val bg: Color = when {
        solved -> c.shu
        today -> c.surface
        else -> c.surfaceSunk
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (today && !solved) Modifier.border(2.5.dp, c.ai, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            solved && label == null ->
                Icon(Lucide.CheckCheck, contentDescription = null, tint = c.onShu, modifier = Modifier.size(16.dp))
            label != null -> Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    solved -> c.onShu
                    today -> c.ai
                    future -> c.inkMuted.copy(alpha = 0.55f)
                    else -> c.inkMuted
                },
            )
        }
    }
}
