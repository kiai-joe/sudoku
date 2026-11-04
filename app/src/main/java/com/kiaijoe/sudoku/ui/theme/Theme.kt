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

/**
 * The ink palettes the player can pick in Settings. Each is drawn from a few
 * base tones (see [Tones]); the seal vermilion stays the same in all of them.
 *
 * @property alwaysDark the palette has no light version and ignores the system setting.
 */
enum class InkTheme(val label: String, val alwaysDark: Boolean = false) {
    /** Sumi ink on washi paper, indigo marks, gold selection. */
    SUMI("Sumi"),

    /** Aizome: paper-white on deep indigo, for playing at night. */
    INDIGO_NIGHT("Indigo night", alwaysDark = true),

    /** Take: green-tinted paper and bamboo-green marks. */
    BAMBOO("Bamboo"),
}

// Shared tones. Kin (gold leaf) marks the selection in every palette, and shu
// (vermilion) marks corrections, the colour calligraphy teachers mark work in.
private val Kin = Color(0xFFE3B23C)
private val Shu = Color(0xFFD2462E)
private val ShuLight = Color(0xFFFF7A5C)
private val WashiLight = Color(0xFFF8F7F2)

/** Vermilion of the hanko seal, used for the logo and stamps. Same in every theme. */
val SealRed = Shu
val SealPaper = WashiLight

/**
 * The few base colours a palette is made from. [ink] is printed clues and
 * text, [accent] the player's own numbers and the primary colour.
 */
private data class Tones(
    val dark: Boolean,
    val paper: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineVariant: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
)

private val SumiLight = Tones(
    dark = false,
    paper = Color(0xFFECEAE3), surface = WashiLight, surfaceVariant = Color(0xFFE2E0D8),
    surfaceContainer = Color(0xFFE6E4DC), surfaceContainerHigh = Color(0xFFDFDDD5),
    outline = Color(0xFF8C8B90), outlineVariant = Color(0xFFD2D0C8),
    ink = Color(0xFF1A1A1C), muted = Color(0xFF5C5C62),
    accent = Color(0xFF26408B), onAccent = Color.White,
    accentContainer = Color(0xFFDDE3F2), onAccentContainer = Color(0xFF0C1C45),
)

private val SumiDark = Tones(
    dark = true,
    paper = Color(0xFF141416), surface = Color(0xFF1C1C20), surfaceVariant = Color(0xFF2A2A30),
    surfaceContainer = Color(0xFF212126), surfaceContainerHigh = Color(0xFF29292F),
    outline = Color(0xFF706F78), outlineVariant = Color(0xFF38383F),
    ink = Color(0xFFE8E6DF), muted = Color(0xFFB0AEB6),
    accent = Color(0xFF9FB3EB), onAccent = Color(0xFF0C1C45),
    accentContainer = Color(0xFF223563), onAccentContainer = Color(0xFFDDE3F2),
)

private val IndigoNight = Tones(
    dark = true,
    paper = Color(0xFF0E1426), surface = Color(0xFF151C33), surfaceVariant = Color(0xFF222B47),
    surfaceContainer = Color(0xFF18203A), surfaceContainerHigh = Color(0xFF1F2842),
    outline = Color(0xFF6A7391), outlineVariant = Color(0xFF2E3756),
    ink = Color(0xFFE6E9F2), muted = Color(0xFFA9B0C6),
    accent = Color(0xFF8FB4FF), onAccent = Color(0xFF0A1A3D),
    accentContainer = Color(0xFF26386B), onAccentContainer = Color(0xFFDCE6FF),
)

private val BambooLight = Tones(
    dark = false,
    paper = Color(0xFFE7ECE1), surface = Color(0xFFF6F8F2), surfaceVariant = Color(0xFFDCE3D3),
    surfaceContainer = Color(0xFFE0E6D8), surfaceContainerHigh = Color(0xFFD7DFCE),
    outline = Color(0xFF858F80), outlineVariant = Color(0xFFC9D2BF),
    ink = Color(0xFF1B231A), muted = Color(0xFF56604F),
    accent = Color(0xFF2F6B3A), onAccent = Color.White,
    accentContainer = Color(0xFFD5E8D0), onAccentContainer = Color(0xFF0C2412),
)

private val BambooDark = Tones(
    dark = true,
    paper = Color(0xFF10150F), surface = Color(0xFF171E16), surfaceVariant = Color(0xFF252E23),
    surfaceContainer = Color(0xFF1B2319), surfaceContainerHigh = Color(0xFF222B20),
    outline = Color(0xFF6C7767), outlineVariant = Color(0xFF333D30),
    ink = Color(0xFFE3E8DD), muted = Color(0xFFAEB8A7),
    accent = Color(0xFF8FD19B), onAccent = Color(0xFF0C2412),
    accentContainer = Color(0xFF24452B), onAccentContainer = Color(0xFFD5E8D0),
)

private fun tonesFor(theme: InkTheme, dark: Boolean): Tones = when (theme) {
    InkTheme.SUMI -> if (dark) SumiDark else SumiLight
    InkTheme.INDIGO_NIGHT -> IndigoNight
    InkTheme.BAMBOO -> if (dark) BambooDark else BambooLight
}

private fun Tones.colorScheme() = if (dark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = accentContainer, onPrimaryContainer = onAccentContainer,
        secondary = Kin, onSecondary = Color(0xFF3A2A00),
        secondaryContainer = Color(0xFF4F3B0C), onSecondaryContainer = Color(0xFFF6E6BC),
        background = paper, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surfaceVariant, onSurfaceVariant = muted,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        outline = outline, outlineVariant = outlineVariant, error = ShuLight,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = accentContainer, onPrimaryContainer = onAccentContainer,
        secondary = Color(0xFF8A6410), onSecondary = Color.White,
        secondaryContainer = Color(0xFFF6E6BC), onSecondaryContainer = Color(0xFF3A2A00),
        background = paper, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surfaceVariant, onSurfaceVariant = muted,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        outline = outline, outlineVariant = outlineVariant, error = Color(0xFFC23B24),
    )
}

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

private fun Tones.boardColors(): BoardColors {
    val wrong = if (dark) ShuLight else Shu
    return BoardColors(
        selected = Kin.copy(alpha = if (dark) 0.34f else 0.50f),
        sameNumber = accent.copy(alpha = if (dark) 0.20f else 0.16f),
        peer = accent.copy(alpha = if (dark) 0.07f else 0.06f),
        wrongBackground = wrong.copy(alpha = if (dark) 0.12f else 0.10f),
        given = ink,
        entered = accent,
        wrong = wrong,
        thinLine = if (dark) Color.White.copy(alpha = 0.11f) else ink.copy(alpha = 0.14f),
        thickLine = ink.copy(alpha = if (dark) 0.70f else 0.85f),
        hintArea = accent.copy(alpha = if (dark) 0.10f else 0.08f),
        hintFocus = Kin.copy(alpha = if (dark) 0.40f else 0.55f),
    )
}

val LocalBoardColors = staticCompositionLocalOf { SumiLight.boardColors() }

/** Whether [theme] draws dark, given the system setting [systemDark]. */
fun InkTheme.isDark(systemDark: Boolean) = alwaysDark || systemDark

/** The paper, accent and ink of [theme], for drawing a small preview swatch. */
fun InkTheme.swatch(dark: Boolean): Triple<Color, Color, Color> =
    tonesFor(this, isDark(dark)).let { Triple(it.paper, it.accent, it.ink) }

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
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: InkTheme = InkTheme.SUMI,
    content: @Composable () -> Unit,
) {
    val tones = tonesFor(theme, theme.isDark(darkTheme))
    CompositionLocalProvider(LocalBoardColors provides tones.boardColors()) {
        MaterialTheme(
            colorScheme = tones.colorScheme(),
            typography = AppTypography,
            content = content,
        )
    }
}
