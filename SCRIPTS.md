# Скрипты для настройки проекта

## generate-binaries.sh

Генерирует все бинарные файлы, необходимые для Android-сборки:
- `gradle/wrapper/gradle-wrapper.jar`
- `app/debug.keystore`
- Делает `gradlew` исполняемым

**Использование:**
```bash
chmod +x generate-binaries.sh
./generate-binaries.sh
```

**Требования:**
- JDK 17+ (для keytool)
- curl или wget (для скачивания Gradle)
- unzip (для распаковки)

Если Gradle установлен глобально, скрипт использует его. Иначе скачивает дистрибутив.

## setup-wrapper.sh

Упрощённая версия — только wrapper и keystore:
```bash
chmod +x setup-wrapper.sh
./setup-wrapper.sh
```

## Что генерируется

### gradle-wrapper.jar
JAR-файл Gradle wrapper версии 8.9. Необходим для работы `./gradlew`.

**Альтернативы:**
```bash
# Если Gradle установлен:
gradle wrapper --gradle-version 8.9

# В CI генерируется автоматически
```

### debug.keystore
Фиксированный debug-ключ для консистентной подписи сборок.

**Параметры:**
- Store password: `android`
- Key alias: `androiddebugkey`
- Key password: `android`
- Algorithm: RSA 2048
- Validity: 10000 дней

**Альтернативы:**
```bash
keytool -genkeypair -v \
  -keystore app/debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

## CI behavior

В GitHub Actions оба файла генерируются автоматически:
- `gradle-wrapper.jar` — через `gradle wrapper --gradle-version 8.9`
- `debug.keystore` — через `keytool -genkeypair`

Это обеспечивает работу CI без необходимости коммитить бинарные файлы.

## Локальная разработка

Для локальной сборки выполните один раз:
```bash
./generate-binaries.sh
```

Или вручную:
```bash
gradle wrapper --gradle-version 8.9
chmod +x gradlew
# + сгенерируйте debug.keystore (см. выше)
```
