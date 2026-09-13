# JARVIS Local Max v5.2 — Voice Fix + Clean HUD

Полностью локальная основа JARVIS без INTERNET permission, облачного AI API и собственного сервера.

## Что исправлено в v5.2

### Критический crash fix
В предыдущей версии callback состояния из `JarvisEngine` вызывал сам себя рекурсивно (`state(s)`), что приводило к переполнению стека и принудительному закрытию приложения. Теперь callback явно вызывает `MainActivity.this.setState(...)`.

### Почему JARVIS не появлялся среди системных ассистентов
Android требует для VoiceInteractionService корректные `sessionService`, `recognitionService` и `supportsAssist=true`. В v5.2 добавлен настоящий `JarvisRecognitionService`, его metadata и отдельный recognition-service XML. MainActivity также объявляет `ACTION_ASSIST` как дополнительный qualifying path. Это соответствует текущей проверке Android assistant role. См. Android RoleManager/AssistantRoleBehavior.

### Вызов с экрана блокировки
- `VoiceInteractionService` — direct-boot aware.
- `VoiceInteractionSessionService` — отдельный process `:voice`.
- `supportsLaunchVoiceAssistFromKeyguard=true`.
- `onLaunchVoiceAssistFromKeyguard()` открывает voice session.
- Activity имеет `showWhenLocked` и `turnScreenOn`.

### Голос
- На Android 12+ при наличии устройства использует `SpeechRecognizer.createOnDeviceSpeechRecognizer()`.
- Иначе использует системный recognition service.
- TTS использует установленный на устройстве движок.
- Никакого собственного сетевого API в приложении нет.

## Новый интерфейс
Главный экран больше не похож на мультитул. Это один голосовой помощник:

`JARVIS → AI CORE → состояние → диалог → ввод → системный ассистент`

Убраны длинные описательные карточки, декоративные абзацы и плотная сетка маленьких кнопок. Core рисуется Canvas-ом без bitmap-heavy эффектов и без software blur.

## Быстрые локальные команды
- время / дата;
- батарея;
- таймер;
- будильник;
- калькулятор;
- локальная память;
- фонарик;
- громкость;
- камера;
- настройки;
- Wi-Fi / Bluetooth / экран;
- календарь;
- музыка;
- набор номера;
- SMS.

Неподдерживаемые команды не маскируются под выполненные.

## GitHub
Workflow использует Gradle 8.11.1 и Java 17 и собирает `app-debug.apk` как artifact.

## Ограничение
Это быстрый локальный foundation. Полноценная локальная LLM уровня ChatGPT, image generation и text-to-video требуют отдельных локальных моделей и аппаратно-зависимого runtime; фиктивных заглушек под эти функции нет.
