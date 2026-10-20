# Nex-Launcher modern Android shell

An isolated Gradle-based launcher shell alongside the inherited Ant project. It does not compile or replace the legacy JNI patching engine.

## Build

Requires JDK 17, Gradle compatible with Android Gradle Plugin 8.13.2, and Android SDK Platform 36:

```sh
gradle --no-daemon :modern-launcher:assembleDebug
```

The APK is emitted under `modern-launcher/build/outputs/apk/debug/`.

## Current scope

- Native Android Activity and simple launcher UI.
- Detects and opens the official Minecraft Android package via Android launch intent.
- Uses Android package visibility declarations rather than broad storage permissions.
- Does not yet import resource packs, patch Minecraft, inject native code, or run ModPE scripts.
- Minecraft 1.21.132.1 is a test target only; launch and gameplay need device verification.

The original BlockLauncher Ant/JNI project remains intact at repository root while its native ABI and dependency migration is investigated separately.
