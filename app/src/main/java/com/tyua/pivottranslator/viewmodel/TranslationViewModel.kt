package com.tyua.pivottranslator.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tyua.pivottranslator.BuildConfig
import com.tyua.pivottranslator.config.AppConfig
import com.tyua.pivottranslator.network.RetrofitClient
import com.tyua.pivottranslator.preferences.PreferencesManager
import com.tyua.pivottranslator.repository.TranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
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

/**
 * 앱 활성화 상태
 *
 * 서버에서 만료일을 조회하여 앱 사용 가능 여부를 결정한다.
 */
sealed interface AppActivationState {
    /** 서버 조회 중 */
    data object Checking : AppActivationState

    /** 사용 가능 */
    data object Active : AppActivationState

    /** 만료됨 */
    data object Expired : AppActivationState

    /** 서버 접속 실패 */
    data class ServerError(val message: String) : AppActivationState
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

    /** 2단계 번역 에러 메시지 (Editing 상태 유지 시 스낵바로 표시) */
    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError.asStateFlow()

    /** 앱 활성화 상태 (서버 조회 완료 전까지 Checking) */
    private val _activationState = MutableStateFlow<AppActivationState>(AppActivationState.Checking)
    val activationState: StateFlow<AppActivationState> = _activationState.asStateFlow()

    /** 번역 기능 비활성 여부 (Active 상태가 아니면 모두 비활성) */
    private val isDisabled: Boolean
        get() = _activationState.value !is AppActivationState.Active

    private var autoTranslateJob: Job? = null

    /** 서버 에러 전환 전 UI 상태 저장 (재연결 시 복원용) */
    private var savedUiState: TranslationUiState? = null

    init {
        fetchExpirationFromServer()
    }

    /** 서버 연결 에러인지 판별 */
    private fun isServerConnectionError(e: Exception): Boolean =
        e is SocketTimeoutException || e is ConnectException || e is UnknownHostException

    /**
     * PivotGate 서버에서 만료일을 조회하여 앱 활성화 상태를 결정한다.
     * 서버 연결 실패 시 앱 사용을 차단한다.
     */
    private fun fetchExpirationFromServer(restoreState: Boolean = false) {
        viewModelScope.launch {
            try {
                // TCP 소켓으로 서버 접속 가능 여부를 먼저 확인 (ECONNREFUSED 즉시 감지)
                checkServerReachable()

                val response = RetrofitClient.pivotGateApi.getExpiration(
                    apiKey = BuildConfig.PIVOT_GATE_API_KEY
                )
                val expiration = LocalDate.parse(
                    response.expirationDate,
                    DateTimeFormatter.ofPattern("yyyyMMdd")
                )
                _activationState.value = if (LocalDate.now().isAfter(expiration)) {
                    AppActivationState.Expired
                } else {
                    AppActivationState.Active
                }
                // 재연결 시 이전 UI 상태 복원
                if (restoreState && savedUiState != null) {
                    _uiState.value = savedUiState!!
                    savedUiState = null
                }
                Log.d("TranslationViewModel", "서버 만료일 적용: ${response.expirationDate}")
            } catch (e: UnknownHostException) {
                _activationState.value = AppActivationState.ServerError(
                    "서버에 접속할 수 없습니다.\n네트워크 연결을 확인해 주세요."
                )
                Log.w("TranslationViewModel", "만료일 서버 조회 실패 (네트워크)", e)
            } catch (e: ConnectException) {
                _activationState.value = AppActivationState.ServerError(
                    "서버가 실행되고 있지 않습니다.\n(Connection refused)"
                )
                Log.w("TranslationViewModel", "만료일 서버 접속 거부", e)
            } catch (e: Exception) {
                _activationState.value = AppActivationState.ServerError(
                    "서버에 접속할 수 없습니다.\n잠시 후 다시 시도해 주세요."
                )
                Log.w("TranslationViewModel", "만료일 서버 조회 실패", e)
            }
        }
    }

    /**
     * TCP 소켓으로 서버 접속 가능 여부를 사전 확인한다.
     * - 서버가 꺼져 있으면 ConnectException(ECONNREFUSED)이 즉시 발생한다.
     * - 호스트 자체에 도달 불가하면 2초 타임아웃으로 빠르게 실패한다.
     */
    private suspend fun checkServerReachable() = withContext(Dispatchers.IO) {
        val uri = URI(AppConfig.PIVOT_GATE_BASE_URL)
        val host = uri.host
        val port = if (uri.port != -1) uri.port else 80
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 2000)
        }
    }

    /** 서버 재접속 시도 — 성공 시 이전 UI 상태 복원 */
    fun retryServerConnection() {
        _activationState.value = AppActivationState.Checking
        fetchExpirationFromServer(restoreState = true)
    }

    /**
     * 번역 중 서버 연결 에러 발생 시 호출.
     * 현재 UI 상태를 저장하고 서버 에러 화면으로 전환한다.
     */
    private fun switchToServerError(currentUiState: TranslationUiState, message: String) {
        savedUiState = currentUiState
        _activationState.value = AppActivationState.ServerError(message)
    }

    fun updateSourceText(text: String) {
        _sourceText.value = text
        if (isDisabled) return
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
        if (text.isBlank() || isDisabled) return
        cancelAutoTranslate()

        val stateBeforeLoading = _uiState.value
        viewModelScope.launch {
            _uiState.value = TranslationUiState.Loading
            try {
                val english = repository.translateToEnglish(text)
                _uiState.value = TranslationUiState.Editing(englishText = english)
            } catch (e: Exception) {
                if (isServerConnectionError(e)) {
                    _uiState.value = stateBeforeLoading
                    switchToServerError(stateBeforeLoading, "서버에 접속할 수 없습니다.\n네트워크 연결을 확인해 주세요.")
                } else {
                    _uiState.value = TranslationUiState.Error(
                        e.message ?: "알 수 없는 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    /**
     * 2단계: 사용자가 수정한 영어 → 최종 언어 번역 (구글 번역)
     *
     * @param editedEnglish 사용자가 확인/수정한 영어 텍스트
     */
    fun translateToTarget(editedEnglish: String) {
        if (editedEnglish.isBlank() || isDisabled) return

        val editingState = TranslationUiState.Editing(englishText = editedEnglish)
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
            } catch (e: Exception) {
                if (isServerConnectionError(e)) {
                    _uiState.value = editingState
                    switchToServerError(editingState, "서버에 접속할 수 없습니다.\n네트워크 연결을 확인해 주세요.")
                } else {
                    _uiState.value = editingState
                    _translationError.value = e.message ?: "알 수 없는 오류가 발생했습니다."
                }
            }
        }
    }

    /** 번역 에러 메시지 소비 완료 */
    fun clearTranslationError() {
        _translationError.value = null
    }

    /** 상태를 Idle로 초기화 */
    fun resetState() {
        cancelAutoTranslate()
        _uiState.value = TranslationUiState.Idle
    }
}
