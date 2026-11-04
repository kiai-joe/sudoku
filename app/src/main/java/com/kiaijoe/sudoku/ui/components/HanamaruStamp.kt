package com.kiaijoe.sudoku.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A hanamaru, the flower circle Japanese teachers draw on top marks: a
 * spiral inside a ring of looping petals. It is drawn in like a brush
 * stroke, spiral first, then the finished mark gives a small pulse.
 *
 * @param animate false shows the finished stamp at once, for example when a
 *   won game is reopened.
 */
@Composable
fun HanamaruStamp(color: Color, modifier: Modifier = Modifier, animate: Boolean = true) {
    if (animate && rememberReduceMotion()) {
        HanamaruStamp(color, modifier, animate = false)
        return
    }
    val draw = remember { Animatable(if (animate) 0f else 1f) }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        draw.animateTo(1f, tween(durationMillis = 1100, easing = LinearEasing))
        pulse.animateTo(1.08f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier
            .semantics { contentDescription = "Solved: hanamaru stamp" }
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
    ) {
        val spiral = spiralPath(size)
        val petals = petalPath(size)
        val stroke = Stroke(
            width = min(size.width, size.height) * 0.035f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        // The spiral takes the first 35% of the drawing time, the petals the rest.
        val spiralShare = 0.35f
        val p = draw.value
        drawPath(partial(spiral, (p / spiralShare).coerceIn(0f, 1f)), color, style = stroke)
        if (p > spiralShare) {
            drawPath(partial(petals, ((p - spiralShare) / (1f - spiralShare)).coerceIn(0f, 1f)), color, style = stroke)
        }
    }
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
