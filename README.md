# Brain Blitz

A fast, offline trivia game for Android. 10 questions per round, 15 seconds each, bonus points for speed and streaks.

Built with plain Kotlin and the Android framework. No third-party libraries, no ads, no network calls.

## Get the APK

1. Open the **Releases** page of this repo.
2. Download `app-debug.apk` from the newest build.
3. On your phone, tap the file and allow "install unknown apps" for your browser when prompted.

Every push to `main` builds a fresh APK automatically through GitHub Actions, so the latest release is always the current code.

## Playing

- **Mixed bag** pulls questions from every category.
- **Categories:** General Knowledge, Science & Tech, Sports, Movies & Music.
- Correct answer: 100 points + up to 75 speed bonus + 20 per streak step (capped).
- A wrong answer or a timeout resets your streak.
- Personal bests are saved on the device per category.

## Adding questions

Everything lives in `app/src/main/assets/questionbank.json`:

```json
{
  "id": "general",
  "name": "General Knowledge",
  "questions": [
    { "q": "Question text?", "options": ["A", "B", "C", "D"], "answer": 2 }
  ]
}
```

`answer` is the zero-based index of the correct option. Malformed entries are skipped at load time instead of crashing the app. Add a whole new object to `categories` and it shows up on the home screen automatically.

## Building locally

Requires JDK 17 and the Android SDK (platform 34):

```bash
gradle assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```
app/src/main/java/com/jerrynifemi/brainblitz/
  MainActivity.kt    home screen, category list, best scores
  QuizActivity.kt    the round: timer, scoring, streaks, feedback
  ResultActivity.kt  final score, verdict, replay
  QuestionBank.kt    loads and validates the JSON bank
  Scores.kt          high scores in SharedPreferences
```

## Publishing to Play Store

The builds here are debug-signed, which is fine for installing on your own devices. For the Play Store you need a release keystore:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias brainblitz
```

Then add a `signingConfigs` block to `app/build.gradle` and build a bundle with `gradle bundleRelease`.
