package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.ui.MISTAKE_LIMIT
import com.kiaijoe.sudoku.ui.SudokuViewModel
import com.kiaijoe.sudoku.ui.UiState
import com.kiaijoe.sudoku.ui.components.GameToolbar
import com.kiaijoe.sudoku.ui.components.NumberPad
import com.kiaijoe.sudoku.ui.components.SudokuBoard
import com.kiaijoe.sudoku.ui.formatTime
import com.kiaijoe.sudoku.ui.components.TimerText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(state: UiState, viewModel: SudokuViewModel, onExit: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setGameScreenActive(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setGameScreenActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setGameScreenActive(false)
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
                        if (state.generating) "New game" else game?.difficulty?.label.orEmpty(),
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
                        IconButton(onClick = viewModel::togglePause) {
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
                else -> Play(state, game, viewModel)
            }
        }
    }

    if (game != null && !state.generating) {
        EndDialogs(state, game, viewModel, onExit)
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
private fun Play(state: UiState, game: GameState, viewModel: SudokuViewModel) {
    Column(
        Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusRow(state, game)
        Spacer(Modifier.height(10.dp))

        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            SudokuBoard(
                game = game,
                selected = state.selected,
                highlightSameNumbers = state.settings.highlightSameNumbers,
                onSelect = viewModel::select,
                modifier = Modifier.fillMaxSize(),
            )
            if (state.paused) PausedOverlay(onResume = viewModel::togglePause)
        }

        Spacer(Modifier.height(18.dp))
        GameToolbar(
            canUndo = state.canUndo,
            notesMode = state.notesMode,
            onUndo = viewModel::undo,
            onErase = viewModel::erase,
            onToggleNotes = viewModel::toggleNotesMode,
            onHint = viewModel::hint,
        )
        Spacer(Modifier.height(14.dp))
        NumberPad(
            remaining = game::remaining,
            notesMode = state.notesMode,
            onDigit = viewModel::input,
        )
        Spacer(Modifier.height(16.dp))
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
        val mistakesText = if (state.settings.mistakeLimit) {
            "Mistakes ${game.mistakes}/$MISTAKE_LIMIT"
        } else {
            "Mistakes ${game.mistakes}"
        }
        Text(
            mistakesText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (game.mistakes > 0) scheme.error else scheme.onSurfaceVariant,
        )
        Text(
            "Hints ${game.hintsUsed}",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        TimerText(formatTime(game.elapsedSeconds), color = scheme.onSurface)
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
private fun EndDialogs(state: UiState, game: GameState, viewModel: SudokuViewModel, onExit: () -> Unit) {
    when (game.status) {
        GameStatus.WON -> {
            val best = state.stats[game.difficulty]?.bestSeconds
            AlertDialog(
                onDismissRequest = {},
                title = { Text(if (state.lastWinWasBest) "New best time!" else "Solved!") },
                text = {
                    Column {
                        Text(
                            "${game.difficulty.label} in ${formatTime(game.elapsedSeconds)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        val details = buildList {
                            if (best != null && !state.lastWinWasBest) add("Best: ${formatTime(best)}")
                            add("Mistakes: ${game.mistakes}")
                            add("Hints: ${game.hintsUsed}")
                        }
                        Text(details.joinToString("  ·  "), textAlign = TextAlign.Start)
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.newGame(game.difficulty) }) { Text("New game") }
                },
                dismissButton = { TextButton(onClick = onExit) { Text("Home") } },
            )
        }
        GameStatus.LOST -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Out of mistakes") },
            text = { Text("That was $MISTAKE_LIMIT wrong numbers. Try this puzzle again or start a fresh one.") },
            confirmButton = { Button(onClick = viewModel::restart) { Text("Try again") } },
            dismissButton = {
                FilledTonalButton(onClick = { viewModel.newGame(game.difficulty) }) { Text("New puzzle") }
            },
        )
        GameStatus.PLAYING -> Unit
    }
}
