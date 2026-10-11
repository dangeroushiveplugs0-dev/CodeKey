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
- Offline English-to-code starter examples with explanations and copy support.
- Optional, explicit OpenAI-compatible chat-completions requests using a configurable endpoint and model.

## English-to-code assistant

The offline starter generator supports common beginner patterns such as greetings/output, variables, functions, loops, conditions, and buttons. It provides an explanation and shows the generated example before copying. It is a starter template engine, not a model that understands arbitrary instructions. The optional online path sends a request only after the user taps its button and uses a configurable OpenAI-compatible endpoint.

## Build

GitHub Actions runs Gradle assembleDebug with JDK 17 and uploads the CodeKey-debug-apk artifact.

## Roadmap

1. Validate keyboard layout and input behavior on real Android devices.
2. Expand language-aware completions and offline English-to-code patterns.
3. Add optional local-model and configurable online-provider adapters.
4. Make network requests opt-in, show the destination/provider, and disclose the exact prompt before sending it.
5. Add tests and release signing configuration.

## Privacy

Offline completion and offline code generation do not send text to a server. Online AI is optional and starts only when the user taps its send button; it sends the instruction field, selected language, and assistant prompt to the configured endpoint. The API key is not saved by CodeKey and is cleared after each request. Do not enter passwords or secrets. Never transmit password-field text or silently upload surrounding text.

## License

Apache-2.0. See LICENSE.
