#pragma once
#include "itemregistry.h"
#include "blockpalette.h"
extern "C" {

extern Level* bl_level;

static inline int itemIdFromBlockId(int blockId) {
	if (blockId > 255) return 255-blockId;
	return blockId;
}

static inline int blockIdFromItemId(int itemId) {
	if (itemId >= 0) return itemId;
	return 255-itemId;
}

static inline bool itemIdIsBlock(int itemId) {
	return itemId >= -256 && itemId <= 255;
}

static inline BlockLegacy* getBlockForItemId(int itemId) {
	if (!itemIdIsBlock(itemId)) return nullptr;
	if (!bl_level) return nullptr;
	Block* block = bl_level->getGlobalBlockPalette()->getBlockFromLegacyData(NewBlockID {(short)blockIdFromItemId(itemId)}, 0);
	if (!block) return nullptr;
	return block->blockBase.get();
}

static inline Item* getItemForId(int itemId) {
	return ItemRegistry::getItem((short)itemId).get();
}

static inline void setItemForId(int itemId, Item* item) {
	abort();
	//ItemRegistry::registerItem(std::unique_ptr<Item>(item));
}
} // extern "C"
