package com.kiaijoe.sudoku.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.ui.icons.Lucide

/** One icon per level, used on the home tiles, the win screen and Progress. */
val Difficulty.icon: ImageVector
    get() = when (this) {
        Difficulty.EASY -> Lucide.Star
        Difficulty.MEDIUM -> Lucide.Sparkles
        Difficulty.HARD -> Lucide.Target
        Difficulty.EXPERT -> Lucide.Medal
    }

/** The mark cut into each achievement's seal. */
val Achievement.icon: ImageVector
    get() = when (this) {
        Achievement.FIRST_STAMP -> Lucide.Check
        Achievement.STEADY_HAND -> Lucide.Target
        Achievement.MASTER -> Lucide.Medal
        Achievement.FULL_LADDER -> Lucide.Layers2
        Achievement.ON_YOUR_OWN -> Lucide.Feather
        Achievement.CLEAN_INK -> Lucide.Eye
        Achievement.SWIFT_BRUSH -> Lucide.Zap
        Achievement.CAGE_BREAKER -> Lucide.SquareDashed
        Achievement.BLANK_PAGE -> Lucide.Crown
        Achievement.REGULAR -> Lucide.CalendarCheck
        Achievement.WEEK_OF_INK -> Lucide.Flame
        Achievement.MOON_CYCLE -> Lucide.Moon
    }
