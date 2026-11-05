package com.kiaijoe.sudoku.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiaijoe.sudoku.data.DifficultyStats
import com.kiaijoe.sudoku.data.BackupException
import com.kiaijoe.sudoku.data.GameRepository
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Achievements
import com.kiaijoe.sudoku.engine.Daily
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.HintCard
import com.kiaijoe.sudoku.engine.HintPlan
import com.kiaijoe.sudoku.engine.Hints
import com.kiaijoe.sudoku.engine.Killer
import com.kiaijoe.sudoku.engine.Progress
import com.kiaijoe.sudoku.widget.SumiWidget
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
import java.time.LocalDate

const val MISTAKE_LIMIT = 3

/**
 * [game] is the game on screen, regular or daily. [savedGame] is the regular
 * game in progress, which the home screen offers to continue.
 */
data class UiState(
    val game: GameState? = null,
    val savedGame: GameState? = null,
    val daily: DailyUi = DailyUi(),
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    val generating: Boolean = false,
    val canUndo: Boolean = false,
    val lastWinWasBest: Boolean = false,
    val hint: HintUi? = null,
    val feedback: Feedback? = null,
    val flagged: Set<Int> = emptySet(),
    val checks: Int = 0,
    val checkMessage: String? = null,
    val settings: Settings = Settings(),
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
    val killerStats: DifficultyStats = DifficultyStats(),
    val achievements: Map<Achievement, Long> = emptyMap(),
    val newAchievements: List<Achievement> = emptyList(),
    /** Lessons finished, by technique name. */
    val lessonsDone: Set<String> = emptySet(),
) {
    val canContinue: Boolean get() = savedGame?.status == GameStatus.PLAYING && !generating
}

/**
 * Today's daily puzzle and the record of past ones.
 *
 * @property today the current epoch day.
 * @property results solved days (epoch day to seconds).
 * @property inProgress today's daily game if started and not finished.
 */
data class DailyUi(
    val today: Long = 0,
    val results: Map<Long, Long> = emptyMap(),
    val inProgress: GameState? = null,
) {
    val difficulty: Difficulty get() = Daily.difficultyFor(today)
    val label: String get() = Daily.label(today)
    val solvedToday: Boolean get() = today in results
    val todaySeconds: Long? get() = results[today]
    val streak: Int get() = Daily.streak(results.keys, today)
    val longestStreak: Int get() = Daily.longestStreak(results.keys)
}

enum class FeedbackKind { PLACE, NOTE, WRONG, COMPLETE }

/**
 * What the last input did, for a vibration and, when a row, column or box
 * was finished, the ink wash over [completed]. [id] changes on every input.
 */
data class Feedback(val id: Long, val kind: FeedbackKind, val completed: List<Int> = emptyList())

/** An open hint: the plan and which of its cards is showing. */
data class HintUi(val plan: HintPlan, val index: Int = 0) {
    private val cards: List<HintCard> get() = (plan as? HintPlan.Teach)?.cards.orEmpty()
    val card: HintCard? get() = cards.getOrNull(index)
    val total: Int get() = cards.size
    val hasNext: Boolean get() = index < total - 1
}

/** How a restore went, and what to tell the player. */
data class BackupResult(val ok: Boolean, val message: String)

class SudokuViewModel(application: Application) : AndroidViewModel(application), GameActions {

    private val repo = GameRepository(application)
    private val undoStack = ArrayDeque<GameState>()
    private var timerJob: Job? = null
    private var gameScreenActive = false
    private var feedbackId = 0L

    private val _ui = MutableStateFlow(loadState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        // Award anything already earned from saved statistics, such as streaks.
        award(won = null)
        _ui.update { it.copy(achievements = repo.loadAchievements()) }
    }

    /** Everything the screens show, as saved. */
    private fun loadState(): UiState = repo.loadGame().let { saved ->
        UiState(
            game = saved,
            savedGame = saved,
            daily = loadDaily(),
            settings = repo.loadSettings(),
            stats = repo.loadStats(),
            killerStats = repo.loadKillerStats(),
            lessonsDone = repo.loadLessonsDone(),
            achievements = repo.loadAchievements(),
        )
    }

    // --- Backup ---------------------------------------------------------

    /** Everything Sumi keeps, as the text of a backup file. */
    fun exportBackup(): String = repo.exportBackup()

    /**
     * Restores the backup in [text], replacing everything on this phone, and
     * reloads every screen from it. Returns a message for the player.
     */
    fun restoreBackup(text: String): BackupResult = try {
        repo.restoreBackup(text)
        undoStack.clear()
        timerJob?.cancel()
        _ui.value = loadState()
        award(won = null)
        _ui.update { it.copy(achievements = repo.loadAchievements()) }
        updateTimer()
        refreshWidget()
        BackupResult(ok = true, message = "Restored. Your games, stats and settings are back.")
    } catch (e: BackupException) {
        BackupResult(ok = false, message = e.message ?: "This backup couldn't be restored.")
    }

    /** What the saved statistics add up to, for achievements. */
    private fun progress(): Progress {
        val daily = repo.loadDailyResults()
        return Progress(
            won = repo.loadStats().mapValues { it.value.won },
            killerWon = repo.loadKillerStats().won,
            dailySolved = daily.size,
            longestStreak = Daily.longestStreak(daily.keys),
        )
    }

    /** Saves any achievements earned now (including by [won], a just-won game) and returns the new ones. */
    private fun award(won: GameState?): List<Achievement> {
        val have = repo.loadAchievements().keys
        val earned = Achievements.earned(progress(), won, mistakesShown = _ui.value.settings.instantCheck)
        val fresh = earned - have
        if (fresh.isEmpty()) return emptyList()
        repo.addAchievements(fresh, LocalDate.now().toEpochDay())
        return Achievement.entries.filter { it in fresh }
    }

    // --- Game lifecycle -------------------------------------------------

    override fun newGame(difficulty: Difficulty) {
        startLoading()
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { Generator.generate(difficulty) }
            val game = GameState.from(puzzle)
            repo.recordStart(difficulty)
            repo.saveGame(game)
            show(game, savedGame = game)
        }
    }

    /** Starts a Killer Sudoku game. It takes the regular game's slot, like any new game. */
    override fun newKiller(difficulty: Difficulty) {
        startLoading()
        viewModelScope.launch {
            val game = GameState.from(withContext(Dispatchers.Default) { Killer.generate(difficulty) })
            repo.recordKillerStart()
            repo.saveGame(game)
            show(game, savedGame = game)
        }
    }

    /** Brings back the regular game in progress, if the screen shows the daily one. */
    fun continueGame() {
        val s = _ui.value
        if (s.game != null && s.game.daily == null) return // the regular game is already the one on screen
        val saved = s.savedGame ?: return
        show(saved, savedGame = saved)
    }

    /** Opens today's daily puzzle, resuming it if already started. */
    fun playDaily() {
        refreshDaily()
        val daily = _ui.value.daily
        if (daily.solvedToday) return
        daily.inProgress?.let {
            show(it, savedGame = _ui.value.savedGame)
            return
        }
        startLoading()
        viewModelScope.launch {
            val game = withContext(Dispatchers.Default) { Daily.gameFor(daily.today) }
            repo.saveDailyGame(game)
            show(game, savedGame = _ui.value.savedGame)
            refreshDaily()
            refreshWidget()
        }
    }

    /** Re-reads today's date and daily progress, in case the day has changed. */
    fun refreshDaily() = _ui.update { it.copy(daily = loadDaily()) }

    /** Brings the home-screen widget up to date with the daily record. */
    private fun refreshWidget() = SumiWidget.refresh(getApplication<Application>())

    private fun loadDaily(): DailyUi {
        val today = LocalDate.now().toEpochDay()
        val inProgress = repo.loadDailyGame()?.takeIf { it.daily == today && it.status == GameStatus.PLAYING }
        return DailyUi(today = today, results = repo.loadDailyResults(), inProgress = inProgress)
    }

    private fun startLoading() {
        _ui.update { it.copy(generating = true, paused = false, selected = null, notesMode = false, hint = null) }
        updateTimer()
    }

    /** Puts [game] on screen with a fresh undo history. */
    private fun show(game: GameState, savedGame: GameState?) {
        undoStack.clear()
        _ui.update {
            it.copy(
                game = game,
                savedGame = savedGame,
                generating = false,
                canUndo = false,
                paused = false,
                selected = null,
                notesMode = false,
                hint = null,
                flagged = emptySet(),
                checks = 0,
                checkMessage = null,
                feedback = null,
                lastWinWasBest = false,
                newAchievements = emptyList(),
                stats = repo.loadStats(),
                killerStats = repo.loadKillerStats(),
            )
        }
        updateTimer()
    }

    override fun restart() {
        val game = _ui.value.game ?: return
        undoStack.clear()
        val fresh = game.restart()
        persist(fresh)
        _ui.update {
            it.copy(
                game = fresh,
                savedGame = if (fresh.daily == null) fresh else it.savedGame,
                selected = null,
                canUndo = false,
                paused = false,
                hint = null,
                flagged = emptySet(),
                checkMessage = null,
            )
        }
        updateTimer()
    }

    /** Called by the game screen as it resumes and pauses, so the clock only runs while visible. */
    override fun setGameScreenActive(active: Boolean) {
        gameScreenActive = active
        updateTimer()
        if (!active) _ui.value.game?.let { game ->
            persist(game)
            _ui.update { synced(it, game) }
        }
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
        val before = s.game ?: return
        val settings = s.settings
        val checking = settings.instantCheck
        move { g ->
            if (s.notesMode) {
                g.toggleNote(cell, digit)
            } else {
                g.place(
                    cell = cell,
                    digit = digit,
                    mistakeLimit = if (settings.mistakeLimit && checking) MISTAKE_LIMIT else null,
                    autoRemoveNotes = settings.autoRemoveNotes,
                    checking = checking,
                )
            }
        }
        val after = _ui.value.game ?: return
        if (after == before) return
        // Finishing a group is only celebrated when it can't give away whether numbers are right.
        val completed = if (!s.notesMode && checking && after.isCorrect(cell)) {
            after.completedHouses(cell).flatMap { it.cells.toList() }.distinct()
        } else {
            emptyList()
        }
        val kind = when {
            s.notesMode -> FeedbackKind.NOTE
            completed.isNotEmpty() -> FeedbackKind.COMPLETE
            checking && after.isWrong(cell) -> FeedbackKind.WRONG
            else -> FeedbackKind.PLACE
        }
        _ui.update {
            it.copy(
                feedback = Feedback(++feedbackId, kind, completed),
                flagged = it.flagged - cell,
                checkMessage = null,
            )
        }
    }

    override fun erase() {
        val cell = _ui.value.selected ?: return
        val checking = _ui.value.settings.instantCheck
        move { it.erase(cell, checking) }
        _ui.update { it.copy(flagged = it.flagged - cell, checkMessage = null) }
    }

    /** Pencils every possible number into the empty cells. Undoable. */
    override fun fillCandidates() = move { it.withAllCandidates() }

    /** Marks the wrong numbers on the board, for players who check only when they ask. */
    override fun check() {
        val game = _ui.value.game ?: return
        if (game.status != GameStatus.PLAYING) return
        val wrong = (0 until 81).filter { game.isWrong(it) }.toSet()
        _ui.update {
            it.copy(
                flagged = wrong,
                checks = it.checks + 1,
                checkMessage = when (wrong.size) {
                    0 -> "Everything so far is right."
                    1 -> "1 number is wrong. It's marked in red."
                    else -> "${wrong.size} numbers are wrong. They're marked in red."
                },
            )
        }
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
        _ui.update { synced(it, counted).copy(game = counted, hint = HintUi(plan), selected = null) }
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
        persist(restored)
        _ui.update { synced(it, restored).copy(game = restored, canUndo = undoStack.isNotEmpty()) }
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
            val day = next.daily
            if (day != null) {
                repo.recordDailyWin(day, next.elapsedSeconds)
                refreshWidget()
                best = false
            } else if (next.isKiller) {
                best = repo.recordKillerWin(next.elapsedSeconds, next.hintsUsed)
            } else {
                best = repo.recordWin(next.difficulty, next.elapsedSeconds, next.hintsUsed)
            }
            undoStack.clear()
        }
        val newAchievements = if (next.status == GameStatus.WON) award(next) else emptyList()
        persist(next)
        _ui.update {
            synced(it, next).copy(
                game = next,
                canUndo = undoStack.isNotEmpty(),
                lastWinWasBest = best,
                stats = if (next.status == GameStatus.WON) repo.loadStats() else it.stats,
                killerStats = if (next.status == GameStatus.WON) repo.loadKillerStats() else it.killerStats,
                newAchievements = newAchievements,
                achievements = if (newAchievements.isEmpty()) it.achievements else repo.loadAchievements(),
            )
        }
        updateTimer()
    }

    /** [state] with the home screen's view of [game]'s slot brought up to date. */
    private fun synced(state: UiState, game: GameState): UiState =
        if (game.daily == null) {
            state.copy(savedGame = game.takeIf { it.status == GameStatus.PLAYING })
        } else {
            state.copy(daily = loadDaily())
        }

    /** Saves [game] to its slot. Finished games are cleared; daily wins live in the results. */
    private fun persist(game: GameState) {
        val keep = if (game.status == GameStatus.WON) null else game
        if (game.daily != null) repo.saveDailyGame(keep) else repo.saveGame(keep)
    }

    // --- Settings and stats ---------------------------------------------

    fun updateSettings(settings: Settings) {
        repo.saveSettings(settings)
        _ui.update { it.copy(settings = settings) }
    }

    /** Marks the lesson for [technique] finished, once its worked example reaches the end. */
    fun completeLesson(technique: String) {
        if (technique in _ui.value.lessonsDone) return
        repo.markLessonDone(technique)
        _ui.update { it.copy(lessonsDone = it.lessonsDone + technique) }
    }

    fun resetStats() {
        repo.resetStats()
        _ui.update { it.copy(stats = repo.loadStats(), killerStats = repo.loadKillerStats()) }
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
