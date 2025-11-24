#!/bin/bash

# SSBMax Cross-Platform Gradle Wrapper Script
# Works on both macOS and Linux (GitHub CI)

# Detect OS and set appropriate JAVA_HOME
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS - use Android Studio bundled JBR
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux (GitHub CI) - use system Java or GitHub Actions setup-java
    if [[ -z "$JAVA_HOME" ]]; then
        # JAVA_HOME is not set, try to find Java in common locations
        if [[ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]]; then
            export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
        elif [[ -d "/usr/lib/jvm/java-17-openjdk" ]]; then
            export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
        elif command -v java &> /dev/null; then
            # Use java command to find JAVA_HOME
            export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
        fi
    fi
    # If JAVA_HOME is still not set, that's an error
fi

# Ensure JAVA_HOME is set
if [[ -z "$JAVA_HOME" ]]; then
    echo "ERROR: JAVA_HOME is not set. Please ensure Java 17 is installed."
    exit 1
fi

echo "Using JAVA_HOME: $JAVA_HOME"

# Execute Gradle wrapper with all arguments
exec ./gradlew "$@"

