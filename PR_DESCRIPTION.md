# PR: Add debug build workflow and Gradle wrapper files

## Summary

Добавляет CI для автоматической сборки debug APK и файлы Gradle wrapper.

## Changes

### `.github/workflows/debug.yml`
- Триггеры: push в `main` + `workflow_dispatch` (ручной запуск)
- Ubuntu Latest, JDK 17 (Temurin), `gradle/actions/setup-gradle@v4`
- Автоматическая генерация `gradle-wrapper.jar` если отсутствует
- Загрузка APK через `actions/upload-artifact@v4` как `shakal-camera-debug`
- Retention: 30 дней

### `gradlew` (Unix)
- Полноценный POSIX shell script от Gradle
- Поддержка Cygwin, MSYS, macOS, AIX
- Автоматический поиск JAVA_HOME
- Увеличение file descriptor limit

### `gradlew.bat` (Windows)
- Стандартный batch script от Gradle
- Поиск java.exe в PATH или JAVA_HOME

### `setup-wrapper.sh`
- Скрипт для разработчиков: скачивает `gradle-wrapper.jar` и выставляет права

## Before merge — manual steps required

Поскольку `gradle-wrapper.jar` — бинарный файл, его нужно сгенерировать локально:

```bash
# Вариант 1: через setup script
chmod +x setup-wrapper.sh
./setup-wrapper.sh

# Вариант 2: через Gradle (если установлен глобально)
gradle wrapper --gradle-version 8.9

# Сделать gradlew исполняемым
chmod +x gradlew
git update-index --chmod=+x gradlew

# Закоммитить wrapper jar
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "chore: add gradle-wrapper.jar"
```

## CI behavior

Workflow `debug.yml` работает в двух режимах:
1. **Если `gradle-wrapper.jar` есть в репо** — использует его напрямую
2. **Если jar отсутствует** — генерирует через `gradle wrapper` (требует установленный Gradle на runner, что обеспечивается `gradle/actions/setup-gradle@v4`)

## Testing

- [ ] Push в main триггерит workflow
- [ ] Ручной запуск через Actions → Debug Build → Run workflow
- [ ] Артефакт `shakal-camera-debug` появляется в Artifacts
- [ ] APK устанавливается на устройство
