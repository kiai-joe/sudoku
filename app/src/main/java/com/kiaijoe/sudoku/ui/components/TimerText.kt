package com.kiaijoe.sudoku.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.kiaijoe.sudoku.ui.theme.TimerStyle

/** A time such as "04:12" in tabular figures, so it doesn't jitter as the digits change. */
@Composable
fun TimerText(text: String, color: Color, style: TextStyle = TimerStyle) {
    Text(text, style = style, color = color, modifier = Modifier.clearAndSetSemantics { contentDescription = text })
}
