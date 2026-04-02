package com.tyua.pivottranslator.repository

import com.tyua.pivottranslator.BuildConfig
import com.tyua.pivottranslator.network.DeepLTranslateRequest
import com.tyua.pivottranslator.network.GoogleTranslateRequest
import com.tyua.pivottranslator.network.RetrofitClient

/**
 * 피벗 번역 Repository
 *
 * 흐름: 원문 → 영어 직역 (PivotGate DeepL) → 사용자 수정 → 최종 언어 번역 (PivotGate Google)
 */
class TranslationRepository {

    private val pivotGateApi = RetrofitClient.pivotGateApi

    /** 한글 언어명 → 언어 코드 매핑 (PivotGate Google 번역 요청용) */
    private val languageCodes = mapOf(
        "영어" to "en",
        "우즈베크어" to "uz",
        "한국어" to "ko",
        "일본어" to "ja",
        "중국어" to "zh-CN",
        "스페인어" to "es",
        "프랑스어" to "fr",
        "독일어" to "de",
        "포르투갈어" to "pt",
        "이탈리아어" to "it",
        "러시아어" to "ru",
        "아랍어" to "ar",
        "힌디어" to "hi",
        "태국어" to "th",
        "베트남어" to "vi",
        "인도네시아어" to "id",
        "터키어" to "tr"
    )

    /**
     * 1단계: 원문 → 영어 직역 (PivotGate DeepL API)
     */
    suspend fun translateToEnglish(sourceText: String): String {
        val response = pivotGateApi.translateDeepL(
            apiKey = BuildConfig.PIVOT_GATE_API_KEY,
            request = DeepLTranslateRequest(text = sourceText)
        )
        return response.translatedText
    }

//    /** 1단계 (기존 직접 호출 방식 — 백업) */
//    suspend fun translateToEnglish(sourceText: String): String =
//        // DeepL HTTP 429(Too Many Requests) 에러로 인해 구글 번역으로 대체
//        // DeepLTranslator.translate(sourceText, "영어")
//        GoogleTranslator.translate(sourceText, "영어")

    /**
     * 2단계: 사용자가 수정한 영어 → 최종 언어 번역 (PivotGate Google API)
     */
    suspend fun translateToTarget(englishText: String, targetLanguage: String): String {
        val targetCode = languageCodes[targetLanguage]
            ?: throw IllegalArgumentException("지원하지 않는 언어입니다: $targetLanguage")

        val response = pivotGateApi.translateGoogle(
            apiKey = BuildConfig.PIVOT_GATE_API_KEY,
            request = GoogleTranslateRequest(text = englishText, targetLang = targetCode)
        )
        return response.translatedText
    }

//    /** 2단계 (기존 직접 호출 방식 — 백업) */
//    suspend fun translateToTarget(englishText: String, targetLanguage: String): String =
//        GoogleTranslator.translate(englishText, targetLanguage)
}
