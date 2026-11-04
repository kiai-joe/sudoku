package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.Example
import com.kiaijoe.sudoku.engine.Grid
import com.kiaijoe.sudoku.engine.Tutorial
import com.kiaijoe.sudoku.ui.components.SudokuBoard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/** Every technique, grouped by the level that first needs it. */
@Composable
fun LearnScreen(onOpen: (Int) -> Unit, onBack: () -> Unit) {
    LearnScaffold("Learn", onBack) {
        Text(
            "The techniques the puzzles are graded by, easiest first. Each one has a worked example " +
                "from a real puzzle, explained the same way hints are.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        )
        val groups = Difficulty.entries.map { level ->
            "${level.label} · ${level.techniques}" to Tutorial.lessons.withIndex().filter {
                !it.value.killer && it.value.technique.level == level
            }
        } + listOf("Killer Sudoku" to Tutorial.lessons.withIndex().filter { it.value.killer })
        for ((heading, lessons) in groups) {
            Spacer(Modifier.height(18.dp))
            Text(
                heading,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            lessons.forEach { (index, lesson) ->
                Surface(
                    onClick = { onOpen(index) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(lesson.technique.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            lesson.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
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
    val lesson = Tutorial.lessons[index]
    // Open at the step that uses this lesson's technique; earlier steps are a tap back.
    var step by remember(index, example) { mutableIntStateOf(example?.start ?: 0) }
    LearnScaffold(lesson.technique.label, onBack) {
        val width = Modifier.widthIn(max = 520.dp).fillMaxWidth()
        Text(lesson.intro, style = MaterialTheme.typography.bodyLarge, modifier = width)
        Spacer(Modifier.height(16.dp))
        when {
            loading -> Box(width.aspectRatio(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            example == null -> Unit
            else -> {
                val cards = example.cards
                // One page per card, then a last page with the number placed.
                val done = step >= cards.size
                val card = cards.getOrNull(step)
                val shown = if (done) {
                    val last = cards.last()
                    example.game.place(last.placeCell, last.placeDigit, mistakeLimit = null, autoRemoveNotes = true)
                } else {
                    example.game
                }
                Text(
                    "Worked example · ${if (lesson.killer) "Killer " else ""}${lesson.level.label} puzzle",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = width,
                )
                Spacer(Modifier.height(8.dp))
                SudokuBoard(
                    game = shown,
                    selected = if (done) cards.last().placeCell else null,
                    highlightSameNumbers = false,
                    onSelect = {},
                    modifier = width.aspectRatio(1f),
                    hint = card,
                )
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = width) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (done) "Result" else card!!.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Step ${minOf(step, cards.size - 1) + 1} of ${cards.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!done && card!!.eliminations.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Crossed-out numbers are the ones this step rules out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { step-- },
                                enabled = step > 0,
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) { Text("Back") }
                            Button(
                                onClick = { step++ },
                                enabled = !done,
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) { Text(if (card?.isPlacement == true) "Place ${card.placeDigit}" else "Next step") }
                        }
                    }
                }
            }
        }
        if (onNextLesson != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onNextLesson, modifier = width.height(48.dp)) {
                Text("Next: ${Tutorial.lessons[index + 1].technique.label}")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
