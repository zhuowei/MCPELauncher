#pragma once
#include "blockentity.h"

class ChestBlockActor : public BlockActor {
public:
	void setItem(int, ItemInstance const&);
	ItemInstance* getItem(int) const;
};
