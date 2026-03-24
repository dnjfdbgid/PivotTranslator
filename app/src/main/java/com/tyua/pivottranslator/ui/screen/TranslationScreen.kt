package com.tyua.pivottranslator.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                onValueChange = viewModel::updateSourceText,
                label = { Text("번역할 텍스트를 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 10,
                enabled = uiState is TranslationUiState.Idle || uiState is TranslationUiState.Error
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 1단계 버튼: 영어로 번역 ──
            if (uiState is TranslationUiState.Idle || uiState is TranslationUiState.Error) {
                Button(
                    onClick = viewModel::translateToEnglish,
                    enabled = sourceText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("영어로 번역하기")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        onLanguageSelected = viewModel::updateTargetLanguage,
                        onTranslate = viewModel::translateToTarget,
                        onBack = viewModel::resetState
                    )
                }

                is TranslationUiState.Success -> {
                    SuccessSection(
                        editedEnglish = state.editedEnglish,
                        finalTranslation = state.finalTranslation,
                        targetLanguage = targetLanguage,
                        onNewTranslation = viewModel::resetState
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
    onNewTranslation: () -> Unit
) {
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
