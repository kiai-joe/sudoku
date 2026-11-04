package com.kiaijoe.sudoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kiaijoe.sudoku.R

// Sumi: printed clues in sumi ink on washi paper, the player's marks in
// indigo (ai), a gold-leaf (kin) selection, and vermilion (shu) for
// corrections, the colour calligraphy teachers mark work in.
private val Washi = Color(0xFFECEAE3)
private val WashiLight = Color(0xFFF8F7F2)
private val Sumi = Color(0xFF1A1A1C)
private val Ai = Color(0xFF26408B)
private val AiLight = Color(0xFF9FB3EB)
private val Kin = Color(0xFFE3B23C)
private val Shu = Color(0xFFD2462E)
private val ShuLight = Color(0xFFFF7A5C)
private val Night = Color(0xFF141416)
private val NightPaper = Color(0xFFE8E6DF)

/** Vermilion of the hanko seal, used for the logo. Same in both themes. */
val SealRed = Shu
val SealPaper = WashiLight

private val LightColors = lightColorScheme(
    primary = Ai,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3F2),
    onPrimaryContainer = Color(0xFF0C1C45),
    secondary = Color(0xFF8A6410),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E6BC),
    onSecondaryContainer = Color(0xFF3A2A00),
    background = Washi,
    onBackground = Sumi,
    surface = WashiLight,
    onSurface = Sumi,
    surfaceVariant = Color(0xFFE2E0D8),
    onSurfaceVariant = Color(0xFF5C5C62),
    surfaceContainer = Color(0xFFE6E4DC),
    surfaceContainerHigh = Color(0xFFDFDDD5),
    outline = Color(0xFF8C8B90),
    outlineVariant = Color(0xFFD2D0C8),
    error = Color(0xFFC23B24),
)

private val DarkColors = darkColorScheme(
    primary = AiLight,
    onPrimary = Color(0xFF0C1C45),
    primaryContainer = Color(0xFF223563),
    onPrimaryContainer = Color(0xFFDDE3F2),
    secondary = Kin,
    onSecondary = Color(0xFF3A2A00),
    secondaryContainer = Color(0xFF4F3B0C),
    onSecondaryContainer = Color(0xFFF6E6BC),
    background = Night,
    onBackground = NightPaper,
    surface = Color(0xFF1C1C20),
    onSurface = NightPaper,
    surfaceVariant = Color(0xFF2A2A30),
    onSurfaceVariant = Color(0xFFB0AEB6),
    surfaceContainer = Color(0xFF212126),
    surfaceContainerHigh = Color(0xFF29292F),
    outline = Color(0xFF706F78),
    outlineVariant = Color(0xFF38383F),
    error = ShuLight,
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
    /** Rows, columns and boxes a hint reasons about. */
    val hintArea: Color,
    /** The cells that make up a hint's pattern. */
    val hintFocus: Color,
)

private val LightBoard = BoardColors(
    selected = Kin.copy(alpha = 0.50f),
    sameNumber = Ai.copy(alpha = 0.16f),
    peer = Ai.copy(alpha = 0.06f),
    wrongBackground = Shu.copy(alpha = 0.10f),
    given = Sumi,
    entered = Ai,
    wrong = Shu,
    thinLine = Sumi.copy(alpha = 0.14f),
    thickLine = Sumi.copy(alpha = 0.85f),
    hintArea = Ai.copy(alpha = 0.08f),
    hintFocus = Kin.copy(alpha = 0.55f),
)

private val DarkBoard = BoardColors(
    selected = Kin.copy(alpha = 0.34f),
    sameNumber = AiLight.copy(alpha = 0.20f),
    peer = AiLight.copy(alpha = 0.07f),
    wrongBackground = ShuLight.copy(alpha = 0.12f),
    given = NightPaper,
    entered = AiLight,
    wrong = ShuLight,
    thinLine = Color.White.copy(alpha = 0.11f),
    thickLine = NightPaper.copy(alpha = 0.70f),
    hintArea = AiLight.copy(alpha = 0.10f),
    hintFocus = Kin.copy(alpha = 0.40f),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

/** Lexend, a typeface designed for easy reading, used for everything. */
val Lexend = FontFamily(
    Font(R.font.lexend_light, FontWeight.Light),
    Font(R.font.lexend_regular, FontWeight.Normal),
    Font(R.font.lexend_medium, FontWeight.Medium),
    Font(R.font.lexend_semibold, FontWeight.SemiBold),
    Font(R.font.lexend_bold, FontWeight.Bold),
)

private val AppTypography = Typography().run {
    fun TextStyle.lexend(weight: FontWeight? = fontWeight) = copy(fontFamily = Lexend, fontWeight = weight)
    copy(
        displayLarge = displayLarge.lexend(),
        displayMedium = displayMedium.lexend(),
        displaySmall = displaySmall.lexend(FontWeight.Light).copy(letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.lexend(),
        headlineMedium = headlineMedium.lexend(),
        headlineSmall = headlineSmall.lexend(FontWeight.Medium),
        titleLarge = titleLarge.lexend(FontWeight.Medium),
        titleMedium = titleMedium.lexend(),
        titleSmall = titleSmall.lexend(),
        bodyLarge = bodyLarge.lexend(),
        bodyMedium = bodyMedium.lexend(),
        bodySmall = bodySmall.lexend(),
        labelLarge = labelLarge.lexend(FontWeight.Medium),
        labelMedium = labelMedium.lexend(),
        labelSmall = labelSmall.lexend(),
    )
}

/**
 * Lexend has proportional digits only, so the timer draws each character in a
 * fixed-width slot (see TimerText) to keep it from jittering as digits change.
 */
val TimerStyle = TextStyle(
    fontFamily = Lexend,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
)

@Composable
fun SudokuTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
