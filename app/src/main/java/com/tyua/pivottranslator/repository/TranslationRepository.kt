package com.tyua.pivottranslator.repository

import com.tyua.pivottranslator.translator.DeepLTranslator
import com.tyua.pivottranslator.translator.GoogleTranslator

/**
 * 피벗 번역 Repository
 *
 * 흐름: 원문 → 영어 직역 (DeepL) → 사용자 수정 → 최종 언어 번역 (구글 번역)
 */
class TranslationRepository {

    /**
     * 1단계: 원문 → 영어 직역
     * DeepL HTTP 429(Too Many Requests) 에러로 인해 구글 번역으로 대체
     */
    suspend fun translateToEnglish(sourceText: String): String =
//        DeepLTranslator.translate(sourceText, "영어")
        GoogleTranslator.translate(sourceText, "영어")

    /**
     * 2단계: 사용자가 수정한 영어 → 최종 언어 번역 (구글 번역)
     */
    suspend fun translateToTarget(englishText: String, targetLanguage: String): String =
        GoogleTranslator.translate(englishText, targetLanguage)
}
