# Brain Blitz

Three offline games in one Android app: fast trivia, word search, and a swap-to-match candy board.

Built with plain Kotlin and the Android framework. No third-party libraries, no ads, no network calls.

## Get the APK

1. Open the **Releases** page of this repo.
2. Download `app-debug.apk` from the newest build.
3. On your phone, tap the file and allow "install unknown apps" for your browser when prompted.

Every push to `main` builds a fresh APK automatically through GitHub Actions, so the latest release is always the current code.

## The games

**Trivia** - 10 questions per round, 15 seconds each, 9 categories plus a mixed bag.
Correct answer scores 100 + up to 75 speed bonus + 20 per streak step (capped at 5).
Categories: General Knowledge, Geography, History, Science, Technology, Sports, Movies & TV, Music, Naija & Africa.

**Word Search** - 10x10 board, words hidden in all eight directions (forwards and backwards).
Drag across letters to claim a word. 2:30 on the clock, 100 points per word, leftover seconds pay a bonus at the end.
Six word packs: Animals, Tech, Africa, Food, Music, Space.

**Sugar Rush** - 8x8 candy board, 25 moves. Tap a candy then a neighbour to swap;
three or more in a row or column clears, cascades multiply the score.

Personal bests are saved on the device per mode and per trivia category.

## Adding content

Questions live in `app/src/main/assets/questionbank.json`:

```json
{
  "id": "general",
  "name": "General Knowledge",
  "questions": [
    { "q": "Question text?", "options": ["A", "B", "C", "D"], "answer": 2 }
  ]
}
```

`answer` is the zero-based index of the correct option. Add a new object to `categories`
and it appears on the category screen automatically. Malformed entries are skipped at load
time instead of crashing the app.

Word search packs live in `app/src/main/assets/wordbank.json` (uppercase, 3-10 letters).

## Building locally

Requires JDK 17 and the Android SDK (platform 34):

```bash
gradle assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```
app/src/main/java/com/jerrynifemi/brainblitz/
  SplashActivity.kt      animated launch screen
  HomeActivity.kt        mode picker
  CategoryActivity.kt    trivia category list
  QuizActivity.kt        the round: timer, scoring, streaks
  ResultActivity.kt      final score, verdict, replay
  WordSearchActivity.kt  word search round and timer
  WordSearchView.kt      canvas board, drag selection, word detection
  Match3Activity.kt      candy grid, swaps, cascades
  QuestionBank.kt        loads and validates the trivia bank
  WordBank.kt            loads the word search packs
  Scores.kt              high scores in SharedPreferences
```

## Publishing to Play Store

The builds here are debug-signed, which is fine for installing on your own devices.
For the Play Store you need a release keystore:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias brainblitz
```

Then add a `signingConfigs` block to `app/build.gradle` and build a bundle with `gradle bundleRelease`.
