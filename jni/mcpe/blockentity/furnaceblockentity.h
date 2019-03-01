#pragma once
#include "blockentity.h"

class FurnaceBlockActor : public BlockActor {
public:
	void setItem(int, ItemStack const&);
	ItemStack* getItem(int) const;
};
