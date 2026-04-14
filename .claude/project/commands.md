# Commands

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config in local.properties)
./gradlew assembleRelease
# → outputs: build/release/PivotTranslator_release_yyyyMMdd.apk
```

## Test

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.tyua.pivottranslator.viewmodel.TranslationViewModelTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest
```

## Lint & Check

```bash
# Android lint
./gradlew lint

# Compile check without building APK
./gradlew compileDebugKotlin
```

## Clean

```bash
./gradlew clean
```

## Required local.properties

```properties
PIVOT_GATE_API_KEY=<api-key>
KEYSTORE_PATH=<path-to-keystore>
KEYSTORE_PASSWORD=<password>
KEY_ALIAS=<alias>
KEY_PASSWORD=<password>
# GEMINI_API_KEY=<optional>
```
