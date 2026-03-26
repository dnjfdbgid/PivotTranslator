package com.tyua.pivottranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tyua.pivottranslator.config.AppConfig
import com.tyua.pivottranslator.preferences.PreferencesManager
import com.tyua.pivottranslator.repository.TranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 번역 화면의 UI 상태
 *
 * 흐름: Idle → Loading → Editing → Loading → Success / Error
 */
sealed interface TranslationUiState {

    /** 대기 — 아직 번역을 시작하지 않음 */
    data object Idle : TranslationUiState

    /** API 호출 진행 중 */
    data object Loading : TranslationUiState

    /** 1단계 완료 — 사용자가 영어를 확인/수정하는 단계 */
    data class Editing(val englishText: String) : TranslationUiState

    /** 최종 번역 완료 */
    data class Success(
        val editedEnglish: String,
        val finalTranslation: String
    ) : TranslationUiState

    /** 에러 발생 */
    data class Error(val message: String) : TranslationUiState
}

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranslationRepository()
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val _sourceText = MutableStateFlow("")
    val sourceText: StateFlow<String> = _sourceText.asStateFlow()

    private val _targetLanguage = MutableStateFlow("우즈베크어")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    private val _autoTranslateDelay = MutableStateFlow(preferencesManager.autoTranslateDelay)
    val autoTranslateDelay: StateFlow<Int> = _autoTranslateDelay.asStateFlow()

    /** 카운트다운 중 남은 초, null이면 카운트다운 비활성 */
    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    /** 번역 기능 만료 여부 */
    val isExpired: Boolean = run {
        val expiration = LocalDate.parse(
            AppConfig.EXPIRATION_DATE,
            DateTimeFormatter.ofPattern("yyyyMMdd")
        )
        LocalDate.now().isAfter(expiration)
    }

    private var autoTranslateJob: Job? = null

    fun updateSourceText(text: String) {
        _sourceText.value = text
        if (isExpired) return
        val state = _uiState.value
        if (text.isNotBlank() && (state is TranslationUiState.Idle || state is TranslationUiState.Error || state is TranslationUiState.Editing)) {
            scheduleAutoTranslate()
        } else {
            cancelAutoTranslate()
        }
    }

    fun updateTargetLanguage(language: String) {
        _targetLanguage.value = language
    }

    fun updateAutoTranslateDelay(seconds: Int) {
        val clamped = seconds.coerceIn(
            PreferencesManager.MIN_DELAY,
            PreferencesManager.MAX_DELAY
        )
        _autoTranslateDelay.value = clamped
        preferencesManager.autoTranslateDelay = clamped
        // 타이머 진행 중이면 새 설정으로 재스케줄
        if (autoTranslateJob?.isActive == true) {
            scheduleAutoTranslate()
        }
    }

    private fun scheduleAutoTranslate() {
        autoTranslateJob?.cancel()
        autoTranslateJob = viewModelScope.launch {
            for (i in _autoTranslateDelay.value downTo 1) {
                _remainingSeconds.value = i
                delay(1000L)
            }
            _remainingSeconds.value = null
            translateToEnglish()
        }
    }

    private fun cancelAutoTranslate() {
        autoTranslateJob?.cancel()
        autoTranslateJob = null
        _remainingSeconds.value = null
    }

    /**
     * 1단계: 원문 → 영어 직역 (DeepL)
     * 완료 후 Editing 상태로 전환하여 사용자가 영어를 수정할 수 있게 한다.
     */
    fun translateToEnglish() {
        val text = _sourceText.value
        if (text.isBlank() || isExpired) return
        cancelAutoTranslate()

        viewModelScope.launch {
            _uiState.value = TranslationUiState.Loading
            try {
                val english = repository.translateToEnglish(text)
                _uiState.value = TranslationUiState.Editing(englishText = english)
            } catch (e: UnknownHostException) {
                _uiState.value = TranslationUiState.Error("네트워크 연결을 확인해 주세요.")
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 2단계: 사용자가 수정한 영어 → 최종 언어 번역 (구글 번역)
     *
     * @param editedEnglish 사용자가 확인/수정한 영어 텍스트
     */
    fun translateToTarget(editedEnglish: String) {
        if (editedEnglish.isBlank() || isExpired) return

        viewModelScope.launch {
            _uiState.value = TranslationUiState.Loading
            try {
                val result = repository.translateToTarget(
                    englishText = editedEnglish,
                    targetLanguage = _targetLanguage.value
                )
                _uiState.value = TranslationUiState.Success(
                    editedEnglish = editedEnglish,
                    finalTranslation = result
                )
            } catch (e: UnknownHostException) {
                _uiState.value = TranslationUiState.Error("네트워크 연결을 확인해 주세요.")
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다."
                )
            }
        }
    }

    /** 상태를 Idle로 초기화 */
    fun resetState() {
        cancelAutoTranslate()
        _uiState.value = TranslationUiState.Idle
    }
}
