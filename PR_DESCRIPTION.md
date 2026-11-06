# PR: Debug build → Releases + фиксированный debug-ключ

## Summary

1. Gradle wrapper и debug keystore генерируются в CI автоматически
2. Каждая сборка debug попадает в GitHub Release `latest-debug`
3. Release-сборка на тегах `v*` с fallback на debug при отсутствии секретов
4. Фиксированный debug-ключ для возможности обновления поверх

## Changes

### `.github/workflows/debug.yml`
- Триггеры: push в `main`, `workflow_dispatch`
- `permissions: contents: write`
- Авто-генерация `gradle-wrapper.jar` и `debug.keystore` если отсутствуют
- Переименование APK: `ShakalCamera-debug-<sha>.apk`
- Удаление старого релиза `latest-debug` перед публикацией нового
- Публикация через `softprops/action-gh-release@v2`:
  - `tag_name: latest-debug`, `prerelease: true`, `make_latest: false`
  - Body: sha коммита, дата, сообщение
- Дополнительно: `actions/upload-artifact@v4`

### `.github/workflows/build.yml`
- Триггер: push тега `v*`
- Если секреты подписи заданы → release APK в релиз тега (`prerelease: false`, `make_latest: true`)
- Если секретов нет → debug APK в релиз тега с пометкой и `prerelease: true`
- Авто-генерация wrapper

### `app/build.gradle.kts`
- `signingConfigs.debug` → `app/debug.keystore`
- `buildTypes.debug.signingConfig = signingConfigs.debug`

### `.gitignore`
- `!app/debug.keystore` — debug keystore коммитится
- `*.jks` — release keystore игнорируется

### `setup-wrapper.sh`
- Скачивает `gradle-wrapper.jar` или подсказывает как сгенерировать
- Генерирует `debug.keystore` через keytool
- Делает `gradlew` исполняемым

### `README.md`
- Ссылки на `drtxc/ShakalCameraAPI`
- Таблица скачивания: stable release + latest-debug
- Бейдж CI

## CI behavior

### Debug (push в main)
```
checkout → setup-java → setup-gradle → generate wrapper → generate keystore
→ assembleDebug → rename APK → upload artifact
→ delete old release → create new release (latest-debug)
```

### Release (push tag v*)
```
checkout → setup-java → setup-gradle → generate wrapper
→ check secrets → [if secrets: decode keystore → assembleRelease]
                  [if no secrets: assembleDebug]
→ rename APK → create release
```

## Before merge — для локальной разработки

```bash
# Настроить wrapper и keystore
chmod +x setup-wrapper.sh && ./setup-wrapper.sh

# Или вручную:
gradle wrapper --gradle-version 8.9
chmod +x gradlew
keytool -genkeypair -v -keystore app/debug.keystore \
  -storepass android -alias androiddebugkey -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

## Testing

- [ ] Push в main → появляется релиз `latest-debug` с APK
- [ ] Ручной запуск workflow_dispatch → появляется релиз
- [ ] Старый релиз удаляется, новый содержит свежий APK
- [ ] APK устанавливается и обновляется поверх (тот же ключ)
- [ ] Push тега v1.0.0 → появляется релиз с тегом
- [ ] Артефакт доступен в Actions → Artifacts
