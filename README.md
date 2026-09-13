# JARVIS 5.17.0 — Premium Assistant

## Что изменено
- Полностью унифицирована типографика и геометрия интерактивных элементов: главные кнопки, быстрые действия, инструменты и системные настройки используют одну адаптивную HUD-систему без фиксированных огромных шрифтов.
- Настройки перестроены как аккуратные карточки с безопасными высотами и переносом описаний; текст больше не должен попадать под кнопки.
- Полноэкранный immersive-режим применяется на главном экране и в настройках и повторно включается при возврате Activity.
- Нажатие на центральное ядро и `ГОВОРИТЬ` запускает реальный Android SpeechRecognizer.
- Фоновый режим «Джарвис» теперь обрабатывает команду внутри foreground-сервиса, поэтому ответ не зависит от того, какое приложение открыто на экране. После ответа микрофон освобождается и слушатель запускается заново.
- Добавлена более аккуратная обработка сегментов распознавания и выбор on-device распознавания, когда оно доступно.
- Системный Assistant Role сохранён как основной и наиболее надёжный способ вызова JARVIS системной кнопкой помощника.
- Реальная текущая погода получается через Open-Meteo; запросы `завтра` и `послезавтра` получают отдельный прогноз. Погода не берётся из Википедии.
- Добавлен отдельный live-поток новостей через Google News RSS для команд `новости`, `новости сейчас` и похожих запросов.
- Добавлен короткий контекст погоды: после запроса погоды фраза `а завтра?` понимается как продолжение разговора.
- Расширены реальные локальные команды: время, дата, батарея, таймер, будильник, фонарик, громкость, камера, Wi-Fi, Bluetooth, экран, системные настройки, разрешения приложения, календарь, музыка, звонок и SMS-композер, локальная память и калькулятор.
- Мужской/женский профили не создают искусственный аудиофайл: JARVIS выбирает реально установленный русский голос Android TTS и меняет тембр/скорость профиля. Кнопка TTS ведёт в настоящие настройки синтеза речи Android.
- Исправлена причина предыдущей ошибки TTS: качество голоса сравнивается через `Voice.QUALITY_NORMAL/HIGH`, а не через несуществующий `TextToSpeech.VOICE_QUALITY_NORMAL`.
- Release поднят до `5.17.0`, versionCode `20`; signing config и applicationId сохранены.

## Важное ограничение Android
Обычный `SpeechRecognizer` не является системным всегда-включённым hotword engine. Поэтому фоновый режим реализован как foreground-сервис с короткими окнами распознавания. Для системного вызова из любой точки Android рекомендуется назначить JARVIS системным помощником. Наличие собственного wake-word «Джарвис» полностью без ограничений зависит от политики конкретной версии Android/OEM и не может быть гарантировано обычному приложению.

## Проверка сборки
В текущем рабочем контейнере Gradle CLI отсутствует, поэтому здесь невозможно честно заявить о локальном `assembleRelease`. Проект подготовлен для GitHub Actions: workflow устанавливает Gradle 8.11.1 и Java 17, собирает release и дополнительно проверяет наличие и целостность APK через `file` и `unzip -t`.


## 5.17 UI / voice stability patch
- Unified density-independent sizing across the main HUD and Settings; long labels use two-line wrap instead of clipped text.
- Settings buttons use safe Android intent resolution with concrete fallbacks, including TTS, notifications, permissions, battery, alarms, Wi-Fi and Bluetooth.
- App launching now targets installed packages by name; Yandex Music uses the verified Android package `ru.yandex.music`, with installed-app fallback.
- Voice recognition prefers the device's normal recognition service for better Russian recognition; the assistant recognition service keeps an on-device fallback.
- Release build remains reproducible through the included GitHub Actions workflow.
