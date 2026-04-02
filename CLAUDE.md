# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PivotTranslator is an Android app that performs **pivot translation** — translating text through English as an intermediate language. The two-step flow:
1. Source text → English literal translation (via PivotGate 서버 → DeepL)
2. User reviews/edits the English → Final target language translation (via PivotGate 서버 → Google Translate)

The user manually edits the English pivot text between steps, giving them control over translation quality. The UI is in Korean.

> **Note:** 번역 요청은 자체 PivotGate 백엔드 서버(`http://10.0.2.2:8000/`)를 경유한다. 1단계는 DeepL, 2단계는 Google Translate를 사용하며, 서버가 API 호출을 대행한다. 기존 직접 호출 코드(`GoogleTranslator`, `DeepLTranslator`)는 폴백용으로 유지되어 있음.

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

**Data flow:** `TranslationScreen` → `TranslationViewModel` → `TranslationRepository` → `PivotGateApi` (Retrofit)

- **Network layer** (`network/`): Retrofit singleton (`RetrofitClient`)이 PivotGate, Google Translate, DeepL 세 서비스의 API 인스턴스를 제공. 공통 `OkHttpClient`를 공유한다.
  - `PivotGateApi` — 자체 PivotGate 서버 (`http://10.0.2.2:8000/`) 경유 번역 및 만료일 조회. `X-API-Key` 헤더로 인증.
    - `GET /api/v1/app/expiration` — 앱 만료일 조회
    - `POST /api/v1/translate/deepl` — DeepL 번역 위임
    - `POST /api/v1/translate/google` — Google 번역 위임
  - `GoogleTranslateApi` — `translate.googleapis.com` 무료 웹 엔드포인트 (`@GET`) — 폴백용
  - `DeepLApi` — `www2.deepl.com/jsonrpc` JSON-RPC 엔드포인트 (`@POST`) — 폴백용
- **Translator layer** (`translator/`): `GoogleTranslator`, `DeepLTranslator` — 각 번역 서비스의 언어 코드 매핑 및 응답 파싱 담당. 현재는 미사용(폴백용 유지).
- **Repository** (`repository/TranslationRepository`): `PivotGateApi`를 통해 1단계(DeepL → 영어)와 2단계(Google → 최종 언어) 번역을 수행. `BuildConfig.PIVOT_GATE_API_KEY`로 인증.
- **ViewModel** (`viewmodel/TranslationViewModel`): `AndroidViewModel` 기반. 두 가지 상태 시스템을 관리:
  - `TranslationUiState` (Idle → Loading → Editing → Loading → Success / Error) — 번역 흐름
  - `AppActivationState` (Checking → Active / Expired / ServerError) — 앱 활성화 상태
  - 앱 시작 시 서버에서 만료일을 조회하고, 서버 연결 실패 시 재시도 기능 제공. 자동 번역 디바운스 타이머 및 카운트다운 기능 포함.
- **Preferences** (`preferences/PreferencesManager`): `SharedPreferences`로 자동 번역 대기 시간(4~10초, 기본 6초)을 영속 저장.
- **Config** (`config/AppConfig`): 앱 설정 상수. 각 서비스의 Base URL 정의 (`GOOGLE_TRANSLATE_BASE_URL`, `DEEPL_BASE_URL`, `PIVOT_GATE_BASE_URL`). 만료일은 서버에서 런타임에 조회.
- **UI** (`ui/screen/TranslationScreen`): 앱 활성화 상태에 따라 다른 화면을 렌더링:
  - `Checking` — 서버 조회 중 로딩 화면
  - `ServerError` — 서버 연결 실패 + 재시도 버튼
  - `Expired` — 만료 안내 배너
  - `Active` — 번역 메인 화면
  - `TranslationScreenContent`로 상태/콜백 분리하여 `@Preview` 지원. Navigation은 `NavGraph`를 통해 설정.

## Key Features

- **자동 번역**: 텍스트 입력 후 N초(기본 6초) 동안 입력이 없으면 자동으로 영어 번역 시작
- **카운트다운 표시**: 스테퍼 UI에서 남은 초가 실시간으로 줄어들며 표시
- **대기 시간 설정**: +/- 버튼으로 4~10초 범위에서 조절 가능, 앱 재시작 후에도 유지
- **Editing 상태 원문 수정**: 영어 번역 결과 확인 중에도 원문을 수정하면 자동 재번역
- **자동 클립보드 복사**: 최종 번역 결과가 나오면 자동으로 클립보드에 복사 + 스낵바 알림
- **번역 기능 만료**: PivotGate 서버에서 만료일을 런타임에 조회. 만료일 이후 번역 기능 비활성화, 만료 안내 표시
- **서버 연결 관리**: 앱 시작 시 PivotGate 서버 연결을 확인하고, 실패 시 에러 화면과 재시도 버튼 제공. TCP 소켓 사전 체크(2초 타임아웃)로 빠른 네트워크 장애 감지
- **2단계 번역 에러 처리**: Editing 상태에서 2단계 번역 실패 시 스낵바로 에러 표시하되 Editing 상태를 유지하여 재시도 가능

## Key Details

- **Target SDK**: 36, **Min SDK**: 26
- **Compose BOM**: 2026.03.00, **Material3**
- **Serialization**: Gson (not kotlinx.serialization)
- **Default target language**: Uzbek (우즈베크어)
- ProGuard/R8 rules in `app/proguard-rules.pro` cover Retrofit, OkHttp, Gson, Coroutines, DeepL DTOs, PivotGate Request/Response DTOs
- Version catalog at `gradle/libs.versions.toml`
- **Material Icons Extended**: `androidx-compose-material-icons-extended` 의존성 사용 (CloudOff, HourglassEmpty, Refresh 등)
- **PivotGate API Key 필요** — `local.properties`에 `PIVOT_GATE_API_KEY` 설정 필요. `BuildConfig.PIVOT_GATE_API_KEY`로 빌드 시 주입됨
- **Network Security Config** — `res/xml/network_security_config.xml`에서 에뮬레이터 localhost(`10.0.2.2`) 대상 HTTP 평문 통신 허용

## Release Build & Keystore Security
- The release APK is signed using a keystore located in the `keystore/` directory.
- **SECURITY:** The `keystore/` directory and `*.jks` files MUST NOT be committed to version control (`.gitignore` applied).
- Keystore credentials (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) 및 `PIVOT_GATE_API_KEY`는 `local.properties`에 저장. 빌드 스크립트에서 `BuildConfig`로 주입. Do NOT hardcode credentials in the build script.

## Coding Guidelines
- Please write all code comments, explanations, and Git commit messages in **Korean (한국어)**.
- All user-facing UI texts and error messages should be in Korean.
