package com.kiaijoe.sudoku.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.Grid
import com.kiaijoe.sudoku.engine.HintCard
import com.kiaijoe.sudoku.ui.Feedback
import com.kiaijoe.sudoku.ui.theme.BoardColors
import com.kiaijoe.sudoku.ui.theme.LocalBoardColors
import com.kiaijoe.sudoku.ui.theme.SealRed
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SudokuBoard(
    game: GameState,
    selected: Int?,
    highlightSameNumbers: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: HintCard? = null,
    mistakeCell: Int? = null,
    revealWrong: Boolean = true,
    flagged: Set<Int> = emptySet(),
    flash: Feedback? = null,
) {
    // With instant checking off, a wrong number only shows once Check has flagged it.
    val wrongShown: (Int) -> Boolean = { game.isWrong(it) && (revealWrong || it in flagged) }
    val colors = LocalBoardColors.current
    val shape = RoundedCornerShape(10.dp)
    val selectedDigit = selected?.let { game.values[it] }?.takeIf { it != 0 && highlightSameNumbers }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val cell: Dp = maxWidth / 9
        val density = LocalDensity.current
        val digitSize = with(density) { (cell * 0.56f).toSp() }
        val noteSize = with(density) { (cell * 0.25f).toSp() }

        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    for (c in 0 until 9) {
                        val index = r * 9 + c
                        Cell(
                            game = game,
                            index = index,
                            background = if (hint != null || mistakeCell != null) {
                                hintBackground(index, hint, mistakeCell, colors)
                            } else {
                                cellBackground(game, index, selected, selectedDigit, colors, wrongShown(index))
                            },
                            wrongShown = wrongShown(index),
                            hintShown = hint?.candidates?.get(index) ?: 0,
                            hintStruck = hint?.eliminations?.get(index) ?: 0,
                            selectedDigit = selectedDigit,
                            digitSize = digitSize,
                            noteSize = noteSize,
                            colors = colors,
                            onClick = { onSelect(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }

        if (flash != null && flash.completed.isNotEmpty()) InkWash(flash)

        // Grid lines are drawn on top so cell backgrounds never cover them.
        Canvas(Modifier.fillMaxSize()) {
            val step = size.width / 9f
            val thin = 1.dp.toPx()
            val thick = 2.dp.toPx()
            for (k in 1 until 9) {
                val major = k % 3 == 0
                val color = if (major) colors.thickLine else colors.thinLine
                val stroke = if (major) thick else thin
                val p = step * k
                drawLine(color, Offset(p, 0f), Offset(p, size.height), stroke)
                drawLine(color, Offset(0f, p), Offset(size.width, p), stroke)
            }
            drawRoundRect(
                color = colors.thickLine,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = thick * 1.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            )
        }
    }
}

/** While a hint is open, the board shows its pattern instead of the selection. */
private fun hintBackground(index: Int, hint: HintCard?, mistakeCell: Int?, colors: BoardColors): Color = when {
    index == mistakeCell -> colors.hintFocus
    hint == null -> Color.Transparent
    index in hint.focus -> colors.hintFocus
    index in hint.eliminations -> colors.wrongBackground
    index in hint.area -> colors.hintArea
    else -> Color.Transparent
}

private fun cellBackground(
    game: GameState,
    index: Int,
    selected: Int?,
    selectedDigit: Int?,
    colors: BoardColors,
    wrongShown: Boolean,
): Color = when {
    index == selected -> colors.selected
    wrongShown -> colors.wrongBackground
    selectedDigit != null && game.values[index] == selectedDigit -> colors.sameNumber
    selected != null && Grid.arePeers(index, selected) -> colors.peer
    else -> Color.Transparent
}

@Composable
private fun Cell(
    game: GameState,
    index: Int,
    background: Color,
    selectedDigit: Int?,
    digitSize: TextUnit,
    noteSize: TextUnit,
    colors: BoardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wrongShown: Boolean = false,
    hintShown: Int = 0,
    hintStruck: Int = 0,
) {
    val value = game.values[index]
    val description = buildString {
        append("Row ${Grid.row(index) + 1}, column ${Grid.col(index) + 1}, ")
        when {
            value == 0 -> append("empty")
            game.isGiven(index) -> append("$value, given")
            wrongShown -> append("$value, incorrect")
            else -> append(value)
        }
    }

    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontSize = digitSize,
                lineHeight = digitSize,
                // Printed clues in a plain weight, the player's own marks heavier, like ink over print.
                fontWeight = if (game.isGiven(index)) FontWeight.Normal else FontWeight.SemiBold,
                color = when {
                    game.isGiven(index) -> colors.given
                    wrongShown -> colors.wrong
                    else -> colors.entered
                },
            )
        } else if (hintShown != 0 || hintStruck != 0) {
            HintNotes(hintShown, hintStruck, noteSize, colors)
        } else if (game.notes[index] != 0) {
            Notes(game, index, selectedDigit, noteSize, colors)
        }
    }
}

@Composable
private fun Notes(game: GameState, index: Int, selectedDigit: Int?, size: TextUnit, colors: BoardColors) {
    Column(Modifier.fillMaxSize()) {
        for (r in 0 until 3) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                for (c in 0 until 3) {
                    val digit = r * 3 + c + 1
                    val shown = game.hasNote(index, digit)
                    val match = shown && digit == selectedDigit
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (shown) {
                            Text(
                                text = digit.toString(),
                                fontSize = size,
                                lineHeight = size,
                                textAlign = TextAlign.Center,
                                fontWeight = if (match) FontWeight.Bold else FontWeight.Normal,
                                color = if (match) colors.entered else colors.given.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A hint's candidates for one cell: [shown] digits in ink, [struck] digits
 * (the ones the hint rules out) in vermilion and crossed through.
 */
@Composable
private fun HintNotes(shown: Int, struck: Int, size: TextUnit, colors: BoardColors) {
    Column(Modifier.fillMaxSize()) {
        for (r in 0 until 3) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                for (c in 0 until 3) {
                    val digit = r * 3 + c + 1
                    val bit = 1 shl (digit - 1)
                    val isStruck = struck and bit != 0
                    val isShown = shown and bit != 0
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isStruck || isShown) {
                            Text(
                                text = digit.toString(),
                                fontSize = size,
                                lineHeight = size,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (isStruck) colors.wrong else colors.entered,
                                textDecoration = if (isStruck) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A brief vermilion wash that sweeps across a just-finished row, column or
 * box, in the order its cells read.
 */
@Composable
private fun InkWash(flash: Feedback) {
    val progress = remember(flash.id) { Animatable(0f) }
    LaunchedEffect(flash.id) { progress.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing)) }
    val cells = flash.completed
    Canvas(Modifier.fillMaxSize()) {
        val step = size.width / 9f
        cells.forEachIndexed { i, cell ->
            // Each cell starts a little after the one before, over the first half.
            val start = i.toFloat() / cells.size * 0.5f
            val t = ((progress.value - start) / 0.5f).coerceIn(0f, 1f)
            val alpha = sin(t * PI.toFloat()) * 0.45f
            if (alpha > 0f) {
                drawRect(
                    color = SealRed.copy(alpha = alpha),
                    topLeft = Offset(Grid.col(cell) * step, Grid.row(cell) * step),
                    size = Size(step, step),
                )
            }
        }
    }
}
