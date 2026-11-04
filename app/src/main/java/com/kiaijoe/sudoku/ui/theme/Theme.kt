package com.kiaijoe.sudoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Ink on paper: a deep blue for the player's marks, warm off-white paper,
// and an amber accent for the selected cell.
private val Ink = Color(0xFF2B4C7E)
private val InkLight = Color(0xFF9DBBEA)
private val Paper = Color(0xFFF6F3EE)
private val Night = Color(0xFF12151C)
private val Amber = Color(0xFFF2B279)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F5),
    onPrimaryContainer = Color(0xFF0E2547),
    secondary = Color(0xFFB4652F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE3CF),
    onSecondaryContainer = Color(0xFF3D1E06),
    background = Paper,
    onBackground = Color(0xFF1C2230),
    surface = Color(0xFFFFFDFA),
    onSurface = Color(0xFF1C2230),
    surfaceVariant = Color(0xFFEAE5DD),
    onSurfaceVariant = Color(0xFF545B6B),
    surfaceContainer = Color(0xFFEFEBE4),
    surfaceContainerHigh = Color(0xFFE9E4DC),
    outline = Color(0xFF8A8F9C),
    outlineVariant = Color(0xFFD3CEC6),
    error = Color(0xFFC0392B),
)

private val DarkColors = darkColorScheme(
    primary = InkLight,
    onPrimary = Color(0xFF0E2547),
    primaryContainer = Color(0xFF243A5E),
    onPrimaryContainer = Color(0xFFDCE6F5),
    secondary = Color(0xFFF2B279),
    onSecondary = Color(0xFF3D1E06),
    secondaryContainer = Color(0xFF5A3415),
    onSecondaryContainer = Color(0xFFFBE3CF),
    background = Night,
    onBackground = Color(0xFFE6E8EE),
    surface = Color(0xFF1A1E28),
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF2A303D),
    onSurfaceVariant = Color(0xFFB5BBC8),
    surfaceContainer = Color(0xFF1F2430),
    surfaceContainerHigh = Color(0xFF262C39),
    outline = Color(0xFF6E7585),
    outlineVariant = Color(0xFF3A4150),
    error = Color(0xFFFF8A80),
)

/** Board colours that Material's scheme has no slot for. */
@Immutable
data class BoardColors(
    val selected: Color,
    val sameNumber: Color,
    val peer: Color,
    val wrongBackground: Color,
    val given: Color,
    val entered: Color,
    val wrong: Color,
    val thinLine: Color,
    val thickLine: Color,
)

private val LightBoard = BoardColors(
    selected = Amber.copy(alpha = 0.55f),
    sameNumber = Ink.copy(alpha = 0.16f),
    peer = Ink.copy(alpha = 0.06f),
    wrongBackground = Color(0xFFC0392B).copy(alpha = 0.10f),
    given = Color(0xFF1C2230),
    entered = Ink,
    wrong = Color(0xFFC0392B),
    thinLine = Color(0xFF1C2230).copy(alpha = 0.16f),
    thickLine = Color(0xFF1C2230).copy(alpha = 0.78f),
)

private val DarkBoard = BoardColors(
    selected = Amber.copy(alpha = 0.38f),
    sameNumber = InkLight.copy(alpha = 0.20f),
    peer = InkLight.copy(alpha = 0.07f),
    wrongBackground = Color(0xFFFF8A80).copy(alpha = 0.12f),
    given = Color(0xFFE6E8EE),
    entered = InkLight,
    wrong = Color(0xFFFF8A80),
    thinLine = Color.White.copy(alpha = 0.12f),
    thickLine = Color.White.copy(alpha = 0.62f),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Tabular figures keep the timer from jittering as digits change. */
val TimerStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
)

@Composable
fun SudokuTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
