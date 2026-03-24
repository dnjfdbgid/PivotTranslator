package com.tyua.pivottranslator.repository

import com.tyua.pivottranslator.network.Content
import com.tyua.pivottranslator.network.GeminiApi
import com.tyua.pivottranslator.network.GeminiRequest
import com.tyua.pivottranslator.network.GenerationConfig
import com.tyua.pivottranslator.network.Part
import com.tyua.pivottranslator.network.extractText

/**
 * 피벗 번역 Repository
 *
 * 흐름: 원문 → 영어 직역 (API) → 사용자 수정 → 최종 언어 번역 (API)
 * 2단계(영어 다듬기)는 사용자가 직접 수행하므로 API 호출을 두 단계로 분리한다.
 */
class TranslationRepository(
    private val api: GeminiApi
) {

    /**
     * 1단계: 원문 → 영어 직역
     */
    suspend fun translateToEnglish(sourceText: String): String {
        return callGemini(
            systemPrompt = "You are a professional translator. " +
                "Translate the user's text into English as accurately as possible. " +
                "Preserve the original meaning and nuance. " +
                "Output ONLY the translated English text with no explanation.",
            userMessage = sourceText
        )
    }

    /**
     * 2단계: 사용자가 수정한 영어 → 최종 언어 번역
     */
    suspend fun translateToTarget(englishText: String, targetLanguage: String): String {
        return callGemini(
            systemPrompt = "You are a professional translator specializing in $targetLanguage. " +
                "Translate the given English text into $targetLanguage naturally and fluently. " +
                "The translation should sound like it was originally written in $targetLanguage. " +
                "Output ONLY the translated text with no explanation.",
            userMessage = englishText
        )
    }

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
