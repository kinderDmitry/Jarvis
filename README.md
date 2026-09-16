# JARVIS 5.31.7 — Adaptive Assistant

Исправленная сборка с реальным системным VoiceInteractionService, отдельной voice-session для экрана блокировки, рабочим экраном настроек и локальной адаптивной памятью.

## Что исправлено
- Убран источник падения SettingsActivity: статусные TextView теперь создаются до добавления карточек. Экран больше не должен сваливаться в сообщение «Экран настроек не удалось построить».
- Убран `ACTION_ASSIST` у MainActivity, чтобы системный вызов ассистента не открывал полный экран приложения поверх экрана блокировки.
- VoiceInteractionSession использует собственную сессию JARVIS вместо MainActivity и может показываться поверх lock screen.
- После фразы «Джарвис» / «Привет, Джарвис» включается 8-секундное окно следующей команды без повторного обращения «Джарвис».
- Добавлено распознавание «продолжай», «продолжить», «дальше», «верни песню», «играй дальше» и других вариантов управления плеером.
- Добавлены команды «что сейчас играет», «поставь лайк», «поставь дизлайк» через Android MediaSession, если конкретный плеер поддерживает соответствующее действие.
- Локальная память теперь автоматически сохраняет успешные командные формулировки и использует их как примеры для будущих перефразировок. Добавлено расстояние Левенштейна к сравнению фраз.
- Сохранена работа с «моей музыкой»/понравившимися треками, персональными плейлистами и выбором музыкального приложения там, где это поддерживается самим приложением.

## Важно про интеллект
JARVIS не «переобучает» закрытую большую языковую модель на телефоне. В этой сборке обучение — локальная персональная адаптация: история успешных команд, предпочтения, контекст и устойчивое сопоставление новых формулировок с уже выполненными действиями. Это позволяет ассистенту становиться точнее именно для конкретного пользователя без внешнего сервера.

## Голосовой вызов
Основной системный механизм — Android `VoiceInteractionService`. Android поддерживает выбранный пользователем системный voice interactor как постоянно доступный сервис для hotword/assist-сценариев; это правильная архитектура для системного помощника. Обычный `SpeechRecognizer` не является заменой закрытого DSP/hotword-движка Google и поэтому фоновое слово активации зависит от ограничений Android, разрешения микрофона, батареи и производителя телефона.

## Сборка
GitHub Actions использует Gradle 8.11.1 и Java 17: `gradle --no-daemon --stacktrace clean :app:assembleRelease`.


## 5.31.8 — Smart Assistant
- исправлена совместимость локальной памяти с Android JSONObject;
- исправлен экран настроек и убран тупиковый экран-заглушка при ошибке построения;
- добавлен локальный слой понимания перефразировок команд;
- добавлено автоматическое запоминание успешных пользовательских формулировок как локальных алиасов;
- улучшен режим последовательного голосового диалога в VoiceInteractionSession;
- усилен кинематографичный мужской профиль TTS без подмены голоса конкретного актёра;
- сохранён системный путь VoiceInteractionService для вызова помощника с экрана блокировки.

Важно: Android не предоставляет стороннему приложению закрытую hotword-технологию Google Assistant, поэтому фоновая фраза «Джарвис» реализуется через разрешённый foreground microphone service, а надёжный вызов с экрана блокировки — через системную роль помощника.


## JARVIS 5.32.0 — Adaptive AI

This revision adds a dependency-free adaptive semantic brain on top of the real Android action layer. It does not claim to retrain a foundation model. Instead it persistently learns successful user phrasing, weakens failed interpretations, stores recent web knowledge with a TTL, and uses semantic token/character similarity to understand paraphrases. Unknown natural-language questions are sent to the existing web knowledge layer rather than being rejected immediately.

### Important behavior
- Web content is treated as information only; JARVIS never executes arbitrary instructions received from a web page.
- Learning is local to the device and can be cleared from Settings.
- The Android assistant role and VoiceInteractionService remain the primary system-wide invocation path. A normal third-party app cannot access Google's private hardware hotword model.
- The recognition-service settings target now opens JARVIS Control Center instead of the main launcher screen.
- Release CI verifies the APK and checks that no forbidden `keySet()` pattern remains.


## JARVIS 5.42.0 Professional AI — media and adaptive control

This revision keeps the existing local-first architecture and adds a provider-neutral media control layer.

### Media behavior
- Uses Android `MediaSession` as the primary control path, so JARVIS is not tied to one music provider.
- Prefers the currently playing/active media session and can control play, pause, next, previous, rewind and fast-forward where the player exposes the corresponding action.
- Supports semantic listening modes: road/driving, dancing, sport, work/focus, relaxation and sleep.
- The current music mode is remembered locally and can be replaced by a later command, e.g. switching from dance mode to road mode.
- Supports liked/favorites entry points for Yandex Music, Spotify, VK Music and YouTube Music where the provider exposes a usable web/deep-link destination.
- Uses provider-specific search URLs only for content discovery; playback itself is handed back to the app through the standard Android media-session path.
- Unknown installed media apps can be resolved by their launcher label instead of requiring a hard-coded package list.

### Important Android limitation
No third-party Android application can guarantee starting an arbitrary song inside every other music application: the target player must expose an Android `MediaSession` and/or an external search/deep-link interface. JARVIS therefore uses a layered strategy rather than pretending that every provider has the same private API.

### Voice
The project continues to support the Android Assistant/VoiceInteractionService path and a foreground microphone service. Android does not expose an unrestricted, battery-free third-party hotword API to ordinary applications; the implementation therefore does not claim guaranteed always-on hotword recognition on every device.

### Release
Version: `5.42.0-professional-ai`


## 5.43.0 — Interval Timer
Добавлен постоянный интервальный таймер: «каждые 10 минут», «повторяй каждые 30 минут», «интервальный таймер на 5 минут». Таймер повторно планирует следующее событие после каждого срабатывания и восстанавливается после BOOT_COMPLETED. Остановка: «останови интервальный таймер».
