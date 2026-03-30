# PivotTranslator 다이어그램

## 1. 아키텍처 다이어그램 (Architecture Diagram)

### 1-1. 전체 계층 구조 (Overview)

```mermaid
graph TB
    UI["UI Layer\nJetpack Compose + Material3"]
    VM["ViewModel Layer\nTranslationViewModel + UiState"]
    DATA["Data Layer\nRepository · PreferencesManager · AppConfig"]
    NET["Network Layer\nGoogleTranslator · DeepLTranslator(비활성) · RetrofitClient"]
    EXT["External Services\nGoogle Translate API · DeepL API(비활성)"]

    UI -->|"collectAsState()"| VM
    VM -->|"translate / config"| DATA
    DATA -->|"translate()"| NET
    NET -->|"HTTP GET/POST"| EXT

    style UI fill:#e3f2fd,stroke:#1565c0
    style VM fill:#fff3e0,stroke:#e65100
    style DATA fill:#f3e5f5,stroke:#6a1b9a
    style NET fill:#e8f5e9,stroke:#2e7d32
    style EXT fill:#fafafa,stroke:#9e9e9e
```

### 1-2. UI Layer 상세

```mermaid
graph LR
    MA[MainActivity] --> NG[NavGraph]
    NG --> TS[TranslationScreen]
    TS --> TSC[TranslationScreenContent]

    TSC --> DS[DelayStepper]
    TSC --> ES[EditingSection]
    TSC --> SS[SuccessSection]
    ES --> LD[LanguageDropdown]

    MA -.-> TH[Theme.kt]
    TH -.-> CL[Color.kt]
    TH -.-> TY[Type.kt]

    style MA fill:#e3f2fd,stroke:#1565c0
    style TS fill:#e3f2fd,stroke:#1565c0
    style TSC fill:#bbdefb,stroke:#1565c0
```

### 1-3. ViewModel Layer 상세

```mermaid
graph LR
    VM[TranslationViewModel]
    VM --- UIS["TranslationUiState"]

    UIS --> IDLE[Idle]
    UIS --> LOAD[Loading]
    UIS --> EDIT[Editing]
    UIS --> SUCC[Success]
    UIS --> ERR[Error]

    VM -->|"위임"| REPO[TranslationRepository]
    VM -->|"대기 시간"| PM[PreferencesManager]
    VM -->|"만료일"| CFG[AppConfig]

    style VM fill:#fff3e0,stroke:#e65100
    style UIS fill:#ffe0b2,stroke:#e65100
```

### 1-4. Network Layer 상세

```mermaid
graph TB
    subgraph DATA_LAYER["Data Layer"]
        REPO[TranslationRepository]
    end

    subgraph NET_LAYER["Network Layer"]
        GT[GoogleTranslator]
        DT[DeepLTranslator]
        RC[RetrofitClient]
        GTA["GoogleTranslateApi\nGET /translate_a/single"]
        DLA["DeepLApi\nPOST /jsonrpc"]
    end

    REPO -->|"활성"| GT
    REPO -.->|"비활성 — HTTP 429"| DT

    GT --> RC
    DT -.-> RC

    RC --> GTA
    RC --> DLA

    GTA -->|"HTTP GET"| GAPI["Google Translate\ntranslate.googleapis.com"]
    DLA -.->|"차단됨"| DAPI["DeepL\nwww2.deepl.com"]

    style DATA_LAYER fill:#f3e5f5,stroke:#6a1b9a
    style NET_LAYER fill:#e8f5e9,stroke:#2e7d32
    style GT fill:#e8f5e9,stroke:#2e7d32
    style DT fill:#fff9c4,stroke:#f9a825
    style DAPI fill:#ffcdd2,stroke:#c62828
    style DLA fill:#fff9c4,stroke:#f9a825
```

## 2. 데이터 플로우 다이어그램 (Data Flow Diagram)

```mermaid
flowchart TD
    START([앱 시작]) --> INIT[ViewModel 초기화]
    INIT --> CHECK{만료일 확인\nAppConfig.EXPIRATION_DATE}
    CHECK -->|만료됨| EXPIRED[번역 비활성화\n만료 안내 표시\n입력 필드 비활성]
    CHECK -->|유효함| LOAD_PREF[PreferencesManager\n자동 번역 대기 시간 로드]
    LOAD_PREF --> IDLE[uiState = Idle\n입력 대기]

    IDLE -->|사용자 텍스트 입력| INPUT[sourceText 업데이트]
    INPUT --> TIMER[자동 번역 타이머 시작\n4~10초 카운트다운]

    TIMER -->|매초| COUNTDOWN[remainingSeconds 갱신\nUI 카운트다운 표시]
    TIMER -->|카운트다운 완료| STAGE1

    subgraph STAGE1["1단계: 원문 → 영어"]
        S1_START[uiState = Loading] --> S1_REPO[TranslationRepository\n.translateToEnglish]
        S1_REPO --> S1_GT[GoogleTranslator\n.translate text, 영어]
        S1_GT --> S1_API["HTTP GET\ntranslate.googleapis.com\n/translate_a/single"]
        S1_API --> S1_PARSE[JsonArray 응답 파싱]
        S1_PARSE --> S1_RESULT[영어 번역 결과]
    end

    S1_RESULT --> EDITING[uiState = Editing\n영어 텍스트 확인/수정]
    EDITING -->|원문 수정 시| INPUT
    EDITING -->|번역 버튼 클릭| STAGE2
    EDITING -->|처음으로 버튼| IDLE

    subgraph STAGE2["2단계: 영어 → 대상 언어"]
        S2_START[uiState = Loading] --> S2_REPO[TranslationRepository\n.translateToTarget]
        S2_REPO --> S2_GT[GoogleTranslator\n.translate english, targetLang]
        S2_GT --> S2_API["HTTP GET\ntranslate.googleapis.com\n/translate_a/single"]
        S2_API --> S2_PARSE[JsonArray 응답 파싱]
        S2_PARSE --> S2_RESULT[최종 번역 결과]
    end

    S2_RESULT --> SUCCESS[uiState = Success]
    SUCCESS --> CLIPBOARD[클립보드 자동 복사]
    SUCCESS --> SNACKBAR[스낵바 알림]
    SUCCESS -->|새 번역 버튼| RESET[uiState = Idle\nresetState]
    RESET --> IDLE

    S1_API -->|에러| ERROR[uiState = Error\n에러 메시지 표시]
    S2_API -->|에러| ERROR
    ERROR -->|텍스트 수정| INPUT

    subgraph PERSIST["영속 저장"]
        PM_SAVE[PreferencesManager\nSharedPreferences]
    end

    TIMER -.->|대기 시간 변경 시| PM_SAVE

    style STAGE1 fill:#e3f2fd,stroke:#1565c0
    style STAGE2 fill:#e8f5e9,stroke:#2e7d32
    style ERROR fill:#ffebee,stroke:#c62828
    style EXPIRED fill:#ffebee,stroke:#c62828
    style PERSIST fill:#f3e5f5,stroke:#6a1b9a
```

## 3. 시퀀스 다이어그램 (Sequence Diagram)

### 3-1. 전체 번역 플로우

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Screen as TranslationScreen
    participant VM as TranslationViewModel
    participant Repo as TranslationRepository
    participant GT as GoogleTranslator
    participant API as Google Translate API
    participant Clip as Clipboard

    Note over User, API: ── 1단계: 원문 → 영어 번역 ──

    User->>Screen: 텍스트 입력
    Screen->>VM: updateSourceText(text)
    VM->>VM: sourceText 업데이트
    VM->>VM: scheduleAutoTranslate()

    loop 매초 카운트다운
        VM->>Screen: remainingSeconds 갱신
        Screen->>User: 카운트다운 표시 (N초)
    end

    VM->>VM: uiState = Loading
    VM->>Repo: translateToEnglish(sourceText)
    Repo->>GT: translate(text, "영어")
    GT->>API: GET /translate_a/single?sl=auto&tl=en&q=text
    API-->>GT: JsonArray 응답
    GT-->>Repo: 영어 번역 텍스트
    Repo-->>VM: englishText

    VM->>VM: uiState = Editing(englishText)
    VM->>Screen: Editing 상태 전달
    Screen->>User: 영어 번역 결과 표시 + 편집 가능

    Note over User, API: ── 사용자 편집 단계 ──

    User->>Screen: 영어 텍스트 수정 (선택)
    User->>Screen: 대상 언어 선택 (기본: 우즈베크어)
    User->>Screen: "번역" 버튼 클릭

    Note over User, API: ── 2단계: 영어 → 대상 언어 번역 ──

    Screen->>VM: translateToTarget(editedEnglish)
    VM->>VM: uiState = Loading
    VM->>Repo: translateToTarget(english, targetLanguage)
    Repo->>GT: translate(english, targetLanguage)
    GT->>API: GET /translate_a/single?sl=auto&tl=uz&q=english
    API-->>GT: JsonArray 응답
    GT-->>Repo: 최종 번역 텍스트
    Repo-->>VM: finalTranslation

    VM->>VM: uiState = Success(english, finalTranslation)
    VM->>Screen: Success 상태 전달
    Screen->>Clip: 최종 번역 결과 복사
    Screen->>User: 결과 표시 + 스낵바 "복사됨"

    Note over User, API: ── 재시작 ──

    User->>Screen: "새 번역" 버튼 클릭
    Screen->>VM: resetState()
    VM->>VM: uiState = Idle
```

### 3-2. 자동 번역 타이머 & 설정 변경

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Screen as TranslationScreen
    participant Stepper as DelayStepper
    participant VM as TranslationViewModel
    participant PM as PreferencesManager

    Note over User, PM: ── 앱 시작 시 설정 로드 ──

    VM->>PM: autoTranslateDelay 읽기
    PM-->>VM: 저장된 값 (기본 6초)
    VM->>Screen: autoTranslateDelay 상태 전달

    Note over User, PM: ── 대기 시간 변경 ──

    User->>Stepper: + 버튼 클릭
    Stepper->>VM: updateAutoTranslateDelay(현재값 + 1)
    VM->>PM: autoTranslateDelay 저장 (clamped 4~10)
    VM->>Screen: 새 대기 시간 표시

    Note over User, PM: ── 입력 중 카운트다운 ──

    User->>Screen: 텍스트 입력
    Screen->>VM: updateSourceText(text)
    VM->>VM: 기존 타이머 취소
    VM->>VM: 새 카운트다운 시작

    loop delay초 → 1초
        VM->>Screen: remainingSeconds 갱신
        Screen->>Stepper: 카운트다운 표시 (색상 변경)
    end

    User->>Screen: 추가 입력 (타이핑 중)
    Screen->>VM: updateSourceText(newText)
    VM->>VM: 기존 타이머 취소 + 재시작

    Note over VM: 카운트다운 완료 → translateToEnglish() 호출
```

### 3-3. 에러 처리 플로우

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Screen as TranslationScreen
    participant VM as TranslationViewModel
    participant Repo as TranslationRepository
    participant GT as GoogleTranslator
    participant API as Google Translate API

    User->>Screen: 텍스트 입력
    Screen->>VM: updateSourceText(text)
    VM->>VM: scheduleAutoTranslate()
    Note over VM: 카운트다운 완료
    VM->>VM: translateToEnglish()
    VM->>VM: uiState = Loading
    VM->>Repo: translateToEnglish(sourceText)

    alt 네트워크 에러
        Repo->>GT: translate(...)
        GT->>API: HTTP 요청
        API--xGT: UnknownHostException
        GT--xRepo: 예외 전파
        Repo--xVM: 예외 전파
        VM->>VM: uiState = Error("네트워크 연결을 확인해 주세요.")
    else 기타 에러
        Repo->>GT: translate(...)
        GT--xRepo: Exception
        Repo--xVM: 예외 전파
        VM->>VM: uiState = Error(e.message ?: "알 수 없는 오류")
    end

    VM->>Screen: Error 상태 전달
    Screen->>User: 에러 카드 표시
    User->>Screen: 텍스트 수정
    Screen->>VM: updateSourceText(newText)
    Note over VM: 카운트다운 → 재시도
```

## 4. 컴포넌트 의존성 다이어그램

### 4-1. UI → ViewModel 의존성

```mermaid
graph LR
    MA[MainActivity] --> NG[NavGraph]
    NG --> TS[TranslationScreen]
    TS --> VM[TranslationViewModel]

    TS --> TSC[TranslationScreenContent]
    TSC --> DS[DelayStepper]
    TSC --> ES[EditingSection]
    TSC --> SS[SuccessSection]
    ES --> LD[LanguageDropdown]

    VM --- UIS[TranslationUiState]

    style VM fill:#fff3e0,stroke:#e65100
    style UIS fill:#fff3e0,stroke:#e65100
```

### 4-2. ViewModel → Services 의존성

```mermaid
graph LR
    VM[TranslationViewModel] --> REPO[TranslationRepository]
    VM --> PM[PreferencesManager]
    VM --> CFG[AppConfig]

    REPO --> GT[GoogleTranslator]
    REPO -.->|"비활성"| DT[DeepLTranslator]

    style VM fill:#fff3e0,stroke:#e65100
    style DT fill:#fff9c4,stroke:#f9a825
```

### 4-3. Network Infrastructure 의존성

```mermaid
graph LR
    GT[GoogleTranslator] -->|".googleTranslateApi"| RC[RetrofitClient]
    DT[DeepLTranslator] -.->|".deepLApi"| RC

    subgraph RC_INTERNAL["RetrofitClient 내부"]
        OK["OkHttpClient\n(private, 공유)"]
        GTA[GoogleTranslateApi]
        DLA[DeepLApi]
        OK --> GTA
        OK --> DLA
    end

    style DT fill:#fff9c4,stroke:#f9a825
    style DLA fill:#fff9c4,stroke:#f9a825
```

## 5. UI 상태 전이 다이어그램 (State Machine)

### 5-1. 정상 플로우 (Happy Path)

```mermaid
stateDiagram-v2
    [*] --> Idle : 앱 시작

    Idle --> Loading : 카운트다운 완료

    state "Loading (1단계)" as Loading
    Loading --> Editing : 영어 번역 완료

    state "Editing (영어 검토/수정)" as Editing
    Editing --> Loading2 : 번역 버튼 클릭

    state "Loading (2단계)" as Loading2
    Loading2 --> Success : 최종 번역 완료

    Editing --> Idle : 처음으로 버튼
    Success --> Idle : 새 번역 버튼

    note right of Editing
        1차 영어 번역 결과를 확인하고
        필요 시 영어 텍스트를 직접 수정한 뒤
        수정된 영어로 2단계 번역 진행
    end note
```

### 5-2. 원문 수정 플로우

```mermaid
stateDiagram-v2
    state "Editing (영어 번역 결과 확인)" as Editing
    state "Loading (1단계 재시작)" as Loading

    Editing --> Loading : 원문 수정 → 카운트다운 → 재번역

    note right of Editing
        1차 번역 결과 확인 후
        원문이 마음에 안 들면
        원문 수정 → 카운트다운 → 1단계 재시작
    end note
```

### 5-3. 에러 플로우

```mermaid
stateDiagram-v2
    state "Loading (1단계)" as Loading1
    state "Loading (2단계)" as Loading2
    state "Error" as Error

    Loading1 --> Error : 네트워크/기타 에러
    Loading2 --> Error : 네트워크/기타 에러

    note right of Error
        1단계, 2단계 모두에서 발생 가능
        에러 상태에서 원문 수정 시
        카운트다운 → 1단계부터 재시작
    end note
```
