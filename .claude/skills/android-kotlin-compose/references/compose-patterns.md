# Compose Patterns

## Composable Function Conventions

```kotlin
// Naming: PascalCase for Composables that emit UI
@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel,  // required params first
    modifier: Modifier = Modifier,    // Modifier as first optional param
    onNavigate: () -> Unit = {},      // callbacks last
) {
    // ...
}

// Naming: camelCase for Composables that return values
@Composable
fun rememberTranslationState(): TranslationState {
    // ...
}
```

## State Hoisting Pattern

```kotlin
// Stateless Composable (preferred)
@Composable
fun SourceTextInput(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier,
    )
}

// Stateful wrapper (when needed)
@Composable
fun SourceTextInputStateful() {
    var text by remember { mutableStateOf("") }
    SourceTextInput(text = text, onTextChange = { text = it })
}
```

## Collecting StateFlow in Compose

```kotlin
@Composable
fun TranslationScreen(viewModel: TranslationViewModel) {
    // Lifecycle-aware collection — pauses when UI is not visible
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sourceText by viewModel.sourceText.collectAsStateWithLifecycle()

    when (uiState) {
        is TranslationUiState.Idle -> IdleContent(/* ... */)
        is TranslationUiState.Loading -> LoadingIndicator()
        is TranslationUiState.Editing -> EditingContent(/* ... */)
        is TranslationUiState.Success -> SuccessContent(/* ... */)
        is TranslationUiState.Error -> ErrorContent(/* ... */)
    }
}
```

## Side Effects

```kotlin
// LaunchedEffect — runs suspend code when key changes
LaunchedEffect(targetLanguage) {
    viewModel.onLanguageChanged(targetLanguage)
}

// DisposableEffect — setup + cleanup
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> /* ... */ }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}

// SideEffect — non-suspend side effect, runs every recomposition
SideEffect {
    analyticsTracker.trackScreenView("Translation")
}
```

## Material 3 Theme

```kotlin
@Composable
fun PivotTranslatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
```

## Common Material 3 Components

```kotlin
// Scaffold with TopAppBar
Scaffold(
    topBar = {
        TopAppBar(title = { Text("Pivot Translator") })
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { innerPadding ->
    Content(modifier = Modifier.padding(innerPadding))
}

// DropdownMenu for language selection
ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = selectedLanguage,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.menuAnchor(),
    )
    ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
        languages.forEach { language ->
            DropdownMenuItem(
                text = { Text(language) },
                onClick = { onLanguageSelected(language); expanded = false },
            )
        }
    }
}
```
