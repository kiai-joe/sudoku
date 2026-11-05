package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.Example
import com.kiaijoe.sudoku.engine.Grid
import com.kiaijoe.sudoku.engine.Tutorial
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.CardShape
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.ListRow
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.Page
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.RowChevron
import com.kiaijoe.sudoku.ui.components.ScreenTitle
import com.kiaijoe.sudoku.ui.components.SudokuBoard
import com.kiaijoe.sudoku.ui.components.SumiCard
import com.kiaijoe.sudoku.ui.components.SumiChip
import com.kiaijoe.sudoku.ui.components.SumiTopBar
import com.kiaijoe.sudoku.ui.components.Tab
import com.kiaijoe.sudoku.ui.components.TabPage
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.components.icon
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi

/** Every technique, grouped by the level that first needs it. */
@Composable
fun LearnScreen(onOpen: (Int) -> Unit, onBack: () -> Unit, onTab: ((Tab) -> Unit)? = null) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    TabPage(Tab.Learn, onTab) {
        ScreenTitle("Learn", "Short lessons, each on a real board.")
        val groups = Difficulty.entries.map { level ->
            Triple(level.label, level.icon, Tutorial.lessons.withIndex().filter {
                !it.value.killer && it.value.technique.level == level
            })
        } + listOf(Triple("Killer Sudoku", Lucide.SquareDashed, Tutorial.lessons.withIndex().filter { it.value.killer }))
        for ((heading, icon, lessons) in groups) {
            if (lessons.isEmpty()) continue
            Spacer(Modifier.height(20.dp))
            Overline(heading)
            Spacer(Modifier.height(10.dp))
            val killer = heading.startsWith("Killer")
            ListCard(
                rows = lessons.map { (index, lesson) ->
                    @Composable {
                        ListRow(
                            icon = if (killer) icon else Lucide.BookOpen,
                            title = lesson.technique.label,
                            subtitle = lesson.summary,
                            iconColor = if (killer) c.shu else c.ai,
                            iconBackground = if (killer) c.shuSoft else c.aiSoft,
                            onClick = { onOpen(index) },
                        ) { RowChevron() }
                    }
                },
            )
        }
    }
}

/**
 * One technique: what it is, then a worked example stepped through card by
 * card on a real board. [example] is null while it loads, and for a lesson
 * with no example.
 */
@Composable
fun LessonScreen(
    index: Int,
    example: Example?,
    loading: Boolean,
    onNextLesson: (() -> Unit)?,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val lesson = Tutorial.lessons[index]
    // Open at the step that uses this lesson's technique; earlier steps are a tap back.
    var step by remember(index, example) { mutableIntStateOf(example?.start ?: 0) }
    val cards = example?.cards.orEmpty()

    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(lesson.technique.label, onBack = onBack) {
                if (cards.isNotEmpty()) {
                    SumiChip("${minOf(step, cards.size - 1) + 1} of ${cards.size}", tone = ChipTone.Ai)
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val width = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                if (cards.size > 1) {
                    Row(width.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in cards.indices) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(if (i <= step) c.ai else c.surfaceSunk),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(lesson.intro, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = width)
                Spacer(Modifier.height(16.dp))
                when {
                    loading -> Box(width.aspectRatio(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.ai, trackColor = c.surfaceSunk)
                    }
                    example == null -> Unit
                    else -> {
                        // One page per card, then a last page with the number placed.
                        val done = step >= cards.size
                        val card = cards.getOrNull(step)
                        val shown = if (done) {
                            val last = cards.last()
                            example.game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
                        } else {
                            example.game
                        }
                        Overline(
                            "Worked example · ${if (lesson.killer) "Killer " else ""}${lesson.level.label} puzzle",
                            width,
                        )
                        Spacer(Modifier.height(10.dp))
                        SudokuBoard(
                            game = shown,
                            selected = if (done) cards.last().placeCell else null,
                            highlightSameNumbers = false,
                            onSelect = {},
                            modifier = width.aspectRatio(1f).card(c, CardShape),
                            hint = card,
                        )
                        Spacer(Modifier.height(14.dp))
                        SumiCard(width, padding = PaddingValues(20.dp)) {
                            Text(
                                if (done) "Result" else card!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = c.ink,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (done) {
                                    val last = cards.last()
                                    "So row ${Grid.row(last.placeCell) + 1}, column ${Grid.col(last.placeCell) + 1} " +
                                        "is ${last.placeDigit}." + if (cards.size > 1) {
                                            " The ${lesson.technique.label.lowercase()} made that possible."
                                        } else {
                                            ""
                                        }
                                } else {
                                    card!!.text
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = c.ink,
                            )
                            if (!done && card!!.eliminations.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Crossed-out numbers are the ones this step rules out.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.inkMuted,
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PillButton(
                                    "Back",
                                    { step-- },
                                    Modifier.weight(1f),
                                    tone = ButtonTone.Soft,
                                    icon = Lucide.ArrowLeft,
                                    enabled = step > 0,
                                )
                                PillButton(
                                    if (card?.isPlacement == true) "Place ${card.placeDigit}" else "Next step",
                                    { step++ },
                                    Modifier.weight(1f),
                                    trailingIcon = if (card?.isPlacement == true) null else Lucide.ChevronRight,
                                    icon = if (card?.isPlacement == true) Lucide.Check else null,
                                    enabled = !done,
                                )
                            }
                        }
                    }
                }
                if (onNextLesson != null) {
                    Spacer(Modifier.height(14.dp))
                    PillButton(
                        "Next: ${Tutorial.lessons[index + 1].technique.label}",
                        onNextLesson,
                        width,
                        tone = ButtonTone.Soft,
                        trailingIcon = Lucide.ChevronRight,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
