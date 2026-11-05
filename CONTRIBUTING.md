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

### Сборка

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

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

## CI/CD

### Debug Build (`debug.yml`)
- Триггер: push в `main`, ручной запуск
- Собирает debug APK
- Загружает как artifact `shakal-camera-debug`

### Release Build (`build.yml`)
- Триггер: push тега `v*`
- Собирает release APK с подписью из secrets
- Создаёт GitHub Release

## Лицензия

Весь код лицензируется под GPL-3.0. Отправляя PR, вы соглашаетесь лицензировать свой вклад под той же лицензией.
