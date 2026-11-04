package com.kiaijoe.sudoku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameToolbar(
    canUndo: Boolean,
    notesMode: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ToolButton(Icons.AutoMirrored.Outlined.Undo, "Undo", enabled = canUndo, onClick = onUndo)
        ToolButton(Icons.AutoMirrored.Outlined.Backspace, "Erase", onClick = onErase)
        ToolButton(
            icon = Icons.Outlined.Edit,
            label = "Notes",
            active = notesMode,
            badge = if (notesMode) "ON" else "OFF",
            onClick = onToggleNotes,
        )
        ToolButton(Icons.Outlined.Lightbulb, "Hint", onClick = onHint)
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    badge: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.38f)
        active -> scheme.primary
        else -> scheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (active) scheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.semantics {
            contentDescription = if (badge != null) "$label $badge" else label
        },
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) scheme.primary else scheme.outline,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 14.dp, y = (-6).dp),
                    ) {
                        Text(
                            badge,
                            color = if (active) scheme.onPrimary else scheme.surface,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

@Composable
fun NumberPad(
    remaining: (Int) -> Int,
    notesMode: Boolean,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (digit in 1..9) {
            val left = remaining(digit)
            val done = left <= 0
            Surface(
                onClick = { onDigit(digit) },
                enabled = !done,
                shape = RoundedCornerShape(12.dp),
                color = when {
                    done -> scheme.surfaceVariant.copy(alpha = 0.4f)
                    notesMode -> scheme.secondaryContainer
                    else -> scheme.surfaceContainerHigh
                },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.72f)
                    .semantics {
                        contentDescription = if (done) "$digit, all placed" else "$digit, $left left"
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = digit.toString(),
                        fontSize = if (notesMode) 20.sp else 26.sp,
                        fontWeight = if (notesMode) FontWeight.Normal else FontWeight.Medium,
                        color = when {
                            done -> scheme.onSurfaceVariant.copy(alpha = 0.3f)
                            notesMode -> scheme.onSecondaryContainer
                            else -> scheme.primary
                        },
                    )
                    Text(
                        text = if (done) "✓" else left.toString(),
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariant.copy(alpha = if (done) 0.4f else 0.8f),
                    )
                }
            }
        }
    }
}
