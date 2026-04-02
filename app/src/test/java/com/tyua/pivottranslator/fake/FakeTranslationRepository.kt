package com.tyua.pivottranslator.fake

import com.tyua.pivottranslator.repository.TranslationRepository

/**
 * 테스트용 TranslationRepository Fake 구현
 *
 * FakePivotGateApi를 내부에 포함하며, 번역 결과와 에러를 외부에서 설정할 수 있다.
 */
class FakeTranslationRepository : TranslationRepository(FakePivotGateApi()) {

    private val fakeApi = FakePivotGateApi()

    /** 다음 translateToEnglish 호출 시 반환할 결과 */
    var englishResult: String = "Hello"

    /** 다음 translateToTarget 호출 시 반환할 결과 */
    var targetResult: String = "Salom"

    /** 설정 시 translateToEnglish에서 이 예외를 던진다 */
    var englishException: Exception? = null

    /** 설정 시 translateToTarget에서 이 예외를 던진다 */
    var targetException: Exception? = null

    /** translateToEnglish 호출 횟수 */
    var englishCallCount = 0
        private set

    /** translateToTarget 호출 횟수 */
    var targetCallCount = 0
        private set

    override suspend fun translateToEnglish(sourceText: String): String {
        englishException?.let { throw it }
        englishCallCount++
        return englishResult
    }

    override suspend fun translateToTarget(englishText: String, targetLanguage: String): String {
        targetException?.let { throw it }
        targetCallCount++
        return targetResult
    }
}
