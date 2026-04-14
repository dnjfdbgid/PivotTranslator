# ViewModel & State Management

## ViewModel with StateFlow

```kotlin
class TranslationViewModel(
    application: Application,
    private val repository: TranslationRepository,
) : AndroidViewModel(application) {

    // Private mutable, public read-only
    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    fun translateToEnglish(text: String) {
        viewModelScope.launch {
            _uiState.value = TranslationUiState.Loading
            try {
                val result = repository.translateToEnglish(text)
                _uiState.value = TranslationUiState.Editing(englishText = result)
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

## Sealed Interface for UI State

```kotlin
// Sealed interface — exhaustive, supports data objects and data classes
sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data object Loading : TranslationUiState
    data class Editing(val englishText: String) : TranslationUiState
    data class Success(val editedEnglish: String, val finalTranslation: String) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

// Usage in Composables — exhaustive when
when (uiState) {
    is TranslationUiState.Idle -> { /* ... */ }
    is TranslationUiState.Loading -> { /* ... */ }
    is TranslationUiState.Editing -> { /* access uiState.englishText */ }
    is TranslationUiState.Success -> { /* access uiState.finalTranslation */ }
    is TranslationUiState.Error -> { /* access uiState.message */ }
}
```

## One-Shot Events

```kotlin
// Pattern: consumed-state (preferred for simple cases)
private val _errorMessage = MutableStateFlow<String?>(null)
val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

fun clearError() { _errorMessage.value = null }

// In Composable
val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
LaunchedEffect(errorMessage) {
    errorMessage?.let {
        snackbarHostState.showSnackbar(it)
        viewModel.clearError()
    }
}
```

## ViewModel Factory (Manual DI)

```kotlin
class MyViewModel(
    application: Application,
    private val repository: MyRepository,
) : AndroidViewModel(application) {

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return MyViewModel(application, MyRepository()) as T
            }
        }
    }
}

// Usage in Composable
val viewModel: MyViewModel = viewModel(factory = MyViewModel.Factory)
```

## Multiple StateFlows

```kotlin
// Prefer multiple focused StateFlows over one large state object
private val _sourceText = MutableStateFlow("")
val sourceText: StateFlow<String> = _sourceText.asStateFlow()

private val _targetLanguage = MutableStateFlow("우즈베크어")
val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

private val _remainingSeconds = MutableStateFlow<Int?>(null)
val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()
```
