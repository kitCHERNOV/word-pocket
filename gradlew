#!/usr/bin/env sh
# Source-only bootstrap for the official, SHA-256 verified Gradle wrapper.
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"
EXPECTED_SHA=2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046
if [ ! -f "$WRAPPER_JAR" ]; then
    TEMP_JAR=$(mktemp "$PROJECT_DIR/gradle/wrapper/download.XXXXXX")
    trap 'rm -f "$TEMP_JAR"' EXIT HUP INT TERM
    curl --fail --location --retry 3 --proto '=https' --tlsv1.2 \
        https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar -o "$TEMP_JAR"
    if command -v sha256sum >/dev/null 2>&1; then
        ACTUAL_SHA=$(sha256sum "$TEMP_JAR" | cut -d ' ' -f 1)
    else
        ACTUAL_SHA=$(shasum -a 256 "$TEMP_JAR" | cut -d ' ' -f 1)
    fi
    [ "$ACTUAL_SHA" = "$EXPECTED_SHA" ] || { echo 'Gradle wrapper checksum mismatch.' >&2; exit 1; }
    mv "$TEMP_JAR" "$WRAPPER_JAR"
    trap - EXIT HUP INT TERM
fi
if [ -n "${JAVA_HOME:-}" ]; then JAVA_COMMAND="$JAVA_HOME/bin/java"; else JAVA_COMMAND=java; fi
exec "$JAVA_COMMAND" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
