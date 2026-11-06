package com.kiaijoe.sudoku

import android.content.Intent
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
import com.kiaijoe.sudoku.ui.components.Tab
import com.kiaijoe.sudoku.ui.screens.CalendarScreen
import com.kiaijoe.sudoku.ui.screens.CombinationsScreen
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.screens.HomeScreen
import com.kiaijoe.sudoku.ui.screens.LearnScreen
import com.kiaijoe.sudoku.ui.screens.LessonScreen
import com.kiaijoe.sudoku.ui.screens.SettingsScreen
import com.kiaijoe.sudoku.ui.screens.StatsScreen
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.SudokuTheme
import com.kiaijoe.sudoku.ui.theme.isDark
import com.kiaijoe.sudoku.ui.theme.resolve
import com.kiaijoe.sudoku.widget.SumiWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    /** Goes up by one each time the widget asks to open today's daily puzzle. */
    private var openDailyRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch, not one restored after rotation, acts on the widget's request.
        if (savedInstanceState == null && intent.wantsDaily()) openDailyRequests++
        setContent { SudokuApp(openDailyRequests = openDailyRequests) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.wantsDaily()) openDailyRequests++
    }

    private fun Intent?.wantsDaily() = this?.getBooleanExtra(SumiWidget.EXTRA_OPEN_DAILY, false) == true
}

private enum class Screen { Home, Game, Stats, Calendar, Learn, Lesson, Settings, Combinations }

/** The screen each tab of the floating bar opens. */
private val Tab.screen: Screen
    get() = when (this) {
        Tab.Home -> Screen.Home
        Tab.Daily -> Screen.Calendar
        Tab.Learn -> Screen.Learn
        Tab.Progress -> Screen.Stats
    }

@Composable
private fun SudokuApp(openDailyRequests: Int, viewModel: SudokuViewModel = viewModel()) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var lessonIndex by rememberSaveable { mutableIntStateOf(0) }
    var calendarKiller by rememberSaveable { mutableStateOf(false) }

    // The widget's Play or Continue: straight into today's puzzle, or to the
    // Daily tab if it was solved since the widget last refreshed.
    LaunchedEffect(openDailyRequests) {
        if (openDailyRequests == 0) return@LaunchedEffect
        viewModel.playDaily()
        calendarKiller = false
        screen = if (viewModel.ui.value.daily.solvedToday) Screen.Calendar else Screen.Game
    }

    val dark = state.settings.darkMode.resolve(isSystemInDarkTheme())
    SudokuTheme(darkTheme = dark, theme = state.settings.theme) {
        SystemBars(state.settings.theme, dark)

        // The date may have changed since the app was last on the home screen.
        LaunchedEffect(screen) { if (screen == Screen.Home || screen == Screen.Calendar) viewModel.refreshDaily() }

        val onTab: (Tab) -> Unit = { screen = it.screen }
        val playDaily = { killer: Boolean ->
            viewModel.playDaily(killer)
            screen = Screen.Game
        }

        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                onContinue = {
                    viewModel.continueGame()
                    screen = Screen.Game
                },
                onPlayDaily = { playDaily(false) },
                onCalendar = {
                    calendarKiller = false
                    screen = Screen.Calendar
                },
                onPlayKillerDaily = { playDaily(true) },
                onKillerCalendar = {
                    calendarKiller = true
                    screen = Screen.Calendar
                },
                onNewGame = { difficulty ->
                    viewModel.newGame(difficulty)
                    screen = Screen.Game
                },
                onNewKiller = { difficulty ->
                    viewModel.newKiller(difficulty)
                    screen = Screen.Game
                },
                onSettings = { screen = Screen.Settings },
                onTab = onTab,
            )
            Screen.Game -> GameScreen(
                state = state,
                actions = viewModel,
                onExit = { screen = Screen.Home },
            )
            Screen.Settings -> SettingsScreen(
                settings = state.settings,
                onChange = viewModel::updateSettings,
                onBack = { screen = Screen.Home },
                onExportBackup = viewModel::exportBackup,
                onRestoreBackup = viewModel::restoreBackup,
            )
            Screen.Learn -> LearnScreen(
                onOpen = {
                    lessonIndex = it
                    screen = Screen.Lesson
                },
                onBack = { screen = Screen.Home },
                onTab = onTab,
                done = state.lessonsDone,
                onCombinations = { screen = Screen.Combinations },
            )
            Screen.Combinations -> CombinationsScreen(onBack = { screen = Screen.Learn })
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
                    onComplete = { viewModel.completeLesson(Tutorial.lessons[lessonIndex].id) },
                    onPractice = if (Tutorial.hasPractice(Tutorial.lessons[lessonIndex])) {
                        {
                            viewModel.practice(Tutorial.lessons[lessonIndex])
                            screen = Screen.Game
                        }
                    } else {
                        null
                    },
                    practiceReplacesGame = state.canContinue,
                )
            }
            Screen.Calendar -> CalendarScreen(
                daily = state.daily,
                onBack = { screen = Screen.Home },
                onPlayDaily = playDaily,
                onTab = onTab,
                killerDaily = state.killerDaily,
                showKiller = calendarKiller,
                onShowKiller = { calendarKiller = it },
            )
            Screen.Stats -> StatsScreen(
                stats = state.stats,
                killerStats = state.killerStats,
                achievements = state.achievements,
                onReset = viewModel::resetStats,
                onBack = { screen = Screen.Home },
                onTab = onTab,
            )
        }
    }
}

/** Light or dark status and navigation bar icons to match the chosen palette. */
@Composable
private fun SystemBars(theme: InkTheme, darkMode: Boolean) {
    val dark = theme.isDark(darkMode)
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
