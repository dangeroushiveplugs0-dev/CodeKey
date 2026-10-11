# CodeKey

CodeKey is an Android coding keyboard designed to make programming more approachable while typing.

## Current foundation

- Native Kotlin Android app and InputMethodService keyboard.
- Offline, rule-based completion suggestions.
- Common programming symbols and indentation shortcuts.
- Language selector covering Python, JavaScript, TypeScript, HTML, CSS, Kotlin, Java, C/C++, C#, JSON, SQL, and Bash.
- Password-field suggestion suppression.
- GitHub Actions debug APK artifact on every push to `main` and manual dispatch.

## Build

GitHub Actions runs `gradle assembleDebug` with JDK 17 and uploads `CodeKey-debug-apk`.

## Roadmap

1. Validate keyboard layout and input behavior on real Android devices.
2. Improve token-aware suggestions and language-specific snippet catalogs.
3. Add a dedicated English-to-code assistant panel with explanation and explicit insert action.
4. Add optional local-model and configurable online-provider adapters.
5. Add privacy controls, connection testing, and request previews before sending text to any online AI.
6. Add tests and release signing configuration.

## Privacy

The current completion engine is local and does not send typed text to a server. AI networking is not implemented in this initial foundation. CodeKey disables suggestions in standard Android text password fields; individual apps may expose different editor behavior.

## License

Apache-2.0. See [LICENSE](LICENSE).
