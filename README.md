# JARVIS 5.21.0 — Premium Assistant

## This build
- Unified premium HUD geometry on the home screen and Settings: one button system, centered labels, stable 48–50dp action heights, controlled typography and no text-driven button expansion.
- Removed quick-action row from the home screen. The command composer is fixed at the bottom, wider and taller, with a send button matching the input height.
- Settings are rebuilt with wrap-content cards instead of fragile fixed card geometry, preventing text from overlapping controls or producing stretched/flattened layouts.
- Fixed the TTS settings compile problem by using the compatible Android TTS settings action string with a safe system-settings fallback.
- Preserved real Android settings destinations: Assistant role, microphone/app permissions, notifications, exact alarms, battery optimization, display, Wi-Fi and Bluetooth.
- VersionCode increased to 25 and versionName to 5.21.0-premium-jarvis. applicationId and release signing key are unchanged so a correctly signed release updates the installed JARVIS in place.
- Central core and the `ГОВОРИТЬ` button use real Android speech recognition.
- Wake-word mode continues to use a microphone foreground service and recognizes `Джарвис`, `Привет Джарвис`, `Джарвису` and English `Jarvis` variants when supplied by the recognizer.
- Natural dialogue improved: combined greetings such as `Привет, Джарвис, как дела?` receive a conversational response instead of a generic greeting.
- Live weather remains sourced from Open-Meteo; weather requests do not fall back to Wikipedia.
- App launching is handled before general web search; unknown `открой/запусти ...` commands return an explicit app-not-found response instead of reading unrelated web results.
- Fixed Java regex word-boundary handling for common app commands (YouTube, Telegram, WhatsApp, Chrome, Maps, Calculator).
- Existing local tools remain: time/date, battery, live weather and forecast, news, timer, alarm, calculator, flashlight, volume, camera, Wi-Fi/Bluetooth/display/settings, calendar, app launching, calls/SMS composer, local notes and web search.
- Male/female profiles use actual Russian Android TTS voices available on the device; the app does not claim to synthesize a cinema voice that Android does not provide.

## Android limitation
A normal `SpeechRecognizer` is not an OEM-level always-on hotword engine. The phrase `Привет, Джарвис` can work system-wide only while the foreground wake service is actually allowed to run and the device/OEM permits microphone background operation. Selecting JARVIS as the Android Assistant remains the reliable system invocation path.

## Build verification
The working container used for this archive does not have the Gradle CLI installed, so a local `assembleRelease` cannot be honestly claimed here. The included GitHub Actions workflow installs Gradle 8.11.1 and Java 17, runs `:app:assembleRelease`, verifies the APK with `file` and `unzip -t`, and uploads the release artifact.


## JARVIS 5.22.0 — major assistant/UI revision
- Reworked home and settings geometry: fixed-height controls, centered text, two-line-safe labels, consistent premium header/button language.
- Removed quick-action buttons from the home screen.
- Enlarged the bottom text composer and send control.
- Hardened SettingsActivity against the previous null-child crash and kept concrete Android settings destinations with safe fallbacks.
- Improved conversational state: casual replies such as «нормально», «хорошо», «а ты?» are handled as dialogue instead of being sent to web search.
- Added persistent local name memory and conversational continuity.
- Added adaptive music-app selection: explicit Yandex/VK choice, automatic single installed music app selection, or a follow-up question when an app must be chosen. Track search is forwarded to the selected music service where its installed deep-link handler permits it.
- Preserved the Android Assistant role, foreground wake-word service and «Привет, Джарвис» recognition path.
- VersionCode 26 / versionName 5.22.0-premium-jarvis. The release keeps the existing applicationId and release keystore so it can be installed as an update over 5.21.

### Important platform behavior
The wake-word implementation uses Android SpeechRecognizer in a foreground microphone service. Android/OEM restrictions can interrupt unrestricted always-on microphone recognition; selecting JARVIS as the Android Assistant remains the strongest system-wide invocation path available to this architecture.


## 5.23 UI
- Station-inspired premium conversational interface.
- Removed quick-action clutter from the main screen.
- Larger bottom composer and centered send control.
- Full-screen immersive mode retained.
- Settings navigation uses resilient Android settings intents.
- VersionCode increased to 27 so an APK signed with the included release key can update 5.22 in place.
