package com.kiaijoe.sudoku.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.data.GameRepository
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.HintCard
import com.kiaijoe.sudoku.engine.HintPlan
import com.kiaijoe.sudoku.engine.Hints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MISTAKE_LIMIT = 3

data class UiState(
    val game: GameState? = null,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    val generating: Boolean = false,
    val canUndo: Boolean = false,
    val lastWinWasBest: Boolean = false,
    val hint: HintUi? = null,
    val settings: Settings = Settings(),
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
) {
    val canContinue: Boolean get() = game?.status == GameStatus.PLAYING && !generating
}

/** An open hint: the plan and which of its cards is showing. */
data class HintUi(val plan: HintPlan, val index: Int = 0) {
    private val cards: List<HintCard> get() = (plan as? HintPlan.Teach)?.cards.orEmpty()
    val card: HintCard? get() = cards.getOrNull(index)
    val total: Int get() = cards.size
    val hasNext: Boolean get() = index < total - 1
}

class SudokuViewModel(application: Application) : AndroidViewModel(application), GameActions {

    private val repo = GameRepository(application)
    private val undoStack = ArrayDeque<GameState>()
    private var timerJob: Job? = null
    private var gameScreenActive = false

    private val _ui = MutableStateFlow(
        UiState(
            game = repo.loadGame(),
            settings = repo.loadSettings(),
            stats = repo.loadStats(),
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    // --- Game lifecycle -------------------------------------------------

    override fun newGame(difficulty: Difficulty) {
        _ui.update { it.copy(generating = true, paused = false, selected = null, notesMode = false, hint = null) }
        updateTimer()
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { Generator.generate(difficulty) }
            val game = GameState.from(puzzle)
            undoStack.clear()
            repo.recordStart(difficulty)
            repo.saveGame(game)
            _ui.update {
                it.copy(
                    game = game,
                    generating = false,
                    canUndo = false,
                    lastWinWasBest = false,
                    stats = repo.loadStats(),
                )
            }
            updateTimer()
        }
    }

    override fun restart() {
        val game = _ui.value.game ?: return
        undoStack.clear()
        val fresh = game.restart()
        repo.saveGame(fresh)
        _ui.update { it.copy(game = fresh, selected = null, canUndo = false, paused = false, hint = null) }
        updateTimer()
    }

    /** Called by the game screen as it resumes and pauses, so the clock only runs while visible. */
    override fun setGameScreenActive(active: Boolean) {
        gameScreenActive = active
        updateTimer()
        if (!active) _ui.value.game?.let(::persist)
    }

    override fun togglePause() {
        val s = _ui.value
        if (s.game?.status != GameStatus.PLAYING) return
        _ui.update { it.copy(paused = !it.paused) }
        updateTimer()
    }

    // --- Input ----------------------------------------------------------

    override fun select(cell: Int) {
        if (_ui.value.paused) return
        _ui.update { it.copy(selected = cell) }
    }

    override fun toggleNotesMode() = _ui.update { it.copy(notesMode = !it.notesMode) }

    override fun input(digit: Int) {
        val s = _ui.value
        val cell = s.selected ?: return
        val settings = s.settings
        move { g ->
            if (s.notesMode) {
                g.toggleNote(cell, digit)
            } else {
                g.place(
                    cell = cell,
                    digit = digit,
                    mistakeLimit = if (settings.mistakeLimit) MISTAKE_LIMIT else null,
                    autoRemoveNotes = settings.autoRemoveNotes,
                )
            }
        }
    }

    override fun erase() {
        val cell = _ui.value.selected ?: return
        move { it.erase(cell) }
    }

    /** Opens a teaching hint for the board as it stands. Opening one counts as a hint used. */
    override fun hint() {
        val s = _ui.value
        val game = s.game ?: return
        if (game.status != GameStatus.PLAYING || s.paused || s.generating) return
        val plan = Hints.forGame(game)
        if (plan is HintPlan.NothingLeft) return
        val counted = game.copy(hintsUsed = game.hintsUsed + 1)
        persist(counted)
        _ui.update { it.copy(game = counted, hint = HintUi(plan), selected = null) }
    }

    override fun hintNext() = _ui.update { s ->
        val h = s.hint ?: return@update s
        if (h.hasNext) s.copy(hint = h.copy(index = h.index + 1)) else s
    }

    /** Places the number the hint's last step arrives at. */
    override fun hintPlace() {
        val card = _ui.value.hint?.card ?: return
        if (!card.isPlacement) return
        _ui.update { it.copy(hint = null, selected = card.placeCell) }
        move { it.place(card.placeCell, card.placeDigit, mistakeLimit = null, autoRemoveNotes = _ui.value.settings.autoRemoveNotes) }
    }

    /** Erases the wrong number a hint pointed out. */
    override fun hintEraseMistake() {
        val plan = _ui.value.hint?.plan as? HintPlan.FixMistake ?: return
        _ui.update { it.copy(hint = null, selected = plan.cell) }
        move { it.erase(plan.cell) }
    }

    override fun closeHint() = _ui.update { it.copy(hint = null) }

    override fun undo() {
        val s = _ui.value
        val game = s.game ?: return
        if (game.status != GameStatus.PLAYING || s.paused) return
        val previous = undoStack.removeLastOrNull() ?: return
        // Undo restores the board but not the clock, mistakes or hints used.
        val restored = game.copy(values = previous.values, notes = previous.notes)
        repo.saveGame(restored)
        _ui.update { it.copy(game = restored, canUndo = undoStack.isNotEmpty()) }
    }

    private fun move(transform: (GameState) -> GameState) {
        val s = _ui.value
        val game = s.game ?: return
        if (game.status != GameStatus.PLAYING || s.paused || s.generating) return
        val next = transform(game)
        if (next == game) return

        undoStack.addLast(game)
        while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()

        var best = s.lastWinWasBest
        if (next.status == GameStatus.WON) {
            best = repo.recordWin(next.difficulty, next.elapsedSeconds)
            undoStack.clear()
        }
        persist(next)
        _ui.update {
            it.copy(
                game = next,
                canUndo = undoStack.isNotEmpty(),
                lastWinWasBest = best,
                stats = if (next.status == GameStatus.WON) repo.loadStats() else it.stats,
            )
        }
        updateTimer()
    }

    private fun persist(game: GameState) {
        repo.saveGame(if (game.status == GameStatus.WON) null else game)
    }

    // --- Settings and stats ---------------------------------------------

    fun updateSettings(settings: Settings) {
        repo.saveSettings(settings)
        _ui.update { it.copy(settings = settings) }
    }

    fun resetStats() {
        repo.resetStats()
        _ui.update { it.copy(stats = repo.loadStats()) }
    }

    // --- Timer ----------------------------------------------------------

    private fun updateTimer() {
        val s = _ui.value
        val shouldRun = gameScreenActive && !s.paused && !s.generating && s.game?.status == GameStatus.PLAYING
        if (shouldRun && timerJob?.isActive != true) {
            timerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    _ui.update { state ->
                        val g = state.game
                        if (g == null || g.status != GameStatus.PLAYING) state
                        else state.copy(game = g.copy(elapsedSeconds = g.elapsedSeconds + 1))
                    }
                }
            }
        } else if (!shouldRun) {
            timerJob?.cancel()
            timerJob = null
        }
    }

    private companion object {
        const val UNDO_LIMIT = 200
    }
}
