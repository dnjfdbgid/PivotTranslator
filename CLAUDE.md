# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Pivot Translator — a two-step translation Android app (Korean → English via DeepL → target language via Google Translate) with editorial control over intermediate English, built with Kotlin and Jetpack Compose.

## Project References

| File | Content |
|------|---------|
| [architecture.md](.claude/project/architecture.md) | Module structure, manual DI, MVVM state machines, networking |
| [overview.md](.claude/project/overview.md) | Domain context, translation flow, app activation, supported languages |
| [commands.md](.claude/project/commands.md) | Build, test, lint commands and local.properties setup |

## Repository Structure

```
app/src/main/java/com/tyua/pivottranslator/
├── config/          # API base URLs
├── network/         # Retrofit APIs, DTOs, RetrofitClient
├── preferences/     # SharedPreferences wrapper
├── repository/      # TranslationRepository
├── ui/
│   ├── navigation/  # NavGraph (single route)
│   ├── screen/      # TranslationScreen
│   └── theme/       # Material 3 theme
└── viewmodel/       # TranslationViewModel + state machines
```

## Quick Reference

| Resource | Location |
|----------|----------|
| Version catalog | `gradle/libs.versions.toml` |
| ProGuard rules | `app/proguard-rules.pro` |
| Network security | `app/src/main/res/xml/network_security_config.xml` |
| Unit tests | `app/src/test/java/com/tyua/pivottranslator/` |

## Critical Rules

**IMPORTANT:** ALWAYS check `.claude/project/` for project-specific context before implementation.

**IMPORTANT:** This project uses manual DI — never introduce Hilt, Koin, or other DI frameworks.

**IMPORTANT:** Analyze the skills catalog and activate the skills needed for the task.
