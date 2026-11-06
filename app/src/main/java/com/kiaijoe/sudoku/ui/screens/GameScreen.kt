package com.kiaijoe.sudoku.ui.screens

import com.kiaijoe.sudoku.engine.tr
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.kiaijoe.sudoku.ui.shareText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Intent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kiaijoe.sudoku.ui.components.rememberReduceMotion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.Hints
import com.kiaijoe.sudoku.engine.HintCard
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
import com.kiaijoe.sudoku.ui.components.SealUnlock
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
import com.kiaijoe.sudoku.engine.Variant
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import kotlinx.coroutines.delay

/** The level as a title: "Medium", "Killer · Medium", "Sudoku X · Hard", "Daily · Killer Medium". */
internal fun GameState.title(): String = when {
    daily != null -> tr("Daily", "Leo") + " · ${if (isKiller) "Killer " else ""}${difficulty.label}"
    custom -> tr("Your puzzle", "Fumbo lako") + " · ${difficulty.label}"
    isKiller -> "Killer · ${difficulty.label}"
    variant != Variant.CLASSIC -> "${variant.label} · ${difficulty.label}"
    else -> difficulty.label
}

/** Opens the replay of the game just won; null where there is none (tests, previews). */
private val LocalOnReplay = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun GameScreen(state: UiState, actions: GameActions, onExit: () -> Unit, onReplay: (() -> Unit)? = null) {
    CompositionLocalProvider(LocalOnReplay provides onReplay) { GameScreenContent(state, actions, onExit) }
}

@Composable
private fun GameScreenContent(state: UiState, actions: GameActions, onExit: () -> Unit) {
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
    // The combinations cheat sheet, opened over a Killer game.
    var showCombinations by rememberSaveable { mutableStateOf(false) }
    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(
                title = when {
                    state.generating || game == null -> tr("New game", "Mchezo mpya")
                    game.practice != null && game.daily == null -> tr("Practice", "Mazoezi") + " · ${game.practice.label}"
                    else -> game.title()
                },
                onBack = onExit,
            ) {
                if (game != null && game.status == GameStatus.PLAYING && !state.generating) {
                    if (!state.settings.instantCheck && !state.paused) {
                        IconTile(Lucide.CheckCheck, tr("Check", "Kagua"), onClick = actions::check)
                    }
                    if (game.isKiller && !state.paused) {
                        IconTile(
                            Lucide.Sigma,
                            tr("Cage combinations", "Michanganyiko ya vizimba"),
                            onClick = { showCombinations = true },
                        )
                    }
                    IconTile(
                        if (state.paused) Lucide.Play else Lucide.Pause,
                        if (state.paused) tr("Resume", "Endelea") else tr("Pause", "Sitisha"),
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
    if (showCombinations && game != null) {
        // Open on the selected cell's cage size, so its sum is right there.
        val cageSize = state.selected?.let { cell -> game.cages.firstOrNull { cell in it.cells }?.cells?.size }
        // Its own full-screen window, so the board underneath can't be tapped through it.
        Dialog(
            onDismissRequest = { showCombinations = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            CombinationsScreen(onBack = { showCombinations = false }, initialSize = (cageSize ?: 2).coerceIn(2, 5))
        }
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
        Text(tr("Building your puzzle…", "Tunaandaa fumbo lako…"), style = MaterialTheme.typography.bodyMedium, color = Sumi.colors.inkMuted)
    }
}

/**
 * Whether a screen this size gets the side-by-side layout: wider than it is
 * tall, with room for a board and a column of controls (a phone on its side,
 * or a tablet).
 */
internal fun isWide(width: Dp, height: Dp) = width >= 600.dp && width > height

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
    Haptics(feedback, enabled = state.settings.haptics)

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (isWide(maxWidth, maxHeight)) {
            // Board on the left as big as the height allows; everything else beside it.
            val board = min(maxHeight - 16.dp, maxWidth * 0.56f)
            // A tablet has the height for a 3×3 pad of big keys; a phone on its side keeps the row.
            val gridPad = maxHeight >= 560.dp
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                BoardArea(state, game, actions, feedback, won, sawPlaying, Modifier.padding(top = 4.dp).size(board))
                Column(
                    Modifier
                        .widthIn(max = 480.dp)
                        .weight(1f, fill = false)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                ) {
                    StatusRow(state, game)
                    CheckMessage(state)
                    Spacer(Modifier.height(16.dp))
                    Controls(state, game, actions, onExit, won, gridPad, celebrate = sawPlaying)
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else {
            // How tall the hint sheet is, so the column can scroll the board clear of it.
            var sheetHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusRow(state, game)
                CheckMessage(state)
                Spacer(Modifier.height(12.dp))
                BoardArea(state, game, actions, feedback, won, sawPlaying, Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(16.dp))
                if (state.hint != null && !won) {
                    // The hint sheet sits over this space instead of the tools and number pad.
                    Spacer(Modifier.height(sheetHeight))
                } else {
                    Controls(state, game, actions, onExit, won, celebrate = sawPlaying)
                    Spacer(Modifier.height(16.dp))
                }
            }
            HintSheet(
                hint = state.hint.takeIf { !won },
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { sheetHeight = with(density) { it.height.toDp() } },
            )
        }
    }
}

/**
 * On a phone held upright, an open hint slides up from the bottom as a sheet
 * over the tools and number pad, leaving the board in view above it. With
 * Android's Remove animations on, it appears and goes without moving.
 */
@Composable
private fun HintSheet(hint: HintUi?, actions: GameActions, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    val reduceMotion = rememberReduceMotion()
    // Keep showing the last hint while the sheet slides away. A plain holder, not
    // state, so remembering it doesn't recompose anything.
    val last = remember { arrayOfNulls<HintUi>(1) }
    if (hint != null) last[0] = hint
    AnimatedVisibility(
        visible = hint != null,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else slideInVertically(tween(250)) { it } + fadeIn(tween(150)),
        exit = if (reduceMotion) ExitTransition.None else slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
    ) {
        val current = last[0] ?: return@AnimatedVisibility
        val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .card(c, shape, elevation = 16.dp)
                .semantics { paneTitle = tr("Hint", "Dokezo") }
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.line),
            )
            Spacer(Modifier.height(16.dp))
            HintContent(
                hint = current,
                onNext = actions::hintNext,
                onMore = actions::hintMore,
                onPlace = actions::hintPlace,
                onErase = actions::hintEraseMistake,
                onClose = actions::closeHint,
            )
        }
    }
}

@Composable
private fun CheckMessage(state: UiState) {
    val message = state.checkMessage ?: return
    Spacer(Modifier.height(8.dp))
    SumiChip(
        message,
        tone = if (state.flagged.isEmpty()) ChipTone.Matcha else ChipTone.Shu,
        icon = if (state.flagged.isEmpty()) Lucide.CheckCheck else Lucide.Eye,
    )
}

/** The board on its card, with the pause cover and the win stamp over it. */
@Composable
private fun BoardArea(
    state: UiState,
    game: GameState,
    actions: GameActions,
    feedback: Feedback?,
    won: Boolean,
    sawPlaying: Boolean,
    modifier: Modifier,
) {
    val c = Sumi.colors
    Box(modifier.card(c, CardShape)) {
        SudokuBoard(
            game = game,
            selected = state.selected,
            highlightSameNumbers = state.settings.highlightSameNumbers,
            onSelect = actions::select,
            modifier = Modifier.fillMaxSize(),
            hint = state.hint?.boardCard(),
            mistakeCell = (state.hint?.plan as? HintPlan.FixMistake)?.cell,
            revealWrong = state.settings.instantCheck,
            flagged = state.flagged,
            flash = feedback,
            largeNumbers = state.settings.largeNumbers,
            spotlight = state.spotlight.takeIf { state.hint == null },
            onLongPress = actions::mark,
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
}

/** Under or beside the board: the win summary, an open hint, or the tools and number pad. */
@Composable
private fun Controls(
    state: UiState,
    game: GameState,
    actions: GameActions,
    onExit: () -> Unit,
    won: Boolean,
    gridPad: Boolean = false,
    celebrate: Boolean = false,
) {
    val hint = state.hint
    if (won) {
        WinPanel(state, game, onNewGame = { actions.newLike(game) }, onHome = onExit, celebrate = celebrate)
    } else if (hint != null) {
        HintPanel(
            hint = hint,
            onNext = actions::hintNext,
            onMore = actions::hintMore,
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
        Spacer(Modifier.height(if (gridPad) 24.dp else 16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            NumberPad(
                remaining = { game.remaining(it, checking = state.settings.instantCheck) },
                notesMode = state.notesMode,
                onDigit = actions::input,
                largeNumbers = state.settings.largeNumbers,
                grid = gridPad,
                spotlight = state.spotlight,
                onLongDigit = actions::toggleSpotlight,
                modifier = if (gridPad) Modifier.widthIn(max = 380.dp) else Modifier,
            )
        }
    }
}

/** The teaching hint as a card, beside the board on a wide screen. */
@Composable
private fun HintPanel(
    hint: HintUi,
    onNext: () -> Unit,
    onPlace: () -> Unit,
    onErase: () -> Unit,
    onClose: () -> Unit,
    onMore: () -> Unit = {},
) {
    SumiCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
        HintContent(hint, onNext, onPlace, onErase, onClose, onMore)
    }
}

/** What a hint shows, in its card or its sheet: what to look at, and the next step. */
@Composable
private fun ColumnScope.HintContent(
    hint: HintUi,
    onNext: () -> Unit,
    onPlace: () -> Unit,
    onErase: () -> Unit,
    onClose: () -> Unit,
    onMore: () -> Unit = {},
) {
    val c = Sumi.colors
    val mistake = hint.plan as? HintPlan.FixMistake
    val card = hint.card
    // A gentle hint first says where to look, then names the technique.
    if (mistake == null && card != null && hint.level < HintUi.FULL) {
        GentleHint(hint.level, card, onClose, onMore)
        return
    }
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
                mistake != null -> tr("Check this cell", "Kagua kisanduku hiki")
                card != null -> card.title
                else -> tr("Hint", "Dokezo")
            },
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (hint.total > 1) SumiChip(tr("Step ${hint.index + 1} of ${hint.total}", "Hatua ${hint.index + 1} kati ya ${hint.total}"), tone = ChipTone.Ai)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        when {
            mistake != null ->
                tr(
                    "The ${mistake.digit} here doesn't match the solution. Erase it, then ask for a hint again.",
                    "Namba ${mistake.digit} hapa hailingani na jibu. Ifute, kisha uombe dokezo tena.",
                )
            card != null -> card.text
            else -> ""
        },
        style = MaterialTheme.typography.bodyLarge,
        color = c.ink,
    )
    if (card != null && !card.isPlacement && card.eliminations.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Crossed-out numbers on the board are the ones this rules out.", "Namba zilizokatwa ubaoni ndizo hatua hii inaziondoa."),
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton(tr("Close", "Funga"), onClose, Modifier.weight(1f), tone = ButtonTone.Soft)
        when {
            mistake != null ->
                PillButton(tr("Erase it", "Ifute"), onErase, Modifier.weight(1f), icon = Lucide.Eraser)
            card != null && card.isPlacement ->
                PillButton(tr("Place ${card.placeDigit}", "Weka ${card.placeDigit}"), onPlace, Modifier.weight(1f), icon = Lucide.Check)
            hint.hasNext ->
                PillButton(tr("Next step", "Hatua ijayo"), onNext, Modifier.weight(1f), trailingIcon = Lucide.ChevronRight)
        }
    }
}

/** A hint before its full explanation: a nudge to where to look, or the technique's name. */
@Composable
private fun GentleHint(level: Int, card: HintCard, onClose: () -> Unit, onMore: () -> Unit) {
    val c = Sumi.colors
    val nudge = level == HintUi.NUDGE
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(Lucide.Lightbulb, tint = c.ink, background = c.kinSoft, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            if (nudge) tr("A nudge", "Kidokezo kidogo") else card.title,
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        if (nudge) Hints.nudge(card).text else Hints.techniqueHint(card),
        style = MaterialTheme.typography.bodyLarge,
        color = c.ink,
    )
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton(tr("Close", "Funga"), onClose, Modifier.weight(1f), tone = ButtonTone.Soft)
        PillButton(
            if (nudge) tr("More help", "Msaada zaidi") else tr("Show me", "Nionyeshe"),
            onMore,
            Modifier.weight(1f),
            trailingIcon = Lucide.ChevronRight,
        )
    }
}

/**
 * What the board shows for an open hint at its level: a nudge lights up only
 * where to look, the technique adds its cells and candidates, and the full
 * hint adds what it rules out.
 */
private fun HintUi.boardCard(): HintCard? {
    val card = card ?: return null
    return when (level) {
        HintUi.NUDGE -> card.copy(
            area = Hints.nudge(card).cells,
            focus = emptySet(),
            eliminations = emptyMap(),
            candidates = emptyMap(),
        )
        HintUi.TECHNIQUE -> card.copy(eliminations = emptyMap())
        else -> card
    }
}

@Composable
private fun WinPanel(state: UiState, game: GameState, onNewGame: () -> Unit, onHome: () -> Unit, celebrate: Boolean = false) {
    val c = Sumi.colors
    val best = state.stats[game.difficulty]?.bestSeconds
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (state.settings.zen) {
                tr("Solved", "Limetatuliwa")
            } else {
                tr("Solved in ${formatTime(game.elapsedSeconds)}", "Limetatuliwa kwa ${formatTime(game.elapsedSeconds)}")
            },
            style = MaterialTheme.typography.displaySmall,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (state.lastWinWasBest) {
            Spacer(Modifier.height(10.dp))
            SumiChip(tr("New best time!", "Muda bora mpya!"), tone = ChipTone.Shu, icon = Lucide.Trophy)
        }
        Spacer(Modifier.height(16.dp))
        val rows = buildList<@Composable () -> Unit> {
            add {
                ListRow(
                    icon = if (game.isKiller) Lucide.SquareDashed else game.difficulty.icon,
                    title = if (game.daily != null) game.difficulty.label.let { if (game.isKiller) "Killer · $it" else it } else game.title(),
                    subtitle = game.hardest?.let { tr("Hardest step: ${it.label.lowercase()}", "Hatua ngumu zaidi: ${it.label.lowercase()}") },
                )
            }
            add {
                ListRow(
                    icon = Lucide.ListChecks,
                    title = "${mistakesCount(game.mistakes)} · ${hintsCount(game.hintsUsed)}",
                    subtitle = if (best != null && !state.lastWinWasBest && !state.settings.zen && !game.custom && game.variant == Variant.CLASSIC && !game.isKiller) {
                        tr("Your best is ${formatTime(best)}", "Muda wako bora ni ${formatTime(best)}")
                    } else {
                        null
                    },
                )
            }
            if (game.daily != null) {
                val streak = (if (game.isKiller) state.killerDaily else state.daily).streak
                add {
                    ListRow(
                        icon = Lucide.Flame,
                        title = if (game.isKiller) {
                            tr("$streak-day Killer streak", "Mfululizo wa Killer wa siku $streak")
                        } else {
                            tr("$streak-day streak", "Mfululizo wa siku $streak")
                        },
                        subtitle = if (game.isKiller) {
                            tr("Daily Killer done. Come back tomorrow to keep it going.", "Killer ya leo imekamilika. Rudi kesho kuendeleza mfululizo.")
                        } else {
                            tr("Daily puzzle done. Come back tomorrow to keep it going.", "Fumbo la leo limekamilika. Rudi kesho kuendeleza mfululizo.")
                        },
                        iconColor = c.shu,
                        iconBackground = c.shuSoft,
                    )
                }
            }
        }
        // Each new achievement stamps in once the hanamaru is down, a moment apart.
        state.newAchievements.forEachIndexed { i, achievement ->
            SealUnlock(achievement, animate = celebrate, delayMs = SEAL_DELAY_MS + i * SEAL_STAGGER_MS)
            Spacer(Modifier.height(12.dp))
        }
        ListCard(rows = rows)
        Spacer(Modifier.height(16.dp))
        val context = LocalContext.current
        val onReplay = LocalOnReplay.current
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                tr("Share", "Shiriki"),
                {
                    val daily = if (game.isKiller) state.killerDaily else state.daily
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, shareText(game, daily.takeIf { game.daily != null }, timed = !state.settings.zen))
                    context.startActivity(Intent.createChooser(send, null))
                },
                Modifier.weight(1f),
                tone = ButtonTone.Outline,
                icon = Lucide.Share2,
            )
            if (onReplay != null && game.log.isNotEmpty()) {
                PillButton(tr("Replay", "Marudio"), onReplay, Modifier.weight(1f), tone = ButtonTone.Outline, icon = Lucide.History)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(tr("Home", "Nyumbani"), onHome, Modifier.weight(1f), tone = ButtonTone.Soft, icon = Lucide.House)
            PillButton(tr("New game", "Mchezo mpya"), onNewGame, Modifier.weight(1f), tone = ButtonTone.Seal, icon = Lucide.RotateCcw)
        }
    }
}

/** The hanamaru takes about two seconds; new achievement seals land just as it settles, then one after another. */
private const val SEAL_DELAY_MS = 1700
private const val SEAL_STAGGER_MS = 450

private fun mistakesCount(n: Int) = tr(if (n == 1) "1 mistake" else "$n mistakes", if (n == 1) "kosa 1" else "makosa $n")

private fun hintsCount(n: Int) = tr(if (n == 1) "1 hint" else "$n hints", if (n == 1) "dokezo 1" else "madokezo $n")

@Composable
private fun StatusRow(state: UiState, game: GameState) {
    val c = Sumi.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val checking = state.settings.instantCheck
        if (state.settings.zen) {
            // Zen: nothing counting up or down, just the hints taken.
            Text(tr("Zen", "Utulivu"), style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
            Text(tr("Hints ${game.hintsUsed}", "Madokezo ${game.hintsUsed}"), style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
            return@Row
        }
        val mistakesText = when {
            !checking -> tr("Checks ${game.checks}", "Ukaguzi ${game.checks}")
            state.settings.mistakeLimit -> tr("Mistakes ${game.mistakes} of $MISTAKE_LIMIT", "Makosa ${game.mistakes} kati ya $MISTAKE_LIMIT")
            else -> tr("Mistakes ${game.mistakes}", "Makosa ${game.mistakes}")
        }
        Text(
            mistakesText,
            style = MaterialTheme.typography.labelMedium,
            color = if (checking && game.mistakes > 0) c.shu else c.inkMuted,
        )
        Text(tr("Hints ${game.hintsUsed}", "Madokezo ${game.hintsUsed}"), style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Timer, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            TimerText(formatTime(game.elapsedSeconds), color = c.ink)
        }
    }
}

/** Starts another game of the same kind as [game]. */
private fun GameActions.newLike(game: GameState) = when {
    game.isKiller -> newKiller(game.difficulty)
    game.variant != Variant.CLASSIC && !game.custom -> newVariant(game.difficulty, game.variant)
    else -> newGame(game.difficulty)
}

/**
 * A short vibration for each input, and a longer pattern for a solved puzzle
 * or a lost game, through the system's own haptic feedback (so it follows the
 * phone's touch-feedback setting and needs no permission). A pattern is a few
 * system effects in a row.
 */
@Composable
private fun Haptics(feedback: Feedback?, enabled: Boolean) {
    val view = LocalView.current
    LaunchedEffect(feedback?.id) {
        if (feedback == null || !enabled) return@LaunchedEffect
        for ((i, step) in hapticPattern(feedback.kind, Build.VERSION.SDK_INT).withIndex()) {
            if (i > 0) delay(step.delayMillis)
            view.performHapticFeedback(step.constant)
        }
    }
}

/** One effect in a vibration pattern, [delayMillis] after the one before. */
internal data class HapticStep(val constant: Int, val delayMillis: Long = 0)

/** The system haptic effects for [kind], in order. [sdk] picks effects the phone has. */
internal fun hapticPattern(kind: FeedbackKind, sdk: Int): List<HapticStep> {
    val modern = sdk >= Build.VERSION_CODES.R
    val confirm = if (modern) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
    val reject = if (modern) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    return when (kind) {
        FeedbackKind.PLACE -> listOf(HapticStep(HapticFeedbackConstants.KEYBOARD_TAP))
        FeedbackKind.NOTE, FeedbackKind.HINT_STEP -> listOf(HapticStep(HapticFeedbackConstants.CLOCK_TICK))
        FeedbackKind.WRONG -> listOf(HapticStep(reject))
        FeedbackKind.COMPLETE -> listOf(HapticStep(confirm))
        // Three rising beats, like the stamp coming down.
        FeedbackKind.WIN -> listOf(
            HapticStep(HapticFeedbackConstants.CLOCK_TICK),
            HapticStep(HapticFeedbackConstants.CONTEXT_CLICK, delayMillis = 110),
            HapticStep(confirm, delayMillis = 140),
            HapticStep(HapticFeedbackConstants.LONG_PRESS, delayMillis = 160),
        )
        // Two heavy buzzes.
        FeedbackKind.LOSE -> listOf(
            HapticStep(reject),
            HapticStep(reject, delayMillis = 220),
        )
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
        Text(tr("Paused", "Imesitishwa"), style = MaterialTheme.typography.headlineSmall, color = c.ink)
        Spacer(Modifier.height(16.dp))
        PillButton(tr("Resume", "Endelea"), onResume, icon = Lucide.Play)
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
            title = { Text(tr("Out of mistakes", "Makosa yamekwisha"), style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    tr(
                        "That was $MISTAKE_LIMIT wrong numbers. Try this puzzle again or start a fresh one.",
                        "Hizo ni namba $MISTAKE_LIMIT zisizo sahihi. Jaribu fumbo hili tena au anza jipya.",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = { PillButton(tr("Try again", "Jaribu tena"), actions::restart, height = 44.dp) },
            dismissButton = {
                val onReplay = LocalOnReplay.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onReplay != null && game.log.isNotEmpty()) {
                        PillButton(tr("Replay", "Marudio"), onReplay, tone = ButtonTone.Ghost, icon = Lucide.History, height = 44.dp)
                    }
                    PillButton(tr("New puzzle", "Fumbo jipya"), { actions.newLike(game) }, tone = ButtonTone.Soft, height = 44.dp)
                }
            },
        )
        GameStatus.PLAYING -> Unit
    }
}
