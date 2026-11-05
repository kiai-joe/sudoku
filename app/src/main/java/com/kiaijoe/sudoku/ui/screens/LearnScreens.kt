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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
fun LearnScreen(
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    onTab: ((Tab) -> Unit)? = null,
    done: Set<String> = emptySet(),
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val total = Tutorial.lessons.size
    val finished = Tutorial.lessons.count { it.technique.name in done }
    // The first lesson not yet finished, easiest first.
    val next = Tutorial.lessons.indexOfFirst { it.technique.name !in done }
    TabPage(Tab.Learn, onTab) {
        ScreenTitle("Learn", "Short lessons, each on a real board.")
        Spacer(Modifier.height(18.dp))
        SumiCard(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "$finished of $total lessons finished" },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$finished of $total lessons", style = MaterialTheme.typography.titleLarge, color = c.ink, modifier = Modifier.weight(1f))
                if (finished == total) SumiChip("All done", tone = ChipTone.Matcha, icon = Lucide.CheckCheck)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(c.surfaceSunk),
            ) {
                if (finished > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth(finished.toFloat() / total)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(c.matcha),
                    )
                }
            }
            if (next >= 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Next up: ${Tutorial.lessons[next].technique.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkMuted,
                )
            }
        }
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
                        val isDone = lesson.technique.name in done
                        ListRow(
                            icon = when {
                                isDone -> Lucide.Check
                                killer -> icon
                                else -> Lucide.BookOpen
                            },
                            title = lesson.technique.label,
                            subtitle = lesson.summary,
                            iconColor = when {
                                isDone -> c.matcha
                                killer -> c.shu
                                else -> c.ai
                            },
                            iconBackground = when {
                                isDone -> c.matchaSoft
                                killer -> c.shuSoft
                                else -> c.aiSoft
                            },
                            onClick = { onOpen(index) },
                            modifier = Modifier.semantics {
                                stateDescription = if (isDone) "Finished" else if (index == next) "Next" else "Not started"
                            },
                        ) {
                            if (index == next) {
                                Spacer(Modifier.width(8.dp))
                                SumiChip("Next", tone = ChipTone.Ai)
                            } else {
                                RowChevron()
                            }
                        }
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
    onComplete: () -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val lesson = Tutorial.lessons[index]
    // Open at the step that uses this lesson's technique; earlier steps are a tap back.
    var step by remember(index, example) { mutableIntStateOf(example?.start ?: 0) }
    val cards = example?.cards.orEmpty()
    // Finished on reaching the end of the worked example, or on opening a lesson that has none.
    val finished = if (example == null) !loading else step >= cards.size
    LaunchedEffect(index, finished) { if (finished) onComplete() }

    // One page per card, then a last page with the number placed.
    val done = example != null && step >= cards.size
    val card = cards.getOrNull(step)
    val shown = when {
        example == null -> null
        done -> cards.last().let { example.game.place(it.placeCell, it.placeDigit, mistakeLimit = null, autoRemoveNotes = true) }
        else -> example.game
    }

    // The worked example's board, or a spinner while it's built.
    val board: @Composable (Modifier) -> Unit = { modifier ->
        when {
            loading -> Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.ai, trackColor = c.surfaceSunk)
            }
            shown != null -> SudokuBoard(
                game = shown,
                selected = if (done) cards.last().placeCell else null,
                highlightSameNumbers = false,
                onSelect = {},
                modifier = modifier.card(c, CardShape),
                hint = card,
            )
        }
    }

    // Progress through the steps, what the technique is, and the current step's card.
    val steps: @Composable ColumnScope.(Modifier) -> Unit = { width ->
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
    }

    val explanation: @Composable ColumnScope.(Modifier) -> Unit = { width ->
        if (example != null) {
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
    }

    val exampleLabel = "Worked example · ${if (lesson.killer) "Killer " else ""}${lesson.level.label} puzzle"

    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(lesson.technique.label, onBack = onBack) {
                if (cards.isNotEmpty()) {
                    SumiChip("${minOf(step, cards.size - 1) + 1} of ${cards.size}", tone = ChipTone.Ai)
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                if (isWide(maxWidth, maxHeight) && (loading || example != null)) {
                    // Board on the left at full height; the reading beside it.
                    val size = min(maxHeight - 16.dp, maxWidth * 0.52f)
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                    ) {
                        board(Modifier.padding(top = 4.dp).size(size))
                        Column(
                            Modifier
                                .widthIn(max = 520.dp)
                                .weight(1f, fill = false)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                        ) {
                            val width = Modifier.fillMaxWidth()
                            steps(width)
                            Spacer(Modifier.height(14.dp))
                            Overline(exampleLabel, width)
                            Spacer(Modifier.height(10.dp))
                            explanation(width)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val width = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                        steps(width)
                        Spacer(Modifier.height(16.dp))
                        if (loading || example != null) {
                            if (example != null) {
                                Overline(exampleLabel, width)
                                Spacer(Modifier.height(10.dp))
                            }
                            board(width.aspectRatio(1f))
                            Spacer(Modifier.height(14.dp))
                        }
                        explanation(width)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
