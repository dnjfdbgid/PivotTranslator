package com.tyua.pivottranslator.network

import com.tyua.pivottranslator.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit + OkHttp 싱글톤 클라이언트
 *
 * API 키는 OkHttp 인터셉터가 모든 요청에 자동으로 ?key=... 쿼리 파라미터를 추가한다.
 * BuildConfig.GEMINI_API_KEY → local.properties의 GEMINI_API_KEY 값
 */
object RetrofitClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // API 키 자동 주입 인터셉터
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("key", BuildConfig.GEMINI_API_KEY)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    /** 앱 전역에서 사용하는 GeminiApi 인스턴스 */
    val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}
