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
- Multi-language code playground with an editable source area, run action, stdout/stderr display, and copy-source action.
- Optional, explicit OpenAI-compatible chat-completions requests using a configurable endpoint and model.

## English-to-code assistant

The offline starter generator supports common beginner patterns such as greetings/output, variables, functions, loops, conditions, and buttons. It provides an explanation and shows the generated example before copying. It is a starter template engine, not a model that understands arbitrary instructions. The optional online path sends a request only after the user taps its button and uses a configurable OpenAI-compatible endpoint.

## Code Playground

The playground offers Python, JavaScript, TypeScript, Java, C, C++, C#, Go, Rust, PHP, Ruby, Bash, Kotlin, Swift, R, Perl, Lua, Dart, Scala, and Elixir through the public Piston runner. Online execution is explicit: source code and the optional stdin field are sent to Piston only when Run is tapped. Do not submit secrets or private source code. Runner language availability and versions may change.

The HTML / CSS / JS mode previews a complete HTML document in an on-device WebView, including embedded styles and scripts. External navigation and network resource requests are blocked in that preview. Draft source, selected language, and stdin are saved locally on the device. This preview is not a general-purpose secure sandbox for untrusted code; only preview code you understand. Other language execution still requires the online runner.

## Build

GitHub Actions runs Gradle assembleDebug with JDK 17 and uploads the CodeKey-debug-apk artifact.

## Roadmap

1. Validate keyboard layout and input behavior on real Android devices.
2. Expand language-aware completions and offline English-to-code patterns.
3. Add a local-model adapter and a model manager with model-size/RAM estimates.
4. Expand local web preview controls and investigate truly local execution options where feasible.
5. Make network requests opt-in, show the destination/provider, and disclose the exact prompt before sending it.
6. Add tests and release signing configuration.

## Privacy

Offline completion and offline code generation do not send text to a server. Online AI is optional and starts only when the user taps its send button; it sends the instruction field, selected language, and assistant prompt to the configured endpoint. The API key is not saved by CodeKey and is cleared after each request. Do not enter passwords or secrets. Never transmit password-field text or silently upload surrounding text.

## License

Apache-2.0. See LICENSE.
