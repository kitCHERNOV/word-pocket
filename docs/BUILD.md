# Результат сборки

11 ноября 2026 года, локально в WSL/Linux x86_64.

- APK: `WordPocket-0.1.0-debug.apk` в корне проекта.
- Размер: 10 807 420 байт (10,3 МиБ).
- Приложение: `app.wordpocket`, версия `0.1.0`, versionCode `1`.
- Минимальная версия: Android 8.0 (API 26), targetSdk 35.
- Тип: debug, для установки и личного тестирования.
- SHA-256: `05c7c1681ed6d13f91a2d1ad0a2f524de68aa0dbc3028c458a52a9231f59f013`.
- Контрольная сумма также записана в `WordPocket-0.1.0-debug.apk.sha256`.

## Проверки

Gradle-задачи `testDebugUnitTest lintDebug assembleDebug` завершились успешно: 53 задачи, 5 минут 42 секунды после подготовки инструментов.

- 12 модульных тестов: 7 для правил обучения, 5 для JSON; ошибок и падений нет.
- Android Lint: 0 ошибок, 7 предупреждений (6 о новых версиях библиотек и 1 о настройках переноса данных Android 12+).
- `apksigner verify --verbose --print-certs`: подпись APK v2 действительна.
- ZIP CRC и наличие AndroidManifest.xml/DEX проверены.
- Пакет, версия и minSdk проверены через `aapt dump badging`.
- Запуск на физическом телефоне и ручные сценарии интерфейса ещё не проверялись.

Отчёты после сборки:

- `app/build/reports/tests/testDebugUnitTest/index.html`.
- `app/build/reports/lint-results-debug.html`.
- `app/build/test-results/testDebugUnitTest/TEST-*.xml`.

## Инструменты и подпись

Использованы JDK Temurin 17.0.21, Gradle 8.11.1, Android SDK Platform 35 и Build Tools 35.0.0. Инструменты подготовлены в `/tmp/wordpocket-build-tools`, системные установки не изменялись. Содержимое `/tmp` может быть удалено при очистке или перезапуске системы; это не затрагивает APK в папке проекта.

Debug-ключ сохранён в `.android/debug.keystore` внутри проекта и исключён из Git. Его нужно сохранить, чтобы последующие локальные сборки могли обновлять эту установку без удаления приложения. При повторной сборке задавайте `ANDROID_USER_HOME` равным абсолютному пути к `.android` этого проекта.

SHA-256 сертификата: `a2d34cb6d9d2b2ce9084fc1c6b527304d7160a4c0ee2e3d1bd7dbf3e54cb6578`.

GitHub Actions в этой работе не запускался; исходники в удалённые репозитории не отправлялись.
