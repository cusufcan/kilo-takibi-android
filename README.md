# Kilo Takibi

A simple, private weight tracker for Android. Log daily weigh-ins, see your trend on a date-based chart and keep notes, all stored on your device.

## Features

- Daily weigh-ins with optional notes, edit and delete
- Smoothed weight trend and date-based progress chart
- Goal weight and BMI
- Weekly summary and a daily reminder at a time you choose
- Home screen widget
- CSV export and import
- English and Turkish, light and dark theme

## Stack

- Kotlin, Jetpack Compose (Material 3)
- MVVM with unidirectional data flow
- Room + Kotlin Flow, DataStore
- Hilt (KSP), Navigation Compose with type-safe routes
- JUnit, coroutines-test and Compose UI tests
- GitHub Actions CI

## Build

Requirements: JDK 17 and the Android SDK (min SDK 25, target SDK 37).

```bash
./gradlew assembleDebug        # debug APK
./gradlew test                 # unit tests
./gradlew lintDebug            # lint
./gradlew connectedAndroidTest # UI tests (emulator or device)
```

## Credits

Built in collaboration with AI coding agents such as Claude and Codex.
