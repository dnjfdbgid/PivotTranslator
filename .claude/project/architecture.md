# Architecture

## Module Structure

Single-module project (`app`). All source code in `com.tyua.pivottranslator`.

## Package Organization

```
com.tyua.pivottranslator/
├── config/          # AppConfig — API base URLs
├── network/         # Retrofit API interfaces, DTOs, RetrofitClient singleton
├── preferences/     # PreferencesManager (SharedPreferences wrapper)
├── repository/      # TranslationRepository — coordinates API calls
├── translator/      # Translation service objects
├── ui/
│   ├── navigation/  # NavGraph — single "translation" route
│   ├── screen/      # TranslationScreen Composable
│   └── theme/       # Material 3 theme (Color, Type, Theme)
└── viewmodel/       # TranslationViewModel with state machines
```

## Dependency Injection

**Manual DI — no Hilt, no Koin.**

- `RetrofitClient` is a singleton `object` providing lazy API instances
- `TranslationRepository` takes API interface as constructor parameter (defaults to RetrofitClient singletons)
- `TranslationViewModel` takes `Application` + `Repository` as constructor parameters
- ViewModel creation uses companion `Factory` with `ViewModelProvider.Factory`
- Test injection via constructor: pass fakes for `Repository` and API interfaces

## State Management

Two sealed-interface state machines in `TranslationViewModel`:

1. **`TranslationUiState`**: `Idle → Loading → Editing → Loading → Success / Error`
2. **`AppActivationState`**: `Checking → Active / Expired / ServerError`

All state exposed via `StateFlow`. UI collects with `collectAsStateWithLifecycle()`.

## Networking

Three Retrofit API services via `RetrofitClient`:
- `PivotGateApi` — main proxy server (DeepL translate, Google translate, expiration check)
- `DeepLApi` — direct DeepL endpoint (unused in current flow)
- `GoogleTranslateApi` — direct Google endpoint (unused in current flow)

All API calls route through PivotGate proxy. API key injected from `local.properties` → `BuildConfig.PIVOT_GATE_API_KEY`.

## Performance

- `androidx.profileinstaller:profileinstaller` — AGP가 릴리즈 빌드 시 생성하는 Baseline Profile을 앱이 수신/설치할 수 있도록 지원

## Configuration

- `AppConfig` — hardcoded base URLs for all API services
- `PreferencesManager` — SharedPreferences for user settings (auto-translate delay)
- `local.properties` — API keys, keystore credentials (not committed)
- `network_security_config.xml` — cleartext HTTP whitelist for PivotGate server
