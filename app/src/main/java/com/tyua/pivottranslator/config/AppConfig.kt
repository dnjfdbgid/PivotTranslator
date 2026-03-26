package com.tyua.pivottranslator.config

/**
 * 앱 설정 상수
 *
 * [EXPIRATION_DATE]를 수정하여 번역 기능의 만료일을 설정한다.
 * 만료일이 지나면 번역 기능이 비활성화된다.
 */
object AppConfig {

    /** 번역 기능 만료일 (yyyyMMdd 형식) */
    const val EXPIRATION_DATE = "20000101"
}
