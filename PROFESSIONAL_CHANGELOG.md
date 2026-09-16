# JARVIS 5.40.0 Professional AI

## Main objective
Transform JARVIS from a command list into a local-first assistant with semantic routing, persistent context, adaptive phrasing and provider-neutral media control.

## Implemented in this revision

### Media
- Android MediaSession is now the primary provider-neutral control path.
- Active playing session is preferred over an arbitrary session.
- Play/resume, pause/stop, next, previous, rewind and fast-forward are routed through TransportControls where supported.
- Current track exposes title, artist, album and provider package.
- Like/dislike is attempted only when the media session advertises rating support.
- Music provider selection supports Yandex Music, VK Music, Spotify, YouTube Music and unknown installed media applications.
- Unknown installed apps can be resolved by their launcher label.
- Provider search URLs are isolated to discovery; playback control remains MediaSession based.
- Local semantic music modes: road/driving, dance, sport, work/focus, relaxation, sleep.
- Current music mode is persisted locally and can be replaced by a subsequent request.
- Liked/favorites entry points are supported for the major providers where a public destination exists.
- Follow-up requests such as continue/next/previous can operate on the currently active player instead of a hard-coded app.

### Adaptive understanding
- Added semantic recognition for mode changes such as switching from dance music to road music.
- Existing adaptive memory remains success-weighted: successful actions strengthen a phrase and failed actions weaken it.
- Context and learned aliases continue to be handled before generic web fallback.

### Voice / lock screen
- Existing VoiceInteractionService path retained.
- Existing foreground wake-word service retained with Android-compliant foreground microphone behavior.
- Recognition uses on-device speech recognition when available and falls back to the system recognizer.
- Lock-screen voice surface remains separate from normal application UI.
- Media commands can remain useful from the lock-screen assistant surface.

### Engineering
- Version bumped to 5.40.0-professional-ai.
- Release workflow updated.
- Removed/verified absence of the previously reported `keySet()` compile marker.
- Static source balance and XML parsing checks pass.
- No placeholder/demo command was introduced.

## Platform boundary
Android does not provide a universal public API that lets a third-party assistant force an arbitrary song to play inside every third-party music application. A provider must expose a MediaSession and/or a supported external search/deep-link surface. JARVIS therefore uses the strongest safe route available per app instead of pretending that private provider APIs exist.

## Build verification
A local Android SDK/Gradle installation was not available in the execution environment used to edit this archive, so a real `assembleRelease` could not be executed here. The included GitHub Actions workflow remains the authoritative release build path and runs Gradle 8.11.1 with Java 17.

## 5.40.1 — Release build fix
- Fixed `JarvisVoiceSession.java`: `VoiceInteractionSession.getWindow()` returns a `Dialog`, not a `Window`.
- The session surface now obtains the actual `Window` via `Dialog.getWindow()` before applying transparent background and dim-behind settings.
- No functional change to voice recognition or media routing.

## 5.42.0 — Command & Device Expansion
- Expanded device command routing.
- Added timer remaining-time query.
- Added persistent local stopwatch state.
- Added percentage-based media volume control.
- Added contacts/device-info/battery settings actions.
- Extended Russian duration parsing with common number words.
- Added `JARVIS_5_42_0_CHECKLIST.md` with implementation and verification status.


## 5.43.0 — Persistent Interval Timer
- Добавлен постоянный интервальный таймер.
- Повторное планирование через AlarmManager после каждого срабатывания.
- Сохранение интервала и метки в SharedPreferences.
- Восстановление после перезагрузки через BOOT_COMPLETED.
- Голосовая остановка.
