#!/bin/zsh

# SSBMax Gradle Wrapper Script
# This fixes the JAVA_HOME issue with spaces in path

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew "$@"

