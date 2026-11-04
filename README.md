# Sumi

An Android Sudoku game built with Kotlin and Jetpack Compose.

Sumi is the black ink of Japanese calligraphy. Printed clues are set in sumi black on washi-grey paper, your own numbers in indigo, the selected cell in gold, and mistakes in vermilion, the colour calligraphy teachers correct work in. The launcher icon is a vermilion hanko seal cut as a 3×3 grid. Text is set in [Lexend](https://www.lexend.com/), licensed under the SIL Open Font License (see `licenses/Lexend-OFL.txt`).

## Features

- Four levels graded by the hardest solving technique a puzzle needs, not by clue count: Easy (singles only), Medium (pointing, box/line reduction, pairs, naked triples), Hard (hidden triples, X-Wing, XY-Wing, naked quads) and Expert (Swordfish, XYZ-Wing, hidden quads). Every puzzle is generated on the device, has exactly one solution and can be solved by logic without guessing.
- Pencil notes, with matching notes cleared automatically when you place a number.
- Highlighting for the selected cell's row, column, box and matching numbers.
- Mistake limit of 3, which you can turn off in Settings.
- Undo, erase and hints.
- Timer that pauses when you leave the app, plus a manual pause that hides the board.
- The current game is saved after every move and can be resumed.
- Statistics per level: games played, wins, win rate and best time.
- Light and dark themes in the Sumi palette.

## Install on your phone

On your Android phone, open
**https://github.com/kiai-joe/sudoku/releases/latest/download/Sumi.apk**
and install it, allowing "Install unknown apps" for your browser when asked. Newer versions install over older ones and keep your saved game and stats.

## Releases

The **Release APK** workflow publishes a GitHub release with `Sumi.apk` attached:

- every push to `main` makes a `build-N` release
- pushing a tag such as `v0.2.0` makes a release with that version name

```bash
git tag v0.2.0
```

```bash
git push origin v0.2.0
```

APKs are signed with the debug key in `app/signing/debug.keystore`, so every build can update the last one. This key is not secret; publishing on Google Play would need a separate private key.

The **Android build** workflow also runs the tests and builds an APK on every pull request, downloadable as `sudoku-debug-apk` from the run's artifacts.

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
│   ├── LogicSolver.kt Human-style solver that grades puzzles by technique
│   ├── Generator.kt   Graded, unique-solution puzzle generator and levels
│   ├── GameState.kt   Immutable game state and moves
│   └── GameCodec.kt   Save format
├── data/          SharedPreferences storage for the game, settings and stats
└── ui/            ViewModel, theme, board, controls and screens
```

The engine has unit tests in `app/src/test`.

## How puzzles are graded

`LogicSolver` solves with pencil-mark candidates the way a person does, always using the easiest technique that makes progress. The hardest technique it needs sets the level. The generator digs clues out of a random grid while the solution stays unique, grades the result, and where logic gets stuck adds back the clue that makes the puzzle land on the requested level. Easy puzzles also keep at least 34 clues. Generation takes about 0–75 ms per puzzle on a desktop JVM, with Expert the slowest.
