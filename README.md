# ZIBE — Android App (Kotlin / Compose)

Android application built with **Kotlin**, **Jetpack Compose**, **Hilt**, and a clean, testable architecture.
The project focuses on modern Android practices, deterministic testing, and CI-driven quality gates.

---

## Tech Stack

- **Kotlin (2.x)**
- **Jetpack Compose**
- **Hilt (DI)**
- **Coroutines / Flow**
- **Firebase (Auth, Messaging — abstracted & faked in tests)**
- **GitHub Actions (CI)**

---

## Architecture Overview

- **MVVM**
- Clear separation between:
    - UI
    - ViewModels
    - Domain / UseCases
    - Data (repositories, providers)
- External dependencies (Firebase, network, preferences) are accessed via **interfaces** and **replaced with fakes in tests**.
- Navigation and startup logic are fully testable.

---

## CI / Quality Gates

This project uses **GitHub Actions** to automatically validate code quality on every push and pull request.

### Pipelines executed

- Unit tests (`./gradlew testDebugUnitTest`)
- Android Lint (`./gradlew lintDebug`)
- Debug build (`./gradlew assembleDebug`)
- Instrumented tests (`./gradlew connectedDebugAndroidTest`)

### Reports

CI uploads build artifacts for review:

- **unit-test-reports**
- **lint-reports**

---

## Local commands

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
