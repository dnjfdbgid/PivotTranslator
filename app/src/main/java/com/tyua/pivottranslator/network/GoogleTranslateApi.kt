package com.tyua.pivottranslator.network

import com.google.gson.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Translate 무료 웹 엔드포인트 Retrofit 인터페이스
 *
 * 응답은 중첩 JSON 배열 형태: [[["번역문","원문",...],...],null,"감지된언어"]
 */
interface GoogleTranslateApi {

    @GET("translate_a/single")
    suspend fun translate(
        @Query("client") client: String = "gtx",
        @Query("sl") sourceLang: String = "auto",
        @Query("tl") targetLang: String,
        @Query("dt") dt: String = "t",
        @Query("q") text: String
    ): JsonArray
}
