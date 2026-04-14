---
name: android-kotlin-compose
description: "Android development with Kotlin and Jetpack Compose. Use this skill when creating screens, ViewModels, Composables, navigation, networking with Retrofit, or testing Android code. Provides architecture patterns and Compose best practices."
user-invocable: false
---

# Android Kotlin Compose

Specialized knowledge for building Android apps with Kotlin, Jetpack Compose, and modern Android architecture components.

## Core Principles

### Jetpack Compose
- Composition over inheritance — build UI by combining small Composables
- State hoisting — push state up, push events down
- Unidirectional data flow — state flows down, events flow up
- Composables should be stateless where possible; state lives in ViewModel
- Use `Modifier` as first optional parameter after required params
- Prefer `remember` for UI-local state, `StateFlow` for shared/business state
- Side effects: `LaunchedEffect` for suspend, `DisposableEffect` for cleanup, `SideEffect` for non-suspend

### Material 3
- Use Material 3 components from `androidx.compose.material3`
- Support dynamic colors (Android 12+) with `dynamicColorScheme`
- Define custom `ColorScheme`, `Typography`, `Shapes` in theme
- Use `MaterialTheme` token accessors, never hardcode colors

### State Management
- Sealed interfaces for UI state modeling (exhaustive when-expressions)
- `MutableStateFlow` + `asStateFlow()` in ViewModels for reactive state
- Expose read-only `StateFlow` to UI, mutate only inside ViewModel
- `collectAsStateWithLifecycle()` in Composables for lifecycle-aware collection
- One-shot events via `SharedFlow` or consumed-state pattern, not Channel

### Coroutines & Flow
- `viewModelScope` for ViewModel coroutines (auto-cancelled)
- `Dispatchers.IO` for blocking I/O, `Dispatchers.Main` for UI
- Use `suspend` functions for one-shot operations
- Use `Flow` for streams, `StateFlow` for state holders
- Handle cancellation gracefully — structured concurrency

### Networking (Retrofit + OkHttp)
- Define API interfaces with `suspend` functions
- Singleton Retrofit instances (object or lazy)
- OkHttp interceptors for cross-cutting concerns (auth, logging, User-Agent)
- Debug-only logging interceptors
- Gson for JSON serialization with data class DTOs

### Navigation
- Compose Navigation with `NavHost` and `composable()` routes
- String-based routes with type-safe arguments
- Navigate via `NavController`, expose it minimally

### Testing
- JUnit 4 as test runner
- Robolectric for Android framework dependencies without emulator
- `kotlinx-coroutines-test` with `TestDispatcher` for deterministic async
- Fake implementations over mocks for testability
- Test state transitions via `StateFlow.value` assertions

### Build System
- Gradle Version Catalog (`libs.versions.toml`) for centralized dependency management
- Alias references in build.gradle.kts: `libs.plugins.*`, `libs.*`
- Compose BOM for consistent Compose library versions
- BuildConfig fields for build-time configuration injection

## References

| Reference | Content |
|-----------|---------|
| [compose-patterns.md](./references/compose-patterns.md) | Composable conventions, state hoisting, effects, Material 3 |
| [viewmodel-state.md](./references/viewmodel-state.md) | ViewModel, StateFlow, sealed interfaces, event handling |
| [networking.md](./references/networking.md) | Retrofit interfaces, OkHttp, Gson, error handling |
| [testing.md](./references/testing.md) | JUnit, Robolectric, TestDispatcher, fakes |
