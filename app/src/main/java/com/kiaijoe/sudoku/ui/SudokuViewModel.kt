package com.kiaijoe.sudoku.ui

import com.kiaijoe.sudoku.engine.tr
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
import com.kiaijoe.sudoku.engine.Lesson
import com.kiaijoe.sudoku.engine.Progress
import com.kiaijoe.sudoku.engine.Puzzle
import com.kiaijoe.sudoku.engine.Variant
import com.kiaijoe.sudoku.engine.Technique
import com.kiaijoe.sudoku.engine.Tutorial
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
 * game in progress, which the home screen offers to continue. [daily] is the
 * classic daily puzzle and [killerDaily] the Killer one.
 */
data class UiState(
    val game: GameState? = null,
    val savedGame: GameState? = null,
    val daily: DailyUi = DailyUi(),
    val killerDaily: DailyUi = DailyUi(killer = true),
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    val generating: Boolean = false,
    val canUndo: Boolean = false,
    val lastWinWasBest: Boolean = false,
    val hint: HintUi? = null,
    val feedback: Feedback? = null,
    val flagged: Set<Int> = emptySet(),
    val checkMessage: String? = null,
    val settings: Settings = Settings(),
    val stats: Map<Difficulty, DifficultyStats> = emptyMap(),
    val killerStats: DifficultyStats = DifficultyStats(),
    val variantStats: Map<Variant, DifficultyStats> = emptyMap(),
    val achievements: Map<Achievement, Long> = emptyMap(),
    val newAchievements: List<Achievement> = emptyList(),
    /** Lessons finished, by lesson id (the technique name for whole-technique lessons). */
    val lessonsDone: Set<String> = emptySet(),
    /** The number whose possible cells are lit up (long-press on its key), or null. */
    val spotlight: Int? = null,
) {
    val canContinue: Boolean get() = savedGame?.status == GameStatus.PLAYING && !generating
}

/**
 * Today's daily puzzle, classic or Killer, and the record of past ones.
 *
 * @property today the current epoch day.
 * @property results solved days (epoch day to seconds).
 * @property inProgress today's daily game if started and not finished.
 * @property killer whether this is the daily Killer, which has its own record and streak.
 */
data class DailyUi(
    val today: Long = 0,
    val results: Map<Long, Long> = emptyMap(),
    val inProgress: GameState? = null,
    val killer: Boolean = false,
) {
    val difficulty: Difficulty get() = Daily.difficultyFor(today, killer)
    val label: String get() = Daily.label(today, killer)
    val solvedToday: Boolean get() = today in results
    val todaySeconds: Long? get() = results[today]
    val streak: Int get() = Daily.streak(results.keys, today)
    val longestStreak: Int get() = Daily.longestStreak(results.keys)
}

/**
 * What a vibration is for: a placed number, a pencil mark, a wrong number, a
 * finished row, column or box, a solved puzzle, a game lost to mistakes, or
 * a step through a hint.
 */
enum class FeedbackKind { PLACE, NOTE, WRONG, COMPLETE, WIN, LOSE, HINT_STEP }

/**
 * What the last input did, for a vibration and, when a row, column or box
 * was finished, the ink wash over [completed]. [id] changes on every input.
 */
data class Feedback(val id: Long, val kind: FeedbackKind, val completed: List<Int> = emptyList())

/**
 * An open hint: the plan, which of its cards is showing, and how much of it
 * is revealed: a [NUDGE] says where to look, [TECHNIQUE] names the step and
 * shows its cells, [FULL] explains every step.
 */
data class HintUi(val plan: HintPlan, val index: Int = 0, val level: Int = FULL) {
    companion object {
        const val NUDGE = 1
        const val TECHNIQUE = 2
        const val FULL = 3
    }

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
            daily = loadDaily(killer = false),
            killerDaily = loadDaily(killer = true),
            settings = repo.loadSettings(),
            stats = repo.loadStats(),
            killerStats = repo.loadKillerStats(),
            variantStats = repo.loadVariantStats(),
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
        BackupResult(ok = true, message = tr(
            "Restored. Your games, stats and settings are back.",
            "Imerejeshwa. Michezo, takwimu na mipangilio yako imerudi.",
        ))
    } catch (e: BackupException) {
        BackupResult(ok = false, message = e.message ?: tr("This backup couldn't be restored.", "Nakala hii haikuweza kurejeshwa."))
    }

    /** What the saved statistics add up to, for achievements. */
    private fun progress(): Progress {
        val daily = repo.loadDailyResults()
        return Progress(
            won = repo.loadStats().mapValues { it.value.won },
            killerWon = repo.loadKillerStats().won,
            variantWon = repo.loadVariantStats().values.sumOf { it.won },
            dailySolved = daily.size,
            longestStreak = Daily.longestStreak(daily.keys),
            dailyDays = daily.keys,
            killerDailyDays = repo.loadDailyResults(killer = true).keys,
            lessonsDone = repo.loadLessonsDone().size,
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

    /** Starts a Sudoku X or Jigsaw game. It takes the regular game's slot, like any new game. */
    override fun newVariant(difficulty: Difficulty, variant: Variant) {
        startLoading()
        viewModelScope.launch {
            val game = GameState.from(withContext(Dispatchers.Default) { Generator.generate(difficulty, variant = variant) })
            repo.recordVariantStart(variant)
            repo.saveGame(game)
            show(game, savedGame = game)
        }
    }

    /** Plays a puzzle the player typed in and [CustomPuzzle] checked. It takes the regular game's slot; it isn't in the stats. */
    fun playCustom(puzzle: Puzzle) {
        val game = GameState.from(puzzle).copy(custom = true)
        repo.saveGame(game)
        show(game, savedGame = game)
    }

    /**
     * Starts a practice game for [lesson]: a fresh puzzle at its level that
     * needs its technique. It takes the regular game's slot and counts in the
     * level's statistics, like any new game.
     */
    fun practice(lesson: Lesson) {
        startLoading()
        viewModelScope.launch {
            val practice = withContext(Dispatchers.Default) { Tutorial.practice(lesson) }
            val game = GameState.from(practice.puzzle).copy(practice = lesson.technique.takeIf { practice.usesTechnique })
            if (lesson.killer) repo.recordKillerStart() else repo.recordStart(lesson.level)
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

    /** Opens today's daily puzzle, or the daily Killer when [killer], resuming it if already started. */
    fun playDaily(killer: Boolean = false) {
        refreshDaily()
        val daily = if (killer) _ui.value.killerDaily else _ui.value.daily
        if (daily.solvedToday) return
        daily.inProgress?.let {
            show(it, savedGame = _ui.value.savedGame)
            return
        }
        startLoading()
        viewModelScope.launch {
            val game = withContext(Dispatchers.Default) { Daily.gameFor(daily.today, killer) }
            repo.saveDailyGame(game, killer)
            show(game, savedGame = _ui.value.savedGame)
            refreshDaily()
            refreshWidget()
        }
    }

    /** Re-reads today's date and both dailies' progress, in case the day has changed. */
    fun refreshDaily() = _ui.update { it.copy(daily = loadDaily(killer = false), killerDaily = loadDaily(killer = true)) }

    /** Brings the home-screen widget up to date with the daily record. */
    private fun refreshWidget() = SumiWidget.refresh(getApplication<Application>())

    private fun loadDaily(killer: Boolean): DailyUi {
        val today = LocalDate.now().toEpochDay()
        val inProgress = repo.loadDailyGame(killer)?.takeIf {
            // Before the daily Killer, Saturday's classic daily was a Killer; such a game isn't today's either way.
            it.daily == today && it.status == GameStatus.PLAYING && it.isKiller == killer
        }
        return DailyUi(today = today, results = repo.loadDailyResults(killer), inProgress = inProgress, killer = killer)
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
                checkMessage = null,
                feedback = null,
                lastWinWasBest = false,
                newAchievements = emptyList(),
                spotlight = null,
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
                    mistakeLimit = if (settings.mistakeLimit && checking && !settings.zen) MISTAKE_LIMIT else null,
                    autoRemoveNotes = settings.autoRemoveNotes,
                    checking = checking,
                )
            }
        }
        val after = _ui.value.game ?: return
        if (after == before) return
        // A win or loss has its own vibration, set by move().
        if (after.status != GameStatus.PLAYING) return
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
        val counted = game.copy(checks = game.checks + 1)
        persist(counted)
        _ui.update {
            synced(it, counted).copy(
                game = counted,
                flagged = wrong,
                checkMessage = when (wrong.size) {
                    0 -> tr("Everything so far is right.", "Kila kitu hadi sasa ni sahihi.")
                    1 -> tr("1 number is wrong. It's marked in red.", "Namba 1 si sahihi. Imewekwa alama nyekundu.")
                    else -> tr(
                        "${wrong.size} numbers are wrong. They're marked in red.",
                        "Namba ${wrong.size} si sahihi. Zimewekwa alama nyekundu.",
                    )
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
        // Step by step, a hint starts with a nudge; a wrong number is always shown straight away.
        val level = if (s.settings.gentleHints && plan is HintPlan.Teach) HintUi.NUDGE else HintUi.FULL
        _ui.update { synced(it, counted).copy(game = counted, hint = HintUi(plan, level = level), selected = null, spotlight = null) }
    }

    /** Reveals more of the open hint: from the nudge to the technique, then the full explanation. */
    override fun hintMore() = _ui.update { s ->
        val h = s.hint ?: return@update s
        if (h.level >= HintUi.FULL) s else s.copy(hint = h.copy(level = h.level + 1), feedback = Feedback(++feedbackId, FeedbackKind.HINT_STEP))
    }

    /** Lights up every cell where [digit] can still go, or turns that off if it's already lit. */
    override fun toggleSpotlight(digit: Int) = _ui.update { s ->
        s.copy(spotlight = if (s.spotlight == digit) null else digit, feedback = Feedback(++feedbackId, FeedbackKind.NOTE))
    }

    /** Moves [cell]'s colour mark on: none, first colour, second colour, none. Not undoable; saved with the game. */
    override fun mark(cell: Int) {
        val s = _ui.value
        val game = s.game ?: return
        if (game.status != GameStatus.PLAYING || s.paused || s.generating) return
        val next = game.cycleMark(cell)
        persist(next)
        _ui.update { synced(it, next).copy(game = next, feedback = Feedback(++feedbackId, FeedbackKind.NOTE)) }
    }

    override fun hintNext() = _ui.update { s ->
        val h = s.hint ?: return@update s
        if (h.hasNext) {
            s.copy(hint = h.copy(index = h.index + 1), feedback = Feedback(++feedbackId, FeedbackKind.HINT_STEP))
        } else {
            s
        }
    }

    /** Places the number the hint's last step arrives at. */
    override fun hintPlace() {
        val card = _ui.value.hint?.card ?: return
        if (!card.isPlacement) return
        _ui.update { it.copy(hint = null, selected = card.placeCell) }
        move(hint = true) {
            it.place(card.placeCell, card.placeDigit, mistakeLimit = null, autoRemoveNotes = _ui.value.settings.autoRemoveNotes)
        }
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
        val restored = game.copy(values = previous.values, notes = previous.notes).logged(game)
        persist(restored)
        _ui.update { synced(it, restored).copy(game = restored, canUndo = undoStack.isNotEmpty()) }
    }

    /** Applies [transform] as one undoable move, logging its board changes for the replay ([hint] when a hint placed it). */
    private fun move(hint: Boolean = false, transform: (GameState) -> GameState) {
        val s = _ui.value
        val game = s.game ?: return
        if (game.status != GameStatus.PLAYING || s.paused || s.generating) return
        val next = transform(game).logged(game, hint)
        if (next == game) return

        undoStack.addLast(game)
        while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()

        var best = s.lastWinWasBest
        if (next.status == GameStatus.WON) {
            val day = next.daily
            if (day != null) {
                repo.recordDailyWin(day, next.elapsedSeconds, killer = next.isKiller)
                refreshWidget()
                best = false
            } else {
                // Zen mode wins count, untimed.
                val timed = !s.settings.zen
                best = when {
                    next.custom -> false
                    next.isKiller -> repo.recordKillerWin(next.elapsedSeconds, next.hintsUsed, timed)
                    next.variant != Variant.CLASSIC -> repo.recordVariantWin(next.variant, next.elapsedSeconds, next.hintsUsed, timed)
                    else -> repo.recordWin(next.difficulty, next.elapsedSeconds, next.hintsUsed, timed)
                }
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
                variantStats = if (next.status == GameStatus.WON) repo.loadVariantStats() else it.variantStats,
                newAchievements = newAchievements,
                achievements = if (newAchievements.isEmpty()) it.achievements else repo.loadAchievements(),
                feedback = when (next.status) {
                    GameStatus.WON -> Feedback(++feedbackId, FeedbackKind.WIN)
                    GameStatus.LOST -> Feedback(++feedbackId, FeedbackKind.LOSE)
                    GameStatus.PLAYING -> it.feedback
                },
            )
        }
        updateTimer()
    }

    /** [state] with the home screen's view of [game]'s slot brought up to date. */
    private fun synced(state: UiState, game: GameState): UiState =
        if (game.daily == null) {
            state.copy(savedGame = game.takeIf { it.status == GameStatus.PLAYING })
        } else {
            state.copy(daily = loadDaily(killer = false), killerDaily = loadDaily(killer = true))
        }

    /** Saves [game] to its slot. Finished games are cleared; daily wins live in the results. */
    private fun persist(game: GameState) {
        val keep = if (game.status == GameStatus.WON) null else game
        if (game.daily != null) repo.saveDailyGame(keep, killer = game.isKiller) else repo.saveGame(keep)
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
        award(won = null)
        _ui.update { it.copy(lessonsDone = it.lessonsDone + technique, achievements = repo.loadAchievements()) }
    }

    fun resetStats() {
        repo.resetStats()
        _ui.update { it.copy(stats = repo.loadStats(), killerStats = repo.loadKillerStats(), variantStats = repo.loadVariantStats()) }
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
