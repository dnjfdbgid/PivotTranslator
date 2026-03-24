package com.tyua.pivottranslator.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Gemini REST API 엔드포인트 정의
 * API 키는 RetrofitClient의 인터셉터가 자동으로 추가하므로 여기서는 다루지 않는다.
 */
interface GeminiApi {

    /** gemini-2.5-flash 모델에 텍스트 생성 요청 */
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Body request: GeminiRequest
    ): GeminiResponse
}
