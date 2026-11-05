package com.kiaijoe.sudoku.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.Sumi
import com.kiaijoe.sudoku.ui.theme.SumiColors
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import com.kiaijoe.sudoku.ui.theme.colorsFor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Keeps every icon present and visible, in light and dark.
 *
 * - Every Lucide icon the app ships must draw something. A path can parse
 *   and still draw nothing, as Play once did.
 * - Every icon colour the screens use must stand out from the background it
 *   sits on, in every palette, light and dark: 3:1, the WCAG minimum for
 *   icons, or 2:1 for a disabled tool, which is meant to look greyed out.
 * - icons_light.png and icons_dark.png show the whole set, for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h1400dp-xxhdpi")
class IconsTest {

    @get:Rule
    val compose = createComposeRule()

    /** Every icon in [Lucide], by name, found through its getters so a new one is covered automatically. */
    private val icons: List<Pair<String, ImageVector>> = Lucide::class.java.declaredMethods
        .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == ImageVector::class.java }
        .map { it.name.removePrefix("get") to it.invoke(Lucide) as ImageVector }
        .sortedBy { it.first }

    private fun paths(group: VectorGroup): List<VectorPath> = group.flatMap { node: VectorNode ->
        when (node) {
            is VectorPath -> listOf(node)
            is VectorGroup -> paths(node)
        }
    }

    /** How many pixels [icon] inks when stroked as the app strokes it, at 48 by 48. */
    private fun inkedPixels(icon: ImageVector): Int {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.scale(48f / icon.viewportWidth, 48f / icon.viewportHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.BLACK
        }
        for (path in paths(icon.root)) {
            canvas.drawPath(PathParser().addPathNodes(path.pathData).toPath().asAndroidPath(), paint)
        }
        var count = 0
        for (x in 0 until 48) for (y in 0 until 48) {
            if (android.graphics.Color.red(bitmap.getPixel(x, y)) < 128) count++
        }
        return count
    }

    @Test
    fun everyIconDrawsSomething() {
        assertTrue("found only ${icons.size} icons", icons.size >= 50)
        val blank = icons.filter { (_, icon) -> inkedPixels(icon) < 15 }.map { it.first }
        assertTrue("Icons that draw nothing: $blank", blank.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    /** Each place an icon is drawn, as its colour on the background behind it. */
    private fun pairings(c: SumiColors): Map<String, Pair<Color, Color>> = mapOf(
        "ink on a card (lists, tools)" to (c.ink to c.surface),
        "ink on a sunk tile (top bar, list icons)" to (c.ink to c.surfaceSunk),
        "muted on a card (inactive tabs, chevrons)" to (c.inkMuted to c.surface),
        "muted on a sunk tile (placed numbers' tick)" to (c.inkMuted to c.surfaceSunk),
        "on-indigo on indigo (buttons, active tab)" to (c.onAi to c.ai),
        "indigo on soft indigo (level and lesson badges)" to (c.ai to c.aiSoft),
        "vermilion on soft vermilion (Killer badge, streak)" to (c.shu to c.shuSoft),
        "on-vermilion on vermilion (seals, solved days)" to (c.onShu to c.shu),
        "matcha on soft matcha (finished lessons)" to (c.matcha to c.matchaSoft),
        "ink on soft gold (hint badge)" to (c.ink to c.kinSoft),
    )

    @Test
    fun everyIconColourStandsOutInEveryPalette() {
        val faint = buildList {
            for (theme in InkTheme.entries) for (dark in listOf(false, true)) {
                val c = colorsFor(theme, dark)
                val mode = "${theme.label} ${if (dark) "dark" else "light"}"
                for ((where, pair) in pairings(c)) {
                    val ratio = contrast(pair.first, pair.second)
                    if (ratio < 3f) add("$mode, $where: ${"%.2f".format(ratio)}")
                }
                // A disabled tool is greyed out on purpose, but must still read as an icon.
                val disabled = c.inkMuted.copy(alpha = 0.6f).compositeOver(c.surfaceSunk)
                val ratio = contrast(disabled, c.surfaceSunk)
                if (ratio < 2f) add("$mode, disabled tool: ${"%.2f".format(ratio)}")
            }
        }
        assertTrue("Icon colours too faint:\n" + faint.joinToString("\n"), faint.isEmpty())
    }

    private fun gallery(dark: Boolean) {
        compose.setContent {
            SudokuTheme(darkTheme = dark) {
                val c = Sumi.colors
                Column(
                    Modifier.background(c.paper).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Each icon as ink on a card, muted on a sunk tile, and white on indigo.
                    for ((tint, bg) in listOf(c.ink to c.surface, c.inkMuted to c.surfaceSunk, c.onAi to c.ai)) {
                        icons.chunked(8).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for ((_, icon) in row) {
                                    Box(Modifier.size(40.dp).background(bg), contentAlignment = Alignment.Center) {
                                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/icons_${if (dark) "dark" else "light"}.png")
    }

    @Test fun galleryLight() = gallery(dark = false)

    @Test fun galleryDark() = gallery(dark = true)
}
