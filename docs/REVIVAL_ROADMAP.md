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
- [x] Added a native Android Activity with a basic launcher UI.
- [x] Added package visibility for `com.mojang.minecraftpe` and launch-intent integration.
- [x] Added a Gradle-based GitHub Actions workflow to assemble a debug APK and upload it as an artifact.
- [x] Kept the inherited Ant/JNI implementation intact while migration is investigated.

## Remaining phases and release gates

### Phase 1 — Reproducible baseline
- [ ] Inventory all Java sources, JNI/C++ sources, bundled native binaries, submodules, and third-party licenses.
- [ ] Run the old build in a controlled environment and record exact success/failure logs.
- [ ] Review CI workflow result for the new Gradle shell.

### Phase 2 — Android project modernization
- [x] Establish a separate Gradle module using JDK 17 and Android SDK 36.
- [ ] Decide whether to migrate legacy source into Gradle after mapping its external projects and generated resources.
- [ ] Replace obsolete NDK build configuration only after a source/dependency audit.
- [ ] Audit supported ABIs and every native dependency before enabling native features.
- [ ] Audit storage, permissions, lifecycle, signing, and release metadata.

### Phase 3 — Safe launcher baseline
- [x] Detect the official Minecraft package through Android package visibility.
- [x] Launch Minecraft through its public launcher intent when available.
- [ ] Add automated UI/instrumentation coverage and test on physical Android devices.
- [ ] Add documented resource-pack import/help without broad storage access.

### Phase 4 — MCPE compatibility investigation
- [ ] Install and test specifically against MCPE 1.21.132.1 on a device.
- [ ] Determine which legacy interfaces rely on obsolete symbols, binary layouts, or libraries.
- [ ] Treat resource packs, ModPE/Rhino scripts, and native patching as separate features with separate evidence.
- [ ] Do not enable injection/patching by default; any implementation must be explicit, documented, and independently tested.

### Phase 5 — Release gates
- [ ] Clean CI build succeeds.
- [ ] APK installs and launches on supported Android versions.
- [ ] Minecraft launch tested on-device with the exact target version.
- [ ] Known limitations, third-party notices, checksums, and release notes are published.

## Compatibility policy

A feature is **implemented** only when source exists. It is **verified** only after the relevant build or device test succeeds. A successful build of the modern launcher shell does not imply the legacy native engine works with modern Bedrock.
