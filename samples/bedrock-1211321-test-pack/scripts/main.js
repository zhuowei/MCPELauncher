import { world, system } from "@minecraft/server";

world.afterEvents.worldLoad.subscribe(() => {
  world.sendMessage("§a[Nex] Compatibility test pack loaded.");
});

world.afterEvents.playerSpawn.subscribe((event) => {
  if (!event.initialSpawn) return;
  event.player.sendMessage("§b[Nex] Script API smoke test passed. Welcome!");
});

world.afterEvents.itemUse.subscribe((event) => {
  if (event.itemStack.typeId === "minecraft:stick") {
    event.source.sendMessage("§e[Nex] Item-use event is working.");
  }
});

system.run(() => {
  world.sendMessage("§7[Nex] Test pack initialized. Hold and use a stick to test events.");
});
