package com.tyua.pivottranslator.network

/**
 * Gemini API generateContent 응답
 *
 * 구조 예시:
 * {
 *   "candidates": [{
 *     "content": { "parts": [{ "text": "..." }], "role": "model" },
 *     "finishReason": "STOP"
 *   }]
 * }
 */
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: CandidateContent?,
    val finishReason: String?
)

/** 응답 전용 Content — 요청의 Content와 분리하여 역직렬화 충돌 방지 */
data class CandidateContent(
    val parts: List<CandidatePart>?,
    val role: String?
)

data class CandidatePart(
    val text: String?
)

/** 응답에서 텍스트만 간편하게 추출하는 확장 함수 */
fun GeminiResponse.extractText(): String {
    return candidates
        ?.firstOrNull()
        ?.content
        ?.parts
        ?.firstOrNull()
        ?.text
        ?.trim()
        .orEmpty()
}
