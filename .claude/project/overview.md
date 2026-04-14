# Project Overview

## What It Does

Pivot Translator is a two-step translation app for precise multilingual translation with editorial control.

## Translation Flow

1. **Step 1 (Korean → English)**: User inputs Korean text → PivotGate/DeepL translates to English
   - User reviews and edits the intermediate English translation
2. **Step 2 (English → Target)**: Edited English → PivotGate/Google Translate → final target language

## UI State Flow

```
Idle → (auto-translate countdown or manual trigger)
     → Loading → Editing (user reviews/edits English)
                  → Loading → Success (final translation + clipboard copy)
                            → Error (stays in Editing, shows snackbar)
```

## Auto-Translate Timer

- Configurable delay: 4–10 seconds (default 6)
- Countdown starts on text input, resets on edits
- Persisted in SharedPreferences via `PreferencesManager`

## App Activation System

On startup, ViewModel checks PivotGate server for app expiration date:
- **Active**: translation enabled
- **Expired**: UI blocked
- **ServerError**: shows retry option, preserves previous UI state for restoration

Pre-check uses TCP socket connection (2s timeout) before Retrofit call.

## Supported Target Languages

우즈베크어 (default), 한국어, 일본어, 중국어(간체), 스페인어, 프랑스어, 독일어, 포르투갈어, 이탈리아어, 러시아어, 아랍어, 힌디어, 태국어, 베트남어, 인도네시아어, 터키어, 말레이어

## Backend

**PivotGate** server (`http://claude.surem.com:19000/`) proxies translation APIs:
- `/deepl/translate` — Korean → English (JSON-RPC)
- `/google/translate` — English → target language
- `/expiration` — app licensing check

Requires `PIVOT_GATE_API_KEY` (from `local.properties` → `BuildConfig`).
