package com.tyua.pivottranslator.repository

import com.tyua.pivottranslator.fake.FakePivotGateApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ConnectException

/**
 * TranslationRepository 단위 테스트
 *
 * FakePivotGateApi를 주입하여 네트워크 없이 Repository 로직을 검증한다.
 */
class TranslationRepositoryTest {

    private lateinit var fakeApi: FakePivotGateApi
    private lateinit var repository: TranslationRepository

    @Before
    fun setUp() {
        fakeApi = FakePivotGateApi()
        repository = TranslationRepository(fakeApi)
    }

    // ── 1단계: 영어 번역 ──

    @Test
    fun `translateToEnglish - 정상 응답 시 번역 텍스트를 반환한다`() = runTest {
        fakeApi.deepLResult = "Hello, nice to meet you."

        val result = repository.translateToEnglish("안녕하세요, 만나서 반갑습니다.")

        assertEquals("Hello, nice to meet you.", result)
        assertEquals(1, fakeApi.deepLCallCount)
    }

    @Test
    fun `translateToEnglish - API 에러 시 예외가 전파된다`() = runTest {
        fakeApi.exception = ConnectException("Connection refused")

        try {
            repository.translateToEnglish("테스트")
            fail("예외가 발생해야 한다")
        } catch (e: ConnectException) {
            assertEquals("Connection refused", e.message)
        }
    }

    // ── 2단계: 타겟 언어 번역 ──

    @Test
    fun `translateToTarget - 정상 응답 시 번역 텍스트를 반환한다`() = runTest {
        fakeApi.googleResult = "Salom, tanishganimdan xursandman."

        val result = repository.translateToTarget("Hello, nice to meet you.", "우즈베크어")

        assertEquals("Salom, tanishganimdan xursandman.", result)
        assertEquals(1, fakeApi.googleCallCount)
        assertEquals("uz", fakeApi.lastGoogleTargetLang)
    }

    @Test
    fun `translateToTarget - 다양한 언어 코드가 올바르게 매핑된다`() = runTest {
        val languageMappings = mapOf(
            "한국어" to "ko",
            "일본어" to "ja",
            "중국어" to "zh-CN",
            "스페인어" to "es",
            "프랑스어" to "fr",
            "독일어" to "de",
            "러시아어" to "ru",
            "아랍어" to "ar"
        )

        for ((koreanName, expectedCode) in languageMappings) {
            fakeApi.googleCallCount.let { /* reset 불필요, lastGoogleTargetLang만 확인 */ }
            repository.translateToTarget("Hello", koreanName)
            assertEquals("$koreanName → $expectedCode", expectedCode, fakeApi.lastGoogleTargetLang)
        }
    }

    @Test
    fun `translateToTarget - 지원하지 않는 언어는 IllegalArgumentException을 발생시킨다`() = runTest {
        try {
            repository.translateToTarget("Hello", "지원하지않는언어")
            fail("예외가 발생해야 한다")
        } catch (e: IllegalArgumentException) {
            assertEquals("지원하지 않는 언어입니다: 지원하지않는언어", e.message)
        }
    }

    @Test
    fun `translateToTarget - API 에러 시 예외가 전파된다`() = runTest {
        fakeApi.exception = RuntimeException("서버 내부 오류")

        try {
            repository.translateToTarget("Hello", "우즈베크어")
            fail("예외가 발생해야 한다")
        } catch (e: RuntimeException) {
            assertEquals("서버 내부 오류", e.message)
        }
    }
}
