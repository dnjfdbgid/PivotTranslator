package com.tyua.pivottranslator.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * DeepL 무료 웹 엔드포인트(JSON-RPC) Retrofit 인터페이스
 */
interface DeepLApi {

    @Headers(
        "Origin: https://www.deepl.com",
        "Referer: https://www.deepl.com/"
    )
    @POST("jsonrpc")
    suspend fun translate(@Body request: DeepLRpcRequest): DeepLRpcResponse
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Request DTO
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class DeepLRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String = "LMT_handle_jobs",
    val id: Int = 1,
    val params: DeepLParams
)

data class DeepLParams(
    @SerializedName("commonJobParams")
    val commonJobParams: DeepLCommonJobParams = DeepLCommonJobParams(),
    val lang: DeepLLangParams,
    val jobs: List<DeepLJob>,
    val priority: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeepLCommonJobParams(
    val mode: String = "translate",
    val textType: String = "plaintext"
)

data class DeepLLangParams(
    @SerializedName("source_lang_user_selected")
    val sourceLang: String = "auto",
    @SerializedName("target_lang")
    val targetLang: String
)

data class DeepLJob(
    val kind: String = "default",
    @SerializedName("preferred_num_beams")
    val preferredNumBeams: Int = 4,
    @SerializedName("raw_en_context_after")
    val rawEnContextAfter: List<String> = emptyList(),
    @SerializedName("raw_en_context_before")
    val rawEnContextBefore: List<String> = emptyList(),
    val sentences: List<DeepLSentence>
)

data class DeepLSentence(
    val id: Int = 0,
    val prefix: String = "",
    val text: String
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Response DTO
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class DeepLRpcResponse(
    val result: DeepLResult?
)

data class DeepLResult(
    val translations: List<DeepLTranslation>
)

data class DeepLTranslation(
    val beams: List<DeepLBeam>
)

data class DeepLBeam(
    val sentences: List<DeepLBeamSentence>
)

data class DeepLBeamSentence(
    val text: String
)
