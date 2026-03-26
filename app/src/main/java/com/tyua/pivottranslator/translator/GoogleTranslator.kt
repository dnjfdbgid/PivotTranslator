package com.tyua.pivottranslator.translator

import com.tyua.pivottranslator.network.RetrofitClient

/**
 * Google Translate 무료 웹 엔드포인트를 이용한 번역 (Retrofit)
 */
object GoogleTranslator {

    private val api = RetrofitClient.googleTranslateApi

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

    suspend fun translate(text: String, targetLanguage: String): String {
        val targetCode = languageCodes[targetLanguage]
            ?: throw IllegalArgumentException("지원하지 않는 언어입니다: $targetLanguage")

        val jsonArray = api.translate(targetLang = targetCode, text = text)
        val sentences = jsonArray[0].asJsonArray

        return buildString {
            for (i in 0 until sentences.size()) {
                val element = sentences[i]
                if (element.isJsonArray) {
                    append(element.asJsonArray[0].asString)
                }
            }
        }
    }
}
