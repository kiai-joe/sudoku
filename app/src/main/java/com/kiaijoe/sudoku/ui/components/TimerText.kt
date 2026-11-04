package com.kiaijoe.sudoku.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.kiaijoe.sudoku.ui.theme.TimerStyle

/**
 * Draws a time such as "04:12" with every digit in a slot of the same width.
 * Lexend's digits are proportional, so plain text would shift as they change.
 */
@Composable
fun TimerText(text: String, color: Color, style: TextStyle = TimerStyle) {
    val density = LocalDensity.current
    // Lexend's widest digit is 0.62 em; separators get a narrower slot.
    val digitSlot = with(density) { (style.fontSize * 0.64f).toDp() }
    val separatorSlot = with(density) { (style.fontSize * 0.34f).toDp() }
    Row(Modifier.clearAndSetSemantics { contentDescription = text }) {
        for (ch in text) {
            Box(
                Modifier.width(if (ch.isDigit()) digitSlot else separatorSlot),
                contentAlignment = Alignment.Center,
            ) {
                Text(ch.toString(), style = style, color = color)
            }
        }
    }
}
