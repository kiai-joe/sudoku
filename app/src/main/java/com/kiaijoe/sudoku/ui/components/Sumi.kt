package com.kiaijoe.sudoku.ui.components

import com.kiaijoe.sudoku.engine.tr
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import com.kiaijoe.sudoku.ui.theme.SumiColors

// Sumi's building blocks, from the brand guideline: one card elevation, pill
// buttons, rounded everything. Screens are made from these, not from raw
// Material components, so they all share one look.

val CardShape = RoundedCornerShape(24.dp)
val TileShape = RoundedCornerShape(16.dp)

/** The one elevation: a soft lift on a surface-coloured card. */
fun Modifier.card(colors: SumiColors, shape: Shape = CardShape, color: Color = colors.surface, elevation: Dp = 6.dp): Modifier =
    this
        .shadow(
            elevation = if (colors.dark) elevation / 2 else elevation,
            shape = shape,
            ambientColor = colors.shadow.copy(alpha = if (colors.dark) 0.5f else 0.10f),
            spotColor = colors.shadow.copy(alpha = if (colors.dark) 0.6f else 0.14f),
        )
        .background(color, shape)

/** A card: surface, soft shadow, 24dp corners. */
@Composable
fun SumiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Sumi.colors.surface,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Sumi.colors
    Column(
        modifier
            .card(colors, color = color)
            .clip(CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/**
 * How a [PillButton] is filled: [Primary] indigo, [Soft] pale indigo, [Seal]
 * vermilion, [Ghost] text only (for a dialog's Cancel), [Outline] text in a
 * thin indigo border (a quiet action that should still read as a button).
 */
enum class ButtonTone { Primary, Soft, Seal, Ghost, Outline }

/** A pill button: one or two words, an optional icon before them. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val c = Sumi.colors
    val (bg, fg) = when (tone) {
        ButtonTone.Primary -> c.ai to c.onAi
        ButtonTone.Soft -> c.aiSoft to c.ai
        ButtonTone.Seal -> c.shu to c.onShu
        ButtonTone.Ghost, ButtonTone.Outline -> Color.Transparent to c.ai
    }
    Row(
        modifier
            .heightIn(min = height)
            .clip(CircleShape)
            .background(if (enabled) bg else c.surfaceSunk)
            .then(
                if (tone == ButtonTone.Outline) {
                    Modifier.border(1.5.dp, if (enabled) c.ai else c.line, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) fg else c.inkMuted
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/** A square, softly rounded icon button on a sunk well, for top bars. */
@Composable
fun IconTile(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val c = Sumi.colors
    Box(
        modifier
            .size(size)
            .clip(TileShape)
            .background(c.surfaceSunk)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(22.dp))
    }
}

enum class ChipTone { Neutral, Ai, Shu, Matcha }

@Composable
fun SumiChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.Neutral,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Sumi.colors
    val (bg, fg) = when (tone) {
        ChipTone.Neutral -> c.surfaceSunk to c.ink
        ChipTone.Ai -> c.aiSoft to c.ai
        ChipTone.Shu -> c.shuSoft to c.shu
        ChipTone.Matcha -> c.matchaSoft to c.matcha
    }
    Row(
        modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(role = Role.Tab, onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Uppercase section label. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Sumi.colors.inkMuted) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

/** A screen's big title, with an optional line under it. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = Sumi.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Sumi.colors.inkMuted)
            }
        }
        trailing()
    }
}

/** A top bar: back on the left, a centred title, room for actions on the right. */
@Composable
fun SumiTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backIcon: ImageVector = Lucide.ArrowLeft,
    backDescription: String = tr("Back", "Rudi"),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(Modifier.align(Alignment.CenterStart)) {
            if (onBack != null) IconTile(backIcon, backDescription, onBack)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Sumi.colors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 104.dp)
                .semantics { heading() },
        )
        Row(
            Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** A card holding rows split by hairlines. */
@Composable
fun ListCard(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    val c = Sumi.colors
    Column(
        modifier
            .fillMaxWidth()
            .card(c)
            .clip(CardShape),
    ) {
        rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = c.line)
            row()
        }
    }
}

/** One row of a [ListCard]: an icon chip, a title and an optional line under it, then [trailing]. */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconColor: Color = Sumi.colors.ink,
    iconBackground: Color = Sumi.colors.surfaceSunk,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = Sumi.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = c.ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
            }
        }
        trailing()
    }
}

@Composable
fun RowChevron() {
    Spacer(Modifier.width(8.dp))
    Icon(Lucide.ChevronRight, contentDescription = null, tint = Sumi.colors.inkMuted, modifier = Modifier.size(20.dp))
}

/** A switch in Sumi's colours. Pass null for [onCheckedChange] when the whole row toggles. */
@Composable
fun SumiSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val c = Sumi.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onAi,
            checkedTrackColor = c.ai,
            checkedBorderColor = c.ai,
            // A muted thumb and outline, so an off switch shows on dark cards as well as light.
            uncheckedThumbColor = c.inkMuted,
            uncheckedTrackColor = c.surfaceSunk,
            uncheckedBorderColor = c.inkMuted.copy(alpha = 0.6f),
        ),
    )
}

/** A round icon chip in a soft tone, used for list rows and stamps. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.36f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** The Sumi seal: a hanko cut as a 3×3 box with the centre filled. */
@Composable
fun SealLogo(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 120f
        drawRoundRect(color = com.kiaijoe.sudoku.ui.theme.SealRed, cornerRadius = CornerRadius(34 * s))
        val white = com.kiaijoe.sudoku.ui.theme.SealPaper
        drawRoundRect(
            color = white,
            topLeft = Offset(27 * s, 27 * s),
            size = Size(66 * s, 66 * s),
            cornerRadius = CornerRadius(14 * s),
            style = Stroke(width = 6 * s),
        )
        for (p in listOf(49f, 71f)) {
            drawLine(white, Offset(p * s, 27 * s), Offset(p * s, 93 * s), 3.5f * s, StrokeCap.Round)
            drawLine(white, Offset(27 * s, p * s), Offset(93 * s, p * s), 3.5f * s, StrokeCap.Round)
        }
        drawRoundRect(
            color = white,
            topLeft = Offset(51.5f * s, 51.5f * s),
            size = Size(17 * s, 17 * s),
            cornerRadius = CornerRadius(4 * s),
        )
    }
}

/** A page that fills the screen with paper. */
@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Sumi.colors.paper),
        content = content,
    )
}

/** One choice in a [SegmentedControl]. */
data class Segment(val label: String, val icon: ImageVector? = null)

/**
 * A segmented control on one pill-shaped card, the chosen segment filled
 * indigo. Greyed out when not [enabled]. Screen readers hear radio buttons.
 */
@Composable
fun SegmentedControl(
    segments: List<Segment>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Sumi.colors
    Row(
        modifier
            .fillMaxWidth()
            .card(c, CircleShape)
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEachIndexed { i, segment ->
            val on = i == selected
            val fg = when {
                !enabled -> c.inkMuted.copy(alpha = 0.5f)
                on -> c.onAi
                else -> c.inkMuted
            }
            Row(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(if (on) (if (enabled) c.ai else c.surfaceSunk) else c.surface)
                    .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(i) }),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (segment.icon != null) {
                    Icon(segment.icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(segment.label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
            }
        }
    }
}
