#!/bin/bash
# Script to switch to Java 21 for SSBMax builds
# Usage: source use-java-21.sh

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "✅ Switched to Java 21"
java --version

