# JARVIS 5.31 — Assistant / Wake / Adaptive Memory

## Что изменено
- Сохранён тот же `applicationId` и release keystore: обновление устанавливается поверх 5.30 без удаления приложения, если предыдущая APK была подписана тем же ключом.
- `VoiceInteractionService` используется как системная точка входа. Пользователь может назначить JARVIS системным помощником Android.
- Поддержаны фразы активации **«Джарвис»** и **«Привет, Джарвис»** в локальном wake-listener.
- При наличии Android on-device SpeechRecognizer он используется первым; иначе применяется системный SpeechRecognizer.
- После выбора JARVIS системным помощником VoiceInteractionService может запускать явно включённый wake-listener; это соответствует архитектуре Android, но не подменяет закрытый низкопотребляющий hotword DSP Google.
- Команды выполняются через защищённый `JarvisEngine`: неизвестная команда больше не должна приводить к падению Activity/сервиса — есть общий safe fallback.
- Локальная память расширена: псевдонимы команд, нечёткое сопоставление похожих фраз, счётчик использования и расширенный контекст диалога.
- Управление медиаплеером использует `MediaSession`/`MediaController`, затем fallback на media key.
- Музыкальные команды умеют выбирать установленное музыкальное приложение, запоминать предпочтение и просить пользователя выбрать сервис, если он не определён.
- Поиск фильма/сериала возвращает несколько найденных веб-источников, а не выдумывает наличие контента.
- Настройки полностью переписаны с defensive UI: ни один элемент не добавляется как `null`, системные статусы читаются из Android, а fallback-экран остаётся доступным даже при исключении.
- Главный экран сохраняет адаптивный composer: при открытии IME строка ввода поднимается над клавиатурой, при закрытии возвращается вниз.

## Важное ограничение вызова
Публичный Android API не предоставляет стороннему приложению возможность просто зарегистрировать произвольную фразу «Джарвис» в том же низкопотребляющем hotword-движке, который использует Google Assistant. Официальный путь для системного помощника — `VoiceInteractionService`; собственная фраза реализована отдельным локальным wake-listener. Для максимально надёжного режима назначьте JARVIS помощником Android, выдайте микрофон и отключите для JARVIS агрессивную оптимизацию батареи.

## Сборка
```bash
gradle --no-daemon --stacktrace :app:assembleRelease
```


## 5.31.x assistant architecture

- `JarvisVoiceInteractionService` is the Android system-assistant entry point.
- `JarvisVoiceSession` is the lock-screen assistant surface; it no longer launches the full `MainActivity`, so the assistant invocation does not replace the keyguard with the application UI.
- `JarvisWakeWordService` provides a best-effort background wake listener for `Джарвис` and `Привет, Джарвис`. It uses Android speech recognition in short sessions because a normal third-party `SpeechRecognizer` is not equivalent to Google's privileged hardware hotword engine.
- `JarvisMemory` stores local aliases and dialogue context. Learned phrases are matched with exact and similarity-based lookup. This is adaptive command learning, not retraining a foundation model.
- Media control uses Android `MediaSession`/`MediaController` after notification-listener access is granted.
- Release CI performs a clean release build and verifies the generated APK.

### Important Android setup

For system-wide invocation, select **JARVIS as the Android Assistant**. For `Джарвис` / `Привет, Джарвис` background activation, grant microphone access and enable the background wake listener. Battery optimization may need to be disabled on devices that aggressively stop microphone foreground services.

## 5.31.4 build fix
- Fixed JSONObject iteration to use `names()`/`jsonKeys()` compatible with Android JSON API.
- Fixed explicit `AlertDialog` reference in SettingsActivity.
- Release version bumped to 5.31.4.
