package com.kiaijoe.sudoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiaijoe.sudoku.R

/**
 * The palettes the player can pick in Settings. Each is drawn from the same
 * colour roles (see [SumiColors]); the seal vermilion and the gold selection
 * stay in all of them.
 *
 * @property alwaysDark the palette has no light version and ignores the system setting.
 */
enum class InkTheme(val label: String, val alwaysDark: Boolean = false) {
    /** Washi by day, Sumi night after dark: ink on paper, indigo marks, gold selection. */
    SUMI("Sumi"),

    /** Aizome: paper-white on deep indigo, for playing at night. */
    INDIGO_NIGHT("Indigo night", alwaysDark = true),

    /** Take: green-tinted paper and bamboo-green marks. */
    BAMBOO("Bamboo"),
}

/**
 * Sumi's colour roles, from the brand guideline. Each has one job:
 *
 * - [paper], [surface], [surfaceSunk], [line]: the page, cards, wells and hairlines.
 * - [ink], [inkMuted]: text and printed clues.
 * - [ai] (indigo): the player's own numbers and primary actions; [aiSoft] and [aiFaint] highlight.
 * - [kin] (gold): where you are. A fill only, never text.
 * - [shu] (vermilion): the seal, streaks, achievements and mistakes.
 * - [matcha] (green): completion.
 */
@Immutable
data class SumiColors(
    val dark: Boolean,
    val paper: Color,
    val surface: Color,
    val surfaceSunk: Color,
    val line: Color,
    val ink: Color,
    val inkMuted: Color,
    val ai: Color,
    val onAi: Color,
    val aiSoft: Color,
    val aiFaint: Color,
    val kin: Color,
    val kinSoft: Color,
    val shu: Color,
    val shuSoft: Color,
    val matcha: Color,
    val matchaSoft: Color,
    /** Text and icons on a [shu] fill. */
    val onShu: Color,
    /** Colour of the drop shadow under cards. */
    val shadow: Color,
)

private val Kin = Color(0xFFF0B232)

/** Vermilion of the hanko seal, used for the logo. Same in every theme. */
val SealRed = Color(0xFFC63D22)
val SealPaper = Color(0xFFFFFFFF)

private val Washi = SumiColors(
    dark = false,
    paper = Color(0xFFF4F2ED), surface = Color(0xFFFFFFFF), surfaceSunk = Color(0xFFECE9E2), line = Color(0xFFE2DED5),
    ink = Color(0xFF1D1F26), inkMuted = Color(0xFF5E6270),
    ai = Color(0xFF3552C7), onAi = Color.White, aiSoft = Color(0xFFE3E8FB), aiFaint = Color(0xFFF1F3FD),
    kin = Kin, kinSoft = Color(0xFFFBEFCF),
    shu = Color(0xFFC63D22), shuSoft = Color(0xFFFBE3DC),
    matcha = Color(0xFF2F7A4A), matchaSoft = Color(0xFFDDF0E3),
    onShu = Color.White, shadow = Color(0xFF1D1F26),
)

private val SumiNight = SumiColors(
    dark = true,
    paper = Color(0xFF121317), surface = Color(0xFF1B1D23), surfaceSunk = Color(0xFF24272F), line = Color(0xFF2F333C),
    ink = Color(0xFFECEDF2), inkMuted = Color(0xFFA3A8B6),
    ai = Color(0xFF8EA6FF), onAi = Color(0xFF0D1640), aiSoft = Color(0xFF232D52), aiFaint = Color(0xFF1C2238),
    kin = Kin, kinSoft = Color(0xFF4A3A12),
    shu = Color(0xFFFF7A5C), shuSoft = Color(0xFF4A2219),
    matcha = Color(0xFF7FCF97), matchaSoft = Color(0xFF1E3A27),
    onShu = Color(0xFF2A0B04), shadow = Color.Black,
)

private val IndigoNight = SumiNight.copy(
    paper = Color(0xFF0E1426), surface = Color(0xFF151C33), surfaceSunk = Color(0xFF1F2842), line = Color(0xFF2B3554),
    ink = Color(0xFFE6E9F2), inkMuted = Color(0xFFA9B0C6),
    ai = Color(0xFF8FB4FF), onAi = Color(0xFF0A1A3D), aiSoft = Color(0xFF26386B), aiFaint = Color(0xFF1A2443),
)

private val BambooDay = Washi.copy(
    paper = Color(0xFFEEF2E8), surface = Color(0xFFFFFFFF), surfaceSunk = Color(0xFFE2E8DA), line = Color(0xFFD8E0CE),
    ink = Color(0xFF1B231A), inkMuted = Color(0xFF56604F),
    ai = Color(0xFF2F6B3A), aiSoft = Color(0xFFD9EBD4), aiFaint = Color(0xFFEFF6EC),
)

private val BambooNight = SumiNight.copy(
    paper = Color(0xFF10150F), surface = Color(0xFF181F17), surfaceSunk = Color(0xFF222B20), line = Color(0xFF2E382B),
    ink = Color(0xFFE3E8DD), inkMuted = Color(0xFFAEB8A7),
    ai = Color(0xFF8FD19B), onAi = Color(0xFF0C2412), aiSoft = Color(0xFF24452B), aiFaint = Color(0xFF1A2A1C),
)

/** The colour roles of [theme], light or [dark]. Internal so tests can check every palette. */
internal fun colorsFor(theme: InkTheme, dark: Boolean): SumiColors = when (theme) {
    InkTheme.SUMI -> if (dark) SumiNight else Washi
    InkTheme.INDIGO_NIGHT -> IndigoNight
    InkTheme.BAMBOO -> if (dark) BambooNight else BambooDay
}

private fun SumiColors.colorScheme() = if (dark) {
    darkColorScheme(
        primary = ai, onPrimary = onAi,
        primaryContainer = aiSoft, onPrimaryContainer = ink,
        secondary = kin, onSecondary = Color(0xFF1D1F26),
        secondaryContainer = kinSoft, onSecondaryContainer = ink,
        tertiary = shu, onTertiary = onShu,
        background = paper, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surfaceSunk, onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = surface, surfaceContainerHighest = surfaceSunk,
        outline = inkMuted, outlineVariant = line, error = shu, onError = onShu,
    )
} else {
    lightColorScheme(
        primary = ai, onPrimary = onAi,
        primaryContainer = aiSoft, onPrimaryContainer = ink,
        secondary = Color(0xFF8A6410), onSecondary = Color.White,
        secondaryContainer = kinSoft, onSecondaryContainer = ink,
        tertiary = shu, onTertiary = onShu,
        background = paper, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surfaceSunk, onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = surface, surfaceContainerHighest = surfaceSunk,
        outline = inkMuted, outlineVariant = line, error = shu, onError = onShu,
    )
}

/** Board colours, all taken from the palette's roles. */
@Immutable
data class BoardColors(
    val well: Color,
    val cell: Color,
    val line: Color,
    val selected: Color,
    val sameNumber: Color,
    val peer: Color,
    val wrongBackground: Color,
    val given: Color,
    val entered: Color,
    val wrong: Color,
    val notes: Color,
    /** Rows, columns and boxes a hint reasons about. */
    val hintArea: Color,
    /** The cells that make up a hint's pattern. */
    val hintFocus: Color,
    /** The wash that sweeps across a finished row, column or box. */
    val wash: Color,
)

private fun SumiColors.boardColors() = BoardColors(
    well = surfaceSunk,
    cell = surface,
    line = line,
    selected = kin,
    sameNumber = aiSoft,
    peer = aiFaint,
    wrongBackground = shuSoft,
    given = ink,
    entered = ai,
    wrong = shu,
    notes = inkMuted,
    hintArea = aiFaint,
    hintFocus = if (dark) kin.copy(alpha = 0.42f) else kinSoft,
    wash = matcha,
)

val LocalSumiColors = staticCompositionLocalOf { Washi }
val LocalBoardColors = staticCompositionLocalOf { Washi.boardColors() }

/** The current palette's colour roles. */
object Sumi {
    val colors: SumiColors
        @Composable @ReadOnlyComposable get() = LocalSumiColors.current
}

/**
 * Light or dark, chosen in Settings. [SYSTEM] follows the phone, which is how
 * Sumi has always behaved; the other two hold it there. A palette with no
 * light version (Indigo night) stays dark whatever this says.
 */
enum class DarkMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Whether the app draws dark in this mode, given the phone's own setting [systemDark]. */
fun DarkMode.resolve(systemDark: Boolean): Boolean = when (this) {
    DarkMode.SYSTEM -> systemDark
    DarkMode.LIGHT -> false
    DarkMode.DARK -> true
}

/** Whether [theme] draws dark, given the system setting [systemDark]. */
fun InkTheme.isDark(systemDark: Boolean) = alwaysDark || systemDark

/** The paper, accent and ink of [theme], for drawing a small preview swatch. */
fun InkTheme.swatch(dark: Boolean): Triple<Color, Color, Color> =
    colorsFor(this, isDark(dark)).let { Triple(it.paper, it.ai, it.ink) }

/** Sora: wide, open figures for titles and every number on the board and pad. */
val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
)

/** Inter: everything you read. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun sora(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold, tracking: Float = 0f) =
    TextStyle(fontFamily = Sora, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

private fun inter(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

/** The type scale from the brand guideline, mapped onto Material's slots. */
private val AppTypography = Typography(
    displayLarge = sora(44, 48, tracking = -0.5f),
    displayMedium = sora(38, 44, tracking = -0.5f),
    displaySmall = sora(30, 36, tracking = -0.3f),
    headlineLarge = sora(30, 36, tracking = -0.3f),
    headlineMedium = sora(26, 32),
    headlineSmall = sora(22, 28),
    titleLarge = sora(20, 26),
    titleMedium = sora(17, 22),
    titleSmall = sora(15, 20),
    bodyLarge = inter(16, 24),
    bodyMedium = inter(14, 20),
    bodySmall = inter(12, 16, FontWeight.Medium),
    labelLarge = inter(15, 20, FontWeight.Bold),
    labelMedium = inter(13, 18, FontWeight.SemiBold),
    labelSmall = inter(11, 16, FontWeight.Bold, tracking = 1.1f),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Tabular figures, so the timer doesn't jitter as its digits change. */
val TimerStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    fontFeatureSettings = "tnum",
)

@Composable
fun SudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: InkTheme = InkTheme.SUMI,
    content: @Composable () -> Unit,
) {
    val colors = colorsFor(theme, theme.isDark(darkTheme))
    CompositionLocalProvider(
        LocalSumiColors provides colors,
        LocalBoardColors provides colors.boardColors(),
    ) {
        MaterialTheme(
            colorScheme = colors.colorScheme(),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
