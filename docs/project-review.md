# PivotTranslator 프로젝트 리뷰

## 1. 프로젝트 개요

### 목적
**피벗 번역** — 번역 품질을 높이기 위해 영어를 중간 언어로 활용하는 2단계 번역 앱

### 왜 피벗 번역인가?
- 직접 번역(예: 한국어 → 우즈베크어)은 학습 데이터가 적어 품질이 낮음
- 영어를 거치면 각 구간의 번역 품질이 높아짐
- **핵심 차별점**: 사용자가 중간 영어 텍스트를 직접 수정할 수 있음

### 번역 흐름
```
원문 입력 → [자동] 영어 번역 → 사용자가 영어 검토/수정 → [수동] 대상 언어 번역
```

### 지원 언어 (16개)
우즈베크어(기본), 한국어, 일본어, 중국어, 스페인어, 프랑스어, 독일어, 포르투갈어, 이탈리아어, 러시아어, 아랍어, 힌디어, 태국어, 베트남어, 인도네시아어, 터키어

---

## 2. 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material3 | BOM 2026.03.00 |
| 아키텍처 | MVVM (ViewModel + StateFlow) | - |
| 네트워크 | Retrofit2 + OkHttp | 2.11.0 / 4.12.0 |
| 직렬화 | Gson | 2.11.0 |
| 비동기 | Kotlin Coroutines | 1.9.0 |
| 네비게이션 | Navigation Compose | 2.8.9 |
| 영속 저장 | SharedPreferences | - |
| 빌드 | Gradle (Kotlin DSL) + AGP | 9.1.0 |
| Target SDK | 36 | Min SDK 26 |

### DI 프레임워크를 사용하지 않은 이유
- 소스 파일 15개, 총 1,188줄 규모의 소형 프로젝트
- 의존성 그래프가 단순 (ViewModel → Repository → Translator → RetrofitClient)
- Hilt/Koin 도입 시 보일러플레이트 대비 이점이 없음

### API 키 없이 동작
- Google Translate 무료 웹 엔드포인트 (`translate.googleapis.com`) 사용
- 별도 API 키 발급/관리 불필요

---

## 3. 아키텍처

> 상세 다이어그램은 `docs/diagrams.md` 참조

### 계층 구조
```
UI Layer          →  TranslationScreen, TranslationScreenContent, 하위 컴포넌트
ViewModel Layer   →  TranslationViewModel + TranslationUiState (sealed interface)
Data Layer        →  TranslationRepository, PreferencesManager, AppConfig
Network Layer     →  GoogleTranslator, DeepLTranslator(비활성), RetrofitClient
External          →  Google Translate API
```

### 데이터 흐름
```
TranslationScreen
  ↕ collectAsState()
TranslationViewModel
  ↓ translate 요청
TranslationRepository
  ↓ 위임
GoogleTranslator
  ↓ Retrofit
Google Translate API
```

---

## 4. 핵심 코드 리뷰 포인트

### 4-1. UI 상태 관리 — sealed interface

**파일**: `viewmodel/TranslationViewModel.kt:24-43`

```kotlin
sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data object Loading : TranslationUiState
    data class Editing(val englishText: String) : TranslationUiState
    data class Success(val editedEnglish: String, val finalTranslation: String) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}
```

**설계 의도**:
- `when` 분기에서 컴파일 타임 완전성 검사 (모든 상태 처리 강제)
- 각 상태에 필요한 데이터만 포함 (Editing에는 englishText, Success에는 결과 2개)
- `data object`로 싱글턴 상태, `data class`로 데이터 보유 상태 구분

### 4-2. 자동 번역 타이머 — Coroutine Job 관리

**파일**: `viewmodel/TranslationViewModel.kt:77-115`

```kotlin
fun updateSourceText(text: String) {
    _sourceText.value = text
    if (isExpired) return
    val state = _uiState.value
    if (text.isNotBlank() && (state is Idle || state is Error || state is Editing)) {
        scheduleAutoTranslate()
    } else {
        cancelAutoTranslate()
    }
}

private fun scheduleAutoTranslate() {
    autoTranslateJob?.cancel()       // 기존 타이머 취소
    autoTranslateJob = viewModelScope.launch {
        for (i in _autoTranslateDelay.value downTo 1) {
            _remainingSeconds.value = i
            delay(1000L)
        }
        _remainingSeconds.value = null
        translateToEnglish()         // 카운트다운 완료 시 번역 시작
    }
}
```

**설계 의도**:
- 입력할 때마다 기존 타이머 취소 후 재시작 (디바운스)
- Idle, Error, Editing 상태에서만 타이머 활성화
- `viewModelScope`로 생명주기 안전한 코루틴 관리

### 4-3. Presenter 패턴 — 상태/콜백 분리

**파일**: `ui/screen/TranslationScreen.kt:62-86`

```kotlin
fun TranslationScreen(viewModel: TranslationViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // ... 상태 수집

    TranslationScreenContent(
        uiState = uiState,
        onSourceTextChange = viewModel::updateSourceText,
        onTranslateToTarget = viewModel::translateToTarget,
        // ... 콜백 연결
    )
}
```

**설계 의도**:
- `TranslationScreen`: 상태 수집 + 콜백 바인딩 (ViewModel 의존)
- `TranslationScreenContent`: 순수 Composable (상태/콜백만 받음, ViewModel 무관)
- `@Preview` 지원 가능 (ViewModel 없이 렌더링)

### 4-4. Repository — 번역 서비스 추상화

**파일**: `repository/TranslationRepository.kt`

```kotlin
suspend fun translateToEnglish(sourceText: String): String =
//  DeepLTranslator.translate(sourceText, "영어")    // HTTP 429로 비활성
    GoogleTranslator.translate(sourceText, "영어")

suspend fun translateToTarget(englishText: String, targetLanguage: String): String =
    GoogleTranslator.translate(englishText, targetLanguage)
```

**설계 의도**:
- 번역 서비스 교체 지점을 Repository 한 곳으로 집중
- DeepL 복구 시 주석 한 줄만 변경하면 됨

### 4-5. 네트워크 — 공유 OkHttpClient

**파일**: `network/RetrofitClient.kt`

```kotlin
object RetrofitClient {
    private val okHttpClient: OkHttpClient by lazy { ... }  // 공유

    val googleTranslateApi: GoogleTranslateApi by lazy {
        Retrofit.Builder().client(okHttpClient).build().create(...)
    }
    val deepLApi: DeepLApi by lazy {
        Retrofit.Builder().client(okHttpClient).build().create(...)
    }
}
```

**설계 의도**:
- 커넥션 풀, 타임아웃, 인터셉터를 두 API가 공유
- `by lazy`로 실제 사용 시점까지 초기화 지연

---

## 5. 주요 기능 상세

| 기능 | 설명 | 관련 코드 |
|------|------|-----------|
| 자동 번역 | 입력 후 N초 대기 → 자동으로 영어 번역 시작 | `scheduleAutoTranslate()` |
| 카운트다운 | 남은 초를 실시간 표시, 추가 입력 시 리셋 | `_remainingSeconds` StateFlow |
| 대기 시간 설정 | 4~10초, +/- 버튼, 앱 재시작 후에도 유지 | `PreferencesManager` |
| 영어 편집 | 1차 영어 결과를 사용자가 직접 수정 가능 | `EditingSection` + `rememberSaveable` |
| 클립보드 복사 | 최종 결과 자동 복사 + 스낵바 알림 | `SuccessSection` + `LaunchedEffect` |
| 번역 기능 만료 | `EXPIRATION_DATE` 이후 전체 기능 비활성화 | `AppConfig` + `isExpired` |

---

## 6. 보안 & 빌드

### 보안
- `keystore/`, `*.jks` → `.gitignore`에 포함
- `local.properties`에 키스토어 크레덴셜 → Git 미추적
- `proguard-rules.pro`에 Retrofit, OkHttp, Gson, Coroutines, DeepL DTO 규칙 적용

### 릴리즈 빌드
```bash
./gradlew assembleRelease
# → build/release/PivotTranslator_release_yyyyMMdd.apk 자동 생성
```

---

## 7. 알려진 제약 & 향후 계획

### 현재 제약
| 항목 | 상태 | 영향 |
|------|------|------|
| DeepL API | HTTP 429로 비활성 | 1단계도 Google Translate 사용 중 |
| 무료 엔드포인트 | API 키 없는 비공식 엔드포인트 | 트래픽 제한/차단 가능성 |
| 만료일 하드코딩 | `AppConfig`에 상수로 관리 | 만료 시 앱 업데이트 필요 |
| 에러 후 복구 | 2단계 에러 시에도 1단계부터 재시작 | 사용자가 영어 편집 내용을 잃음 |
| 테스트 | 최소한의 예제 테스트만 존재 | 회귀 테스트 커버리지 부족 |

### 향후 개선 가능 방향
- DeepL 유료 API 또는 대안 서비스 도입 검토
- 만료일 서버 기반 관리로 전환
- 2단계 에러 시 영어 편집 상태 보존 후 재시도
- 번역 히스토리 저장 기능
- 오프라인 캐시 지원

---

## 8. 프로젝트 규모

| 항목 | 수치 |
|------|------|
| 소스 파일 | 15개 (.kt) |
| 총 코드 라인 | 1,188줄 |
| 커밋 수 | 18개 |
| 개발 기간 | 2026.03.24 ~ 현재 |
| 지원 언어 | 16개 |
