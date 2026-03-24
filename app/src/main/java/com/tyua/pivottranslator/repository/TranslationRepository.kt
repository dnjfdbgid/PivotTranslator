package com.tyua.pivottranslator.repository

import com.tyua.pivottranslator.network.Content
import com.tyua.pivottranslator.network.GeminiApi
import com.tyua.pivottranslator.network.GeminiRequest
import com.tyua.pivottranslator.network.GenerationConfig
import com.tyua.pivottranslator.network.Part
import com.tyua.pivottranslator.network.extractText

/**
 * 3단계 피벗 번역 로직을 조율하는 Repository
 *
 * 흐름: 원문 → 영어 직역 → 영어 다듬기 → 최종 언어 번역
 * API 키는 네트워크 계층(RetrofitClient)에서 자동 주입되므로 여기서는 다루지 않는다.
 */
class TranslationRepository(
    private val api: GeminiApi
) {

    /** 3단계 번역 결과를 한꺼번에 담는 데이터 클래스 */
    data class PivotResult(
        val rawEnglish: String,
        val refinedEnglish: String,
        val finalTranslation: String
    )

    /**
     * 피벗 번역 실행 (suspend — 코루틴에서 호출)
     *
     * @param sourceText      번역할 원문
     * @param targetLanguage  최종 번역 목표 언어 (예: "한국어", "일본어")
     */
    suspend fun translatePivot(sourceText: String, targetLanguage: String): PivotResult {

        // ── 1단계: 원문 → 영어 직역 ──
        val rawEnglish = callGemini(
            systemPrompt = "You are a professional translator. " +
                "Translate the user's text into English as accurately as possible. " +
                "Preserve the original meaning and nuance. " +
                "Output ONLY the translated English text with no explanation.",
            userMessage = sourceText
        )

        // ── 2단계: 영어 문장 다듬기 ──
        val refinedEnglish = callGemini(
            systemPrompt = "You are a native English editor and proofreader. " +
                "Refine the given English text to be more natural, fluent, and grammatically perfect. " +
                "Maintain the original meaning while improving readability. " +
                "Output ONLY the refined English text with no explanation.",
            userMessage = rawEnglish
        )

        // ── 3단계: 다듬어진 영어 → 최종 언어 번역 ──
        val finalTranslation = callGemini(
            systemPrompt = "You are a professional translator specializing in $targetLanguage. " +
                "Translate the given English text into $targetLanguage naturally and fluently. " +
                "The translation should sound like it was originally written in $targetLanguage. " +
                "Output ONLY the translated text with no explanation.",
            userMessage = refinedEnglish
        )

        return PivotResult(
            rawEnglish = rawEnglish,
            refinedEnglish = refinedEnglish,
            finalTranslation = finalTranslation
        )
    }

    /**
     * Gemini API 단일 호출 헬퍼
     *
     * @param systemPrompt  시스템 역할 지시문
     * @param userMessage   사용자 입력 텍스트
     */
    private suspend fun callGemini(systemPrompt: String, userMessage: String): String {
        val request = GeminiRequest(
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            ),
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = userMessage))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.3,
                maxOutputTokens = 2048
            )
        )
        val response = api.generateContent(request)
        return response.extractText()
    }
}
