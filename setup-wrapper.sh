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
    
    # Try multiple sources
    URLS="
        https://github.com/gradle/gradle/raw/v${GRADLE_VERSION}.0/gradle/wrapper/gradle-wrapper.jar
        https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}.0/gradle/wrapper/gradle-wrapper.jar
    "
    
    DOWNLOADED=false
    for url in $URLS; do
        if command -v curl >/dev/null 2>&1; then
            if curl -fsSL -o "$WRAPPER_JAR" "$url" 2>/dev/null; then
                DOWNLOADED=true
                break
            fi
        elif command -v wget >/dev/null 2>&1; then
            if wget -q -O "$WRAPPER_JAR" "$url" 2>/dev/null; then
                DOWNLOADED=true
                break
            fi
        fi
    done
    
    if [ "$DOWNLOADED" = false ]; then
        echo "Не удалось скачать gradle-wrapper.jar автоматически."
        echo ""
        echo "Альтернативные варианты:"
        echo "  1. Установите Gradle и выполните:"
        echo "     gradle wrapper --gradle-version ${GRADLE_VERSION}"
        echo ""
        echo "  2. Скачайте вручную:"
        echo "     https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
        echo "     Распакуйте и скопируйте gradle-${GRADLE_VERSION}/lib/gradle-wrapper-${GRADLE_VERSION}.jar"
        echo "     в gradle/wrapper/gradle-wrapper.jar"
        exit 1
    fi
    
    echo "✓ gradle-wrapper.jar скачан"
else
    echo "✓ gradle-wrapper.jar уже существует"
fi

# Set executable permission on gradlew
chmod +x "$SCRIPT_DIR/gradlew"
echo "✓ gradlew сделан исполняемым"

# Generate debug keystore if not present
DEBUG_KEYSTORE="$SCRIPT_DIR/app/debug.keystore"
if [ ! -f "$DEBUG_KEYSTORE" ]; then
    echo "Генерирую debug keystore..."
    keytool -genkeypair -v \
        -keystore "$DEBUG_KEYSTORE" \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || {
        echo "Предупреждение: не удалось сгенерировать debug keystore"
        echo "Keystore будет создан автоматически при сборке в CI"
    }
    if [ -f "$DEBUG_KEYSTORE" ]; then
        echo "✓ debug.keystore создан"
    fi
else
    echo "✓ debug.keystore уже существует"
fi

echo ""
echo "=== Готово! Теперь можно собирать: ==="
echo "  ./gradlew assembleDebug"
echo ""
