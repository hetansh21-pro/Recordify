#!/usr/bin/env bash

# Resolve project directory
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

# Use cached Gradle 8.14.3 binary if present, else wrapper
GRADLE_BIN="/home/hetansh-patel/.gradle/wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3/bin/gradle"

if [ -x "$GRADLE_BIN" ]; then
    exec "$GRADLE_BIN" --no-daemon "$@"
else
    CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"
    exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi
