package com.kiaijoe.sudoku.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi

/** The four places the bottom bar goes. */
enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Lucide.House),
    Daily("Daily", Lucide.CalendarDays),
    Learn("Learn", Lucide.GraduationCap),
    Progress("Progress", Lucide.ChartNoAxesColumnIncreasing),
}

/**
 * The floating navigation bar: a pill above the page. Only the active tab
 * shows its name, in an indigo pill; the others are icons with a spoken label.
 */
@Composable
fun FloatingNavBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Sumi.colors
    Row(
        modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .card(c, shape = CircleShape, elevation = 14.dp)
            .padding(8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (tab in Tab.entries) {
            val on = tab == current
            Row(
                Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(if (on) c.ai else c.surface)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab) })
                    // The active tab's name is visible text; the others need a spoken label.
                    .then(if (on) Modifier else Modifier.semantics { contentDescription = tab.label })
                    .padding(start = if (on) 16.dp else 13.dp, end = if (on) 20.dp else 13.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(tab.icon, contentDescription = null, tint = if (on) c.onAi else c.inkMuted, modifier = Modifier.size(22.dp))
                if (on) {
                    Spacer(Modifier.width(8.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelLarge, color = c.onAi, maxLines = 1)
                }
            }
        }
    }
}

/**
 * One of the four tab pages: paper, a scrolling column with the screen gutter,
 * and the floating bar over the bottom when [onTab] is given.
 */
@Composable
fun TabPage(tab: Tab, onTab: ((Tab) -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Page {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Spacer(Modifier.height(16.dp))
                content()
                // Room to scroll the last card clear of the floating bar.
                Spacer(Modifier.height(if (onTab != null) 104.dp else 24.dp))
            }
        }
        if (onTab != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                FloatingNavBar(tab, onTab)
            }
        }
    }
}
