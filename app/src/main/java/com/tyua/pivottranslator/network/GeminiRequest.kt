package com.tyua.pivottranslator.network

/**
 * Gemini API generateContent 요청 바디
 *
 * 구조 예시:
 * {
 *   "systemInstruction": { "parts": [{ "text": "..." }] },
 *   "contents": [{ "role": "user", "parts": [{ "text": "..." }] }],
 *   "generationConfig": { "temperature": 0.3, "maxOutputTokens": 2048 }
 * }
 */
data class GeminiRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

/** 하나의 메시지 단위 (시스템 지시 또는 사용자 메시지) */
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

/** 메시지 내 텍스트 파트 */
data class Part(
    val text: String
)

/** 생성 옵션 (온도, 최대 토큰 등) */
data class GenerationConfig(
    val temperature: Double = 0.3,
    val maxOutputTokens: Int = 2048
)
