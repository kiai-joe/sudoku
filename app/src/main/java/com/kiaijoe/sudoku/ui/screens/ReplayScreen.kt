package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Grid
import com.kiaijoe.sudoku.engine.LogEntry
import com.kiaijoe.sudoku.engine.tr
import com.kiaijoe.sudoku.ui.components.CardShape
import com.kiaijoe.sudoku.ui.components.IconTile
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.ListRow
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.Page
import com.kiaijoe.sudoku.ui.components.SudokuBoard
import com.kiaijoe.sudoku.ui.components.SumiTopBar
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import kotlinx.coroutines.delay

/** A moment worth jumping to in a replay: the log [index] it happened at, and why. */
internal data class Moment(val index: Int, val kind: Kind, val seconds: Long = 0) {
    enum class Kind { MISTAKE, HINT, PAUSE }
}

/**
 * The moments in [game]'s log worth looking back at: every wrong number,
 * every number a hint placed, and the three longest pauses of 30 seconds
 * or more before a move. In log order.
 */
internal fun moments(game: GameState): List<Moment> {
    val log = game.log
    val out = mutableListOf<Moment>()
    log.forEachIndexed { i, e ->
        if (e.value != 0 && e.value != game.solution[e.cell]) out += Moment(i, Moment.Kind.MISTAKE)
        if (e.hint) out += Moment(i, Moment.Kind.HINT)
    }
    val pauses = log.indices
        .map { i -> i to log[i].second - (if (i == 0) 0L else log[i - 1].second) }
        .filter { (i, s) -> s >= 30 && log[i].value != 0 }
        .sortedByDescending { it.second }
        .take(3)
        .map { (i, s) -> Moment(i, Moment.Kind.PAUSE, s) }
    return (out + pauses).sortedBy { it.index }
}

/** The board after the first [count] changes in [game]'s log. */
internal fun boardAt(game: GameState, count: Int): List<Int> {
    val values = game.givens.toMutableList()
    for (e: LogEntry in game.log.take(count)) values[e.cell] = e.value
    return values
}

/**
 * Watches a finished game back: the board fills in change by change, with
 * play and pause, a step either way and a slider. Below it, the moments
 * worth a look (mistakes, hints and the longest pauses) jump straight there.
 */
@Composable
fun ReplayScreen(game: GameState, onBack: () -> Unit, initialShown: Int = 0) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val log = game.log
    var shown by rememberSaveable { mutableIntStateOf(initialShown) }
    var playing by remember { mutableStateOf(false) }
    val moments = remember(game) { moments(game) }

    // Plays about four changes a second, stopping at the end.
    LaunchedEffect(playing) {
        while (playing && shown < log.size) {
            delay(250)
            shown++
        }
        playing = false
    }

    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(tr("Replay", "Marudio"), onBack = onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    val last = log.getOrNull(shown - 1)
                    val board = game.copy(
                        values = boardAt(game, shown),
                        notes = List(Grid.CELLS) { 0 },
                        status = GameStatus.PLAYING,
                    )
                    SudokuBoard(
                        game = board,
                        selected = last?.cell,
                        highlightSameNumbers = false,
                        onSelect = {},
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).card(c, CardShape),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (log.isEmpty()) {
                            tr("This game has no moves to replay.", "Mchezo huu hauna hatua za kurudia.")
                        } else {
                            tr(
                                "Move $shown of ${log.size} · ${formatTime(last?.second ?: 0)}",
                                "Hatua $shown kati ya ${log.size} · ${formatTime(last?.second ?: 0)}",
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = c.inkMuted,
                    )
                    if (log.isNotEmpty()) {
                        Slider(
                            value = shown.toFloat(),
                            onValueChange = {
                                playing = false
                                shown = it.toInt()
                            },
                            valueRange = 0f..log.size.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = c.ai,
                                activeTrackColor = c.ai,
                                inactiveTrackColor = c.surfaceSunk,
                            ),
                            modifier = Modifier.semantics { contentDescription = tr("Replay position", "Mahali pa marudio") },
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            IconTile(Lucide.ChevronLeft, tr("Back one move", "Rudi hatua moja"), onClick = {
                                playing = false
                                if (shown > 0) shown--
                            })
                            IconTile(
                                if (playing) Lucide.Pause else Lucide.Play,
                                if (playing) tr("Pause", "Sitisha") else tr("Play", "Cheza"),
                                onClick = {
                                    if (!playing && shown >= log.size) shown = 0
                                    playing = !playing
                                },
                            )
                            IconTile(Lucide.ChevronRight, tr("Forward one move", "Mbele hatua moja"), onClick = {
                                playing = false
                                if (shown < log.size) shown++
                            })
                        }
                    }
                    if (moments.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Overline(tr("Moments", "Matukio"))
                        Spacer(Modifier.height(8.dp))
                        ListCard(
                            rows = moments.map { m ->
                                @Composable {
                                    val e = log[m.index]
                                    val where = tr(
                                        "Row ${Grid.row(e.cell) + 1}, column ${Grid.col(e.cell) + 1}",
                                        "Mstari ${Grid.row(e.cell) + 1}, safu ${Grid.col(e.cell) + 1}",
                                    )
                                    ListRow(
                                        icon = when (m.kind) {
                                            Moment.Kind.MISTAKE -> Lucide.X
                                            Moment.Kind.HINT -> Lucide.Lightbulb
                                            Moment.Kind.PAUSE -> Lucide.Timer
                                        },
                                        title = when (m.kind) {
                                            Moment.Kind.MISTAKE -> tr("A wrong ${e.value}", "${e.value} isiyo sahihi")
                                            Moment.Kind.HINT -> tr("A hint placed ${e.value}", "Dokezo liliweka ${e.value}")
                                            Moment.Kind.PAUSE -> tr(
                                                "Thought for ${formatTime(m.seconds)}, then placed ${e.value}",
                                                "Ulifikiri kwa ${formatTime(m.seconds)}, kisha ukaweka ${e.value}",
                                            )
                                        },
                                        subtitle = "$where · ${formatTime(e.second)}",
                                        iconColor = when (m.kind) {
                                            Moment.Kind.MISTAKE -> c.shu
                                            Moment.Kind.HINT -> c.ink
                                            Moment.Kind.PAUSE -> c.ai
                                        },
                                        iconBackground = when (m.kind) {
                                            Moment.Kind.MISTAKE -> c.shuSoft
                                            Moment.Kind.HINT -> c.kinSoft
                                            Moment.Kind.PAUSE -> c.aiSoft
                                        },
                                        onClick = {
                                            playing = false
                                            shown = m.index + 1
                                        },
                                    )
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
