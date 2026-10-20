# Nex-Launcher — BlockLauncher Revival

Nex-Launcher is a revival effort based on the legacy BlockLauncher / MCPELauncher Android source. The goal is to modernize Android packaging and investigate compatibility with current Minecraft Bedrock Edition without claiming unverified features.

> **Status:** the new Gradle launcher shell is implemented; CI/device verification is pending. Minecraft Bedrock **1.21.132.1 compatibility has not been verified**. The shell launches the official Minecraft app only; legacy ModPE scripts, native patches, and injection are not implemented in the new module.

## Two project tracks

- `modern-launcher/` — isolated Gradle Android app module using JDK 17 and Android SDK 36. It provides a native launcher screen, checks for the official Minecraft package, and opens it using Android's launch intent. See [modern launcher README](modern-launcher/README.md).
- Root legacy project — original Ant/JNI BlockLauncher implementation. It remains intact while its old native build and binary assumptions are audited.

## Build the modern launcher

With JDK 17, Android SDK Platform 36, and a compatible Gradle installation:

```sh
gradle --no-daemon :modern-launcher:assembleDebug
```

The CI workflow at [modern-launcher.yml](.github/workflows/modern-launcher.yml) builds the debug APK and uploads it as an artifact when the workflow succeeds.

## Legacy baseline findings

The inherited project uses Ant, targets Android API 23 in `project.properties`, declares minimum SDK 9 in `AndroidManifest.xml`, and references legacy Java/native dependencies. Its JNI configuration uses deprecated `gnustl_shared` and NDK toolchain 4.9 settings and links against Minecraft-specific native libraries. These are significant blockers for a direct modern NDK migration; a modern Gradle shell is not proof that the legacy patching engine builds or works.

See [docs/REVIVAL_ROADMAP.md](docs/REVIVAL_ROADMAP.md) for source audit, migration gates, and device testing requirements.

## Licensing and attribution

This project derives from BlockLauncher / MCPELauncher. Most code is licensed under Apache-2.0, while some components have separate licenses. Review the root license, third-party notices, submodules, and bundled native binaries before redistribution and preserve all applicable notices.

## Scope and disclaimer

Nex-Launcher is an independent community project and is not affiliated with or endorsed by Mojang or Microsoft. Minecraft is a trademark of its respective owner. Test using an official, lawfully installed copy of Minecraft.
