# Nex Launcher Bedrock compatibility test pack

This is a small **official Bedrock behavior pack**, not an injected ModPE runtime.

## Test on Minecraft 1.21.132.1

1. Download the `nex-bedrock-test-pack` artifact from the GitHub Actions workflow.
2. Open the generated `.mcpack` on your Android device and choose Minecraft.
3. In Minecraft, create a test world and activate **Nex Launcher Compatibility Test** under Behavior Packs.
4. Start the world. Look for the green `[Nex]` message.
5. Join/rejoin to test the player-spawn message. Hold and use a stick to test the item-use callback.

The pack requests the stable `@minecraft/server` module version `2.7.0` and sets minimum engine version to 1.21.130. The exact runtime behavior still needs verification on the target 1.21.132.1 installation. If Minecraft reports a missing script module version, change the dependency to a stable version supported by that installation and rebuild the pack.

## What this verifies

- The official Bedrock Add-On import and activation flow.
- Whether the target game loads a script module.
- Basic world-load, player-spawn, and item-use event callbacks.

It does not verify legacy BlockLauncher native hooks, private memory offsets, or old ModPE APIs.
