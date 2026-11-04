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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
import com.kiaijoe.sudoku.ui.components.GameToolbar
import com.kiaijoe.sudoku.ui.components.HanamaruStamp
import com.kiaijoe.sudoku.ui.components.NumberPad
import com.kiaijoe.sudoku.ui.components.SudokuBoard
import com.kiaijoe.sudoku.ui.components.TimerText
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.theme.SealRed

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when {
                            state.generating -> "New game"
                            game?.daily != null ->
                                "Daily · ${if (game.isKiller) "Killer " else ""}${game.difficulty.label}"
                            game?.isKiller == true -> "Killer · ${game.difficulty.label}"
                            else -> game?.difficulty?.label.orEmpty()
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (game != null && game.status == GameStatus.PLAYING && !state.generating) {
                        if (!state.settings.instantCheck && !state.paused) {
                            TextButton(onClick = actions::check) { Text("Check") }
                        }
                        IconButton(onClick = actions::togglePause) {
                            Icon(
                                if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (state.paused) "Resume" else "Pause",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.generating || game == null -> Generating()
                else -> Play(state, game, actions, onExit)
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
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Building your puzzle…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Play(state: UiState, game: GameState, actions: GameActions, onExit: () -> Unit) {
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
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusRow(state, game)
        state.checkMessage?.let { message ->
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.flagged.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        }
        Haptics(feedback, enabled = state.settings.haptics)
        Spacer(Modifier.height(10.dp))

        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)),
                )
                HanamaruStamp(
                    color = SealRed,
                    animate = sawPlaying,
                    modifier = Modifier
                        .fillMaxSize(0.82f)
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
            Spacer(Modifier.height(14.dp))
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

@Composable
private fun HintPanel(
    hint: HintUi,
    onNext: () -> Unit,
    onPlace: () -> Unit,
    onErase: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val mistake = hint.plan as? HintPlan.FixMistake
    val card = hint.card
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        mistake != null -> "Check this cell"
                        card != null -> card.title
                        else -> "Hint"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (hint.total > 1) {
                    Text(
                        "Step ${hint.index + 1} of ${hint.total}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    mistake != null ->
                        "The ${mistake.digit} here doesn't match the solution. Erase it, then ask for a hint again."
                    card != null -> card.text
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (card != null && !card.isPlacement && card.eliminations.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Crossed-out numbers on the board are the ones this rules out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(48.dp)) { Text("Close") }
                when {
                    mistake != null ->
                        Button(onClick = onErase, modifier = Modifier.weight(1f).height(48.dp)) { Text("Erase it") }
                    card != null && card.isPlacement ->
                        Button(onClick = onPlace, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Place ${card.placeDigit}")
                        }
                    hint.hasNext ->
                        Button(onClick = onNext, modifier = Modifier.weight(1f).height(48.dp)) { Text("Next step") }
                }
            }
        }
    }
}

@Composable
private fun WinPanel(state: UiState, game: GameState, onNewGame: () -> Unit, onHome: () -> Unit) {
    val best = state.stats[game.difficulty]?.bestSeconds
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (state.lastWinWasBest) "New best time!" else "Solved!",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${if (game.isKiller) "Killer · " else ""}${game.difficulty.label} in ${formatTime(game.elapsedSeconds)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        val details = buildList {
            if (best != null && !state.lastWinWasBest) add("Best: ${formatTime(best)}")
            add("Mistakes: ${game.mistakes}")
            add("Hints: ${game.hintsUsed}")
        }
        if (game.daily != null) {
            val streak = state.daily.streak
            Text(
                if (streak == 1) "Daily puzzle done · streak 1 day" else "Daily puzzle done · streak $streak days",
                style = MaterialTheme.typography.bodyMedium,
                color = SealRed,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
        }
        for (achievement in state.newAchievements) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                HanamaruStamp(SealRed, Modifier.size(20.dp), animate = false)
                Spacer(Modifier.size(6.dp))
                Text(
                    "New achievement: ${achievement.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SealRed,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        game.hardest?.let { technique ->
            Text(
                "Hardest step: ${technique.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            details.joinToString("  ·  "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.weight(1f).height(52.dp)) { Text("Home") }
            Button(onClick = onNewGame, modifier = Modifier.weight(1f).height(52.dp)) { Text("New game") }
        }
    }
}

@Composable
private fun StatusRow(state: UiState, game: GameState) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val checking = state.settings.instantCheck
        val mistakesText = when {
            !checking -> "Checks ${state.checks}"
            state.settings.mistakeLimit -> "Mistakes ${game.mistakes}/$MISTAKE_LIMIT"
            else -> "Mistakes ${game.mistakes}"
        }
        Text(
            mistakesText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checking && game.mistakes > 0) scheme.error else scheme.onSurfaceVariant,
        )
        Text(
            "Hints ${game.hintsUsed}",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        TimerText(formatTime(game.elapsedSeconds), color = scheme.onSurface)
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Paused", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onResume) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("Resume")
            }
        }
    }
}

@Composable
private fun EndDialogs(game: GameState, actions: GameActions) {
    when (game.status) {
        GameStatus.WON -> Unit // Shown in place by the stamp and WinPanel.
        GameStatus.LOST -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Out of mistakes") },
            text = { Text("That was $MISTAKE_LIMIT wrong numbers. Try this puzzle again or start a fresh one.") },
            confirmButton = { Button(onClick = actions::restart) { Text("Try again") } },
            dismissButton = {
                FilledTonalButton(onClick = { actions.newLike(game) }) { Text("New puzzle") }
            },
        )
        GameStatus.PLAYING -> Unit
    }
}
