# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PivotTranslator is an Android app that performs **pivot translation** — translating text through English as an intermediate language. The two-step flow:
1. Source text → English literal translation (via Gemini API)
2. User reviews/edits the English → Final target language translation (via Gemini API)

The user manually edits the English pivot text between steps, giving them control over translation quality. The UI is in Korean.

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (minified with R8)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew testDebugUnitTest --tests "com.tyua.pivottranslator.ExampleUnitTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

## API Key Setup

The app requires a Gemini API key. Add to `local.properties` (not committed):
```
GEMINI_API_KEY=your_key_here
```
This is injected into `BuildConfig.GEMINI_API_KEY` at build time via `app/build.gradle.kts`.

## Architecture

Single-module app using **MVVM** with Jetpack Compose. No DI framework — dependencies are wired manually.

**Data flow:** `TranslationScreen` → `TranslationViewModel` → `TranslationRepository` → `GeminiApi` (Retrofit)

- **Network layer** (`network/`): Retrofit singleton (`RetrofitClient`) calls Gemini REST API (`v1beta/models/gemini-2.5-flash:generateContent`). Request/response DTOs use `@Keep` for R8/Gson compatibility. The API key is auto-appended as a query parameter by an OkHttp interceptor.
- **Repository** (`repository/TranslationRepository`): Two methods mapping to the two translation steps. Each constructs a `GeminiRequest` with a system prompt and user message.
- **ViewModel** (`viewmodel/TranslationViewModel`): Manages a sealed `TranslationUiState` (Idle → Loading → Editing → Loading → Success / Error). Exposes `StateFlow`s consumed by the UI.
- **UI** (`ui/screen/TranslationScreen`): Single screen with state-driven rendering. Navigation is set up via `NavGraph` but currently has only one route (`"translation"`).

## Key Details

- **Target SDK**: 36, **Min SDK**: 26
- **Compose BOM**: 2024.09.00, **Material3**
- **Serialization**: Gson (not kotlinx.serialization)
- **Default target language**: Uzbek (우즈베크어)
- ProGuard/R8 rules in `app/proguard-rules.pro` cover Retrofit, OkHttp, Gson, and Coroutines
- Version catalog at `gradle/libs.versions.toml`

## Release Build & Keystore Security
- The release APK is signed using a keystore located in the `keystore/` directory.
- **SECURITY:** The `keystore/` directory and `*.jks` files MUST NOT be committed to version control (`.gitignore` applied).
- Keystore credentials (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are stored in `local.properties` and injected into the `release` signingConfig in `app/build.gradle.kts`. Do NOT hardcode keystore credentials in the build script.

## Coding Guidelines
- Please write all code comments, explanations, and Git commit messages in **Korean (한국어)**.
- All user-facing UI texts and error messages should be in Korean.