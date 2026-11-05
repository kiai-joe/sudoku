package com.kiaijoe.sudoku.ui.screens

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.HintPlan
import com.kiaijoe.sudoku.ui.Feedback
import com.kiaijoe.sudoku.ui.FeedbackKind
import com.kiaijoe.sudoku.ui.GameActions
import com.kiaijoe.sudoku.ui.HintUi
import com.kiaijoe.sudoku.ui.MISTAKE_LIMIT
import com.kiaijoe.sudoku.ui.UiState
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.CardShape
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.GameToolbar
import com.kiaijoe.sudoku.ui.components.HanamaruStamp
import com.kiaijoe.sudoku.ui.components.IconBadge
import com.kiaijoe.sudoku.ui.components.IconTile
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.ListRow
import com.kiaijoe.sudoku.ui.components.NumberPad
import com.kiaijoe.sudoku.ui.components.Page
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.SudokuBoard
import com.kiaijoe.sudoku.ui.components.SumiCard
import com.kiaijoe.sudoku.ui.components.SumiChip
import com.kiaijoe.sudoku.ui.components.SumiTopBar
import com.kiaijoe.sudoku.ui.components.TimerText
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.components.icon
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi

/** The level as a title: "Medium", "Killer · Medium", "Daily · Killer Medium". */
private fun GameState.title(): String = when {
    daily != null -> "Daily · ${if (isKiller) "Killer " else ""}${difficulty.label}"
    isKiller -> "Killer · ${difficulty.label}"
    else -> difficulty.label
}

@Composable
fun GameScreen(state: UiState, actions: GameActions, onExit: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> actions.setGameScreenActive(true)
                Lifecycle.Event.ON_PAUSE -> actions.setGameScreenActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            actions.setGameScreenActive(false)
        }
    }
    BackHandler(onBack = onExit)

    val game = state.game
    if (game == null && !state.generating) {
        // Nothing to show, e.g. after the app was restored without a saved game.
        LaunchedEffect(Unit) { onExit() }
        return
    }
    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(
                title = if (state.generating || game == null) "New game" else game.title(),
                onBack = onExit,
            ) {
                if (game != null && game.status == GameStatus.PLAYING && !state.generating) {
                    if (!state.settings.instantCheck && !state.paused) {
                        IconTile(Lucide.CheckCheck, "Check", onClick = actions::check)
                    }
                    IconTile(
                        if (state.paused) Lucide.Play else Lucide.Pause,
                        if (state.paused) "Resume" else "Pause",
                        onClick = actions::togglePause,
                    )
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when {
                    state.generating || game == null -> Generating()
                    else -> Play(state, game, actions, onExit)
                }
            }
        }
    }

    if (game != null && !state.generating) {
        EndDialogs(game, actions)
    }
}

@Composable
private fun Generating() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Sumi.colors.ai, trackColor = Sumi.colors.surfaceSunk)
        Spacer(Modifier.height(16.dp))
        Text("Building your puzzle…", style = MaterialTheme.typography.bodyMedium, color = Sumi.colors.inkMuted)
    }
}

@Composable
private fun Play(state: UiState, game: GameState, actions: GameActions, onExit: () -> Unit) {
    val c = Sumi.colors
    val won = game.status == GameStatus.WON
    // Draw the stamp in only for a win seen happen, not when reopening a won game.
    var sawPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(game.status) {
        if (game.status == GameStatus.PLAYING) sawPlaying = true
    }
    // Only react to inputs made while this screen is open, not one left over from before.
    val staleFeedbackId = remember { state.feedback?.id }
    val feedback = state.feedback?.takeIf { it.id != staleFeedbackId }
    Column(
        Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusRow(state, game)
        state.checkMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            SumiChip(
                message,
                tone = if (state.flagged.isEmpty()) ChipTone.Matcha else ChipTone.Shu,
                icon = if (state.flagged.isEmpty()) Lucide.CheckCheck else Lucide.Eye,
            )
        }
        Haptics(feedback, enabled = state.settings.haptics)
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .card(c, CardShape),
        ) {
            SudokuBoard(
                game = game,
                selected = state.selected,
                highlightSameNumbers = state.settings.highlightSameNumbers,
                onSelect = actions::select,
                modifier = Modifier.fillMaxSize(),
                hint = state.hint?.card,
                mistakeCell = (state.hint?.plan as? HintPlan.FixMistake)?.cell,
                revealWrong = state.settings.instantCheck,
                flagged = state.flagged,
                flash = feedback,
                largeNumbers = state.settings.largeNumbers,
            )
            if (state.paused) PausedOverlay(onResume = actions::togglePause)
            if (won) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CardShape)
                        .background(c.paper.copy(alpha = 0.55f)),
                )
                HanamaruStamp(
                    color = c.shu,
                    animate = sawPlaying,
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .align(Alignment.Center)
                        .rotate(-8f),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        val hint = state.hint
        if (won) {
            WinPanel(state, game, onNewGame = { actions.newLike(game) }, onHome = onExit)
        } else if (hint != null) {
            HintPanel(
                hint = hint,
                onNext = actions::hintNext,
                onPlace = actions::hintPlace,
                onErase = actions::hintEraseMistake,
                onClose = actions::closeHint,
            )
        } else {
            GameToolbar(
                canUndo = state.canUndo,
                notesMode = state.notesMode,
                onUndo = actions::undo,
                onErase = actions::erase,
                onToggleNotes = actions::toggleNotesMode,
                onFill = actions::fillCandidates,
                onHint = actions::hint,
            )
            Spacer(Modifier.height(16.dp))
            NumberPad(
                remaining = { game.remaining(it, checking = state.settings.instantCheck) },
                notesMode = state.notesMode,
                onDigit = actions::input,
                largeNumbers = state.settings.largeNumbers,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** The teaching hint, as a card under the board: what to look at, and the next step. */
@Composable
private fun HintPanel(
    hint: HintUi,
    onNext: () -> Unit,
    onPlace: () -> Unit,
    onErase: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Sumi.colors
    val mistake = hint.plan as? HintPlan.FixMistake
    val card = hint.card
    SumiCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (mistake != null) Lucide.Eye else Lucide.Lightbulb,
                tint = c.ink,
                background = c.kinSoft,
                size = 40.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                when {
                    mistake != null -> "Check this cell"
                    card != null -> card.title
                    else -> "Hint"
                },
                style = MaterialTheme.typography.titleLarge,
                color = c.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (hint.total > 1) SumiChip("Step ${hint.index + 1} of ${hint.total}", tone = ChipTone.Ai)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                mistake != null ->
                    "The ${mistake.digit} here doesn't match the solution. Erase it, then ask for a hint again."
                card != null -> card.text
                else -> ""
            },
            style = MaterialTheme.typography.bodyLarge,
            color = c.ink,
        )
        if (card != null && !card.isPlacement && card.eliminations.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Crossed-out numbers on the board are the ones this rules out.",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Close", onClose, Modifier.weight(1f), tone = ButtonTone.Soft)
            when {
                mistake != null ->
                    PillButton("Erase it", onErase, Modifier.weight(1f), icon = Lucide.Eraser)
                card != null && card.isPlacement ->
                    PillButton("Place ${card.placeDigit}", onPlace, Modifier.weight(1f), icon = Lucide.Check)
                hint.hasNext ->
                    PillButton("Next step", onNext, Modifier.weight(1f), trailingIcon = Lucide.ChevronRight)
            }
        }
    }
}

@Composable
private fun WinPanel(state: UiState, game: GameState, onNewGame: () -> Unit, onHome: () -> Unit) {
    val c = Sumi.colors
    val best = state.stats[game.difficulty]?.bestSeconds
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Solved in ${formatTime(game.elapsedSeconds)}",
            style = MaterialTheme.typography.displaySmall,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (state.lastWinWasBest) {
            Spacer(Modifier.height(10.dp))
            SumiChip("New best time!", tone = ChipTone.Shu, icon = Lucide.Trophy)
        }
        Spacer(Modifier.height(18.dp))
        val rows = buildList<@Composable () -> Unit> {
            add {
                ListRow(
                    icon = if (game.isKiller) Lucide.SquareDashed else game.difficulty.icon,
                    title = if (game.isKiller) "Killer · ${game.difficulty.label}" else game.difficulty.label,
                    subtitle = game.hardest?.let { "Hardest step: ${it.label.lowercase()}" },
                )
            }
            add {
                ListRow(
                    icon = Lucide.Target,
                    title = "${plural(game.mistakes, "mistake")} · ${plural(game.hintsUsed, "hint")}",
                    subtitle = if (best != null && !state.lastWinWasBest) "Your best is ${formatTime(best)}" else null,
                )
            }
            if (game.daily != null) {
                val streak = state.daily.streak
                add {
                    ListRow(
                        icon = Lucide.Flame,
                        title = "$streak-day streak",
                        subtitle = "Daily puzzle done. Come back tomorrow to keep it going.",
                        iconColor = c.shu,
                        iconBackground = c.shuSoft,
                    )
                }
            }
            for (achievement in state.newAchievements) {
                add {
                    ListRow(
                        icon = achievement.icon,
                        title = "New achievement: ${achievement.title}",
                        subtitle = achievement.description,
                        iconColor = c.onShu,
                        iconBackground = c.shu,
                    )
                }
            }
        }
        ListCard(rows = rows)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Home", onHome, Modifier.weight(1f), tone = ButtonTone.Soft, icon = Lucide.House)
            PillButton("New game", onNewGame, Modifier.weight(1f), tone = ButtonTone.Seal, icon = Lucide.RotateCcw)
        }
    }
}

private fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"

@Composable
private fun StatusRow(state: UiState, game: GameState) {
    val c = Sumi.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val checking = state.settings.instantCheck
        val mistakesText = when {
            !checking -> "Checks ${state.checks}"
            state.settings.mistakeLimit -> "Mistakes ${game.mistakes} of $MISTAKE_LIMIT"
            else -> "Mistakes ${game.mistakes}"
        }
        Text(
            mistakesText,
            style = MaterialTheme.typography.labelMedium,
            color = if (checking && game.mistakes > 0) c.shu else c.inkMuted,
        )
        Text("Hints ${game.hintsUsed}", style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Timer, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            TimerText(formatTime(game.elapsedSeconds), color = c.ink)
        }
    }
}

/** Starts another game of the same kind as [game]. */
private fun GameActions.newLike(game: GameState) =
    if (game.isKiller) newKiller(game.difficulty) else newGame(game.difficulty)

/** A short vibration for each input, through the system's own haptic feedback settings. */
@Composable
private fun Haptics(feedback: Feedback?, enabled: Boolean) {
    val view = LocalView.current
    LaunchedEffect(feedback?.id) {
        if (feedback == null || !enabled) return@LaunchedEffect
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val constant = when (feedback.kind) {
            FeedbackKind.PLACE -> HapticFeedbackConstants.KEYBOARD_TAP
            FeedbackKind.NOTE -> HapticFeedbackConstants.CLOCK_TICK
            FeedbackKind.WRONG -> if (modern) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            FeedbackKind.COMPLETE -> if (modern) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    val c = Sumi.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(Lucide.Pause, tint = c.ai, background = c.aiSoft, size = 56.dp)
        Spacer(Modifier.height(14.dp))
        Text("Paused", style = MaterialTheme.typography.headlineSmall, color = c.ink)
        Spacer(Modifier.height(16.dp))
        PillButton("Resume", onResume, icon = Lucide.Play)
    }
}

@Composable
private fun EndDialogs(game: GameState, actions: GameActions) {
    val c = Sumi.colors
    when (game.status) {
        GameStatus.WON -> Unit // Shown in place by the stamp and WinPanel.
        GameStatus.LOST -> AlertDialog(
            onDismissRequest = {},
            containerColor = c.surface,
            title = { Text("Out of mistakes", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "That was $MISTAKE_LIMIT wrong numbers. Try this puzzle again or start a fresh one.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { PillButton("Try again", actions::restart, height = 44.dp) },
            dismissButton = {
                PillButton("New puzzle", { actions.newLike(game) }, tone = ButtonTone.Soft, height = 44.dp)
            },
        )
        GameStatus.PLAYING -> Unit
    }
}
