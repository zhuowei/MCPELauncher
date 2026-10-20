# Nex-Launcher modern Android shell

An isolated Gradle-based Android launcher alongside the inherited Ant project.

## Build

Requires JDK 17, Android SDK Platform 36, and the Android Gradle Plugin toolchain used by the workflow:

```sh
gradle --no-daemon :modern-launcher:assembleDebug
```

The APK is emitted under `modern-launcher/build/outputs/apk/debug/`.

## Implemented in the modern shell

- Detects and opens the official Minecraft Android package through its public launch intent.
- Uses Android's Storage Access Framework to select a pack without broad storage permissions.
- Copies selected input to app cache with a 512 MiB size limit, validates ZIP structure, rejects unsafe paths, checks for a root `manifest.json`, and validates basic header/module UUID, version, and type fields.
- Hands a validated archive to Android's app chooser using a scoped FileProvider URI, so Minecraft can import it if the installed version registers the relevant import intent.
- Runs local JavaScript with Rhino 1.7.15 using interpreter mode, a source-size limit, and an instruction budget.
- Includes a small logging-only ModPE compatibility subset: `print()`, `clientMessage()`, `log()`, and `ModPE.getApiStatus()`.
- Shows the installed Minecraft version and explicit capability/limitation status in the UI.
- Includes a sample official Bedrock behavior pack in `samples/bedrock-1211321-test-pack/`; CI validates and publishes it as a `.mcpack` artifact.

## Important limitations

- The ModPE subset runs inside Nex-Launcher only. It does not execute scripts inside Minecraft and does not implement world, player, entity, rendering, or game-event APIs.
- No Bedrock memory patching, process injection, asset-loader hooks, or native in-game hooks are implemented.
- Minecraft Bedrock 1.21.132.1 is a target for testing, not a verified compatibility claim.
- Pack validation checks archive safety and basic manifest structure; it does not prove that every pack asset is valid or that Minecraft will accept the pack. Import depends on the installed Minecraft app's intent handling and must be tested on-device.
- The legacy BlockLauncher-derived Ant/JNI project remains at repository root and has not been migrated into this module.

A successful Gradle build means only that the launcher shell compiles. It does not establish in-game hook compatibility.
