#!/usr/bin/env sh
echo "This repo contains Gradle Kotlin build files. If gradle wrapper isn't present, generate it in Android Studio or install Gradle locally."
./gradlew || true
