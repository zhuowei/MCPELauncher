# Nex-Launcher Revival Roadmap

## Goal

Revive this legacy BlockLauncher-derived Android project as a maintainable launcher for current Android devices, while investigating what is realistically compatible with Minecraft Bedrock/MCPE 1.21.132.1.

**Compatibility is not yet verified.** A successful Android build alone does not prove that the app can launch, modify, or inject into the target Minecraft version.

## Baseline findings

- The current project is an old Ant-based Android project.
- `project.properties` targets Android API 23 and references sibling projects (`aFileChooser`, `libsuperuser`) plus legacy components.
- `AndroidManifest.xml` declares minimum SDK 9 and uses the legacy package `net.zhuoweizhang.mcpelauncher`.
- The existing build instructions rely on an old Java 8 / Ant toolchain.
- The project includes native/JNI code and legacy patching dependencies. Those interfaces must be audited before claiming support for modern Bedrock.

## Phased work

### Phase 1 — Reproducible baseline
- [ ] Inventory Java sources, JNI/C++ sources, native libraries, submodules, licenses, and bundled binaries.
- [ ] Record a reproducible legacy build result (success or exact failure).
- [ ] Add CI that reports toolchain/build status without hiding legacy failures.
- [ ] Preserve the original implementation on the default branch; do modernization on a feature branch.

### Phase 2 — Android project modernization
- [ ] Migrate from Ant to Gradle/Android Gradle Plugin in a dedicated branch.
- [ ] Select a supported JDK and Android SDK/NDK based on dependency compatibility.
- [ ] Raise min/target SDK only after auditing storage, permissions, package visibility, and lifecycle behavior.
- [ ] Audit 32-bit/64-bit ABIs and every bundled native dependency.
- [ ] Update app identity, signing, versioning, and release metadata for Nex-Launcher.

### Phase 3 — Safe launcher baseline
- [ ] Launch the installed official Minecraft app through supported Android intents where possible.
- [ ] Detect whether Minecraft is installed and display actionable diagnostics when it is not.
- [ ] Keep resource-pack import/help separate from native patching.
- [ ] Avoid requesting root or broad storage access unless a documented feature truly requires it.

### Phase 4 — MCPE compatibility investigation
- [ ] Test against the user's target Minecraft version: **1.21.132.1**.
- [ ] Identify whether legacy hooks depend on obsolete symbols, binary layouts, or native libraries.
- [ ] Treat ModPE/Rhino scripts, native patches, and resource packs as separate compatibility surfaces.
- [ ] Do not enable injection or patching by default; require an explicit, documented compatibility implementation.
- [ ] Verify launch, crash handling, resource import, and supported features on a real device.

### Phase 5 — Release gates
- [ ] Clean build from a fresh checkout.
- [ ] Install/launch test on supported Android versions.
- [ ] Test on-device with the exact Minecraft version.
- [ ] Publish a changelog, known limitations, licenses, and checksums for artifacts.

## Compatibility policy

A feature is **implemented** only when its source and tests exist. It is **verified** only after the relevant build or device test succeeds. Do not describe legacy ModPE scripts or native patching as compatible with MCPE 1.21.132.1 until proven by testing.

## Upstream and licensing

This project is derived from the BlockLauncher/MCPELauncher codebase. Preserve upstream attribution and each component's applicable license when changing or redistributing code and binaries.
