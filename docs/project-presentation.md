# PivotTranslator 프로젝트 발표

## 1. 프로젝트 소개

### 앱 이름
**Pivot Translator**

### 개발 배경
- 한국어 → 우즈베크어처럼 학습 데이터가 적은 언어 쌍은 직접 번역 시 품질이 낮음
- 영어를 중간 언어(피벗)로 활용하면 각 구간의 번역 품질이 높아짐
- 기존 번역기는 중간 결과를 확인/수정할 수 없음

### 핵심 아이디어
> 사용자가 영어 번역 결과를 직접 검토하고 수정한 뒤 최종 번역을 진행한다.

### 번역 흐름
```
원문 입력
  ↓ (자동, 카운트다운 후)
영어 번역 결과 표시
  ↓ (사용자가 영어를 검토/수정)
대상 언어로 최종 번역
  ↓ (자동 클립보드 복사)
결과 확인
```

---

## 2. 주요 기능

| 기능 | 설명 |
|------|------|
| 피벗 번역 | 원문 → 영어 → 대상 언어 2단계 번역 |
| 영어 편집 | 1차 영어 번역 결과를 사용자가 직접 수정 가능 |
| 자동 번역 | 텍스트 입력 후 N초 대기 → 자동으로 영어 번역 시작 |
| 카운트다운 표시 | 자동 번역까지 남은 시간을 실시간 표시 |
| 대기 시간 설정 | 4~10초 범위에서 조절 가능, 앱 재시작 후에도 유지 |
| 클립보드 자동 복사 | 최종 번역 결과 자동 복사 + 스낵바 알림 |
| 번역 기능 만료 | 설정된 날짜 이후 번역 기능 비활성화 |

### 지원 언어 (16개)
우즈베크어(기본), 한국어, 일본어, 중국어, 스페인어, 프랑스어, 독일어, 포르투갈어, 이탈리아어, 러시아어, 아랍어, 힌디어, 태국어, 베트남어, 인도네시아어, 터키어

---

## 3. 기술 스택

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
| SDK | Target 36 / Min 26 | - |

### 기술 선택 근거
- **DI 프레임워크 미사용**: 15개 파일, 1,188줄 규모 — 의존성 그래프가 단순하여 Hilt/Koin 도입 시 오버엔지니어링
- **API 키 불필요**: Google Translate 무료 웹 엔드포인트 사용
- **Gson 선택**: Retrofit과의 통합이 간편하고, Google Translate 응답이 비정형 JsonArray라 유연한 파싱 필요

---

## 4. 아키텍처

> 상세 다이어그램은 `docs/diagrams.md` 또는 `docs/diagrams-offline.html` 참조

### 계층 구조

```
UI Layer        TranslationScreen → TranslationScreenContent → 하위 컴포넌트
                (상태 수집 + 콜백 바인딩)   (순수 Composable, Preview 지원)

ViewModel       TranslationViewModel + TranslationUiState (5가지 상태)
                (상태 관리, 자동 번역 타이머, 만료일 확인)

Data Layer      TranslationRepository    PreferencesManager    AppConfig
                (번역 서비스 위임)        (대기 시간 저장)      (만료일 상수)

Network         GoogleTranslator         DeepLTranslator(비활성)
                (언어 코드 매핑 + 응답 파싱)

Infrastructure  RetrofitClient (OkHttpClient 공유, API 인스턴스 제공)

External        Google Translate API (translate.googleapis.com)
```

### UI 상태 흐름

```
앱 시작 → 만료일 확인 → Idle (입력 대기)

Idle → (카운트다운 완료) → Loading(1단계) → Editing (영어 검토/수정)
                                              ↓ 번역 버튼
                                         Loading(2단계) → Success → Idle
```

### 주요 설계 포인트
- **sealed interface**: 5가지 UI 상태를 타입으로 표현, 컴파일 타임에 모든 상태 처리 보장
- **Presenter 패턴**: Screen(상태 수집) / ScreenContent(순수 렌더링) 분리로 Preview 지원
- **Repository 추상화**: 번역 서비스 교체 지점을 한 곳으로 집중 (DeepL ↔ Google 전환이 주석 한 줄)
- **코루틴 디바운스**: 입력마다 기존 타이머 취소 후 재시작, viewModelScope로 생명주기 관리

---

## 5. 데모 시나리오

### 정상 플로우
1. 앱 실행 → 원문 입력 (예: "안녕하세요, 만나서 반갑습니다")
2. 카운트다운 표시 (6초 → 5초 → ... → 0초)
3. 영어 번역 결과 표시 (예: "Hello, nice to meet you")
4. 필요 시 영어 텍스트 수정
5. 대상 언어 선택 (우즈베크어) → 번역 버튼 클릭
6. 최종 결과 표시 + 클립보드 자동 복사

### 원문 수정 플로우
1. 영어 결과 확인 후 원문이 마음에 안 들면 원문 수정
2. 카운트다운 재시작 → 1단계부터 재번역

### 대기 시간 변경
1. +/- 버튼으로 4~10초 범위 조절
2. 앱 종료 후 재시작해도 설정 유지

---

## 6. 보안

| 항목 | 조치 |
|------|------|
| 키스토어 | `keystore/`, `*.jks` → `.gitignore` 적용 |
| 크레덴셜 | `local.properties`에 저장, Git 미추적 |
| 난독화 | ProGuard/R8 규칙 적용 (Retrofit, OkHttp, Gson, Coroutines, DeepL DTO) |
| API 키 | 없음 — 무료 웹 엔드포인트만 사용 |

---

## 7. 알려진 제약 사항

| 항목 | 현재 상태 | 영향 |
|------|-----------|------|
| DeepL API | HTTP 429로 비활성 | 1단계도 Google Translate 사용 중 |
| 무료 엔드포인트 | API 키 없는 비공식 엔드포인트 | 트래픽 제한/차단 가능성 |
| 만료일 | 앱 내 상수로 하드코딩 | 만료 시 앱 업데이트 배포 필요 |
| 에러 복구 | 2단계 에러 시 1단계부터 재시작 | 사용자가 수정한 영어 텍스트를 잃을 수 있음 |

---

## 8. 향후 계획

- DeepL 유료 API 또는 대안 서비스 도입 검토
- 만료일 서버 기반 관리로 전환
- 2단계 에러 시 영어 편집 상태 보존 후 재시도
- 번역 히스토리 저장 기능
- 오프라인 캐시 지원

---

## 9. 프로젝트 규모

| 항목 | 수치 |
|------|------|
| 소스 파일 | 15개 (.kt) |
| 총 코드 라인 | 1,188줄 |
| 개발 기간 | 2026.03.24 ~ 현재 |
| 지원 언어 | 16개 |

---

## 참고 자료

- 아키텍처/데이터 플로우/시퀀스 다이어그램: `docs/diagrams.md`
- 다이어그램 (오프라인 브라우저): `docs/diagrams-offline.html`
- 코드 리뷰 상세: `docs/project-review.md`
