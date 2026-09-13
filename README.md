# JARVIS — Professional HUD 5.10.0

Android voice assistant / local multitool.

## Build
`gradle --no-daemon --stacktrace :app:assembleDebug`

## Release/update
The application keeps the same `applicationId` (`com.jarvis.homemultitool`) and increments `versionCode` for each release. Installing a newer APK signed with the same signing key is therefore an Android update, not a second app. The GitHub Actions debug build uses the standard debug key; do not mix APKs signed by different keys.

## Voice
- Tap the main voice control or JARVIS core to start recognition.
- In Settings, grant microphone access and explicitly enable background wake word.
- Selecting JARVIS as the Android Assistant enables system assistant entry points where supported by the device/OEM.
- Background microphone operation is implemented as a foreground microphone service and is subject to Android/OEM restrictions.

## Timer
Timer commands first use `AlarmClock.ACTION_SET_TIMER` with skip-UI. If the device exposes no compatible Clock handler, JARVIS uses a persistent `AlarmManager` notification fallback and reports that only after scheduling succeeds.
