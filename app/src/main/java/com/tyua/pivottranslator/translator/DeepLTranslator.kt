package com.tyua.pivottranslator.translator

import com.tyua.pivottranslator.network.DeepLJob
import com.tyua.pivottranslator.network.DeepLLangParams
import com.tyua.pivottranslator.network.DeepLParams
import com.tyua.pivottranslator.network.DeepLRpcRequest
import com.tyua.pivottranslator.network.DeepLSentence
import com.tyua.pivottranslator.network.RetrofitClient

/**
 * DeepL 무료 웹 엔드포인트(JSON-RPC)를 이용한 번역 (Retrofit)
 */
object DeepLTranslator {

    private val api = RetrofitClient.deepLApi

    private val languageCodes = mapOf(
        "영어" to "EN",
        "우즈베크어" to "UZ",
        "한국어" to "KO",
        "일본어" to "JA",
        "중국어" to "ZH",
        "스페인어" to "ES",
        "프랑스어" to "FR",
        "독일어" to "DE",
        "포르투갈어" to "PT-BR",
        "이탈리아어" to "IT",
        "러시아어" to "RU",
        "아랍어" to "AR",
        "힌디어" to "HI",
        "태국어" to "TH",
        "베트남어" to "VI",
        "인도네시아어" to "ID",
        "터키어" to "TR"
    )

    suspend fun translate(text: String, targetLanguage: String): String {
        val targetCode = languageCodes[targetLanguage]
            ?: throw IllegalArgumentException(
                "DeepL에서 지원하지 않는 언어입니다: $targetLanguage"
            )

        val request = DeepLRpcRequest(
            params = DeepLParams(
                lang = DeepLLangParams(targetLang = targetCode),
                jobs = listOf(
                    DeepLJob(
                        sentences = listOf(DeepLSentence(text = text))
                    )
                )
            )
        )

        val response = api.translate(request)
        val translations = response.result?.translations
            ?: throw Exception("DeepL 응답에 번역 결과가 없습니다.")

        return buildString {
            for (translation in translations) {
                val sentences = translation.beams.firstOrNull()?.sentences ?: continue
                for (sentence in sentences) {
                    append(sentence.text)
                }
            }
        }
    }
}
