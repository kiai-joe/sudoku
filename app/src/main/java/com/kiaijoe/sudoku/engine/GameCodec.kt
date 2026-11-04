package com.kiaijoe.sudoku.engine

/** Compact text format for saving a game, so no serialization library is needed. */
object GameCodec {
    private const val VERSION = "v1"

    fun encode(state: GameState): String = listOf(
        VERSION,
        state.difficulty.name,
        state.givens.joinToString(""),
        state.solution.joinToString(""),
        state.values.joinToString(""),
        state.notes.joinToString(","),
        state.mistakes.toString(),
        state.hintsUsed.toString(),
        state.elapsedSeconds.toString(),
        state.status.name,
        state.hardest?.name ?: "-",
        state.daily?.toString() ?: "-",
        if (state.cages.isEmpty()) "-" else state.cages.joinToString(";") { "${it.sum}:${it.cells.joinToString(".")}" },
    ).joinToString("|")

    /**
     * Returns null for anything that isn't a well-formed save. Older saves
     * have 10 fields (before grading), 11 (before daily puzzles) or 12 (before Killer).
     */
    fun decode(text: String): GameState? = runCatching {
        val parts = text.split("|")
        if (parts.size !in 10..13 || parts[0] != VERSION) return null
        val givens = digits(parts[2])
        val solution = digits(parts[3])
        val values = digits(parts[4])
        val notes = parts[5].split(",").map { it.toInt() }
        if (notes.size != Grid.CELLS || notes.any { it !in 0..Grid.ALL_DIGITS }) return null
        if (!Grid.isValidSolution(solution.toIntArray())) return null
        GameState(
            difficulty = Difficulty.valueOf(parts[1]),
            givens = givens,
            solution = solution,
            values = values,
            notes = notes,
            mistakes = parts[6].toInt(),
            hintsUsed = parts[7].toInt(),
            elapsedSeconds = parts[8].toLong(),
            status = GameStatus.valueOf(parts[9]),
            hardest = parts.getOrNull(10)?.takeIf { it != "-" }?.let { Technique.valueOf(it) },
            daily = parts.getOrNull(11)?.takeIf { it != "-" }?.toLong(),
            cages = parts.getOrNull(12)?.takeIf { it != "-" }?.let(::cages).orEmpty(),
        )
    }.getOrNull()

    private fun cages(s: String): List<Cage> = s.split(";").map { cage ->
        val (sum, cells) = cage.split(":")
        Cage(sum.toInt(), cells.split(".").map { it.toInt().also { c -> require(c in 0 until Grid.CELLS) } })
    }

    private fun digits(s: String): List<Int> {
        require(s.length == Grid.CELLS)
        return s.map { ch -> ch.digitToInt().also { require(it in 0..9) } }
    }
}
