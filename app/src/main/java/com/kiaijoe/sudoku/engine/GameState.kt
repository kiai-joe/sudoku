package com.kiaijoe.sudoku.engine

enum class GameStatus { PLAYING, WON, LOST }

/**
 * Immutable snapshot of a game. Every move returns a new state, which keeps
 * undo, saving and Compose recomposition simple.
 *
 * [notes] holds one 9-bit mask per cell: bit (d - 1) is set when pencil mark d is shown.
 * [hardest] is the hardest technique the puzzle needs, when known.
 * [daily] is the epoch day of the daily puzzle this game is, or null for a regular game.
 * [cages] are the Killer Sudoku cages, or empty for a classic game.
 */
data class GameState(
    val difficulty: Difficulty,
    val givens: List<Int>,
    val solution: List<Int>,
    val values: List<Int>,
    val notes: List<Int>,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val hardest: Technique? = null,
    val daily: Long? = null,
    val cages: List<Cage> = emptyList(),
) {
    val isKiller: Boolean get() = cages.isNotEmpty()

    fun isGiven(cell: Int) = givens[cell] != 0
    fun isWrong(cell: Int) = values[cell] != 0 && values[cell] != solution[cell]
    fun isCorrect(cell: Int) = values[cell] != 0 && values[cell] == solution[cell]
    fun hasNote(cell: Int, digit: Int) = notes[cell] and bit(digit) != 0

    /**
     * How many more times [digit] has to be placed. With [checking] off,
     * every placed copy counts, so the number pad gives nothing away.
     */
    fun remaining(digit: Int, checking: Boolean = true): Int =
        9 - values.indices.count { values[it] == digit && (!checking || isCorrect(it)) }

    val isSolved: Boolean get() = values == solution

    /**
     * Places [digit] in [cell]. A wrong digit counts as a mistake; when
     * [mistakeLimit] is reached the game is lost. Correct cells are locked.
     *
     * With [checking] off (the player checks only when they ask), nothing
     * reveals whether a number is right: no mistakes are counted, the game
     * can't be lost, any non-given cell can be changed, and notes are cleared
     * for every placed number, right or wrong.
     */
    fun place(
        cell: Int,
        digit: Int,
        mistakeLimit: Int?,
        autoRemoveNotes: Boolean,
        checking: Boolean = true,
    ): GameState {
        require(digit in 1..9)
        if (status != GameStatus.PLAYING || isGiven(cell) || values[cell] == digit) return this
        if (checking && isCorrect(cell)) return this

        val newValues = values.toMutableList().also { it[cell] = digit }
        val newNotes = notes.toMutableList().also { it[cell] = 0 }
        val correct = digit == solution[cell]
        if ((correct || !checking) && autoRemoveNotes) {
            for (peer in Grid.peers[cell]) newNotes[peer] = newNotes[peer] and bit(digit).inv()
        }
        val newMistakes = if (correct || !checking) mistakes else mistakes + 1
        val next = copy(values = newValues, notes = newNotes, mistakes = newMistakes)
        return next.copy(
            status = when {
                next.isSolved -> GameStatus.WON
                checking && mistakeLimit != null && newMistakes >= mistakeLimit -> GameStatus.LOST
                else -> GameStatus.PLAYING
            },
        )
    }

    /** Adds or removes pencil mark [digit] in an empty cell. */
    fun toggleNote(cell: Int, digit: Int): GameState {
        require(digit in 1..9)
        if (status != GameStatus.PLAYING || isGiven(cell) || values[cell] != 0) return this
        return copy(notes = notes.toMutableList().also { it[cell] = it[cell] xor bit(digit) })
    }

    /**
     * Clears a value or the notes in [cell]. Givens stay, and so do correct
     * values unless [checking] is off.
     */
    fun erase(cell: Int, checking: Boolean = true): GameState {
        if (status != GameStatus.PLAYING || isGiven(cell) || (checking && isCorrect(cell))) return this
        if (values[cell] == 0 && notes[cell] == 0) return this
        return copy(
            values = values.toMutableList().also { it[cell] = 0 },
            notes = notes.toMutableList().also { it[cell] = 0 },
        )
    }

    /**
     * Pencils every possible number into each empty cell: the digits not
     * already in its row, column or box. Uses the numbers on the board as
     * they stand, so it reveals nothing about which are right.
     */
    fun withAllCandidates(): GameState {
        if (status != GameStatus.PLAYING) return this
        val newNotes = notes.toMutableList()
        for (cell in 0 until Grid.CELLS) {
            if (values[cell] != 0) continue
            var mask = Grid.ALL_DIGITS
            for (peer in Grid.peers[cell]) {
                val v = values[peer]
                if (v != 0) mask = mask and bit(v).inv()
            }
            newNotes[cell] = mask
        }
        return copy(notes = newNotes)
    }

    /** The row, column and box through [cell] that are now completely and correctly filled. */
    fun completedHouses(cell: Int): List<House> =
        listOf(House.ROWS[Grid.row(cell)], House.COLUMNS[Grid.col(cell)], House.BOXES[Grid.box(cell)])
            .filter { house -> house.cells.all { isCorrect(it) } }

    /** Back to the starting grid. Time, mistakes and hints are reset. */
    fun restart(): GameState = copy(
        values = givens,
        notes = List(Grid.CELLS) { 0 },
        mistakes = 0,
        hintsUsed = 0,
        elapsedSeconds = 0,
        status = GameStatus.PLAYING,
    )

    companion object {
        fun bit(digit: Int) = 1 shl (digit - 1)

        fun from(puzzle: Puzzle) = GameState(
            difficulty = puzzle.difficulty,
            givens = puzzle.givens.toList(),
            solution = puzzle.solution.toList(),
            values = puzzle.givens.toList(),
            notes = List(Grid.CELLS) { 0 },
            hardest = puzzle.hardest,
            cages = puzzle.cages,
        )
    }
}
