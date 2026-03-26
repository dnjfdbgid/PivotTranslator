# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PivotTranslator is an Android app that performs **pivot translation** — translating text through English as an intermediate language. The two-step flow:
1. Source text → English literal translation (via Google Translate web endpoint)
2. User reviews/edits the English → Final target language translation (via Google Translate web endpoint)

The user manually edits the English pivot text between steps, giving them control over translation quality. The UI is in Korean.

> **Note:** 1단계는 원래 DeepL을 사용할 예정이었으나 DeepL HTTP 429(Too Many Requests) 에러로 인해 현재 양쪽 모두 Google Translate를 사용 중. DeepL 코드는 `DeepLTranslator`에 유지되어 있음.

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

## Architecture

Single-module app using **MVVM** with Jetpack Compose. No DI framework — dependencies are wired manually.

**Data flow:** `TranslationScreen` → `TranslationViewModel` → `TranslationRepository` → `GoogleTranslator` / `DeepLTranslator` (Retrofit)

- **Network layer** (`network/`): Retrofit singleton (`RetrofitClient`)이 Google Translate와 DeepL 두 서비스의 API 인스턴스를 제공. 공통 `OkHttpClient`를 공유한다.
  - `GoogleTranslateApi` — `translate.googleapis.com` 무료 웹 엔드포인트 (`@GET`)
  - `DeepLApi` — `www2.deepl.com/jsonrpc` JSON-RPC 엔드포인트 (`@POST`), 요청/응답 DTO는 `@SerializedName`으로 snake_case 매핑
- **Translator layer** (`translator/`): `GoogleTranslator`, `DeepLTranslator` — 각 번역 서비스의 언어 코드 매핑 및 응답 파싱 담당.
- **Repository** (`repository/TranslationRepository`): 1단계(→ 영어)와 2단계(→ 최종 언어) 번역을 Translator에 위임.
- **ViewModel** (`viewmodel/TranslationViewModel`): `AndroidViewModel` 기반. sealed `TranslationUiState` (Idle → Loading → Editing → Loading → Success / Error) 관리. 자동 번역 디바운스 타이머 및 카운트다운 기능 포함.
- **Preferences** (`preferences/PreferencesManager`): `SharedPreferences`로 자동 번역 대기 시간(4~10초, 기본 6초)을 영속 저장.
- **Config** (`config/AppConfig`): 앱 설정 상수. `EXPIRATION_DATE`(yyyyMMdd)로 번역 기능 만료일을 하드코딩. 만료일 이후 번역 기능이 비활성화된다.
- **UI** (`ui/screen/TranslationScreen`): Single screen with state-driven rendering. `TranslationScreenContent`로 상태/콜백 분리하여 `@Preview` 지원. Navigation은 `NavGraph`를 통해 설정되어 있으며 현재 하나의 route(`"translation"`)만 사용.

## Key Features

- **자동 번역**: 텍스트 입력 후 N초(기본 6초) 동안 입력이 없으면 자동으로 영어 번역 시작
- **카운트다운 표시**: 스테퍼 UI에서 남은 초가 실시간으로 줄어들며 표시
- **대기 시간 설정**: +/- 버튼으로 4~10초 범위에서 조절 가능, 앱 재시작 후에도 유지
- **Editing 상태 원문 수정**: 영어 번역 결과 확인 중에도 원문을 수정하면 자동 재번역
- **자동 클립보드 복사**: 최종 번역 결과가 나오면 자동으로 클립보드에 복사 + 스낵바 알림
- **번역 기능 만료**: `AppConfig.EXPIRATION_DATE`에 설정된 날짜 이후 번역 기능 비활성화, 만료 안내 표시

## Key Details

- **Target SDK**: 36, **Min SDK**: 26
- **Compose BOM**: 2026.03.00, **Material3**
- **Serialization**: Gson (not kotlinx.serialization)
- **Default target language**: Uzbek (우즈베크어)
- ProGuard/R8 rules in `app/proguard-rules.pro` cover Retrofit, OkHttp, Gson, Coroutines, and DeepL DTOs
- Version catalog at `gradle/libs.versions.toml`
- **No API key required** — 외부 API 키 없이 무료 웹 엔드포인트만 사용

## Release Build & Keystore Security
- The release APK is signed using a keystore located in the `keystore/` directory.
- **SECURITY:** The `keystore/` directory and `*.jks` files MUST NOT be committed to version control (`.gitignore` applied).
- Keystore credentials (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are stored in `local.properties` and injected into the `release` signingConfig in `app/build.gradle.kts`. Do NOT hardcode keystore credentials in the build script.

## Coding Guidelines
- Please write all code comments, explanations, and Git commit messages in **Korean (한국어)**.
- All user-facing UI texts and error messages should be in Korean.
