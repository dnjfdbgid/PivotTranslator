package com.tyua.pivottranslator.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tyua.pivottranslator.preferences.PreferencesManager
import com.tyua.pivottranslator.ui.theme.PivotTranslatorTheme
import com.tyua.pivottranslator.viewmodel.AppActivationState
import com.tyua.pivottranslator.viewmodel.TranslationUiState
import com.tyua.pivottranslator.viewmodel.TranslationViewModel

/** 지원하는 도착 언어 목록 */
private val supportedLanguages = listOf(
    "우즈베크어", "한국어", "일본어", "중국어", "스페인어", "프랑스어",
    "독일어", "포르투갈어", "이탈리아어", "러시아어", "아랍어",
    "힌디어", "태국어", "베트남어", "인도네시아어", "터키어"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val autoTranslateDelay by viewModel.autoTranslateDelay.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val activationState by viewModel.activationState.collectAsState()
    val translationError by viewModel.translationError.collectAsState()

    TranslationScreenContent(
        uiState = uiState,
        sourceText = sourceText,
        targetLanguage = targetLanguage,
        autoTranslateDelay = autoTranslateDelay,
        remainingSeconds = remainingSeconds,
        activationState = activationState,
        translationError = translationError,
        onSourceTextChange = viewModel::updateSourceText,
        onLanguageSelected = viewModel::updateTargetLanguage,
        onTranslateToTarget = viewModel::translateToTarget,
        onDelayChange = viewModel::updateAutoTranslateDelay,
        onReset = viewModel::resetState,
        onRetryConnection = viewModel::retryServerConnection,
        onTranslationErrorShown = viewModel::clearTranslationError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationScreenContent(
    uiState: TranslationUiState,
    sourceText: String,
    targetLanguage: String,
    autoTranslateDelay: Int,
    remainingSeconds: Int?,
    activationState: AppActivationState,
    translationError: String? = null,
    onSourceTextChange: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onTranslateToTarget: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onReset: () -> Unit,
    onRetryConnection: () -> Unit,
    onTranslationErrorShown: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is TranslationUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as TranslationUiState.Error).message
            )
        }
    }

    LaunchedEffect(translationError) {
        if (translationError != null) {
            snackbarHostState.showSnackbar(message = translationError)
            onTranslationErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pivot Translator",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->

        // ── 앱 활성화 상태에 따른 화면 분기 ──
        if (activationState is AppActivationState.Checking) {
            CheckingScreen(modifier = Modifier.padding(innerPadding))
        } else if (activationState is AppActivationState.ServerError) {
            ServerErrorScreen(
                message = (activationState as AppActivationState.ServerError).message,
                onRetry = onRetryConnection,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // Active 또는 Expired 상태 — 번역 UI 표시
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                if (activationState is AppActivationState.Expired) {
                    ExpiredBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val isActive = activationState is AppActivationState.Active

                // ── 원문 입력 ──
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = onSourceTextChange,
                    label = { Text("번역할 텍스트를 입력하세요") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp),
                    enabled = isActive && (uiState is TranslationUiState.Idle || uiState is TranslationUiState.Error || uiState is TranslationUiState.Editing)
                )

                // ── 자동 번역 대기 시간 설정 ──
                if (isActive && (uiState is TranslationUiState.Idle || uiState is TranslationUiState.Error || uiState is TranslationUiState.Editing)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DelayStepper(
                        delay = autoTranslateDelay,
                        remainingSeconds = remainingSeconds,
                        onDelayChange = onDelayChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 상태별 UI ──
                when (val state = uiState) {
                    is TranslationUiState.Idle -> { /* 대기 */ }

                    is TranslationUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    is TranslationUiState.Editing -> {
                        EditingSection(
                            initialEnglish = state.englishText,
                            targetLanguage = targetLanguage,
                            onLanguageSelected = onLanguageSelected,
                            onTranslate = onTranslateToTarget,
                            onBack = onReset
                        )
                    }

                    is TranslationUiState.Success -> {
                        SuccessSection(
                            editedEnglish = state.editedEnglish,
                            finalTranslation = state.finalTranslation,
                            targetLanguage = targetLanguage,
                            snackbarHostState = snackbarHostState,
                            onNewTranslation = onReset
                        )
                    }

                    is TranslationUiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 전체 화면 상태 컴포저블
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/** 서버 접속 확인 중 화면 */
@Composable
private fun CheckingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(0.5f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "서버 접속 확인 중",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 서버 접속 에러 화면 */
@Composable
private fun ServerErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 아이콘
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 제목
            Text(
                text = "서버에 연결할 수 없습니다",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // 설명
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 재시도 버튼
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("다시 시도")
            }
        }
    }
}

/** 만료 안내 배너 */
@Composable
private fun ExpiredBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.TimerOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "번역 기능의 사용 기간이 만료되었습니다.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 하위 컴포저블
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/** 자동 번역 대기 시간 스테퍼 (카운트다운 표시) */
@Composable
private fun DelayStepper(
    delay: Int,
    remainingSeconds: Int?,
    onDelayChange: (Int) -> Unit
) {
    val isCounting = remainingSeconds != null

    Surface(
        color = if (isCounting)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isCounting)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCounting) "번역 시작까지" else "자동 번역 대기",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCounting)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (isCounting) {
                Text(
                    text = "${remainingSeconds}초",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                FilledTonalIconButton(
                    onClick = { onDelayChange(delay - 1) },
                    enabled = delay > PreferencesManager.MIN_DELAY,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("\u2212", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = "${delay}초",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )

                FilledTonalIconButton(
                    onClick = { onDelayChange(delay + 1) },
                    enabled = delay < PreferencesManager.MAX_DELAY,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Editing 상태 — 영어 직역 결과를 사용자가 확인/수정 후 최종 번역 요청 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditingSection(
    initialEnglish: String,
    targetLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onTranslate: (String) -> Unit,
    onBack: () -> Unit
) {
    var editedEnglish by rememberSaveable(initialEnglish) { mutableStateOf(initialEnglish) }

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1단계: 영어 번역 결과",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "아래 영어 텍스트를 확인하고 필요하면 수정하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = editedEnglish,
        onValueChange = { editedEnglish = it },
        label = { Text("영어 텍스트 (수정 가능)") },
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        maxLines = 10,
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    LanguageDropdown(
        selectedLanguage = targetLanguage,
        onLanguageSelected = onLanguageSelected
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { onTranslate(editedEnglish) },
        enabled = editedEnglish.isNotBlank(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("${targetLanguage}(으)로 번역하기")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onBack,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("처음으로")
    }
}

/** Success 상태 — 최종 번역 결과 표시 */
@Composable
private fun SuccessSection(
    editedEnglish: String,
    finalTranslation: String,
    targetLanguage: String,
    snackbarHostState: SnackbarHostState,
    onNewTranslation: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(finalTranslation) {
        clipboardManager.setText(AnnotatedString(finalTranslation))
        snackbarHostState.showSnackbar("번역 결과가 클립보드에 복사되었습니다")
    }

    // 최종 번역 결과
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "번역 결과 ($targetLanguage)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "클립보드에 복사됨",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = finalTranslation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 24.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 사용된 영어 텍스트
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "사용된 영어 텍스트",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = editedEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    FilledTonalButton(
        onClick = onNewTranslation,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("새 번역하기")
    }
}

/** 번역할 언어 선택 드롭다운 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLanguage,
            onValueChange = {},
            readOnly = true,
            label = { Text("번역할 언어") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 프리뷰
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Preview(showBackground = true)
@Composable
private fun TranslationScreenPreview() {
    PivotTranslatorTheme(dynamicColor = false) {
        TranslationScreenContent(
            uiState = TranslationUiState.Idle,
            sourceText = "안녕하세요, 만나서 반갑습니다.",
            targetLanguage = "우즈베크어",
            autoTranslateDelay = 6,
            remainingSeconds = null,
            activationState = AppActivationState.Active,
            onSourceTextChange = {},
            onLanguageSelected = {},
            onTranslateToTarget = {},
            onDelayChange = {},
            onReset = {},
            onRetryConnection = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerErrorPreview() {
    PivotTranslatorTheme(dynamicColor = false) {
        TranslationScreenContent(
            uiState = TranslationUiState.Idle,
            sourceText = "",
            targetLanguage = "우즈베크어",
            autoTranslateDelay = 6,
            remainingSeconds = null,
            activationState = AppActivationState.ServerError(
                "서버에 접속할 수 없습니다.\n잠시 후 다시 시도해 주세요."
            ),
            onSourceTextChange = {},
            onLanguageSelected = {},
            onTranslateToTarget = {},
            onDelayChange = {},
            onReset = {},
            onRetryConnection = {}
        )
    }
}
