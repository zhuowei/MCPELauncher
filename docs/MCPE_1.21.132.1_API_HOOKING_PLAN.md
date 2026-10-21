# MCPE 1.21.132.1 API and Hooking Upgrade Plan

## Compatibility truth

The target profile is **Minecraft Bedrock/MCPE 1.21.132.1 on Android ARM64**. A launcher build is not evidence that in-game APIs or native hooks work. Until a physical device test records the exact package version, ABI, library identities, and results, this profile stays **unverified** and native hook installation must remain disabled.

## API layers

1. **Launcher API:** launch the installed official app through Android public intents; import .mcpack/.mcaddon through Android's document picker and share/import flow; report explicit capabilities.
2. **Pack API:** use official Bedrock resource packs, behavior packs, and stable Script API modules where supported by the target client. The Dedicated Server Script API is not a client-side native-hook API; server-only modules must not be advertised as available in MCPE.
3. **Legacy ModPE compatibility (partial):** keep the existing Rhino runtime sandboxed in the launcher process. print, clientMessage, and log are logging shims, not Minecraft chat or world APIs. Any future API must declare its execution context and capability status.
4. **Native integration (experimental/disabled):** no guessed offsets, symbol names, structure layouts, or version-number fuzzy matches. Do not patch the official process until a lawful, reproducible profile is independently validated for the exact build and ABI.

## Hook lifecycle contract

A future native adapter must implement these gates in order:

- Identify the installed package and exact version; reject unknown or ambiguous versions.
- Confirm ABI is arm64-v8a and the expected library is actually mapped before resolving symbols.
- Resolve symbols from verified symbols/signatures or a versioned profile; never use hard-coded offsets from a different build.
- Validate the target function and memory region before any modification.
- Install hooks only after all checks pass; otherwise return a typed UNSUPPORTED_VERSION, LIBRARY_NOT_READY, SYMBOL_NOT_FOUND, or VALIDATION_FAILED status.
- Provide idempotent install/uninstall, original-call preservation, recursion protection, thread-safety, and crash-safe diagnostics.
- Keep the feature disabled by default until tests pass on the exact 1.21.132.1 APK and supported Android devices.

## API status model

Every exposed feature should report one of: AVAILABLE, LIMITED, UNAVAILABLE, or UNVERIFIED, plus a reason string and target profile. Do not infer support from a successful JNI load.

## Test gates for 1.21.132.1

- [ ] Record Minecraft package version and versionName/versionCode from a real device.
- [ ] Record Android version, device ABI, and Minecraft library names/build IDs.
- [ ] Clean Gradle build and native build pass in CI.
- [ ] Verify launcher intent and pack import on-device.
- [ ] Verify each official pack/script API against the exact client; record unsupported server-only APIs.
- [ ] Add unit tests for version matching, status serialization, and fail-closed behavior.
- [ ] Only then evaluate a minimal native integration proof, with no offsets copied from other versions.
- [ ] Regression-test launch, world loading, suspend/resume, and uninstall/disable path.

## Immediate scope

Implement the launcher and pack API first, improve structured diagnostics and compatibility reporting, and keep native hooks gated. The public Bedrock creator Script API documentation describes behavior-pack/server scripting, not a guarantee of arbitrary client-process access. See [Microsoft's Bedrock Dedicated Server scripting documentation](https://github.com/MicrosoftDocs/minecraft-creator/blob/main/creator/Documents/BedrockServer/scripting.md) and the upstream [BlockLauncher source](https://github.com/zhuowei/MCPELauncher) for historical context.
