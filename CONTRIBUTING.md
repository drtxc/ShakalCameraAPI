# Contributing to ШакалCamera

## Первоначальная настройка

### Требования
- JDK 17+
- Android SDK 35
- Git

### Установка Gradle Wrapper

После клонирования репозитория нужно сгенерировать `gradle-wrapper.jar`:

```bash
# Вариант 1: через setup script (рекомендуется)
chmod +x setup-wrapper.sh
./setup-wrapper.sh

# Вариант 2: если Gradle установлен глобально
gradle wrapper --gradle-version 8.9

# Сделать gradlew исполняемым
chmod +x gradlew
```

### Генерация debug keystore

Для консистентной подписи debug-сборок:

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

Или запустите `./setup-wrapper.sh` — он создаст keystore автоматически.

### Сборка

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## CI/CD

### Debug Build (debug.yml)
- **Триггеры:** push в `main`, `workflow_dispatch`
- **Результат:** APK публикуется в Release `latest-debug`
- **Подпись:** фиксированный debug-ключ (можно обновлять поверх)

### Release Build (build.yml)
- **Триггер:** push тега `v*`
- **С секретами:** подписанный release APK
- **Без секретов:** debug APK с пометкой

## Структура коммитов

```
feat: описание фичи
fix: описание исправления
docs: изменения в документации
chore: служебные изменения (wrapper, CI)
refactor: рефакторинг без изменения функциональности
```

## Pull Requests

1. Создайте ветку от `main`: `git checkout -b feat/my-feature`
2. Сделайте изменения и закоммитьте
3. Убедитесь что `./gradlew assembleDebug` проходит
4. Откройте PR в `main`
5. Дождитесь зелёного CI (workflow `debug.yml`)
6. После мёрджа автоматически появится релиз `latest-debug`

## Лицензия

Весь код лицензируется под GPL-3.0. Отправляя PR, вы соглашаетесь лицензировать свой вклад под той же лицензией.
