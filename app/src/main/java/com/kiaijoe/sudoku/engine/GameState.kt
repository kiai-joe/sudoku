package com.kiaijoe.sudoku.engine

import kotlin.random.Random

enum class GameStatus { PLAYING, WON, LOST }

/**
 * Immutable snapshot of a game. Every move returns a new state, which keeps
 * undo, saving and Compose recomposition simple.
 *
 * [notes] holds one 9-bit mask per cell: bit (d - 1) is set when pencil mark d is shown.
 * [hardest] is the hardest technique the puzzle needs, when known.
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
) {
    fun isGiven(cell: Int) = givens[cell] != 0
    fun isWrong(cell: Int) = values[cell] != 0 && values[cell] != solution[cell]
    fun isCorrect(cell: Int) = values[cell] != 0 && values[cell] == solution[cell]
    fun hasNote(cell: Int, digit: Int) = notes[cell] and bit(digit) != 0

    /** How many more times [digit] still has to be placed correctly. */
    fun remaining(digit: Int): Int = 9 - values.indices.count { values[it] == digit && isCorrect(it) }

    val isSolved: Boolean get() = values == solution

    /**
     * Places [digit] in [cell]. A wrong digit counts as a mistake; when
     * [mistakeLimit] is reached the game is lost. Correct cells are locked.
     */
    fun place(cell: Int, digit: Int, mistakeLimit: Int?, autoRemoveNotes: Boolean): GameState {
        require(digit in 1..9)
        if (status != GameStatus.PLAYING || isGiven(cell) || isCorrect(cell) || values[cell] == digit) return this

        val newValues = values.toMutableList().also { it[cell] = digit }
        val newNotes = notes.toMutableList().also { it[cell] = 0 }
        val correct = digit == solution[cell]
        if (correct && autoRemoveNotes) {
            for (peer in Grid.peers[cell]) newNotes[peer] = newNotes[peer] and bit(digit).inv()
        }
        val newMistakes = if (correct) mistakes else mistakes + 1
        val next = copy(values = newValues, notes = newNotes, mistakes = newMistakes)
        return next.copy(
            status = when {
                next.isSolved -> GameStatus.WON
                mistakeLimit != null && newMistakes >= mistakeLimit -> GameStatus.LOST
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

    /** Clears a wrong value or the notes in [cell]. Givens and correct values stay. */
    fun erase(cell: Int): GameState {
        if (status != GameStatus.PLAYING || isGiven(cell) || isCorrect(cell)) return this
        if (values[cell] == 0 && notes[cell] == 0) return this
        return copy(
            values = values.toMutableList().also { it[cell] = 0 },
            notes = notes.toMutableList().also { it[cell] = 0 },
        )
    }

    /**
     * Which cell a hint should fill: [preferred] if it still needs a value,
     * otherwise a random unsolved cell. Null when nothing is left to fill.
     */
    fun hintTarget(preferred: Int?, random: Random = Random.Default): Int? {
        if (status != GameStatus.PLAYING) return null
        if (preferred != null && !isGiven(preferred) && !isCorrect(preferred)) return preferred
        val open = values.indices.filter { !isCorrect(it) && !isGiven(it) }
        return if (open.isEmpty()) null else open.random(random)
    }

    /** Fills [cell] with its solution value without counting a mistake. */
    fun reveal(cell: Int, autoRemoveNotes: Boolean): GameState {
        if (status != GameStatus.PLAYING || isGiven(cell) || isCorrect(cell)) return this
        val digit = solution[cell]
        val placed = place(cell, digit, mistakeLimit = null, autoRemoveNotes = autoRemoveNotes)
        return placed.copy(hintsUsed = hintsUsed + 1)
    }

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
        )
    }
}
