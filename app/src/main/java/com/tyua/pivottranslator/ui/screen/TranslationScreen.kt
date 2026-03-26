package com.tyua.pivottranslator.ui.screen

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tyua.pivottranslator.preferences.PreferencesManager
import com.tyua.pivottranslator.ui.theme.PivotTranslatorTheme
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

    TranslationScreenContent(
        uiState = uiState,
        sourceText = sourceText,
        targetLanguage = targetLanguage,
        autoTranslateDelay = autoTranslateDelay,
        remainingSeconds = remainingSeconds,
        onSourceTextChange = viewModel::updateSourceText,
        onLanguageSelected = viewModel::updateTargetLanguage,
        onTranslateToTarget = viewModel::translateToTarget,
        onDelayChange = viewModel::updateAutoTranslateDelay,
        onReset = viewModel::resetState
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
    onSourceTextChange: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onTranslateToTarget: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is TranslationUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as TranslationUiState.Error).message
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pivot Translator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── 원문 입력 ──
            OutlinedTextField(
                value = sourceText,
                onValueChange = onSourceTextChange,
                label = { Text("번역할 텍스트를 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 10,
                enabled = uiState is TranslationUiState.Idle || uiState is TranslationUiState.Error || uiState is TranslationUiState.Editing
            )

            // ── 자동 번역 안내 + 대기 시간 설정 ──
            if (uiState is TranslationUiState.Idle || uiState is TranslationUiState.Error || uiState is TranslationUiState.Editing) {
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
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
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

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCounting)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                FilledTonalIconButton(
                    onClick = { onDelayChange(delay - 1) },
                    enabled = delay > PreferencesManager.MIN_DELAY,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("−", style = MaterialTheme.typography.titleMedium)
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

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1단계: 영어 번역 결과",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "아래 영어 텍스트를 확인하고 필요하면 수정하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
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
        maxLines = 10
    )

    Spacer(modifier = Modifier.height(16.dp))

    // ── 번역할 언어 선택 (Editing 단계에서 선택) ──
    LanguageDropdown(
        selectedLanguage = targetLanguage,
        onLanguageSelected = onLanguageSelected
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 최종 번역 버튼
    Button(
        onClick = { onTranslate(editedEnglish) },
        enabled = editedEnglish.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("${targetLanguage}(으)로 번역하기")
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 처음으로 돌아가기
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth()
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

    // 번역 결과가 나오면 자동으로 클립보드에 복사
    LaunchedEffect(finalTranslation) {
        clipboardManager.setText(AnnotatedString(finalTranslation))
        snackbarHostState.showSnackbar("번역 결과가 클립보드에 복사되었습니다")
    }

    // 최종 번역 결과 카드
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "번역 결과 ($targetLanguage)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = finalTranslation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 사용된 영어 텍스트 참고 카드
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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

    // 새 번역 시작 버튼
    OutlinedButton(
        onClick = onNewTranslation,
        modifier = Modifier.fillMaxWidth()
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
            onSourceTextChange = {},
            onLanguageSelected = {},
            onTranslateToTarget = {},
            onDelayChange = {},
            onReset = {}
        )
    }
}
