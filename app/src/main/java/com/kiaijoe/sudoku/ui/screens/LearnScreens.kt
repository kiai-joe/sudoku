package com.kiaijoe.sudoku.ui.screens

import com.kiaijoe.sudoku.engine.tr
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
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
    onCombinations: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val total = Tutorial.lessons.size
    val finished = Tutorial.lessons.count { it.id in done }
    // The first lesson not yet finished, easiest first.
    val next = Tutorial.lessons.indexOfFirst { it.id !in done }
    TabPage(Tab.Learn, onTab) {
        ScreenTitle(tr("Learn", "Jifunze"), tr("Short lessons, each on a real board.", "Masomo mafupi, kila moja kwenye ubao halisi."))
        Spacer(Modifier.height(16.dp))
        SumiCard(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = tr("$finished of $total lessons finished", "Masomo $finished kati ya $total yamekamilika")
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("$finished of $total lessons", "Masomo $finished kati ya $total"), style = MaterialTheme.typography.titleLarge, color = c.ink, modifier = Modifier.weight(1f))
                if (finished == total) SumiChip(tr("All done", "Yote yamekamilika"), tone = ChipTone.Matcha, icon = Lucide.CheckCheck)
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
                    tr("Next up: ${Tutorial.lessons[next].title}", "Linalofuata: ${Tutorial.lessons[next].title}"),
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
            Spacer(Modifier.height(24.dp))
            Overline(heading)
            Spacer(Modifier.height(8.dp))
            val killer = icon == Lucide.SquareDashed
            ListCard(
                rows = lessons.map { (index, lesson) ->
                    @Composable {
                        val isDone = lesson.id in done
                        ListRow(
                            icon = when {
                                isDone -> Lucide.Check
                                killer -> icon
                                else -> Lucide.BookOpen
                            },
                            title = lesson.title,
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
                                stateDescription = when {
                                    isDone -> tr("Finished", "Limekamilika")
                                    index == next -> tr("Next", "Linalofuata")
                                    else -> tr("Not started", "Halijaanzwa")
                                }
                            },
                        ) {
                            if (index == next) {
                                Spacer(Modifier.width(8.dp))
                                SumiChip(tr("Next", "Linalofuata"), tone = ChipTone.Ai)
                            } else {
                                RowChevron()
                            }
                        }
                    }
                },
            )
            if (killer && onCombinations != null) {
                Spacer(Modifier.height(12.dp))
                ListCard(
                    rows = listOf(
                        @Composable {
                            ListRow(
                                icon = Lucide.Sigma,
                                title = tr("Combinations cheat sheet", "Orodha ya michanganyiko"),
                                subtitle = tr(
                                    "Every way to make each sum, in 2 to 5 cells",
                                    "Kila njia ya kupata kila jumla, katika visanduku 2 hadi 5",
                                ),
                                iconColor = c.shu,
                                iconBackground = c.shuSoft,
                                onClick = onCombinations,
                            ) { RowChevron() }
                        },
                    ),
                )
            }
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
    onPractice: (() -> Unit)? = null,
    practiceReplacesGame: Boolean = false,
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    val lesson = Tutorial.lessons[index]
    var confirmPractice by remember { mutableStateOf(false) }
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
                    if (done) tr("Result", "Matokeo") else card!!.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (done) {
                        val last = cards.last()
                        val row = Grid.row(last.placeCell) + 1
                        val col = Grid.col(last.placeCell) + 1
                        val technique = (cards.getOrNull(example.start)?.technique ?: lesson.technique).label.lowercase()
                        tr(
                            "So row $row, column $col is ${last.placeDigit}." +
                                if (cards.size > 1) " The $technique made that possible." else "",
                            "Kwa hiyo mstari wa $row, safu ya $col ni ${last.placeDigit}." +
                                if (cards.size > 1) " Hatua ya ${technique} ndiyo iliyowezesha hilo." else "",
                        )
                    } else {
                        card!!.text
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.ink,
                )
                if (!done && card!!.eliminations.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("Crossed-out numbers are the ones this step rules out.", "Namba zilizokatwa ndizo hatua hii inaziondoa."),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(
                        tr("Back", "Rudi"),
                        { step-- },
                        Modifier.weight(1f),
                        tone = ButtonTone.Soft,
                        icon = Lucide.ArrowLeft,
                        enabled = step > 0,
                    )
                    PillButton(
                        if (card?.isPlacement == true) tr("Place ${card.placeDigit}", "Weka ${card.placeDigit}") else tr("Next step", "Hatua ijayo"),
                        { step++ },
                        Modifier.weight(1f),
                        trailingIcon = if (card?.isPlacement == true) null else Lucide.ChevronRight,
                        icon = if (card?.isPlacement == true) Lucide.Check else null,
                        enabled = !done,
                    )
                }
            }
        }
        if (onPractice != null) {
            Spacer(Modifier.height(12.dp))
            PillButton(
                tr("Practise on a new puzzle", "Fanya mazoezi kwenye fumbo jipya"),
                { if (practiceReplacesGame) confirmPractice = true else onPractice() },
                width,
                tone = if (done) ButtonTone.Primary else ButtonTone.Soft,
                icon = Lucide.Play,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (lesson.focus != null) {
                    tr(
                        "A fresh Killer ${lesson.level.label} puzzle where this comes up along the way.",
                        "Fumbo jipya la Killer ${lesson.level.label} ambapo hili linajitokeza njiani.",
                    )
                } else {
                    tr(
                        "A fresh ${if (lesson.killer) "Killer " else ""}${lesson.level.label} puzzle that needs a " +
                            "${lesson.technique.label.lowercase()} somewhere along the way.",
                        "Fumbo jipya la ${if (lesson.killer) "Killer " else ""}${lesson.level.label} linalohitaji " +
                            "${lesson.technique.label.lowercase()} mahali fulani njiani.",
                    ),
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
                modifier = width,
            )
        }
        if (onNextLesson != null) {
            Spacer(Modifier.height(12.dp))
            PillButton(
                tr("Next: ${Tutorial.lessons[index + 1].title}", "Linalofuata: ${Tutorial.lessons[index + 1].title}"),
                onNextLesson,
                width,
                tone = ButtonTone.Soft,
                trailingIcon = Lucide.ChevronRight,
            )
        }
    }

    val exampleLabel = tr(
        "Worked example · ${if (lesson.killer) "Killer " else ""}${lesson.level.label} puzzle",
        "Mfano uliofanyiwa kazi · fumbo la ${if (lesson.killer) "Killer " else ""}${lesson.level.label}",
    )

    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(lesson.title, onBack = onBack) {
                if (cards.isNotEmpty()) {
                    SumiChip(tr("${minOf(step, cards.size - 1) + 1} of ${cards.size}", "${minOf(step, cards.size - 1) + 1} kati ya ${cards.size}"), tone = ChipTone.Ai)
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
                            Spacer(Modifier.height(16.dp))
                            Overline(exampleLabel, width)
                            Spacer(Modifier.height(8.dp))
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
                                Spacer(Modifier.height(8.dp))
                            }
                            board(width.aspectRatio(1f))
                            Spacer(Modifier.height(12.dp))
                        }
                        explanation(width)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (confirmPractice && onPractice != null) {
        AlertDialog(
            onDismissRequest = { confirmPractice = false },
            containerColor = c.surface,
            title = { Text(tr("Start a practice game?", "Anza mchezo wa mazoezi?"), style = MaterialTheme.typography.titleLarge) },
            text = { Text(tr("Your game in progress will be replaced.", "Mchezo wako unaoendelea utabadilishwa."), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                PillButton(tr("Start", "Anza"), { confirmPractice = false; onPractice() }, height = 44.dp)
            },
            dismissButton = {
                PillButton(tr("Cancel", "Ghairi"), { confirmPractice = false }, tone = ButtonTone.Ghost, height = 44.dp)
            },
        )
    }
}
