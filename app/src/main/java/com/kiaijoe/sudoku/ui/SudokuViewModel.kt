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
    val settings: Settings = Settings(),
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
) {
    val canContinue: Boolean get() = game?.status == GameStatus.PLAYING && !generating
}

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

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

    fun newGame(difficulty: Difficulty) {
        _ui.update { it.copy(generating = true, paused = false, selected = null, notesMode = false) }
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

    fun restart() {
        val game = _ui.value.game ?: return
        undoStack.clear()
        val fresh = game.restart()
        repo.saveGame(fresh)
        _ui.update { it.copy(game = fresh, selected = null, canUndo = false, paused = false) }
        updateTimer()
    }

    /** Called by the game screen as it resumes and pauses, so the clock only runs while visible. */
    fun setGameScreenActive(active: Boolean) {
        gameScreenActive = active
        updateTimer()
        if (!active) _ui.value.game?.let(::persist)
    }

    fun togglePause() {
        val s = _ui.value
        if (s.game?.status != GameStatus.PLAYING) return
        _ui.update { it.copy(paused = !it.paused) }
        updateTimer()
    }

    // --- Input ----------------------------------------------------------

    fun select(cell: Int) {
        if (_ui.value.paused) return
        _ui.update { it.copy(selected = cell) }
    }

    fun toggleNotesMode() = _ui.update { it.copy(notesMode = !it.notesMode) }

    fun input(digit: Int) {
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

    fun erase() {
        val cell = _ui.value.selected ?: return
        move { it.erase(cell) }
    }

    fun hint() {
        val s = _ui.value
        val game = s.game ?: return
        val target = game.hintTarget(s.selected) ?: return
        _ui.update { it.copy(selected = target) }
        move { it.reveal(target, s.settings.autoRemoveNotes) }
    }

    fun undo() {
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
