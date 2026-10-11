# CodeKey

CodeKey is a native Android coding keyboard designed to make programming approachable while typing.

## Direction

CodeKey is intended to be an open, user-controlled coding assistant rather than a general-purpose content censor. Adult users should be able to discuss or write mature/NSFW material, including code or creative text involving adult themes. The product should not generate or facilitate sexual content involving minors, and should avoid extreme graphic gore involving exposed organs or similarly disturbing injury detail. These limits apply to assistant-generated content, not ordinary code syntax or neutral discussion.

## Current foundation

- Native Kotlin Android app and InputMethodService keyboard.
- Offline, rule-based code completion.
- Common programming symbols and indentation shortcuts.
- Language selector covering Python, JavaScript, TypeScript, HTML, CSS, Kotlin, Java, C/C++, C#, JSON, SQL, and Bash.
- Password-field suggestion suppression.
- GitHub Actions debug APK artifact on every push to main and manual dispatch.

## English-to-code assistant

The offline starter generator supports common beginner patterns such as greetings/output, variables, functions, loops, conditions, and buttons. It provides an explanation and should always show the generated code before the user chooses to insert it. It is a starter template engine, not a model that understands arbitrary instructions.

## Build

GitHub Actions runs Gradle assembleDebug with JDK 17 and uploads the CodeKey-debug-apk artifact.

## Roadmap

1. Validate keyboard layout and input behavior on real Android devices.
2. Expand language-aware completions and offline English-to-code patterns.
3. Add optional local-model and configurable online-provider adapters.
4. Make network requests opt-in, show the destination/provider, and disclose the exact prompt before sending it.
5. Add tests and release signing configuration.

## Privacy

Offline completion does not send typed text to a server. Network AI is optional and must not be called without an explicit user action. Never transmit password-field text or silently upload surrounding text. The keyboard should use only the selected instruction or text the user explicitly approves for an AI request.

## License

Apache-2.0. See LICENSE.
