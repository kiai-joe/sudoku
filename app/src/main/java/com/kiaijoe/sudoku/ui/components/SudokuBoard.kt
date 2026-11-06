package com.kiaijoe.sudoku.ui.components

import com.kiaijoe.sudoku.engine.tr
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.kiaijoe.sudoku.engine.Cage
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.Grid
import com.kiaijoe.sudoku.engine.HintCard
import com.kiaijoe.sudoku.ui.Feedback
import com.kiaijoe.sudoku.ui.theme.BoardColors
import com.kiaijoe.sudoku.ui.theme.Inter
import com.kiaijoe.sudoku.ui.theme.LocalBoardColors
import com.kiaijoe.sudoku.ui.theme.Sora
import kotlin.math.PI
import kotlin.math.sin

/**
 * Where every cell sits on a board [width] wide: nine rounded boxes on a sunk
 * well with [gap] between them, and cells inside a box split by [hair] lines.
 * The well replaces the heavy 3×3 rules of a printed grid.
 */
private class BoardGeometry(width: Dp) {
    val pad = 6.dp
    val gap = 6.dp
    val hair = 1.dp
    val box = (width - pad * 2 - gap * 2) / 3
    val cell = (box - hair * 2) / 3

    /** Offset of row or column [k] of cells from the board's edge. */
    fun origin(k: Int): Dp = pad + (box + gap) * (k / 3) + (cell + hair) * (k % 3)

    /** Offset of box row or column [k]. */
    fun boxOrigin(k: Int): Dp = pad + (box + gap) * k

    /** Offset of cell [k] within its box. */
    fun inBox(k: Int): Dp = (cell + hair) * (k % 3)
}

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
    largeNumbers: Boolean = false,
    spotlight: Int? = null,
    onLongPress: ((Int) -> Unit)? = null,
) {
    val cageOf: Map<Int, Cage> = remember(game.cages) {
        buildMap { for (cage in game.cages) for (c in cage.cells) put(c, cage) }
    }
    // With instant checking off, a wrong number only shows once Check has flagged it.
    val wrongShown: (Int) -> Boolean = { game.isWrong(it) && (revealWrong || it in flagged) }
    val colors = LocalBoardColors.current
    val selectedDigit = selected?.let { game.values[it] }?.takeIf { it != 0 && highlightSameNumbers }
    val wash = rememberInkWash(flash)
    // Where the highlighted number can still go, from the numbers on the board.
    val spotCells: Set<Int> = remember(game.values, spotlight) {
        val d = spotlight ?: return@remember emptySet()
        val cands = game.boardCandidates()
        (0 until Grid.CELLS).filter { game.values[it] == d || cands[it] and (1 shl (d - 1)) != 0 }.toSet()
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.well),
    ) {
        val geo = BoardGeometry(maxWidth)
        val density = LocalDensity.current
        // Sized from the cell, not the system font scale, so text can never overflow the grid.
        val digitSize = with(density) { (geo.cell * if (largeNumbers) 0.68f else 0.54f).toSp() }
        val noteSize = with(density) { (geo.cell * if (largeNumbers) 0.30f else 0.25f).toSp() }
        val boxShape = RoundedCornerShape(min(16.dp, geo.box * 0.14f))

        for (b in 0 until 9) {
            val br = b / 3
            val bc = b % 3
            Box(
                Modifier
                    .offset(geo.boxOrigin(bc), geo.boxOrigin(br))
                    .size(geo.box)
                    .clip(boxShape)
                    .background(colors.line),
            ) {
                for (k in 0 until 9) {
                    val r = br * 3 + k / 3
                    val c = bc * 3 + k % 3
                    val index = r * 9 + c
                    Cell(
                        game = game,
                        index = index,
                        background = if (hint != null || mistakeCell != null) {
                            hintBackground(index, hint, mistakeCell, colors)
                        } else {
                            cellBackground(game, index, selected, selectedDigit, colors, wrongShown(index), index in spotCells)
                        },
                        wrongShown = wrongShown(index),
                        isSelected = index == selected && hint == null,
                        cage = cageOf[index],
                        hintShown = hint?.candidates?.get(index) ?: 0,
                        hintStruck = hint?.eliminations?.get(index) ?: 0,
                        selectedDigit = selectedDigit,
                        digitSize = digitSize,
                        noteSize = noteSize,
                        colors = colors,
                        washAlpha = { wash?.alpha(index) ?: 0f },
                        onClick = { onSelect(index) },
                        onLongClick = onLongPress?.let { { it(index) } },
                        modifier = Modifier
                            .offset(geo.inBox(c), geo.inBox(r))
                            .size(geo.cell),
                    )
                }
            }
        }

        if (game.isKiller) CageOverlay(game.cages, geo, colors)
    }
}

/**
 * Killer cages: a dashed outline just inside each cage's edge, and its sum
 * in the top-left corner of its first cell, on a patch of paper so the
 * outline doesn't run through the number. Where a cage crosses into the next
 * box, the line bridges the gap.
 */
@Composable
private fun CageOverlay(cages: List<Cage>, geo: BoardGeometry, colors: BoardColors) {
    val measurer = rememberTextMeasurer()
    val owner = remember(cages) {
        IntArray(Grid.CELLS).also { o -> cages.forEachIndexed { i, cage -> cage.cells.forEach { o[it] = i } } }
    }
    Canvas(Modifier.fillMaxSize()) {
        val step = geo.cell.toPx()
        fun at(k: Int) = geo.origin(k).toPx()
        val inset = step * 0.09f
        val lineColor = colors.given.copy(alpha = 0.55f)
        val width = 1.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
        for (cell in 0 until Grid.CELLS) {
            val r = Grid.row(cell)
            val c = Grid.col(cell)
            fun same(dr: Int, dc: Int): Boolean {
                val rr = r + dr
                val cc = c + dc
                return rr in 0..8 && cc in 0..8 && owner[rr * 9 + cc] == owner[cell]
            }
            val left = at(c)
            val top = at(r)
            val right = left + step
            val bottom = top + step
            // Where the cage continues, run the line on to the next cell so they join.
            val x0 = if (same(0, -1)) left else left + inset
            val x1 = if (same(0, 1)) at(c + 1) else right - inset
            val y0 = if (same(-1, 0)) top else top + inset
            val y1 = if (same(1, 0)) at(r + 1) else bottom - inset
            if (!same(-1, 0)) drawLine(lineColor, Offset(x0, top + inset), Offset(x1, top + inset), width, pathEffect = dash)
            if (!same(1, 0)) drawLine(lineColor, Offset(x0, bottom - inset), Offset(x1, bottom - inset), width, pathEffect = dash)
            if (!same(0, -1)) drawLine(lineColor, Offset(left + inset, y0), Offset(left + inset, y1), width, pathEffect = dash)
            if (!same(0, 1)) drawLine(lineColor, Offset(right - inset, y0), Offset(right - inset, y1), width, pathEffect = dash)
        }
        val style = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = (step * 0.21f).toSp(),
            lineHeight = (step * 0.21f).toSp(),
            color = colors.given,
        )
        for (cage in cages) {
            val anchor = cage.cells.first()
            val layout = measurer.measure(cage.sum.toString(), style)
            val pos = Offset(at(Grid.col(anchor)) + inset * 0.7f, at(Grid.row(anchor)) + inset * 0.4f)
            drawRoundRect(
                colors.cell,
                topLeft = pos - Offset(2.dp.toPx(), 0f),
                size = Size(layout.size.width + 4.dp.toPx(), layout.size.height.toFloat()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawText(layout, topLeft = pos)
        }
    }
}

/**
 * What a screen reader says for a cell: where it is, what's in it (given,
 * yours, or wrong), its pencil marks, its Killer cage, and during a hint
 * which candidates the hint shows and rules out.
 */
internal fun cellDescription(
    game: GameState,
    index: Int,
    wrongShown: Boolean,
    hintShown: Int = 0,
    hintStruck: Int = 0,
    cage: Cage? = null,
): String = buildString {
    val value = game.values[index]
    val row = Grid.row(index) + 1
    val col = Grid.col(index) + 1
    append(tr("Row $row, column $col, ", "Mstari $row, safu $col, "))
    when {
        value == 0 -> append(tr("empty", "kitupu"))
        game.isGiven(index) -> append(tr("$value, given", "$value, imetolewa"))
        wrongShown -> append(tr("$value, incorrect", "$value, si sahihi"))
        else -> append(value)
    }
    if (value == 0 && game.notes[index] != 0) {
        append(tr(", notes ", ", penseli ")).append(digitsText(game.notes[index]))
    }
    if (cage != null) {
        append(
            tr(
                ", cage of ${cage.cells.size} adding to ${cage.sum}",
                ", kizimba cha ${cage.cells.size} chenye jumla ya ${cage.sum}",
            ),
        )
    }
    if (hintShown != 0) append(tr(", hint candidates ", ", namba za dokezo ")).append(digitsText(hintShown))
    if (hintStruck != 0) append(tr(", ruled out ", ", zimeondolewa ")).append(digitsText(hintStruck))
    when (game.mark(index)) {
        1 -> append(tr(", marked green", ", alama ya kijani"))
        2 -> append(tr(", marked gold", ", alama ya dhahabu"))
    }
}

private fun digitsText(mask: Int) = (1..9).filter { mask and (1 shl (it - 1)) != 0 }.joinToString(", ")

/** While a hint is open, the board shows its pattern instead of the selection. */
private fun hintBackground(index: Int, hint: HintCard?, mistakeCell: Int?, colors: BoardColors): Color = when {
    index == mistakeCell -> colors.hintFocus
    hint == null -> colors.cell
    index in hint.focus -> colors.hintFocus
    index in hint.eliminations -> colors.wrongBackground
    index in hint.area -> colors.hintArea
    else -> colors.cell
}

private fun cellBackground(
    game: GameState,
    index: Int,
    selected: Int?,
    selectedDigit: Int?,
    colors: BoardColors,
    wrongShown: Boolean,
    spotlit: Boolean = false,
): Color = when {
    index == selected -> colors.selected
    wrongShown -> colors.wrongBackground
    spotlit -> colors.spotlight
    game.mark(index) == 1 -> colors.markA
    game.mark(index) == 2 -> colors.markB
    selectedDigit != null && game.values[index] == selectedDigit -> colors.sameNumber
    selected != null && Grid.arePeers(index, selected) -> colors.peer
    else -> colors.cell
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
    washAlpha: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    wrongShown: Boolean = false,
    hintShown: Int = 0,
    hintStruck: Int = 0,
    isSelected: Boolean = false,
    cage: Cage? = null,
) {
    val value = game.values[index]
    val description = cellDescription(game, index, wrongShown, hintShown, hintStruck, cage)

    Box(
        modifier = modifier
            .background(background)
            .drawWithContent {
                drawContent()
                val a = washAlpha()
                if (a > 0f) drawRect(colors.wash.copy(alpha = a))
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                contentDescription = description
                if (onLongClick != null) {
                    // `this.` picks the semantics action, not the onLongClick parameter of the same name.
                    this.onLongClick(label = tr("Change colour mark", "Badilisha alama ya rangi")) { onLongClick(); true }
                }
                selected = isSelected
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontFamily = Sora,
                fontSize = digitSize,
                lineHeight = digitSize,
                // Printed clues in a plain weight, the player's own heavier and in indigo.
                fontWeight = if (game.isGiven(index)) FontWeight.Normal else FontWeight.SemiBold,
                color = when {
                    wrongShown -> colors.wrong
                    isSelected -> colors.given
                    game.isGiven(index) -> colors.given
                    else -> colors.entered
                },
                // Underlined as well as red, so a mistake doesn't rely on colour alone.
                textDecoration = if (wrongShown) TextDecoration.Underline else null,
            )
        } else if (hintShown != 0 || hintStruck != 0) {
            HintNotes(hintShown, hintStruck, noteSize, colors)
        } else if (game.notes[index] != 0) {
            Notes(game, index, selectedDigit, noteSize, colors)
        }
    }
}

@Composable
private fun NoteGrid(size: TextUnit, digit: (Int) -> NoteMark?) {
    Column(Modifier.fillMaxSize()) {
        for (r in 0 until 3) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                for (c in 0 until 3) {
                    val d = r * 3 + c + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val mark = digit(d)
                        if (mark != null) {
                            Text(
                                text = d.toString(),
                                fontFamily = Inter,
                                fontSize = size,
                                lineHeight = size,
                                textAlign = TextAlign.Center,
                                fontWeight = mark.weight,
                                color = mark.color,
                                textDecoration = mark.decoration,
                            )
                        }
                    }
                }
            }
        }
    }
}

private class NoteMark(val color: Color, val weight: FontWeight, val decoration: TextDecoration? = null)

@Composable
private fun Notes(game: GameState, index: Int, selectedDigit: Int?, size: TextUnit, colors: BoardColors) {
    NoteGrid(size) { d ->
        when {
            !game.hasNote(index, d) -> null
            d == selectedDigit -> NoteMark(colors.entered, FontWeight.Bold)
            else -> NoteMark(colors.notes, FontWeight.SemiBold)
        }
    }
}

/**
 * A hint's candidates for one cell: [shown] digits in indigo, [struck] digits
 * (the ones the hint rules out) in vermilion and crossed through.
 */
@Composable
private fun HintNotes(shown: Int, struck: Int, size: TextUnit, colors: BoardColors) {
    NoteGrid(size) { d ->
        val bit = 1 shl (d - 1)
        when {
            struck and bit != 0 -> NoteMark(colors.wrong, FontWeight.Bold, TextDecoration.LineThrough)
            shown and bit != 0 -> NoteMark(colors.entered, FontWeight.Bold)
            else -> null
        }
    }
}

/** The matcha wash that sweeps across a just-finished row, column or box, in reading order. */
private class InkWash(private val progress: Animatable<Float, AnimationVector1D>, cells: List<Int>) {
    private val order: Map<Int, Int> = cells.withIndex().associate { (i, cell) -> cell to i }
    private val count = cells.size

    /** How strongly [cell] is washed right now. Read while drawing, so only drawing repeats. */
    fun alpha(cell: Int): Float {
        val i = order[cell] ?: return 0f
        // Each cell starts a little after the one before, over the first half.
        val start = i.toFloat() / count * 0.5f
        val t = ((progress.value - start) / 0.5f).coerceIn(0f, 1f)
        return sin(t * PI.toFloat()) * 0.40f
    }
}

@Composable
private fun rememberInkWash(flash: Feedback?): InkWash? {
    val reduceMotion = rememberReduceMotion()
    if (flash == null || flash.completed.isEmpty() || reduceMotion) return null
    val progress = remember(flash.id) { Animatable(0f) }
    LaunchedEffect(flash.id) { progress.animateTo(1f, tween(durationMillis = 600, easing = LinearEasing)) }
    return remember(flash.id) { InkWash(progress, flash.completed) }
}
