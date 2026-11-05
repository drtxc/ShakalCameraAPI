import { useState } from 'react'

const presets = [
  { level: 1, id: 'alu_720', res: '1280×720', desc: 'На свету почти прилично' },
  { level: 2, id: 'white_plastic_480', res: '640×480', desc: 'Мягко, тепло, лёгкое свечение' },
  { level: 3, id: 'netbook_03mp', res: '640×480', desc: 'Шум, мыло, JPEG q≈40' },
  { level: 4, id: 'laptop_2008', res: '640×480', desc: 'Шум в тенях, зелёный перекос' },
  { level: 5, id: 'usb_2005', res: '352×288', desc: 'MJPEG-квадраты 8×8' },
  { level: 6, id: 'office_2002', res: '320×240', desc: 'Крупные блоки, фиолетовая кайма' },
  { level: 7, id: 'flashlight_phone', res: '160×120', desc: 'Цветные блоки, горячее пятно' },
  { level: 8, id: 'cmos_2000', res: '176×144', desc: 'Rolling shutter «желе»' },
  { level: 9, id: 'forwarded_50', res: '320×240', desc: 'Переслано 50 раз' },
  { level: 10, id: 'bottom', res: '160×120', desc: 'Рыжая каша, фризы кадра' },
  { level: 12, id: 'four_tone', res: '128×112', desc: '4 оттенка серого, дизеринг Байера' },
]

function App() {
  const [activeTab, setActiveTab] = useState('overview')

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center gap-4">
          <div className="text-4xl">📷</div>
          <div>
            <h1 className="text-2xl font-bold text-amber-400">ШакалCamera</h1>
            <p className="text-sm text-gray-400">Делаем ваше фото прекрасным</p>
          </div>
          <div className="ml-auto flex gap-2">
            <span className="px-3 py-1 bg-green-900 text-green-300 rounded-full text-xs font-medium">GPL-3.0</span>
            <span className="px-3 py-1 bg-blue-900 text-blue-300 rounded-full text-xs font-medium">F-Droid Ready</span>
          </div>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-gray-800/50 border-b border-gray-700 px-6">
        <div className="max-w-6xl mx-auto flex gap-1">
          {['overview', 'presets', 'architecture', 'build', 'testing'].map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab
                  ? 'border-amber-400 text-amber-400'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              {tab === 'overview' && 'Обзор'}
              {tab === 'presets' && 'Пресеты'}
              {tab === 'architecture' && 'Архитектура'}
              {tab === 'build' && 'Сборка'}
              {tab === 'testing' && 'Тестирование'}
            </button>
          ))}
        </div>
      </nav>

      {/* Content */}
      <main className="max-w-6xl mx-auto px-6 py-8">
        {activeTab === 'overview' && <OverviewTab />}
        {activeTab === 'presets' && <PresetsTab />}
        {activeTab === 'architecture' && <ArchitectureTab />}
        {activeTab === 'build' && <BuildTab />}
        {activeTab === 'testing' && <TestingTab />}
      </main>

      {/* Footer */}
      <footer className="bg-gray-800 border-t border-gray-700 px-6 py-4 mt-12">
        <div className="max-w-6xl mx-auto text-center text-sm text-gray-500">
          <p>ШакалCamera © 2024 — Свободное программное обеспечение (GPL-3.0)</p>
          <p className="mt-1">Никакой аналитики, рекламы и Google Play Services</p>
        </div>
      </footer>
    </div>
  )
}

function OverviewTab() {
  return (
    <div className="space-y-8">
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">📱 О приложении</h2>
        <p className="text-gray-300 leading-relaxed">
          ШакалCamera — нативное Android-приложение, которое снимает фото и видео с намеренно «шакальным» качеством.
          11 уровней деградации — от «почти прилично» до «4 оттенка серого с дизерингом Байера».
        </p>
        <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          <FeatureCard icon="🎬" title="Камера" desc="CameraX Preview + ImageCapture + ImageAnalysis" />
          <FeatureCard icon="✨" title="GPU-эффекты" desc="OpenGL ES 3.0 шейдеры для живого превью" />
          <FeatureCard icon="🎞️" title="Видео" desc="MediaCodec H.264 + AAC, до 3 минут" />
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">🏗️ Структура проекта</h2>
        <div className="font-mono text-sm text-gray-300 bg-gray-900 rounded-lg p-4 overflow-x-auto">
          <pre>{`ШакалCamera/
├── app/                          # UI модуль (Compose)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/.../ui/          # Compose UI
│       └── res/                  # Ресурсы
├── cam-core/                     # Библиотека движка
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/presets.json   # Параметры пресетов
│       └── java/.../core/        # GLSL, CPU-пайплайн, энкодер
├── gradle/libs.versions.toml     # Version catalog
├── settings.gradle.kts
├── build.gradle.kts
├── LICENSE                       # GPL-3.0
├── README.md
├── TESTING.md
├── fastlane/metadata/android/    # F-Droid метаданные
└── .github/workflows/build.yml   # CI/CD`}</pre>
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">⚙️ Технологии</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {['Kotlin', 'Jetpack Compose', 'CameraX', 'OpenGL ES 3.0', 'MediaCodec', 'MediaMuxer', 'DataStore', 'MediaStore'].map(tech => (
            <div key={tech} className="bg-gray-900 rounded-lg px-3 py-2 text-center text-sm text-gray-300 border border-gray-700">
              {tech}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function PresetsTab() {
  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-2">🦊 Шкала шакалов</h2>
        <p className="text-gray-400 text-sm mb-4">Уровней 11 не существует. В UI показывается только цифра.</p>
        
        <div className="space-y-3">
          {presets.map(p => (
            <div key={p.level} className="flex items-center gap-4 bg-gray-900 rounded-lg p-4 border border-gray-700">
              <div className="w-12 h-12 rounded-full bg-amber-900/50 flex items-center justify-center text-amber-400 font-bold text-lg">
                {p.level}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm text-gray-400">{p.id}</span>
                  <span className="px-2 py-0.5 bg-gray-700 rounded text-xs text-gray-300">{p.res}</span>
                </div>
                <p className="text-gray-300 text-sm mt-1">{p.desc}</p>
              </div>
              <div className="text-gray-500 text-sm">{p.level}/10</div>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">📋 Поля пресета (presets.json)</h2>
        <div className="font-mono text-xs text-gray-300 bg-gray-900 rounded-lg p-4 overflow-x-auto">
          <pre>{`{
  "id": "alu_720",
  "level": 1,
  "width": 1280,
  "height": 720,
  "fpsMax": 30,
  "fpsLowLight": 8,
  "jpegQuality": 85,
  "jpegPasses": 1,
  "noise": 0.05,
  "shadowNoise": 0.1,
  "denoiseSmear": 0.3,
  "wbShift": [1.1, 1.05, 0.95],
  "saturation": 0.9,
  "chromaFringe": 0.0,
  "blockSize": 0,
  "vignette": 0.1,
  "hotspot": 0.0,
  "bloom": 0.0,
  "posterize": 0,
  "paletteSize": 0,
  "dither": 0,
  "freezeChance": 0.0,
  "rollingShutter": 0.0,
  "rowGlitch": 0.0
}`}</pre>
        </div>
      </div>
    </div>
  )
}

function ArchitectureTab() {
  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">🔄 GL-пайплайн превью</h2>
        <div className="font-mono text-xs text-gray-300 bg-gray-900 rounded-lg p-4 overflow-x-auto">
          <pre>{`Camera2 → SurfaceTexture
    ↓
OpenGL ES 3.0 Pipeline:
    1. Даунскейл в FBO (nearest) → разрешение пресета
    2. Цвет: ББ сдвиг → сатурация → постеризация → палитра+дизеринг
    3. Шум: зерно (сильнее в тенях, зависит от времени)
    4. Блоки 8×8: квантование яркости/цвета (MJPEG-имитация)
    5. Хроматическая кайма
    6. Виньетка / хотспот / bloom
    7. Апскейл на экран (nearest)
    ↓
GLSurfaceView / TextureView`}</pre>
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">📸 CPU-пайплайн фото</h2>
        <div className="font-mono text-xs text-gray-300 bg-gray-900 rounded-lg p-4 overflow-x-auto">
          <pre>{`ImageCapture (полное разрешение)
    ↓
CPU Pipeline (IntArray пиксели):
    1. Применить те же трансформации что и в GL
    2. Даунскейл до разрешения пресета (nearest)
    3. jpegPasses раз: Bitmap.compress(JPEG, q) → decode
    4. Апскейл nearest до 1280 по длинной стороне
    5. JPEG q=85, без EXIF геолокации
    ↓
MediaStore → Pictures/ShakalCamera/
    shakal_<level>_<timestamp>.jpg`}</pre>
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">🎞️ Видео-пайплайн</h2>
        <div className="font-mono text-xs text-gray-300 bg-gray-900 rounded-lg p-4 overflow-x-auto">
          <pre>{`GL Output → MediaCodec InputSurface
    H.264: разрешение = preset ×2 (nearest)
    Битрейт: lvl1=1.5Mbps ... lvl10,12=64kbps (log scale)
    
AudioRecord → MediaCodec (AAC)
    Lvl 1-6: mono 22050Hz, 24kbps
    Lvl 7-12: mono 8000Hz, 12kbps + clipping
    
MediaMuxer → Movies/ShakalCamera/
    Лимит: 3 минуты`}</pre>
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">📦 Зависимости</h2>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-gray-400 border-b border-gray-700">
              <th className="pb-2">Библиотека</th>
              <th className="pb-2">Версия</th>
              <th className="pb-2">Модуль</th>
            </tr>
          </thead>
          <tbody className="text-gray-300">
            <tr className="border-b border-gray-800"><td className="py-2">Kotlin</td><td>2.0.21</td><td>all</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">Android Gradle Plugin</td><td>8.7.3</td><td>build</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">Compose BOM</td><td>2024.12.01</td><td>app</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">CameraX</td><td>1.4.1</td><td>app</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">DataStore Preferences</td><td>1.1.1</td><td>app</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">Activity Compose</td><td>1.9.3</td><td>app</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">Lifecycle</td><td>2.8.7</td><td>app</td></tr>
            <tr className="border-b border-gray-800"><td className="py-2">JUnit</td><td>4.13.2</td><td>test</td></tr>
            <tr><td className="py-2">Kotlinx Serialization</td><td>1.7.3</td><td>cam-core</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}

function BuildTab() {
  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">🔨 Локальная сборка</h2>
        <div className="font-mono text-sm text-gray-300 bg-gray-900 rounded-lg p-4">
          <pre>{`# Клонировать репозиторий
git clone https://github.com/example/shakal-camera.git
cd shakal-camera

# Собрать debug APK
./gradlew assembleDebug

# APK будет в:
# app/build/outputs/apk/debug/app-debug.apk

# Собрать release (нужна подпись)
./gradlew assembleRelease`}</pre>
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">🚀 CI/CD (GitHub Actions)</h2>
        <div className="font-mono text-sm text-gray-300 bg-gray-900 rounded-lg p-4 overflow-x-auto">
          <pre>{`# .github/workflows/build.yml
# Триггер: push tag v*
# Секреты репозитория:
#   KEYSTORE_BASE64    — base64 от keystore файла
#   KEYSTORE_PASSWORD  — пароль keystore
#   KEY_ALIAS          — alias ключа
#   KEY_PASSWORD       — пароль ключа
#
# Результат: release APK приложен к GitHub Release`}</pre>
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">📱 F-Droid</h2>
        <div className="text-gray-300 space-y-2">
          <p>✅ Нет Google Play Services</p>
          <p>✅ Нет Firebase, аналитики, рекламы</p>
          <p>✅ Нет бинарных блобов</p>
          <p>✅ Нет разрешения INTERNET</p>
          <p>✅ Все зависимости из Maven Central / Google Maven</p>
          <p>✅ Лицензия GPL-3.0</p>
          <p>✅ fastlane metadata для ru-RU и en-US</p>
        </div>
      </div>
    </div>
  )
}

function TestingTab() {
  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">🧪 Юнит-тесты</h2>
        <div className="space-y-3">
          <TestItem name="PresetsTest" desc="presets.json парсится, уровни ровно {1..10, 12}, все поля присутствуют" />
          <TestItem name="CpuPipelineTest" desc="Каждый уровень на Bitmap 1920×1080 отдаёт результат без исключений с ожидаемыми размерами" />
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">📋 Ручной чек-лист (TESTING.md)</h2>
        <div className="space-y-2">
          <CheckItem text="Превью ≥25 fps на уровне 1 при хорошем освещении" />
          <CheckItem text="Просадка fps в темноте на уровнях 1 и 8" />
          <CheckItem text="Фото визуально совпадает с превью (палитра, шум, блоки)" />
          <CheckItem text="Видео играется в стандартном плеере" />
          <CheckItem text="Видео отправляется и воспроизводится в Telegram" />
          <CheckItem text="Фризы кадра работают на уровне 10" />
          <CheckItem text="Rolling shutter работает на уровне 8" />
          <CheckItem text="Переключение фронт/тыл без краша" />
          <CheckItem text="Смена уровня мгновенная, без пересоздания камеры" />
          <CheckItem text="Запоминание уровня и камеры после перезапуска" />
          <CheckItem text="Работа в портретной и альбомной ориентации" />
          <CheckItem text="Лимит записи видео — 3 минуты" />
        </div>
      </div>

      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-xl font-bold text-amber-400 mb-4">⚠️ Известные ограничения</h2>
        <div className="space-y-2 text-gray-300 text-sm">
          <p>• На очень слабых устройствах уровень 1 может не держать 30 fps</p>
          <p>• Уровень 9 (forwarded_50) — самый тяжёлый для CPU при сохранении фото</p>
          <p>• Rolling shutter (уровень 8) зависит от скорости движения камеры</p>
          <p>• На устройствах с ограниченным MediaCodec может не работать запись на уровне 1</p>
        </div>
      </div>
    </div>
  )
}

function FeatureCard({ icon, title, desc }: { icon: string; title: string; desc: string }) {
  return (
    <div className="bg-gray-900 rounded-lg p-4 border border-gray-700">
      <div className="text-2xl mb-2">{icon}</div>
      <h3 className="font-semibold text-gray-200">{title}</h3>
      <p className="text-sm text-gray-400 mt-1">{desc}</p>
    </div>
  )
}

function TestItem({ name, desc }: { name: string; desc: string }) {
  return (
    <div className="bg-gray-900 rounded-lg p-3 border border-gray-700 flex items-start gap-3">
      <span className="text-green-400 mt-0.5">✓</span>
      <div>
        <span className="font-mono text-sm text-amber-300">{name}</span>
        <p className="text-sm text-gray-400 mt-0.5">{desc}</p>
      </div>
    </div>
  )
}

function CheckItem({ text }: { text: string }) {
  return (
    <div className="flex items-center gap-2 text-sm text-gray-300">
      <span className="w-4 h-4 rounded border border-gray-500 flex-shrink-0"></span>
      {text}
    </div>
  )
}

export default App
