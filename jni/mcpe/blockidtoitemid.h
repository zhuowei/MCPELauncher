#pragma once
#include "itemregistry.h"
extern "C" {

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
	//if (!bl_level) return nullptr;
	return nullptr; // fix this pls
}

static inline Item* getItemForId(int itemId) {
	return ItemRegistry::getItem((short)itemId);
}

static inline void setItemForId(int itemId, Item* item) {
	return ItemRegistry::_setItem((short)itemId, item);
}
} // extern "C"
