# Nex-Launcher — BlockLauncher Revival

Nex-Launcher is a revival effort based on the legacy BlockLauncher / MCPELauncher Android source. The goal is to modernize the Android project and investigate compatibility with current Minecraft Bedrock Edition, without claiming unverified features.

> **Status:** modernization in progress. Minecraft Bedrock **1.21.132.1 compatibility has not been verified**. A successful app build does not prove that legacy ModPE scripts, native patches, or injection work with a modern game engine.

## Current legacy baseline

The inherited project uses the Android Ant build system, targets Android API 23 in `project.properties`, declares minimum SDK 9 in `AndroidManifest.xml`, and relies on legacy Java/native dependencies. The original build instructions require an older Java 8 / Ant toolchain. These are audit findings, not a claim that the current source builds successfully.

## Revival plan

See [docs/REVIVAL_ROADMAP.md](docs/REVIVAL_ROADMAP.md) for the phased modernization plan, test gates, and compatibility policy.

Priorities:
1. Inventory source, submodules, JNI code, native binaries, and licenses.
2. Establish a reproducible baseline build before migrating build systems.
3. Modernize Android packaging and lifecycle behavior in isolated changes.
4. Separate ordinary launcher/resource-pack workflows from legacy native patching.
5. Test on-device against the exact Minecraft version before declaring support.

## Building

The inherited build is Ant-based and may not work with current Android SDK installations. The old upstream instructions refer to Java 8 and Ant; see `tools/ci/ci_build.sh` for the repository's legacy build setup. Do not assume an Android Studio/Gradle build is available until a migration is implemented and tested.

## Licensing and attribution

This project derives from the BlockLauncher / MCPELauncher codebase. Most code is licensed under Apache-2.0, but some components use separate licenses. Review the root `LICENSE`, third-party notices, submodules, and bundled native binaries before redistribution. Preserve all applicable notices.

## Scope and disclaimer

Nex-Launcher is an independent community project and is not affiliated with or endorsed by Mojang or Microsoft. Minecraft is a trademark of its respective owner. Use an official, lawfully installed copy of Minecraft for testing.
