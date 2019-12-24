#pragma once
#include "blockidtoitemid.h"
class ItemStack : public ItemStackBase {
public:
	// this is actually the same layout as ItemInstance
	ItemStack();
	ItemStack(ItemInstance const&);
	ItemStack(int id, int count, int data) : ItemStack() {
		init(id, count, data);
		_setItem(id);
		bool isBlock = itemIdIsBlock(id);
		// for a block, init it with a BlockAndData
		if (isBlock) {
			BlockLegacy* block = getBlockForItemId(id);
			if (!block) return;
			BlockAndData* blockAndData = block->getStateFromLegacyData(data);
			if (!blockAndData) return; // should never happen, but...
			setBlock(blockAndData);
		}
	}
	virtual ~ItemStack();
};
static_assert(sizeof(ItemStack) == 84, "ItemStack size");
