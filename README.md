# JARVIS 5.5 — Web Agent

Voice-first Android assistant with a premium dark/cyan HUD interface.

## What changed
- Fixed `RecognitionListener` bridge with `onEvent(int, Bundle)`.
- Added INTERNET permission and a dependency-free web retrieval layer.
- Unknown questions are searched online instead of returning a canned local-only message.
- Fast deterministic commands stay local for low latency.
- Main screen is voice-first: large JARVIS Core, compact chat, microphone action and four useful quick commands.
- System-assistant and voice configuration remain in Settings, not on the home screen.
- No documentation, reference PNGs, demo assets or unused design files are included.

## Build
`gradle --no-daemon --stacktrace :app:assembleDebug`

## Network retrieval
The current WebSearchEngine uses public Wikipedia and DuckDuckGo endpoints and does not require an API key. It is a retrieval layer, not a general-purpose LLM. A future AI provider can be inserted behind the same engine interface without changing the UI.
