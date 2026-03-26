package com.tyua.pivottranslator.preferences

import android.content.Context

/**
 * 앱 설정을 SharedPreferences로 관리
 */
class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 자동 번역 대기 시간 (초), 기본값 6, 범위 4~10 */
    var autoTranslateDelay: Int
        get() = prefs.getInt(KEY_AUTO_TRANSLATE_DELAY, DEFAULT_DELAY)
        set(value) = prefs.edit()
            .putInt(KEY_AUTO_TRANSLATE_DELAY, value.coerceIn(MIN_DELAY, MAX_DELAY))
            .apply()

    companion object {
        const val DEFAULT_DELAY = 6
        const val MIN_DELAY = 4
        const val MAX_DELAY = 10
        private const val KEY_AUTO_TRANSLATE_DELAY = "auto_translate_delay"
    }
}
