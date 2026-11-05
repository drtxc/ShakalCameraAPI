#!/bin/sh
# Setup script: downloads gradle-wrapper.jar and sets permissions
# Run this once after cloning the repository

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"
GRADLE_VERSION="8.9"

echo "=== ШакалCamera: настройка Gradle wrapper ==="

# Download gradle-wrapper.jar if not present
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Скачиваю gradle-wrapper.jar (Gradle $GRADLE_VERSION)..."
    
    # Download from Gradle distributions
    WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
    
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL" || {
            echo "Не удалось скачать через curl, пробую wget..."
            wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" || {
                echo "ОШИБКА: Не удалось скачать gradle-wrapper.jar"
                echo "Установите Gradle и выполните: gradle wrapper --gradle-version ${GRADLE_VERSION}"
                exit 1
            }
        }
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" || {
            echo "ОШИБКА: Не удалось скачать gradle-wrapper.jar"
            exit 1
        }
    else
        echo "ОШИБКА: Не найден curl или wget"
        echo "Установите Gradle и выполните: gradle wrapper --gradle-version ${GRADLE_VERSION}"
        exit 1
    fi
    
    echo "✓ gradle-wrapper.jar скачан"
else
    echo "✓ gradle-wrapper.jar уже существует"
fi

# Set executable permission on gradlew
chmod +x "$SCRIPT_DIR/gradlew"
echo "✓ gradlew сделан исполняемым"

echo ""
echo "=== Готово! Теперь можно собирать: ==="
echo "  ./gradlew assembleDebug"
echo ""
