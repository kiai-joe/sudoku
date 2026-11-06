package com.kiaijoe.sudoku.ui

import com.kiaijoe.sudoku.engine.Difficulty

/**
 * What the game screen can ask for. [SudokuViewModel] implements it; tests
 * and previews pass [GameActions.None] to draw the screen from a fixed state.
 */
interface GameActions {
    fun setGameScreenActive(active: Boolean)
    fun newGame(difficulty: Difficulty)
    fun newKiller(difficulty: Difficulty)
    fun restart()
    fun togglePause()
    fun select(cell: Int)
    fun input(digit: Int)
    fun erase()
    fun undo()
    fun toggleNotesMode()
    fun fillCandidates()
    fun check()
    fun hint()
    fun hintNext()
    fun hintPlace()
    fun hintEraseMistake()
    fun closeHint()
    fun hintMore()
    fun toggleSpotlight(digit: Int)
    fun mark(cell: Int)

    /** Does nothing; for screenshots and previews. */
    object None : GameActions {
        override fun setGameScreenActive(active: Boolean) = Unit
        override fun newGame(difficulty: Difficulty) = Unit
        override fun newKiller(difficulty: Difficulty) = Unit
        override fun restart() = Unit
        override fun togglePause() = Unit
        override fun select(cell: Int) = Unit
        override fun input(digit: Int) = Unit
        override fun erase() = Unit
        override fun undo() = Unit
        override fun toggleNotesMode() = Unit
        override fun fillCandidates() = Unit
        override fun check() = Unit
        override fun hint() = Unit
        override fun hintNext() = Unit
        override fun hintPlace() = Unit
        override fun hintEraseMistake() = Unit
        override fun closeHint() = Unit
        override fun hintMore() = Unit
        override fun toggleSpotlight(digit: Int) = Unit
        override fun mark(cell: Int) = Unit
    }
}
