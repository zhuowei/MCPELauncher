# Nex-Launcher Revival Roadmap

## Goal

Modernize the Android project while preserving the original BlockLauncher-derived code and honestly separating launcher support from legacy game patching. Target test version: Minecraft Bedrock/MCPE 1.21.132.1.

## Source audit findings

- Legacy root project uses Ant; `project.properties` targets API 23 and references sibling libraries and legacy components.
- `AndroidManifest.xml` declares min SDK 9 and the old `net.zhuoweizhang.mcpelauncher` package.
- `jni/Application.mk` specifies `gnustl_shared`, NDK toolchain 4.9, and Android platform 14.
- `jni/Android.mk` links Minecraft-specific native libraries and builds legacy patching, script-engine, and hook-related sources. These assumptions are not a safe direct fit for current Bedrock or current NDKs.
- `.gitmodules` contains a libcorkscrew NDK submodule.
- Root CI instructions depend on the older Java 8 / Ant toolchain.

## Implemented in the revival branch

- [x] Added an isolated Gradle Android application module at `modern-launcher/`.
- [x] Added a native Android Activity with launcher and JNI diagnostics.
- [x] Added package visibility for `com.mojang.minecraftpe` and public launch-intent integration.
- [x] Added a Gradle-based GitHub Actions workflow to assemble a debug APK and upload it as an artifact.
- [x] Added a sandboxed Rhino 1.7.15 launcher-side script runtime with source and instruction limits.
- [x] Added a small logging-only ModPE compatibility shim (`print`, `clientMessage`, `log`, `ModPE.getApiStatus`).
- [x] Added Storage Access Framework file selection for pack import without broad storage permissions.
- [x] Added bounded archive copy, ZIP entry/path checks, and basic Bedrock manifest validation.
- [x] Added scoped FileProvider handoff to Android's app chooser for Minecraft pack import.

## Remaining phases and release gates

### Phase 1 — Reproducible baseline
- [ ] Inventory all Java sources, JNI/C++ sources, bundled native binaries, submodules, and third-party licenses.
- [ ] Run the old build in a controlled environment and record exact success/failure logs.
- [ ] Confirm the latest Gradle workflow is green and retain its APK artifact.

### Phase 2 — Android project modernization
- [x] Establish a separate Gradle module using JDK 17 and Android SDK 36.
- [ ] Decide whether to migrate legacy source into Gradle after mapping its external projects and generated resources.
- [ ] Replace obsolete NDK build configuration only after a source/dependency audit.
- [ ] Audit supported ABIs and every native dependency before enabling native features.
- [ ] Audit storage, permissions, lifecycle, signing, and release metadata.

### Phase 3 — Safe launcher baseline
- [x] Detect the official Minecraft package through Android package visibility.
- [x] Launch Minecraft through its public launcher intent when available.
- [x] Add a no-broad-storage-permission pack picker and bounded archive/manifest validation.
- [ ] Verify the import chooser actually imports resource and behavior packs on a physical device.
- [ ] Add automated unit/instrumentation coverage and test on supported Android versions.

### Phase 4 — MCPE compatibility investigation
- [ ] Install and test specifically against MCPE 1.21.132.1 on a device.
- [ ] Determine which legacy interfaces rely on obsolete symbols, binary layouts, or libraries.
- [ ] Expand ModPE compatibility only against documented, testable API behavior.
- [ ] Investigate official/resource-pack-supported extension points before considering any native integration.
- [ ] Do not claim asset-loader hooks, in-game JavaScript, or native process hooks until implemented and independently tested.

### Phase 5 — Release gates
- [ ] Clean CI build succeeds.
- [ ] APK installs and launches on supported Android versions.
- [ ] Minecraft launch and pack import are tested on-device with the exact target version.
- [ ] Known limitations, third-party notices, checksums, and release notes are published.

## Compatibility policy

A feature is **implemented** only when source exists. It is **verified** only after the relevant build or device test succeeds. Basic manifest validation does not guarantee a pack will load. A successful build of the modern launcher shell does not imply the legacy native engine works with modern Bedrock.
