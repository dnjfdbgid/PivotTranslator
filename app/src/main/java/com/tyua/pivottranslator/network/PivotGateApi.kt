package com.tyua.pivottranslator.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * PivotGate API 서버 Retrofit 인터페이스
 */
interface PivotGateApi {

    @GET("api/v1/app/expiration")
    suspend fun getExpiration(
        @Header("X-API-Key") apiKey: String
    ): ExpirationResponse

    /** 한국어 → 영어 번역 (DeepL) */
    @POST("api/v1/translate/deepl")
    suspend fun translateDeepL(
        @Header("X-API-Key") apiKey: String,
        @Body request: DeepLTranslateRequest
    ): TranslateResponse

    /** 영어 → 타겟 언어 번역 (Google) */
    @POST("api/v1/translate/google")
    suspend fun translateGoogle(
        @Header("X-API-Key") apiKey: String,
        @Body request: GoogleTranslateRequest
    ): TranslateResponse
}

/** 만료일 응답 */
data class ExpirationResponse(
    @SerializedName("expiration_date")
    val expirationDate: String
)

/** DeepL 번역 요청 (한국어 → 영어) */
data class DeepLTranslateRequest(
    @SerializedName("text")
    val text: String
)

/** Google 번역 요청 (영어 → 타겟 언어) */
data class GoogleTranslateRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("target_lang")
    val targetLang: String
)

/** 번역 응답 (DeepL, Google 공통) */
data class TranslateResponse(
    @SerializedName("translated_text")
    val translatedText: String
)
