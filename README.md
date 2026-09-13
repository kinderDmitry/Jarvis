# JARVIS Local Max v5.0

Полностью локальная, dependency-free основа голосового JARVIS без облачного AI API и без собственного сервера.

## Быстрый принцип
- UI не выполняет тяжёлые операции.
- Быстрые команды идут напрямую в `JarvisEngine` без LLM.
- Голосовой ввод и TTS используют возможности Android.
- Системный ассистент поддерживает вызов с экрана блокировки в рамках Android API.
- Память хранится локально в SharedPreferences.
- Нет INTERNET permission.
- Никаких fake API responses: неподдерживаемые команды честно сообщаются пользователю.

## Реальные локальные функции
- голосовой/текстовый диалог;
- время и дата;
- батарея;
- таймер;
- будильник;
- калькулятор;
- локальная память/заметки;
- фонарик;
- громкость;
- камера;
- настройки Android;
- Wi-Fi/Bluetooth/экран — открытие системных настроек;
- календарь;
- музыкальное приложение;
- набор номера;
- подготовка SMS;
- системный Voice Assistant / Lock Screen entry point.

## Важно
Уровень ChatGPT-класса LLM, локальное image generation и text-to-video намеренно не маскируются под готовые функции: для них требуется соответствующая локальная модель и аппаратно-зависимый runtime. В этот базовый быстрый APK они не добавлены как фиктивные заглушки.

## JARVIS v5.1 — Voice Assistant / Wake Word

This revision fixes the assistant selection flow by using Android's `RoleManager.ROLE_ASSISTANT` request instead of merely opening generic voice-input settings. The app exposes a proper `VoiceInteractionService` and a keyguard-capable session.

It also adds an optional **«Джарвис» wake-word bridge**. When enabled by the user, a lightweight foreground microphone service listens in short recognition windows and opens JARVIS when it hears the name. This is intentionally optional because continuous microphone recognition has battery/privacy implications and is constrained by Android/OEM policies.

The visual direction is a dark, blue/cyan HUD with a central JARVIS Core, state transitions (LISTENING / THINKING / EXECUTING / SPEAKING), and a dedicated system-assistant card.

No cloud AI/API was added.

## v5.1 visual redesign

The UI has been redesigned against the supplied JARVIS reference: black/deep-navy surfaces, brighter cyan-blue neon core, concentric HUD rings, compact header, voice-first hierarchy, conversation card, voice-call controls, system-assistant controls and compact bottom navigation. The central core is drawn with Canvas to keep animation lightweight and avoid heavy bitmap/blur rendering.

The user reference is included at `docs/JARVIS_UI_REFERENCE_USER.png`; the internal visual prompt is documented in `docs/JARVIS_DESIGN_PROMPT.md`.
