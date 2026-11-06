package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.kiaijoe.sudoku.engine.Scanner
import com.kiaijoe.sudoku.ui.PhotoScan
import com.kiaijoe.sudoku.ui.components.Overline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.engine.CustomCheck
import com.kiaijoe.sudoku.engine.CustomPuzzle
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.Grid
import com.kiaijoe.sudoku.engine.Puzzle
import com.kiaijoe.sudoku.engine.tr
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.CardShape
import com.kiaijoe.sudoku.ui.components.IconTile
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.ListRow
import com.kiaijoe.sudoku.ui.components.NumberPad
import com.kiaijoe.sudoku.ui.components.Page
import com.kiaijoe.sudoku.ui.components.PillButton
import com.kiaijoe.sudoku.ui.components.SudokuBoard
import com.kiaijoe.sudoku.ui.components.SumiTopBar
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.components.icon
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.Sumi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 81 digits, 0 for an empty cell, as the screen keeps them across rotation. */
private fun parse(text: String): IntArray = IntArray(Grid.CELLS) { text.getOrNull(it)?.digitToIntOrNull() ?: 0 }

/**
 * The board being typed in, drawn as a game: every number in indigo, any
 * that repeats in a row, column or box marked wrong, and numbers a scan
 * wasn't sure of tinted gold.
 */
private fun entryBoard(values: IntArray, unsure: Set<Int>): GameState {
    val clashes = CustomPuzzle.clashes(values)
    val solution = IntArray(Grid.CELLS) { if (it in clashes) 0 else values[it] }
    return GameState.from(Puzzle(IntArray(Grid.CELLS), solution, Difficulty.EASY)).copy(
        values = values.toList(),
        marks = if (unsure.isEmpty()) emptyList() else List(Grid.CELLS) { if (it in unsure) 2 else 0 },
    )
}

/**
 * Type in a puzzle from a book or a newspaper. Sumi checks it has exactly one
 * solution, grades it by the techniques it needs, and plays it with hints.
 */
@Composable
fun EnterPuzzleScreen(
    onBack: () -> Unit,
    onPlay: (Puzzle) -> Unit,
    replacesGame: Boolean,
    initial: String = "",
    initialSelected: Int = 0,
    initialUnsure: Set<Int> = emptySet(),
) {
    BackHandler(onBack = onBack)
    val c = Sumi.colors
    var text by rememberSaveable { mutableStateOf(initial.padEnd(Grid.CELLS, '0')) }
    var selected by rememberSaveable { mutableIntStateOf(initialSelected) }
    var result by remember { mutableStateOf<CustomCheck?>(null) }
    var checking by remember { mutableStateOf(false) }
    // From the last scan: numbers it wasn't sure of, the straightened photo, and what it found.
    var unsure by remember { mutableStateOf(initialUnsure) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var scanNote by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val values = parse(text)
    val clues = values.count { it != 0 }

    fun set(cell: Int, digit: Int) {
        text = text.substring(0, cell) + digit + text.substring(cell + 1)
        unsure = unsure - cell
        result = null
    }

    fun read(uri: Uri) {
        scanning = true
        scanNote = null
        scope.launch {
            val image = withContext(Dispatchers.Default) { PhotoScan.load(context, uri) }
            val scan = image?.let { withContext(Dispatchers.Default) { Scanner.scan(it, PhotoScan.reader(context)) } }
            scanning = false
            when {
                image == null -> scanNote = tr("That photo couldn't be opened.", "Picha hiyo haikuweza kufunguliwa.")
                scan == null -> scanNote = tr(
                    "No Sudoku grid found in that photo. Try again with the whole grid in view, flat and well lit.",
                    "Hakuna gridi ya Sudoku kwenye picha hiyo. Jaribu tena gridi yote ikionekana, ikiwa bapa na yenye mwanga.",
                )
                else -> {
                    text = scan.digits.joinToString("")
                    unsure = scan.unsure
                    photo = PhotoScan.bitmap(scan.grid).asImageBitmap()
                    selected = 0
                    // Checked straight away: a misread number often leaves no solution, or a clash, which shows at once.
                    result = withContext(Dispatchers.Default) { CustomPuzzle.check(scan.digits) }
                    val found = scan.digits.count { it != 0 }
                    scanNote = if (scan.unsure.isEmpty()) {
                        tr(
                            "Read $found numbers. Check them against your photo below before you play.",
                            "Namba $found zimesomwa. Zilinganishe na picha yako hapa chini kabla ya kucheza.",
                        )
                    } else {
                        tr(
                            "Read $found numbers. The ${scan.unsure.size} in gold may be wrong: check them against your photo below.",
                            "Namba $found zimesomwa. Zile ${scan.unsure.size} za dhahabu huenda si sahihi: zilinganishe na picha yako hapa chini.",
                        )
                    }
                }
            }
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) read(Uri.parse(uri))
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) read(uri)
    }

    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar(tr("Enter a puzzle", "Ingiza fumbo"), onBack = onBack)
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
                            "Scan it from a photo, or tap a cell and a number. The next cell is selected for you, so you can type a row at a time.",
                            "Lichanganue kutoka picha, au gusa kisanduku kisha namba. Kisanduku kinachofuata huchaguliwa, hivyo unaweza kuandika mstari mzima.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(
                            tr("Camera", "Kamera"),
                            {
                                try {
                                    val uri = PhotoScan.newPhotoUri(context)
                                    cameraUri = uri.toString()
                                    takePhoto.launch(uri)
                                } catch (e: ActivityNotFoundException) {
                                    scanNote = tr("This phone has no camera app to take the photo.", "Simu hii haina programu ya kamera ya kupiga picha.")
                                }
                            },
                            Modifier.weight(1f),
                            tone = ButtonTone.Outline,
                            icon = Lucide.Camera,
                            height = 44.dp,
                            enabled = !scanning,
                        )
                        PillButton(
                            tr("Photos", "Picha"),
                            { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            Modifier.weight(1f),
                            tone = ButtonTone.Outline,
                            icon = Lucide.Image,
                            height = 44.dp,
                            enabled = !scanning,
                        )
                    }
                    if (scanning || scanNote != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (scanning) tr("Reading the photo…", "Inasoma picha…") else scanNote.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SudokuBoard(
                        game = entryBoard(values, unsure),
                        selected = selected,
                        highlightSameNumbers = false,
                        onSelect = { selected = it },
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).card(c, CardShape),
                    )
                    Spacer(Modifier.height(12.dp))
                    NumberPad(
                        remaining = { d -> (Grid.SIZE - values.count { it == d }).coerceAtLeast(0) },
                        notesMode = false,
                        onDigit = { d ->
                            set(selected, d)
                            if (selected < Grid.CELLS - 1) selected++
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(Lucide.Eraser, tr("Erase", "Futa"), onClick = { set(selected, 0) })
                        Text(
                            tr("$clues numbers", "namba $clues"),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.inkMuted,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(
                            tr("Clear", "Safisha"),
                            {
                                text = "0".repeat(Grid.CELLS)
                                selected = 0
                                result = null
                                unsure = emptySet()
                                photo = null
                                scanNote = null
                            },
                            tone = ButtonTone.Ghost,
                            height = 44.dp,
                            enabled = clues > 0,
                        )
                        PillButton(
                            if (checking) tr("Checking…", "Inakagua…") else tr("Check", "Kagua"),
                            {
                                checking = true
                                scope.launch {
                                    result = withContext(Dispatchers.Default) { CustomPuzzle.check(values) }
                                    checking = false
                                }
                            },
                            tone = ButtonTone.Seal,
                            icon = Lucide.CheckCheck,
                            height = 44.dp,
                            enabled = !checking && clues > 0,
                        )
                    }
                    result?.let { r ->
                        Spacer(Modifier.height(16.dp))
                        CheckResult(r, replacesGame, onPlay)
                    }
                    photo?.let { bitmap ->
                        Spacer(Modifier.height(24.dp))
                        Overline(tr("Your photo, straightened", "Picha yako, imenyooshwa"))
                        Spacer(Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap,
                            contentDescription = tr("The scanned grid, to compare with the board", "Gridi iliyochanganuliwa, ya kulinganisha na ubao"),
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CardShape),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckResult(result: CustomCheck, replacesGame: Boolean, onPlay: (Puzzle) -> Unit) {
    val c = Sumi.colors
    val live = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    if (result is CustomCheck.Ok) {
        val p = result.puzzle
        Column(live) {
            ListCard(
                rows = listOf(
                    @Composable {
                        ListRow(
                            icon = p.difficulty.icon,
                            title = tr("One solution · ${p.difficulty.label}", "Suluhisho moja · ${p.difficulty.label}"),
                            subtitle = if (result.byLogic) {
                                p.hardest?.let { tr("Hardest step: ${it.label.lowercase()}", "Hatua ngumu zaidi: ${it.label.lowercase()}") }
                            } else {
                                tr(
                                    "Logic alone can't finish it, so at some point you'll have to guess. Hints reveal a cell there.",
                                    "Mantiki pekee haiwezi kulimaliza, kwa hiyo wakati fulani utalazimika kubahatisha. Madokezo hufunua kisanduku hapo.",
                                )
                            },
                            iconColor = c.ai,
                            iconBackground = c.aiSoft,
                        )
                    },
                ),
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                tr("Play it", "Licheze"),
                { onPlay(p) },
                Modifier.fillMaxWidth(),
                tone = ButtonTone.Seal,
                icon = Lucide.Play,
            )
            if (replacesGame) {
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Your game in progress will be replaced.", "Mchezo wako unaoendelea utabadilishwa."),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        return
    }
    val message = when (result) {
        is CustomCheck.Clash -> tr(
            "A number repeats in a row, column or box. The repeats are marked in red.",
            "Namba inajirudia katika mstari, safu au sanduku. Marudio yamewekwa alama nyekundu.",
        )
        is CustomCheck.TooFew -> tr(
            "${result.clues} numbers isn't enough: a Sudoku with one solution needs at least ${CustomPuzzle.MIN_CLUES}.",
            "Namba ${result.clues} hazitoshi: Sudoku yenye suluhisho moja inahitaji angalau ${CustomPuzzle.MIN_CLUES}.",
        )
        CustomCheck.NoSolution -> tr(
            "This puzzle has no solution. Check the numbers against the page.",
            "Fumbo hili halina suluhisho. Linganisha namba na ukurasa.",
        )
        CustomCheck.ManySolutions -> tr(
            "This puzzle has more than one solution, so logic can't decide between them. A number may be missing.",
            "Fumbo hili lina suluhisho zaidi ya moja, kwa hiyo mantiki haiwezi kuamua. Huenda namba moja imekosekana.",
        )
        is CustomCheck.Ok -> ""
    }
    ListCard(
        modifier = live,
        rows = listOf(
            @Composable {
                ListRow(
                    icon = Lucide.X,
                    title = message,
                    iconColor = c.shu,
                    iconBackground = c.shuSoft,
                )
            },
        ),
    )
}
