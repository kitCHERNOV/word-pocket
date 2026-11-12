# Repository Guidelines

## Project Structure & Module Organization

WordPocket is an offline Android vocabulary app built with Kotlin, Jetpack Compose, and Room. The single `app` module keeps production code in `app/src/main/java/app/wordpocket/`: `data/` handles persistence and JSON, `domain/` contains learning rules, and `ui/` contains screens, theming, and view-model state. Android resources live in `app/src/main/res/`; exported Room schemas live in `app/schemas/`.

JVM tests mirror production packages under `app/src/test/java/`. Use `examples/` for import fixtures and the JSON schema, and `docs/` for build records and manual acceptance scenarios. `design/index.html` is a standalone browser prototype with independent local storage.

## Build, Test, and Development Commands

Use JDK 17, Android SDK Platform 35, and Build Tools 35.0.0. Open the repository in Android Studio to sync and run on an emulator or device running API 26 or newer.

From PowerShell:

- `.\gradlew.bat assembleDebug`: build `app/build/outputs/apk/debug/app-debug.apk`.
- `.\gradlew.bat testDebugUnitTest`: run JVM unit tests.
- `.\gradlew.bat lintDebug`: run Android Lint.
- `.\gradlew.bat installDebug`: install on a connected device or emulator.

On Linux/macOS, use `bash gradlew` instead. Match the manual GitHub Actions workflow with `bash gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug`.

## Coding Style & Naming Conventions

Follow the configured official Kotlin style with four-space indentation. Use PascalCase for classes and composable functions, camelCase for ordinary functions and properties, and UPPER_SNAKE_CASE for constants. Keep package names lowercase and files named for their primary responsibility, such as `WordRepository.kt`. Preserve Russian user-facing text. Android Lint is configured; no dedicated formatter is configured.

## Testing Guidelines

Use JUnit 4, `*Test.kt` filenames, and descriptive backtick test names. Add regression cases for changed learning rules, answer normalization, and JSON validation. No numeric coverage threshold is configured. Follow `docs/ACCEPTANCE.md` for device checks, especially persistence after restart, dictionary isolation, and file import/export.

## Commit & Pull Request Guidelines

The current history contains one commit, `feat: initialize WordPocket Android app`. Continue with short, imperative, type-prefixed subjects. Pull requests should describe behavior changes, link relevant issues, report test and lint results, and include screenshots for UI changes. Identify device checks that remain unverified.

## Configuration & Data

Keep SDK paths in ignored `local.properties`. Never commit keystores or generated build outputs. Preserve Room schema history and plan migrations for database changes.
