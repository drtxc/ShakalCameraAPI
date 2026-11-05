# 📷 ШакалCamera

Android-камера, которая снимает сразу шакальное фото и видео.

![Screenshot placeholder](docs/screenshots/preview.png)

## О приложении

ШакалCamera — нативное Android-приложение с 11 уровнями «шакальности» — от «почти прилично» (1280×720) до «4 оттенка серого с дизерингом Байера» (128×112).

**Делаем ваше фото прекрасным.**

### Возможности

- 🎬 Живое превью с эффектами через OpenGL ES 3.0
- 📸 Фото через CPU-пайплайн (идентично превью)
- 🎞️ Видео через MediaCodec (H.264 + AAC)
- 🦊 11 уровней шакалов (1–10 и 12)
- 💾 Сохранение через MediaStore
- 🔒 Никакой аналитики, рекламы и интернета
- 🌍 Готово для F-Droid

## Уровни

| Уровень | Разрешение | Характер |
|---------|-----------|----------|
| 1/10 | 1280×720 | На свету почти прилично |
| 2/10 | 640×480 | Мягко, тепло, лёгкое свечение |
| 3/10 | 640×480 | Шум, мыло, JPEG q≈40 |
| 4/10 | 640×480 | Шум в тенях, зелёный перекос |
| 5/10 | 352×288 | MJPEG-квадраты 8×8 |
| 6/10 | 320×240 | Крупные блоки, фиолетовая кайма |
| 7/10 | 160×120 | Цветные блоки, горячее пятно |
| 8/10 | 176×144 | Rolling shutter «желе» |
| 9/10 | 320×240 | Переслано 50 раз |
| 10/10 | 160×120 | Рыжая каша, фризы кадра |
| 12/10 | 128×112 | 4 оттенка серого, дизеринг Байера |

## Структура проекта

```
ШакалCamera/
├── app/                          # UI модуль (Jetpack Compose)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/.../ui/          # Compose UI, разрешения, MediaStore
│       └── res/                  # Ресурсы
├── cam-core/                     # Библиотека движка (без UI)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── assets/presets.json   # Параметры пресетов
│       └── java/.../core/        # GLSL, CPU-пайплайн, энкодер
├── gradle/libs.versions.toml     # Version catalog
├── LICENSE                       # GPL-3.0
├── README.md
├── TESTING.md
├── fastlane/metadata/android/    # F-Droid метаданные
└── .github/workflows/build.yml   # CI/CD
```

## Сборка

### Требования

- JDK 17+
- Android SDK 35
- Gradle 8.9+

### Локальная сборка

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (нужна подпись)
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### CI/CD

GitHub Actions собирает release APK на теге `v*` и прикладывает к GitHub Release.

Секреты репозитория:
- `KEYSTORE_BASE64` — base64 от keystore файла
- `KEYSTORE_PASSWORD` — пароль keystore
- `KEY_ALIAS` — alias ключа
- `KEY_PASSWORD` — пароль ключа

## Зависимости

| Библиотека | Версия | Модуль |
|-----------|--------|--------|
| Kotlin | 2.0.21 | all |
| AGP | 8.7.3 | build |
| Compose BOM | 2024.12.01 | app |
| CameraX | 1.4.1 | app |
| DataStore | 1.1.1 | app |
| Kotlinx Serialization | 1.7.3 | cam-core |

## Лицензия

GPL-3.0. См. файл [LICENSE](LICENSE).

## F-Droid

✅ Нет Google Play Services  
✅ Нет Firebase, аналитики, рекламы  
✅ Нет бинарных блобов  
✅ Нет разрешения INTERNET  
✅ Все зависимости из Maven Central / Google Maven  
✅ Лицензия GPL-3.0  
