package com.kiaijoe.sudoku.ui.components

import com.kiaijoe.sudoku.engine.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sora
import com.kiaijoe.sudoku.ui.theme.Sumi

@Composable
fun GameToolbar(
    canUndo: Boolean,
    notesMode: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onFill: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ToolButton(Lucide.Undo2, tr("Undo", "Tendua"), enabled = canUndo, onClick = onUndo)
        ToolButton(Lucide.Eraser, tr("Erase", "Futa"), onClick = onErase)
        ToolButton(
            icon = Lucide.Pencil,
            label = tr("Notes", "Penseli"),
            active = notesMode,
            state = if (notesMode) tr("on", "imewashwa") else tr("off", "imezimwa"),
            onClick = onToggleNotes,
        )
        ToolButton(
            Lucide.Grid3x3,
            tr("Fill", "Jaza"),
            onClick = onFill,
            description = tr("Fill in all candidates", "Jaza namba zote zinazowezekana"),
        )
        ToolButton(Lucide.Lightbulb, tr("Hint", "Dokezo"), onClick = onHint)
    }
}

/** A tool: a raised rounded chip with its icon, and its name under it. Active tools fill indigo. */
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    state: String? = null,
    description: String = label,
) {
    val c = Sumi.colors
    val tap = onClick
    val tint = when {
        // Greyed out, but still clearly an icon on the dark palettes too.
        !enabled -> c.inkMuted.copy(alpha = 0.6f)
        active -> c.onAi
        else -> c.ink
    }
    Column(
        Modifier
            .width(64.dp)
            .clip(TileShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (state != null) stateDescription = state
                if (!enabled) disabled()
                this.onClick { tap(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(50.dp)
                .then(
                    if (active) {
                        Modifier.background(c.ai, TileShape)
                    } else if (enabled) {
                        Modifier.card(c, TileShape, elevation = 4.dp)
                    } else {
                        Modifier.background(c.surfaceSunk, TileShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> c.inkMuted
                active -> c.ai
                else -> c.ink
            },
        )
    }
}

/**
 * The number keys. In one row by default; with [grid], three rows of three
 * bigger keys, for a tablet with room beside the board.
 */
@Composable
fun NumberPad(
    remaining: (Int) -> Int,
    notesMode: Boolean,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    largeNumbers: Boolean = false,
    grid: Boolean = false,
) {
    if (grid) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0 until 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 3) {
                        val digit = row * 3 + col + 1
                        PadKey(
                            digit, remaining(digit), notesMode, largeNumbers, big = true, onDigit = onDigit,
                            // Wider than tall, so three rows sit comfortably under the tools.
                            modifier = Modifier.weight(1f).aspectRatio(if (largeNumbers) 1.15f else 1.35f),
                        )
                    }
                }
            }
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (digit in 1..9) {
                PadKey(
                    digit, remaining(digit), notesMode, largeNumbers, big = false, onDigit = onDigit,
                    // Taller in large mode so the bigger digit still leaves room for the count under it.
                    modifier = Modifier.weight(1f).aspectRatio(if (largeNumbers) 0.6f else 0.72f),
                )
            }
        }
    }
}

/** One number key: the digit in indigo with how many are left under it, or a tick once all nine are placed. */
@Composable
private fun PadKey(
    digit: Int,
    left: Int,
    notesMode: Boolean,
    largeNumbers: Boolean,
    big: Boolean,
    onDigit: (Int) -> Unit,
    modifier: Modifier,
) {
    val c = Sumi.colors
    val done = left <= 0
    Box(
        modifier
            .then(
                when {
                    done -> Modifier.background(c.surfaceSunk, TileShape)
                    notesMode -> Modifier.background(c.aiSoft, TileShape)
                    else -> Modifier.card(c, TileShape, elevation = 4.dp)
                },
            )
            .clip(TileShape)
            .clickable(enabled = !done, role = Role.Button) { onDigit(digit) }
            .clearAndSetSemantics {
                contentDescription = if (done) {
                    tr("$digit, all placed", "$digit, zote zimewekwa")
                } else {
                    tr("$digit, $left left", "$digit, zimebaki $left")
                }
                role = Role.Button
                if (done) disabled()
                onClick { onDigit(digit); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val size = when {
                largeNumbers && notesMode -> 24
                largeNumbers -> 32
                notesMode -> 20
                else -> 26
            }.let { if (big) it + 8 else it }
            Text(
                text = digit.toString(),
                fontFamily = Sora,
                fontSize = size.sp,
                lineHeight = (size + 2).sp,
                fontWeight = if (notesMode) FontWeight.Medium else FontWeight.SemiBold,
                color = if (done) c.inkMuted.copy(alpha = 0.5f) else c.ai,
            )
            Spacer(Modifier.height(if (big) 4.dp else 2.dp))
            if (done) {
                Icon(Lucide.Check, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(if (big) 16.dp else 12.dp))
            } else {
                Text(
                    text = left.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (big) 13.sp else 11.sp,
                    lineHeight = if (big) 14.sp else 12.sp,
                    color = c.inkMuted,
                )
            }
        }
    }
}
