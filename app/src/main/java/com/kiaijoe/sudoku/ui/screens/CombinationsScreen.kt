package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.Killer
import com.kiaijoe.sudoku.engine.tr
import com.kiaijoe.sudoku.ui.components.ChipTone
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.Page
import com.kiaijoe.sudoku.ui.components.Segment
import com.kiaijoe.sudoku.ui.components.SegmentedControl
import com.kiaijoe.sudoku.ui.components.SumiChip
import com.kiaijoe.sudoku.ui.components.SumiTopBar
import com.kiaijoe.sudoku.ui.theme.Sumi

/** Every way to make [sum] from [size] different numbers 1 to 9, smallest first, as sorted lists. */
internal fun cageSets(size: Int, sum: Int): List<List<Int>> =
    Killer.combinations(List(size) { 0x1FF }, sum, 0)
        .map { mask -> (1..9).filter { mask and (1 shl (it - 1)) != 0 } }
        .sortedWith { a, b -> a.zip(b).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: 0 }

/** The sums [size] different numbers can make, smallest to largest. */
internal fun cageSums(size: Int): IntRange = (size * (size + 1) / 2)..(45 - (9 - size) * (10 - size) / 2)

/**
 * The Killer combinations cheat sheet: for cages of 2 to 5 cells, every sum
 * with every set of numbers that makes it. Sums with one answer are marked,
 * and so are numbers every set uses, which a cage must contain.
 */
@Composable
fun CombinationsScreen(onBack: () -> Unit, initialSize: Int = 2) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    var size by rememberSaveable { mutableIntStateOf(initialSize) }
    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(tr("Cage combinations", "Michanganyiko ya vizimba"), onBack = onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    Text(
                        tr(
                            "Every way to make each sum from different numbers. Sums with one answer are marked; " +
                                "\"always\" shows numbers every way uses, so the cage must hold them.",
                            "Kila njia ya kupata kila jumla kwa namba tofauti. Jumla zenye jibu moja zimewekwa alama; " +
                                "\"daima\" inaonyesha namba ambazo kila njia hutumia, kwa hiyo kizimba lazima kiwe nazo.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkMuted,
                    )
                    Spacer(Modifier.height(16.dp))
                    SegmentedControl(
                        segments = (2..5).map { Segment(tr("$it cells", "visanduku $it")) },
                        selected = size - 2,
                        onSelect = { size = it + 2 },
                    )
                    Spacer(Modifier.height(16.dp))
                    ListCard(rows = cageSums(size).map { sum -> @Composable { SumRow(size, sum) } })
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SumRow(size: Int, sum: Int) {
    val c = Sumi.colors
    val sets = cageSets(size, sum)
    val one = sets.size == 1
    // Numbers in every set: the cage must contain them. Only worth saying when there's more than one set.
    val always = if (one) emptyList() else sets.reduce { a, b -> a.intersect(b.toSet()).toList() }.sorted()
    val text = sets.joinToString("   ") { it.joinToString("+") }
    val spoken = buildString {
        append(tr("Sum $sum: ", "Jumla $sum: "))
        append(sets.joinToString(tr(", or ", ", au ")) { it.joinToString(tr(" plus ", " jumlisha ")) })
        if (one) append(tr(". One answer.", ". Jibu moja."))
        if (always.isNotEmpty()) append(tr(". Always has ", ". Daima ina ")).append(always.joinToString(tr(" and ", " na ")))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (one) Modifier.background(c.shuSoft) else Modifier)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sum.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (one) c.shu else c.ink,
            modifier = Modifier.width(44.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = c.ink)
            if (always.isNotEmpty()) {
                Text(
                    tr("always ", "daima ") + always.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
        }
        if (one) {
            Spacer(Modifier.width(8.dp))
            SumiChip(tr("One answer", "Jibu moja"), tone = ChipTone.Shu)
        }
    }
}
