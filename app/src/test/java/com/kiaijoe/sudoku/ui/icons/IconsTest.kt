package com.kiaijoe.sudoku.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.kiaijoe.sudoku.ui.theme.Sumi
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Draws every Lucide icon the app ships, in the colour pairs screens use,
 * in light and dark, and fails if any of them comes out blank. A path that
 * parses but draws nothing (as Play once did) is caught here, not on a phone.
 * Also renders the set as icons_light.png and icons_dark.png.
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

    /** An icon colour on a background colour, as the app pairs them. */
    private class Pairing(val name: String, val tint: @Composable () -> Color, val background: @Composable () -> Color)

    private val pairings = listOf(
        // Lists, tools and top bars.
        Pairing("ink", { Sumi.colors.ink }, { Sumi.colors.surface }),
        // Disabled tools, row chevrons, captions.
        Pairing("muted", { Sumi.colors.inkMuted }, { Sumi.colors.surfaceSunk }),
        // Primary buttons, the active tab and the Continue card.
        Pairing("onAi", { Sumi.colors.onAi }, { Sumi.colors.ai }),
    )

    private fun tag(icon: String, pairing: String) = "$icon/$pairing"

    private fun showAll(dark: Boolean) {
        compose.setContent {
            SudokuTheme(darkTheme = dark) {
                Column(
                    Modifier.background(Sumi.colors.paper).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (p in pairings) {
                        val tint = p.tint()
                        val bg = p.background()
                        icons.chunked(8).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for ((name, icon) in row) {
                                    Box(
                                        Modifier.size(40.dp).background(bg).testTag(tag(name, p.name)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** How many pixels in [tag]'s cell differ clearly from the cell's corner, which is background. */
    private fun inkedPixels(tag: String): Int {
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        val bg = pixels[0, 0]
        var count = 0
        for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
            val c = pixels[x, y]
            if (abs(c.red - bg.red) + abs(c.green - bg.green) + abs(c.blue - bg.blue) > 0.25f) count++
        }
        return count
    }

    private fun everyIconIsVisible(dark: Boolean) {
        assertTrue("found only ${icons.size} icons", icons.size >= 50)
        showAll(dark)
        val blank = buildList {
            for ((name, _) in icons) for (p in pairings) {
                if (inkedPixels(tag(name, p.name)) < 15) add(tag(name, p.name))
            }
        }
        assertTrue("Icons that draw nothing visible (${if (dark) "dark" else "light"}): $blank", blank.isEmpty())
        compose.onRoot().captureRoboImage("src/test/screenshots/icons_${if (dark) "dark" else "light"}.png")
    }

    @Test fun everyIconIsVisibleInLight() = everyIconIsVisible(dark = false)

    @Test fun everyIconIsVisibleInDark() = everyIconIsVisible(dark = true)
}
