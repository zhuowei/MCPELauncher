#pragma once
extern "C" {
extern Item** bl_Item_mItems;
extern int bl_item_id_count;

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
	return BlockLegacy::mBlocks[blockIdFromItemId(itemId)];
}

static inline Item* getItemForId(int itemId) {
	if (itemId < -512 || itemId >= bl_item_id_count) return nullptr;
	if (itemId < 0) {
		return Item::mExtraItems[-itemId];
	}
	return bl_Item_mItems[itemId];
}

static inline void setItemForId(int itemId, Item* item) {
	if (itemId < -512 || itemId >= bl_item_id_count) return;
	if (itemId < 0) {
		Item::mExtraItems[-itemId] = item;
	}
	bl_Item_mItems[itemId] = item;
}
} // extern "C"
