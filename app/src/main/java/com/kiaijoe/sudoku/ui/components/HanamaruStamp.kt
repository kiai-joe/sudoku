package com.kiaijoe.sudoku.ui.components

import com.kiaijoe.sudoku.engine.tr
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A hanamaru, the flower circle Japanese teachers draw on top marks: a
 * spiral inside a ring of looping petals.
 *
 * It arrives like a brush on washi: red ink first bleeds into the paper, the
 * brush draws the spiral and then the petals, and the finished mark presses
 * down and settles like a stamp while a few small petals scatter from it.
 * With the system's Remove animations setting on, it appears finished.
 *
 * @param animate false shows the finished stamp at once, for example when a
 *   won game is reopened.
 */
@Composable
fun HanamaruStamp(color: Color, modifier: Modifier = Modifier, animate: Boolean = true) {
    val still = !animate || rememberReduceMotion()
    val time = remember { Animatable(if (still) Hanamaru.TOTAL_MS else 0f) }
    LaunchedEffect(still) {
        if (still) time.snapTo(Hanamaru.TOTAL_MS) else time.animateTo(Hanamaru.TOTAL_MS, tween(Hanamaru.TOTAL_MS.toInt(), easing = LinearEasing))
    }
    HanamaruFrame(color, time.value, modifier)
}

/** The stamp as it looks [ms] milliseconds into its animation; [Hanamaru.TOTAL_MS] or more is the finished mark. */
@Composable
internal fun HanamaruFrame(color: Color, ms: Float, modifier: Modifier = Modifier) {
    val f = Hanamaru.at(ms)
    Canvas(
        modifier
            .semantics { contentDescription = tr("Solved: hanamaru stamp", "Limetatuliwa: muhuri wa hanamaru") }
            .graphicsLayer {
                scaleX = f.scale
                scaleY = f.scale
            },
    ) {
        val s = min(size.width, size.height) / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)

        // The ink bleeding into the paper under the mark.
        if (f.bleed > 0f) {
            val radius = s * (0.35f + 0.65f * f.bleedSize)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = 0.20f * f.bleed),
                    0.7f to color.copy(alpha = 0.09f * f.bleed),
                    1f to Color.Transparent,
                    center = centre,
                    radius = radius,
                ),
                radius = radius,
                center = centre,
            )
        }

        val stroke = Stroke(width = s * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(partial(spiralPath(size), f.spiral), color, style = stroke)
        if (f.petals > 0f) drawPath(partial(petalPath(size), f.petals), color, style = stroke)

        // Small petals scattering outward once the mark is down, fading as they go.
        if (f.burst > 0f && f.burst < 1f) {
            val alpha = 1f - f.burst
            for (k in 0 until Hanamaru.SCATTER) {
                val angle = 2f * PI.toFloat() * (k + 0.5f) / Hanamaru.SCATTER - PI.toFloat() / 2f
                val r = s * (0.9f + 0.32f * f.burst)
                val at = Offset(centre.x + r * cos(angle), centre.y + r * sin(angle))
                val w = s * 0.11f * (1f - 0.4f * f.burst)
                rotate(degrees = angle * 180f / PI.toFloat() + 90f + 60f * f.burst, pivot = at) {
                    drawOval(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(at.x - w / 2f, at.y - w * 0.3f),
                        size = Size(w, w * 0.6f),
                    )
                }
            }
        }
    }
}

/** The hanamaru's timeline: what each part of the stamp looks like at a moment in its animation. */
internal object Hanamaru {
    const val TOTAL_MS = 2000f
    const val SCATTER = 7

    // When each part runs, in milliseconds.
    private const val BLEED_END = 700f
    private const val DRAW_START = 150f
    private const val DRAW_END = 1250f
    private const val PRESS_END = 1600f
    private const val BURST_END = 2000f

    // The spiral takes the first 35% of the drawing, the petals the rest.
    private const val SPIRAL_SHARE = 0.35f

    /**
     * One frame. [bleed] is the ink wash's strength and [bleedSize] its reach;
     * [spiral] and [petals] how much of each stroke is drawn; [scale] the
     * press-and-settle; [burst] how far the scattered petals have flown.
     */
    data class Frame(
        val bleed: Float,
        val bleedSize: Float,
        val spiral: Float,
        val petals: Float,
        val scale: Float,
        val burst: Float,
    )

    fun at(ms: Float): Frame {
        val t = ms.coerceIn(0f, TOTAL_MS)
        val bleedT = (t / BLEED_END).coerceIn(0f, 1f)
        // The wash darkens quickly, then settles back to a faint stain.
        val bleed = if (bleedT < 0.3f) bleedT / 0.3f else 1f - 0.45f * ((bleedT - 0.3f) / 0.7f)
        val draw = easeInOut(((t - DRAW_START) / (DRAW_END - DRAW_START)).coerceIn(0f, 1f))
        val pressT = ((t - DRAW_END) / (PRESS_END - DRAW_END)).coerceIn(0f, 1f)
        return Frame(
            bleed = bleed,
            bleedSize = easeOut(bleedT),
            spiral = (draw / SPIRAL_SHARE).coerceIn(0f, 1f),
            petals = ((draw - SPIRAL_SHARE) / (1f - SPIRAL_SHARE)).coerceIn(0f, 1f),
            scale = press(pressT),
            burst = if (t <= DRAW_END) 0f else easeOut(((t - DRAW_END) / (BURST_END - DRAW_END)).coerceIn(0f, 1f)),
        )
    }

    /** Pushed down, springs past its size, then settles: 1, 0.93, 1.06, 1. */
    private fun press(x: Float): Float = when {
        x <= 0f || x >= 1f -> 1f
        x < 0.25f -> 1f - 0.07f * easeOut(x / 0.25f)
        x < 0.6f -> 0.93f + 0.13f * easeInOut((x - 0.25f) / 0.35f)
        else -> 1.06f - 0.06f * easeInOut((x - 0.6f) / 0.4f)
    }

    private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

    private fun easeInOut(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f
}

private fun partial(path: Path, fraction: Float): Path {
    if (fraction >= 1f) return path
    val out = Path()
    if (fraction <= 0f) return out
    val measure = PathMeasure()
    measure.setPath(path, false)
    measure.getSegment(0f, measure.length * fraction, out, true)
    return out
}

/** Archimedean spiral from the centre out, 2.5 turns. */
private fun spiralPath(size: Size): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = min(size.width, size.height) / 2f
    val turns = 2.5f
    val maxT = turns * 2f * PI.toFloat()
    val steps = 240
    return Path().apply {
        for (i in 0..steps) {
            val t = maxT * i / steps
            val r = 0.42f * s * t / maxT
            val a = t - PI.toFloat() / 2f
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
}

/**
 * Six scalloped petals with a small loop between each pair: an epitrochoid,
 * a point on a small circle rolling around a larger one.
 */
private fun petalPath(size: Size): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = min(size.width, size.height) / 2f
    val petals = 6
    val ring = 0.78f
    val loop = 0.18f
    val steps = 720
    return Path().apply {
        for (i in 0..steps) {
            val t = 2f * PI.toFloat() * i / steps
            val a = t - PI.toFloat() / 2f
            val b = (petals + 1) * t + PI.toFloat() / 2f
            val x = cx + s * (ring * cos(a) + loop * cos(b))
            val y = cy + s * (ring * sin(a) + loop * sin(b))
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}
