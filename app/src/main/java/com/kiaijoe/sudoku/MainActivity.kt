package com.kiaijoe.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiaijoe.sudoku.ui.SudokuViewModel
import com.kiaijoe.sudoku.ui.screens.GameScreen
import com.kiaijoe.sudoku.ui.screens.HomeScreen
import com.kiaijoe.sudoku.ui.screens.StatsScreen
import com.kiaijoe.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                SudokuApp()
            }
        }
    }
}

private enum class Screen { Home, Game, Stats }

@Composable
private fun SudokuApp(viewModel: SudokuViewModel = viewModel()) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    when (screen) {
        Screen.Home -> HomeScreen(
            state = state,
            onContinue = { screen = Screen.Game },
            onNewGame = { difficulty ->
                viewModel.newGame(difficulty)
                screen = Screen.Game
            },
            onStats = { screen = Screen.Stats },
            onSettingsChange = viewModel::updateSettings,
        )
        Screen.Game -> GameScreen(
            state = state,
            actions = viewModel,
            onExit = { screen = Screen.Home },
        )
        Screen.Stats -> StatsScreen(
            stats = state.stats,
            onReset = viewModel::resetStats,
            onBack = { screen = Screen.Home },
        )
    }
}
