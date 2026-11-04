package com.kiaijoe.sudoku.engine

object TestGrids {
    val PUZZLE = parse(
        "530070000" +
            "600195000" +
            "098000060" +
            "800060003" +
            "400803001" +
            "700020006" +
            "060000280" +
            "000419005" +
            "000080079",
    )

    val SOLUTION = parse(
        "534678912" +
            "672195348" +
            "198342567" +
            "859761423" +
            "426853791" +
            "713924856" +
            "961537284" +
            "287419635" +
            "345286179",
    )

    fun parse(s: String) = IntArray(81) { s[it].digitToInt() }

    fun game() = GameState.from(Puzzle(PUZZLE, SOLUTION, Difficulty.EASY))
}
