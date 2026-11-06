package com.kiaijoe.sudoku.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.tr
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** An achievement's round seal: vermilion with a white inner ring when earned, grey with a lock when not. */
@Composable
fun SealMark(a: Achievement, earned: Boolean, size: Int, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (earned) c.shu else c.surfaceSunk)
            .then(if (earned) Modifier.padding(3.dp).border(2.dp, c.onShu, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (earned) a.icon else Lucide.Lock,
            contentDescription = null,
            tint = if (earned) c.onShu else c.inkMuted,
            modifier = Modifier.size((size * 0.42f).dp),
        )
    }
}

/**
 * A newly earned achievement on the win screen. Its seal drops onto the
 * card and presses down like a stamp, a gold ring and rays spread from it,
 * and then its name and what it was for fade in. [delayMs] lets the
 * hanamaru finish first and staggers several seals. With [animate] false,
 * or the system's Remove animations setting on, it shows at once.
 */
@Composable
fun SealUnlock(a: Achievement, animate: Boolean, delayMs: Int = 0, modifier: Modifier = Modifier) {
    val still = !animate || rememberReduceMotion()
    val time = remember { Animatable(if (still) SealAnim.TOTAL_MS else 0f) }
    LaunchedEffect(still) {
        if (still) {
            time.snapTo(SealAnim.TOTAL_MS)
        } else {
            time.animateTo(SealAnim.TOTAL_MS, tween(SealAnim.TOTAL_MS.toInt(), delayMillis = delayMs, easing = LinearEasing))
        }
    }
    SealUnlockFrame(a, time.value, modifier)
}

/** The card as it looks [ms] milliseconds into its animation. */
@Composable
internal fun SealUnlockFrame(a: Achievement, ms: Float, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    val f = SealAnim.at(ms)
    Row(
        modifier
            .fillMaxWidth()
            // The card arrives with its seal, so nothing sits empty while the hanamaru draws.
            .graphicsLayer { alpha = f.alpha }
            .card(c, color = c.kinSoft)
            .clearAndSetSemantics {
                contentDescription = tr("New achievement: ${a.title}. ${a.description}", "Tuzo mpya: ${a.title}. ${a.description}")
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            // The gold ring and rays spread from under the seal as it lands.
            Canvas(Modifier.size(64.dp)) {
                val centre = Offset(size.width / 2f, size.height / 2f)
                val seal = size.minDimension / 2f * (56f / 64f)
                if (f.ring > 0f && f.ring < 1f) {
                    drawCircle(
                        color = c.kin.copy(alpha = 0.7f * (1f - f.ring)),
                        radius = seal * (1f + 0.55f * f.ring),
                        center = centre,
                        style = Stroke(width = 3.dp.toPx() * (1f - 0.6f * f.ring)),
                    )
                }
                if (f.rays > 0f && f.rays < 1f) {
                    for (k in 0 until SealAnim.RAYS) {
                        val angle = 2f * PI.toFloat() * k / SealAnim.RAYS - PI.toFloat() / 2f
                        val inner = seal * (1.1f + 0.35f * f.rays)
                        val outer = inner + seal * 0.32f * (1f - f.rays)
                        drawLine(
                            color = c.kin.copy(alpha = 1f - f.rays),
                            start = Offset(centre.x + inner * cos(angle), centre.y + inner * sin(angle)),
                            end = Offset(centre.x + outer * cos(angle), centre.y + outer * sin(angle)),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            SealMark(
                a,
                earned = true,
                size = 56,
                modifier = Modifier.graphicsLayer {
                    scaleX = f.scale
                    scaleY = f.scale
                    rotationZ = f.rotation
                },
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha = f.text
                    translationY = (1f - f.text) * 8.dp.toPx()
                },
        ) {
            Text(tr("NEW ACHIEVEMENT", "TUZO MPYA"), style = MaterialTheme.typography.labelSmall, color = c.shu)
            Spacer(Modifier.height(2.dp))
            Text(a.title, style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(a.description, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
    }
}

/** The seal's timeline: how each part looks at a moment in the animation. */
internal object SealAnim {
    const val TOTAL_MS = 1100f
    const val RAYS = 8

    private const val DROP_END = 380f
    private const val PRESS_END = 700f
    private const val RING_END = 1000f
    private const val TEXT_START = 520f
    private const val TEXT_END = 900f

    /**
     * One frame. The seal's [scale], [rotation] (degrees) and [alpha] as it
     * falls and presses; how far the gold [ring] and [rays] have spread (0
     * not yet, 1 gone); and how far the [text] has faded in.
     */
    data class Frame(
        val scale: Float,
        val rotation: Float,
        val alpha: Float,
        val ring: Float,
        val rays: Float,
        val text: Float,
    )

    fun at(ms: Float): Frame {
        val t = ms.coerceIn(0f, TOTAL_MS)
        val drop = (t / DROP_END).coerceIn(0f, 1f)
        val fall = drop * drop // Speeds up as it falls.
        val pressT = ((t - DROP_END) / (PRESS_END - DROP_END)).coerceIn(0f, 1f)
        val spread = ((t - DROP_END) / (RING_END - DROP_END)).coerceIn(0f, 1f)
        return Frame(
            scale = if (t < DROP_END) 2.2f - 1.2f * fall else press(pressT),
            rotation = -24f * (1f - fall),
            alpha = (t / 120f).coerceIn(0f, 1f),
            ring = if (t <= DROP_END) 0f else easeOut(spread),
            rays = if (t <= DROP_END) 0f else easeOut(spread),
            text = easeOut(((t - TEXT_START) / (TEXT_END - TEXT_START)).coerceIn(0f, 1f)),
        )
    }

    /** Lands, squashes, springs past its size, settles: 1, 0.88, 1.06, 1. */
    private fun press(x: Float): Float = when {
        x <= 0f || x >= 1f -> 1f
        x < 0.25f -> 1f - 0.12f * easeOut(x / 0.25f)
        x < 0.6f -> 0.88f + 0.18f * ((x - 0.25f) / 0.35f)
        else -> 1.06f - 0.06f * easeOut((x - 0.6f) / 0.4f)
    }

    private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)
}
