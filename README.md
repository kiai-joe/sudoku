# Sumi

An Android Sudoku game built with Kotlin and Jetpack Compose.

Sumi is the black ink of Japanese calligraphy. Printed clues are set in sumi black on washi paper, your own numbers in indigo, the selected cell in gold, mistakes in vermilion (the colour calligraphy teachers correct work in), and a finished row, column or box in matcha green. The launcher icon is a vermilion hanko seal cut as a 3×3 grid.

The look is soft and rounded: the board is nine rounded boxes on a sunk well instead of heavy grid lines, cards and pill buttons have one soft shadow, and a floating bar at the bottom moves between Home, Daily, Learn and Progress (only the open tab shows its name). Titles and every number are set in [Sora](https://github.com/sora-xor/sora-font) and everything you read in [Inter](https://rsms.me/inter/), both under the SIL Open Font License (`licenses/Sora-OFL.txt`, `licenses/Inter-OFL.txt`). Icons are [Lucide](https://lucide.dev) outlines (ISC licence, `licenses/Lucide-ISC.txt`), compiled into the app as vectors in `ui/icons/Lucide.kt`.

## Features

- Four levels graded by the hardest solving technique a puzzle needs, not by clue count: Easy (singles only), Medium (pointing, box/line reduction, pairs, naked triples), Hard (hidden triples, X-Wing, XY-Wing, naked quads) and Expert (Swordfish, XYZ-Wing, hidden quads). Every puzzle is generated on the device, has exactly one solution and can be solved by logic without guessing.
- Pencil notes, with matching notes cleared automatically when you place a number. **Fill** pencils in every number still possible in each empty cell.
- Highlighting for the selected cell's row, column, box and matching numbers.
- Mistake limit of 3, which you can turn off in Settings.
- Optional "check when I ask" mode: turn off *Show mistakes right away* and nothing reveals whether a number is right (no red numbers, no mistake count, correct cells stay editable, the number pad counts every placed copy) until you tap **Check**.
- Finishing a row, column or box sends a brief matcha ink wash across it, and placing numbers gives a light vibration (both can be turned off).
- Undo and erase.
- Teaching hints: a hint finds the easiest next step on your board, highlights the cells involved, and explains it in plain words ("In the centre box, 4 can only go in row 5…"). Steps that only rule out candidates show them crossed out on the board, and you step through to the number they lead to. A wrong number on the board is pointed out first.
- Timer that pauses when you leave the app, plus a manual pause that hides the board.
- Killer Sudoku in four levels: the grid is split into cages, each showing the sum of its cells, with no repeats inside a cage. Killer has its own ladder, because cage logic solves most puzzles before classic techniques are needed: Easy needs cage combinations, Medium the 45 rule (innies and outies), Hard classic techniques such as pointing and pairs, and Expert the same with no printed numbers at all. Every puzzle is solvable by logic, and hints explain cage combinations and the 45 rule.
- Learn: a lesson for every technique the puzzles are graded by, each with a worked example from a real puzzle that you step through on the board, explained the same way hints are. Reaching the end of a lesson ticks it off, and Learn shows how many you've finished and which is next.
- Daily puzzle: the same puzzle for everyone each day, made from the date so it works offline. It runs from Easy on Monday to Expert on Sunday, with a Killer puzzle on Saturday. A streak counts the days in a row you solve it, and the Daily tab's calendar marks each solved day in vermilion. It's saved separately, so it never replaces your regular game.
- The current game is saved after every move and can be resumed.
- A home-screen widget (Daily puzzle, 4×2) shows today's level, your streak and this week, with a button that opens straight into today's puzzle. It updates when you start or solve the daily, just after midnight and at least hourly, and needs no permission. A 2×1 Streak widget shows just the streak (or today's level) and a round Play button.
- Tablets and landscape: on a screen wider than it is tall, the board sits on the left at full height with the controls, hint or win summary beside it, and lessons do the same. On a tablet the number pad becomes a 3×3 grid of bigger keys. Home and the Daily tab use two columns on wide screens.
- Your progress comes with you to a new phone: Android's backup and phone-to-phone transfer include it (see `res/xml/backup_rules.xml` and `data_extraction_rules.xml`), and Settings › Your progress saves and restores a backup file through Android's file picker, with no permission needed. A file Sumi can't read changes nothing.
- Progress per level: games played and solved, win rate, best time, average time, and how often you solve without hints.
- Twelve achievements, from a first solve to a 30-day daily streak and an Expert Killer with no printed numbers. New ones show on the win screen; all are shown as seals under Progress, and tapping one shows what it needs and when you earned it. Ones already earned from saved statistics are awarded on first launch.
- Accessibility: a Larger numbers setting; wrong numbers are underlined as well as red, so they don't rely on colour; every board cell is fully described to screen readers (value, pencil marks, Killer cage, and what a hint shows or rules out), achievement and weekday rows read as one sentence, and the ink wash and stamp animation are skipped when the system's Remove animations setting is on. `AccessibilityTest` checks what TalkBack hears.
- Three palettes, picked in Settings: Sumi (Washi by day, Sumi night after dark), Indigo night (paper-white on deep indigo, always dark) and Bamboo (green-tinted paper). Sumi and Bamboo follow the phone's light or dark setting, or you can pick Light or Dark in Settings (the home-screen widgets still follow the phone).

## Privacy

Sumi collects nothing, works entirely offline and has no ads. It asks for no Android permissions, not even internet access. See the [privacy policy](docs/privacy-policy.md); `PrivacyTest` fails if a permission is ever added, so the policy stays true.

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
│   ├── Hints.kt       Turns solver steps into explained, step-by-step hints
│   ├── Daily.kt       Daily puzzle from the date, weekly levels, streaks
│   ├── Tutorial.kt    Lessons and their worked examples (stored as seed + moves)
│   ├── Killer.kt      Killer cages, cage combinations and the Killer generator
│   ├── Achievements.kt Achievements and the rules for earning them
│   ├── Generator.kt   Graded, unique-solution puzzle generator and levels
│   ├── GameState.kt   Immutable game state and moves
│   └── GameCodec.kt   Save format
├── data/          SharedPreferences storage for the game, settings and stats
└── ui/            ViewModel, theme, board, controls and screens
```

The engine has unit tests in `app/src/test`.

## Screenshots

`ScreenshotTest` renders the main screens (home, a game in progress, a hint, the win stamp, the Daily tab, Progress, Learn and Settings, in light and dark) to `app/src/test/screenshots` using [Roborazzi](https://github.com/takahirom/roborazzi) on Robolectric, so no device or emulator is needed. On every pull request CI re-renders them and commits any changes back to the branch, so visual changes show up as image diffs in the review. To render them yourself:

```bash
./gradlew recordRoborazziDebug
```

## How puzzles are graded

`LogicSolver` solves with pencil-mark candidates the way a person does, always using the easiest technique that makes progress. The hardest technique it needs sets the level. The generator digs clues out of a random grid while the solution stays unique, grades the result, and where logic gets stuck adds back the clue that makes the puzzle land on the requested level. Easy puzzles also keep at least 34 clues. Generation takes about 0–75 ms per puzzle on a desktop JVM, with Expert the slowest.
