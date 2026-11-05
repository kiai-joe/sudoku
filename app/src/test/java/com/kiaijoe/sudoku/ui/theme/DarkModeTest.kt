package com.kiaijoe.sudoku.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkModeTest {

    @Test
    fun systemFollowsThePhoneAndTheOthersHold() {
        assertEquals(true, DarkMode.SYSTEM.resolve(systemDark = true))
        assertEquals(false, DarkMode.SYSTEM.resolve(systemDark = false))
        for (phone in listOf(true, false)) {
            assertEquals(false, DarkMode.LIGHT.resolve(phone))
            assertEquals(true, DarkMode.DARK.resolve(phone))
        }
    }

    @Test
    fun indigoNightStaysDarkEvenInLight() {
        assertTrue(InkTheme.INDIGO_NIGHT.isDark(DarkMode.LIGHT.resolve(systemDark = false)))
    }
}
