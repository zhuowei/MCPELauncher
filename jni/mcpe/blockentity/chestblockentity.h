#pragma once
#include "blockentity.h"

class ChestBlockActor : public BlockActor {
public:
	void setItem(int, ItemStack const&);
	ItemStack* getItem(int) const;
};
