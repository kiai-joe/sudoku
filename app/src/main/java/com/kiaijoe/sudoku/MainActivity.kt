package com.kiaijoe.sudoku

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiaijoe.sudoku.engine.Example
import com.kiaijoe.sudoku.engine.Tutorial
import com.kiaijoe.sudoku.ui.SudokuViewModel
import com.kiaijoe.sudoku.ui.screens.CalendarScreen
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.screens.HomeScreen
import com.kiaijoe.sudoku.ui.screens.LearnScreen
import com.kiaijoe.sudoku.ui.screens.LessonScreen
import com.kiaijoe.sudoku.ui.screens.StatsScreen
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import com.kiaijoe.sudoku.ui.theme.isDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SudokuApp() }
    }
}

private enum class Screen { Home, Game, Stats, Calendar, Learn, Lesson }

@Composable
private fun SudokuApp(viewModel: SudokuViewModel = viewModel()) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var lessonIndex by rememberSaveable { mutableIntStateOf(0) }

    SudokuTheme(theme = state.settings.theme) {
        SystemBars(state.settings.theme)

        // The date may have changed since the app was last on the home screen.
        LaunchedEffect(screen) { if (screen == Screen.Home) viewModel.refreshDaily() }

        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                onContinue = {
                    viewModel.continueGame()
                    screen = Screen.Game
                },
                onPlayDaily = {
                    viewModel.playDaily()
                    screen = Screen.Game
                },
                onCalendar = { screen = Screen.Calendar },
                onNewGame = { difficulty ->
                    viewModel.newGame(difficulty)
                    screen = Screen.Game
                },
                onStats = { screen = Screen.Stats },
                onLearn = { screen = Screen.Learn },
                onSettingsChange = viewModel::updateSettings,
            )
            Screen.Game -> GameScreen(
                state = state,
                actions = viewModel,
                onExit = { screen = Screen.Home },
            )
            Screen.Learn -> LearnScreen(
                onOpen = {
                    lessonIndex = it
                    screen = Screen.Lesson
                },
                onBack = { screen = Screen.Home },
            )
            Screen.Lesson -> {
                // Building an example replays a real puzzle, so do it off the main thread.
                val example by produceState<Example?>(null, lessonIndex) {
                    value = withContext(Dispatchers.Default) { Tutorial.example(Tutorial.lessons[lessonIndex]) }
                }
                val hasExample = Tutorial.lessons[lessonIndex].seed != null
                LessonScreen(
                    index = lessonIndex,
                    example = example,
                    loading = hasExample && example == null,
                    onNextLesson = if (lessonIndex < Tutorial.lessons.lastIndex) {
                        { lessonIndex++ }
                    } else {
                        null
                    },
                    onBack = { screen = Screen.Learn },
                )
            }
            Screen.Calendar -> CalendarScreen(
                daily = state.daily,
                onBack = { screen = Screen.Home },
            )
            Screen.Stats -> StatsScreen(
                stats = state.stats,
                onReset = viewModel::resetStats,
                onBack = { screen = Screen.Home },
            )
        }
    }
}

/** Light or dark status and navigation bar icons to match the chosen palette. */
@Composable
private fun SystemBars(theme: InkTheme) {
    val dark = theme.isDark(isSystemInDarkTheme())
    val activity = LocalContext.current as? ComponentActivity ?: return
    LaunchedEffect(dark) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
