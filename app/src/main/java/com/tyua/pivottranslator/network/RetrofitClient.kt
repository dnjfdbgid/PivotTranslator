package com.tyua.pivottranslator.network

import com.tyua.pivottranslator.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit + OkHttp 싱글톤 클라이언트
 *
 * Google Translate, DeepL, PivotGate 세 서비스의 API 인스턴스를 제공한다.
 */
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
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    /** Google Translate API */
    val googleTranslateApi: GoogleTranslateApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.GOOGLE_TRANSLATE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleTranslateApi::class.java)
    }

    /** DeepL JSON-RPC API */
    val deepLApi: DeepLApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.DEEPL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepLApi::class.java)
    }

    /** PivotGate API 서버 */
    val pivotGateApi: PivotGateApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.PIVOT_GATE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PivotGateApi::class.java)
    }
}
