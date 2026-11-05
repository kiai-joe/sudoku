package com.kiaijoe.sudoku.engine

import java.util.Locale

/**
 * The languages Sumi speaks: English and Swahili. Every piece of text is
 * written as a pair with [tr], English first, and the phone's language picks
 * one. Pairs sit side by side in the code, so a translation is reviewed next
 * to the English it comes from, and the engine (hints, lessons, achievements)
 * is translated the same way as the screens.
 */
enum class Lang {
    EN, SW;

    companion object {
        /** Overrides the phone's language; for tests and screenshots. */
        @Volatile
        var forced: Lang? = null

        val current: Lang get() = forced ?: of(Locale.getDefault())

        fun of(locale: Locale): Lang = if (locale.language == "sw") SW else EN
    }
}

/** [en] or [sw], whichever the phone's language is (English for anything but Swahili). */
fun tr(en: String, sw: String): String = if (Lang.current == Lang.SW) sw else en
