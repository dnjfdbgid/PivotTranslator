package com.tyua.pivottranslator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyua.pivottranslator.network.RetrofitClient
import com.tyua.pivottranslator.repository.TranslationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException

/**
 * 번역 화면의 UI 상태를 나타내는 sealed interface
 */
sealed interface TranslationUiState {

    /** 대기 상태 — 아직 번역을 시작하지 않음 */
    data object Idle : TranslationUiState

    /** 번역 진행 중 */
    data object Loading : TranslationUiState

    /** 번역 성공 — 3단계 결과를 모두 포함 */
    data class Success(
        val rawEnglish: String,
        val refinedEnglish: String,
        val finalTranslation: String
    ) : TranslationUiState

    /** 에러 발생 */
    data class Error(val message: String) : TranslationUiState
}

class TranslationViewModel : ViewModel() {

    private val repository = TranslationRepository(
        api = RetrofitClient.geminiApi
    )

    // ── UI 상태 (번역 결과) ──
    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    // ── 사용자 입력 필드 ──
    private val _sourceText = MutableStateFlow("")
    val sourceText: StateFlow<String> = _sourceText.asStateFlow()

    private val _targetLanguage = MutableStateFlow("우즈베크어")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    /** 원문 텍스트 업데이트 */
    fun updateSourceText(text: String) {
        _sourceText.value = text
    }

    /** 도착 언어 변경 */
    fun updateTargetLanguage(language: String) {
        _targetLanguage.value = language
    }

    /**
     * 3단계 피벗 번역 실행
     *
     * 흐름: Idle → Loading → Success / Error
     */
    fun translate() {
        val text = _sourceText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.value = TranslationUiState.Loading

            try {
                val result = repository.translatePivot(
                    sourceText = text,
                    targetLanguage = _targetLanguage.value
                )
                _uiState.value = TranslationUiState.Success(
                    rawEnglish = result.rawEnglish,
                    refinedEnglish = result.refinedEnglish,
                    finalTranslation = result.finalTranslation
                )
            } catch (e: UnknownHostException) {
                _uiState.value = TranslationUiState.Error(
                    "네트워크 연결을 확인해 주세요."
                )
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다."
                )
            }
        }
    }

    /** 상태를 Idle로 초기화 */
    fun resetState() {
        _uiState.value = TranslationUiState.Idle
    }
}
