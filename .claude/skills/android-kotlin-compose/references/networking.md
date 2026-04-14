# Networking (Retrofit + OkHttp)

## Retrofit API Interface

```kotlin
// Suspend functions for coroutine integration
interface PivotGateApi {
    @POST("deepl/translate")
    suspend fun translateDeepL(@Body request: DeepLTranslateRequest): DeepLTranslateResponse

    @POST("google/translate")
    suspend fun translateGoogle(@Body request: GoogleTranslateRequest): GoogleTranslateResponse

    @GET("expiration")
    suspend fun getExpiration(@Query("apiKey") apiKey: String): ExpirationResponse
}
```

## DTOs as Data Classes

```kotlin
// Request DTOs
data class DeepLTranslateRequest(
    val text: String,
    val source_lang: String,
    val target_lang: String,
)

// Response DTOs
data class DeepLTranslateResponse(
    val translations: List<Translation>,
) {
    data class Translation(val text: String)
}
```

## Singleton Retrofit Client

```kotlin
object RetrofitClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()
    }

    val pivotGateApi: PivotGateApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.PIVOT_GATE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PivotGateApi::class.java)
    }
}
```

## Repository Pattern

```kotlin
class TranslationRepository(
    private val pivotGateApi: PivotGateApi = RetrofitClient.pivotGateApi,
) {
    suspend fun translateToEnglish(text: String): String {
        val response = pivotGateApi.translateDeepL(
            DeepLTranslateRequest(text = text, source_lang = "KO", target_lang = "EN")
        )
        return response.translations.first().text
    }
}
```

## Error Handling

```kotlin
// In ViewModel — categorize errors for appropriate UI response
viewModelScope.launch {
    try {
        val result = repository.translateToEnglish(text)
        _uiState.value = TranslationUiState.Editing(result)
    } catch (e: SocketTimeoutException) {
        // Server unreachable
    } catch (e: ConnectException) {
        // Connection refused
    } catch (e: UnknownHostException) {
        // DNS failure
    } catch (e: Exception) {
        // Generic error
        _uiState.value = TranslationUiState.Error(e.message ?: "Unknown error")
    }
}
```

## ProGuard Rules for Networking

```proguard
# Retrofit
-keepattributes Signature, Exceptions, InnerClasses
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson DTOs — keep fields for reflection
-keep class com.tyua.pivottranslator.network.** { *; }
```
