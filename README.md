# Brain Blitz

Five games in one offline Android app: trivia, word search, a candy match board, snake, and falling blocks.

Plain Kotlin and the Android framework. No third-party libraries, no ads, no network calls. Every
sound and both music loops were synthesised from scratch for this game (see `tools/audio_gen.py`).

## Get the APK

1. Open the **Releases** page of this repo.
2. Download `app-debug.apk` from the newest build.
3. On your phone, tap the file and allow "install unknown apps" for your browser when prompted.

Every push to `main` builds a fresh APK through GitHub Actions, so the latest release is always current.

## The games

**Trivia** - 10 questions per round, 15 seconds each, 9 categories plus a mixed bag.
100 points per correct answer + up to 75 speed bonus + 20 per streak step (capped at 5).
Categories: General Knowledge, Geography, History, Science, Technology, Sports, Movies & TV, Music, Naija & Africa.

**Word Search** - 10x10 board, words hidden in all eight directions. Drag to claim a word.
Each word you find paints its letters on the board in its own colour, and the word list matches,
so you always know what has already been traced. 2:30 on the clock, leftover seconds pay a bonus.

**Sugar Rush** - 8x8 candy board, 25 moves. Swap neighbours, clear three or more, chain cascades.

**Pixel Snake** - 18x18 grid, swipe or tap the arrows, the snake speeds up with every bite.

**Blockfall** - 10x20 block board, seven shapes, rotation with wall kicks, line clears, and gravity
that speeds up as your level climbs.

## Sound

- Two looping scores: a warm lofi-afro menu theme and a driving arcade loop.
- Seventeen effects, each tuned by ear and level-matched, from the marimba arpeggio on a correct
  answer to the bubble cascade in Sugar Rush.
- The pool in `Audio.kt` keeps them on a game audio stream; the Home screen has a SOUND ON/OFF
  toggle that persists.

Everything is generated, so nothing here is licensed from anyone. To change the soundtrack, edit
`tools/audio_gen.py` and run:

```bash
python3 tools/audio_gen.py
```

It writes the WAV files straight into `app/src/main/res/raw/`. Because the loops ship uncompressed
(Android keeps `.wav` out of the zip deflate list), they are also what gives the APK its size.

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

`answer` is the zero-based index of the correct option. Add a new object to `categories` and it
appears on the category screen automatically. Malformed entries are skipped at load time.

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
  HomeActivity.kt        mode picker and sound toggle
  CategoryActivity.kt    trivia category list
  QuizActivity.kt        the round: timer, scoring, streaks
  ResultActivity.kt      final score, verdict, replay
  WordSearchActivity.kt  word search round and timer
  WordSearchView.kt      canvas board, drag selection, per-word colours
  Match3Activity.kt      candy grid, swaps, cascades
  SnakeActivity.kt       snake round, ticker and controls
  SnakeView.kt           canvas snake board
  BlockfallActivity.kt   block puzzle round, gravity and controls
  BlockfallView.kt       canvas block board, rotation and line clears
  Audio.kt               sound pool, music player, mute preference
  QuestionBank.kt        loads and validates the trivia bank
  WordBank.kt            loads the word search packs
  Scores.kt              high scores in SharedPreferences
tools/audio_gen.py       synthesises every sound in the game
```

## Publishing to Play Store

The builds here are debug-signed, which is fine for installing on your own devices.
For the Play Store you need a release keystore:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias brainblitz
```

Then add a `signingConfigs` block to `app/build.gradle` and build a bundle with `gradle bundleRelease`.
Note that the API limit for APKs on Play is 100 MB; with the music this sits around 15 MB.
