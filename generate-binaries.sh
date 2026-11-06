#!/bin/sh
# Генерация всех бинарных файлов для Android-сборки
# Запустите один раз после клонирования репозитория

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔══════════════════════════════════════════════════╗"
echo "║  ШакалCamera: Генерация бинарных файлов         ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# 1. Gradle wrapper jar
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "📦 [1/3] Генерация gradle-wrapper.jar..."
    
    # Try to use gradle if available
    if command -v gradle >/dev/null 2>&1; then
        echo "   Используем установленный Gradle..."
        gradle wrapper --gradle-version 8.9
        echo "   ✅ gradle-wrapper.jar создан через gradle wrapper"
    else
        # Download from Gradle distributions
        echo "   Gradle не установлен, скачиваем wrapper jar..."
        
        GRADLE_VERSION="8.9"
        TEMP_DIR=$(mktemp -d)
        ZIP_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
        
        if command -v curl >/dev/null 2>&1; then
            curl -fsSL -o "$TEMP_DIR/gradle.zip" "$ZIP_URL"
        elif command -v wget >/dev/null 2>&1; then
            wget -q -O "$TEMP_DIR/gradle.zip" "$ZIP_URL"
        else
            echo "   ❌ ОШИБКА: Не найден curl или wget"
            echo "   Установите Gradle: https://gradle.org/install/"
            exit 1
        fi
        
        echo "   Распаковываем..."
        cd "$TEMP_DIR"
        unzip -q gradle.zip
        cp "gradle-${GRADLE_VERSION}/lib/plugins/gradle-wrapper-${GRADLE_VERSION}.jar" "$SCRIPT_DIR/$WRAPPER_JAR" 2>/dev/null || \
        cp "gradle-${GRADLE_VERSION}/lib/gradle-wrapper-${GRADLE_VERSION}.jar" "$SCRIPT_DIR/$WRAPPER_JAR" 2>/dev/null || {
            # Try to find it
            find . -name "gradle-wrapper*.jar" -exec cp {} "$SCRIPT_DIR/$WRAPPER_JAR" \;
        }
        cd "$SCRIPT_DIR"
        rm -rf "$TEMP_DIR"
        
        if [ -f "$WRAPPER_JAR" ]; then
            echo "   ✅ gradle-wrapper.jar скачан и установлен"
        else
            echo "   ❌ Не удалось найти wrapper jar в дистрибутиве"
            echo "   Установите Gradle и выполните: gradle wrapper --gradle-version 8.9"
            exit 1
        fi
    fi
else
    echo "✅ [1/3] gradle-wrapper.jar уже существует"
fi

# 2. gradlew executable
echo ""
echo "🔧 [2/3] Настройка прав на gradlew..."
chmod +x gradlew
echo "   ✅ gradlew сделан исполняемым"

# 3. Debug keystore
DEBUG_KEYSTORE="app/debug.keystore"
if [ ! -f "$DEBUG_KEYSTORE" ]; then
    echo ""
    echo "🔑 [3/3] Генерация debug.keystore..."
    
    if command -v keytool >/dev/null 2>&1; then
        keytool -genkeypair -v \
            -keystore "$DEBUG_KEYSTORE" \
            -storepass android \
            -alias androiddebugkey \
            -keypass android \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -dname "CN=Android Debug,O=Android,C=US" 2>&1 | head -1
        echo "   ✅ debug.keystore создан"
    else
        echo "   ⚠️  keytool не найден"
        echo "   debug.keystore будет создан автоматически в CI"
        echo "   Для локальной сборки установите JDK и повторите"
    fi
else
    echo ""
    echo "✅ [3/3] debug.keystore уже существует"
fi

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║  ✅ Готово! Можно собирать:                      ║"
echo "║                                                  ║"
echo "║  ./gradlew assembleDebug                         ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
