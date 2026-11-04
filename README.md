# Sumi

An Android Sudoku game built with Kotlin and Jetpack Compose.

Sumi is the black ink of Japanese calligraphy. Printed clues are set in sumi black on washi-grey paper, your own numbers in indigo, the selected cell in gold, and mistakes in vermilion, the colour calligraphy teachers correct work in. The launcher icon is a vermilion hanko seal cut as a 3×3 grid. Text is set in [Lexend](https://www.lexend.com/), licensed under the SIL Open Font License (see `licenses/Lexend-OFL.txt`).

## Features

- Four levels: Easy (40 clues), Medium (32), Hard (27), Expert (23). Every puzzle is generated on the device and has exactly one solution.
- Pencil notes, with matching notes cleared automatically when you place a number.
- Highlighting for the selected cell's row, column, box and matching numbers.
- Mistake limit of 3, which you can turn off in Settings.
- Undo, erase and hints.
- Timer that pauses when you leave the app, plus a manual pause that hides the board.
- The current game is saved after every move and can be resumed.
- Statistics per level: games played, wins, win rate and best time.
- Light and dark themes in the Sumi palette.

## Getting the APK

Every push and pull request runs the **Android build** workflow, which runs the unit tests and builds a debug APK. Open the workflow run in the **Actions** tab and download `sudoku-debug-apk` from the run's artifacts. Install it on a phone with "Install unknown apps" allowed.

## Building locally

Requirements: Android Studio (Ladybug or newer) or JDK 17 with the Android SDK.

```bash
./gradlew testDebugUnitTest   # engine tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/java/com/kiaijoe/sudoku/
├── engine/        Pure Kotlin game logic, no Android dependencies
│   ├── Grid.kt        Cell indexing, peers, solution check
│   ├── Solver.kt      Bitmask backtracking solver and solution counter
│   ├── Generator.kt   Unique-solution puzzle generator and difficulty levels
│   ├── GameState.kt   Immutable game state and moves
│   └── GameCodec.kt   Save format
├── data/          SharedPreferences storage for the game, settings and stats
└── ui/            ViewModel, theme, board, controls and screens
```

The engine has unit tests in `app/src/test`.

Difficulty is currently set by the number of clues. A later step would be to grade puzzles by the solving techniques they need.
