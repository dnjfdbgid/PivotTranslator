# Testing

## JUnit 4 + Robolectric Setup

```kotlin
@RunWith(RobolectricTestRunner::class)
class TranslationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TranslationViewModel
    private lateinit var fakeRepository: FakeTranslationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fakeRepository = FakeTranslationRepository()
        viewModel = TranslationViewModel(
            application = ApplicationProvider.getApplicationContext(),
            repository = fakeRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

## Fake Implementations

```kotlin
// Prefer fakes over mocks — explicit, readable, no mocking library needed
class FakeTranslationRepository : TranslationRepository() {
    var translateToEnglishResult: String = "translated text"
    var translateToTargetResult: String = "final translation"
    var shouldThrow: Exception? = null
    var callCount = 0

    override suspend fun translateToEnglish(text: String): String {
        callCount++
        shouldThrow?.let { throw it }
        return translateToEnglishResult
    }

    override suspend fun translateToTarget(
        englishText: String,
        targetLanguage: String,
    ): String {
        callCount++
        shouldThrow?.let { throw it }
        return translateToTargetResult
    }
}
```

## Testing State Transitions

```kotlin
@Test
fun `translateToEnglish transitions from Idle to Loading to Editing`() = runTest {
    fakeRepository.translateToEnglishResult = "Hello"

    viewModel.updateSourceText("안녕하세요")
    viewModel.translateToEnglish()

    // After completion, should be in Editing state
    val state = viewModel.uiState.value
    assertThat(state).isInstanceOf(TranslationUiState.Editing::class.java)
    assertThat((state as TranslationUiState.Editing).englishText).isEqualTo("Hello")
}

@Test
fun `translateToEnglish shows error on failure`() = runTest {
    fakeRepository.shouldThrow = RuntimeException("Network error")

    viewModel.translateToEnglish()

    val state = viewModel.uiState.value
    assertThat(state).isInstanceOf(TranslationUiState.Error::class.java)
}
```

## Testing with Coroutines

```kotlin
// UnconfinedTestDispatcher — immediate execution, good for simple tests
@Before
fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

// StandardTestDispatcher — explicit advancement, good for timing-sensitive tests
@Before
fun setUp() {
    val testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)
}

@Test
fun `countdown timer decrements correctly`() = runTest {
    viewModel.updateSourceText("test")
    // advanceTimeBy, advanceUntilIdle for explicit control
    advanceTimeBy(3000)
    assertThat(viewModel.remainingSeconds.value).isEqualTo(3)
}
```

## Test Dependencies (libs.versions.toml)

```toml
[libraries]
junit = { group = "junit", name = "junit", version = "4.13.2" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
robolectric = { group = "org.robolectric", name = "robolectric", version = "4.14.1" }
androidx-arch-core-testing = { group = "androidx.arch.core", name = "core-testing", version = "2.2.0" }
androidx-test-core = { group = "androidx.test", name = "core", version = "1.6.1" }
```
