package com.kiaijoe.sudoku.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.ui.UiState
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.theme.SealPaper
import com.kiaijoe.sudoku.ui.theme.SealRed

@Composable
fun HomeScreen(
    state: UiState,
    onContinue: () -> Unit,
    onPlayDaily: () -> Unit,
    onCalendar: () -> Unit,
    onNewGame: (Difficulty) -> Unit,
    onNewKiller: () -> Unit,
    onStats: () -> Unit,
    onLearn: () -> Unit,
    onSettingsChange: (Settings) -> Unit,
) {
    var pending by remember { mutableStateOf<Difficulty?>(null) }
    var pendingKiller by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Logo()
            Spacer(Modifier.height(20.dp))
            Text("Sumi", style = MaterialTheme.typography.displaySmall)
            Text(
                "Fill every row, column and box with 1 to 9.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))

            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                DailyCard(state.daily, onPlay = onPlayDaily, onCalendar = onCalendar)
                Spacer(Modifier.height(20.dp))

                val game = state.savedGame
                if (state.canContinue && game != null) {
                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Continue", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${if (game.isKiller) "Killer · " else ""}${game.difficulty.label} · ${formatTime(game.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }

                Text(
                    "New game",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Difficulty.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { d ->
                            OutlinedButton(
                                onClick = { if (state.canContinue) pending = d else onNewGame(d) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(64.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(d.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        d.techniques,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = { if (state.canContinue) pendingKiller = true else onNewKiller() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Killer Sudoku", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Cages that add up",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onStats) {
                        Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Statistics")
                    }
                    TextButton(onClick = onLearn) {
                        Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Learn")
                    }
                    TextButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Settings")
                    }
                }
            }
        }
    }

    pending?.let { d ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Start a new ${d.label} game?") },
            text = { Text("Your game in progress will be replaced.") },
            confirmButton = {
                Button(onClick = { pending = null; onNewGame(d) }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }

    if (pendingKiller) {
        AlertDialog(
            onDismissRequest = { pendingKiller = false },
            title = { Text("Start a Killer Sudoku game?") },
            text = { Text("Your game in progress will be replaced.") },
            confirmButton = {
                Button(onClick = { pendingKiller = false; onNewKiller() }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { pendingKiller = false }) { Text("Cancel") } },
        )
    }

    if (showSettings) {
        SettingsDialog(
            settings = state.settings,
            onChange = onSettingsChange,
            onDismiss = { showSettings = false },
        )
    }
}

/** A small 3x3 grid with the centre cell lit, matching the launcher icon. */
@Composable
private fun Logo() {
    // A hanko seal cut as a 3x3 grid, matching the launcher icon.
    Canvas(Modifier.size(72.dp)) {
        val inset = size.width * 0.2f
        val inner = size.width - inset * 2
        val step = inner / 3f
        drawRoundRect(color = SealRed, cornerRadius = CornerRadius(14.dp.toPx()))
        drawRect(color = SealPaper, topLeft = Offset(inset + step, inset + step), size = Size(step, step))
        for (k in 1..2) {
            val p = inset + step * k
            drawLine(SealPaper, Offset(p, inset), Offset(p, inset + inner), 1.5.dp.toPx())
            drawLine(SealPaper, Offset(inset, p), Offset(inset + inner, p), 1.5.dp.toPx())
        }
        drawRect(
            color = SealPaper,
            topLeft = Offset(inset, inset),
            size = Size(inner, inner),
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}
