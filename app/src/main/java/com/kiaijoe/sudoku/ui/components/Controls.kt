package com.kiaijoe.sudoku.ui.components

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
        ToolButton(Lucide.Undo2, "Undo", enabled = canUndo, onClick = onUndo)
        ToolButton(Lucide.Eraser, "Erase", onClick = onErase)
        ToolButton(
            icon = Lucide.Pencil,
            label = "Notes",
            active = notesMode,
            state = if (notesMode) "on" else "off",
            onClick = onToggleNotes,
        )
        ToolButton(Lucide.Grid3x3, "Fill", onClick = onFill, description = "Fill in all candidates")
        ToolButton(Lucide.Lightbulb, "Hint", onClick = onHint)
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
        !enabled -> c.inkMuted.copy(alpha = 0.45f)
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

@Composable
fun NumberPad(
    remaining: (Int) -> Int,
    notesMode: Boolean,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    largeNumbers: Boolean = false,
) {
    val c = Sumi.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (digit in 1..9) {
            val left = remaining(digit)
            val done = left <= 0
            Box(
                Modifier
                    .weight(1f)
                    // Taller in large mode so the bigger digit still leaves room for the count under it.
                    .aspectRatio(if (largeNumbers) 0.6f else 0.72f)
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
                        contentDescription = if (done) "$digit, all placed" else "$digit, $left left"
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
                    Text(
                        text = digit.toString(),
                        fontFamily = Sora,
                        fontSize = when {
                            largeNumbers && notesMode -> 24.sp
                            largeNumbers -> 32.sp
                            notesMode -> 20.sp
                            else -> 26.sp
                        },
                        lineHeight = if (largeNumbers) 34.sp else 28.sp,
                        fontWeight = if (notesMode) FontWeight.Medium else FontWeight.SemiBold,
                        color = if (done) c.inkMuted.copy(alpha = 0.5f) else c.ai,
                    )
                    Spacer(Modifier.height(2.dp))
                    if (done) {
                        Icon(Lucide.Check, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(12.dp))
                    } else {
                        Text(
                            text = left.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            color = c.inkMuted,
                        )
                    }
                }
            }
        }
    }
}
