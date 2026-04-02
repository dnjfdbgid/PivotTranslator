package com.tyua.pivottranslator.fake

import com.tyua.pivottranslator.network.DeepLTranslateRequest
import com.tyua.pivottranslator.network.ExpirationResponse
import com.tyua.pivottranslator.network.GoogleTranslateRequest
import com.tyua.pivottranslator.network.PivotGateApi
import com.tyua.pivottranslator.network.TranslateResponse
import java.net.ConnectException

/**
 * 테스트용 PivotGateApi Fake 구현
 *
 * 응답 값과 에러를 외부에서 설정할 수 있다.
 */
class FakePivotGateApi : PivotGateApi {

    /** 다음 DeepL 번역 호출 시 반환할 텍스트 */
    var deepLResult: String = "Hello"

    /** 다음 Google 번역 호출 시 반환할 텍스트 */
    var googleResult: String = "Salom"

    /** 다음 만료일 응답 */
    var expirationDate: String = "20261231"

    /** 설정 시 모든 호출에서 이 예외를 던진다 */
    var exception: Exception? = null

    /** DeepL 번역 호출 횟수 */
    var deepLCallCount = 0
        private set

    /** Google 번역 호출 횟수 */
    var googleCallCount = 0
        private set

    /** 마지막 Google 번역 요청의 targetLang */
    var lastGoogleTargetLang: String? = null
        private set

    override suspend fun getExpiration(apiKey: String): ExpirationResponse {
        exception?.let { throw it }
        return ExpirationResponse(expirationDate)
    }

    override suspend fun translateDeepL(
        apiKey: String,
        request: DeepLTranslateRequest
    ): TranslateResponse {
        exception?.let { throw it }
        deepLCallCount++
        return TranslateResponse(deepLResult)
    }

    override suspend fun translateGoogle(
        apiKey: String,
        request: GoogleTranslateRequest
    ): TranslateResponse {
        exception?.let { throw it }
        googleCallCount++
        lastGoogleTargetLang = request.targetLang
        return TranslateResponse(googleResult)
    }
}
