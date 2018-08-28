#pragma once
#include "blockentity.h"

class FurnaceBlockActor : public BlockActor {
public:
	void setItem(int, ItemInstance const&);
	ItemInstance* getItem(int) const;
};
