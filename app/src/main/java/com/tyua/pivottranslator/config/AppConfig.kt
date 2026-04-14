package com.tyua.pivottranslator.config

/**
 * 앱 설정 상수
 *
 * 만료일은 PivotGate 서버에서 조회한다.
 * 서버 접속 실패 시 앱 사용이 차단된다.
 */
object AppConfig {

    /** Google Translate 기본 URL */
    const val GOOGLE_TRANSLATE_BASE_URL = "https://translate.googleapis.com/"

    /** DeepL JSON-RPC 기본 URL */
    const val DEEPL_BASE_URL = "https://www2.deepl.com/"

    /** PivotGate API 서버 기본 URL */
//    const val PIVOT_GATE_BASE_URL = "http://10.0.2.2:8000/"
    const val PIVOT_GATE_BASE_URL = "http://claude.surem.com:19000/"
}
