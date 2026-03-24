package com.tyua.pivottranslator.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    // ── StateFlow 관찰 ──
    val uiState by viewModel.uiState.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Error 상태일 때 Snackbar 표시
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

            // ── 원문 입력 필드 ──
            OutlinedTextField(
                value = sourceText,
                onValueChange = viewModel::updateSourceText,
                label = { Text("번역할 텍스트를 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 도착 언어 드롭다운 ──
            LanguageDropdown(
                selectedLanguage = targetLanguage,
                onLanguageSelected = viewModel::updateTargetLanguage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 번역 실행 버튼 ──
            Button(
                onClick = viewModel::translate,
                enabled = sourceText.isNotBlank() && uiState !is TranslationUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("번역하기")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 상태별 UI ──
            when (val state = uiState) {
                is TranslationUiState.Idle -> { /* 대기 — 아무것도 표시하지 않음 */ }

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

                is TranslationUiState.Success -> {
                    SuccessResultCard(
                        rawEnglish = state.rawEnglish,
                        refinedEnglish = state.refinedEnglish,
                        finalTranslation = state.finalTranslation,
                        targetLanguage = targetLanguage
                    )
                }

                is TranslationUiState.Error -> {
                    // Snackbar와 함께 인라인 에러 메시지도 표시
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

/** 도착 언어 선택 드롭다운 */
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
            label = { Text("도착 언어") },
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

/** 번역 성공 결과 카드 — 최종 번역 + 중간 과정 접기/펴기 */
@Composable
private fun SuccessResultCard(
    rawEnglish: String,
    refinedEnglish: String,
    finalTranslation: String,
    targetLanguage: String
) {
    var showDetails by rememberSaveable { mutableStateOf(false) }

    // ── 최종 번역 결과 카드 ──
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

    // ── 중간 과정 접기/펴기 토글 ──
    TextButton(
        onClick = { showDetails = !showDetails },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (showDetails) "중간 과정 접기" else "중간 과정 보기")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (showDetails) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }

    AnimatedVisibility(
        visible = showDetails,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column {
            // 1단계: 영어 직역
            StepCard(
                stepLabel = "1단계",
                title = "영어 직역",
                content = rawEnglish
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 2단계: 다듬어진 영어
            StepCard(
                stepLabel = "2단계",
                title = "다듬어진 영어",
                content = refinedEnglish
            )
        }
    }
}

/** 중간 과정 단계별 카드 */
@Composable
private fun StepCard(
    stepLabel: String,
    title: String,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$stepLabel: $title",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
