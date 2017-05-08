#pragma once
#include "blockentity.h"

class ChestBlockEntity : public BlockEntity {
public:
	void setItem(int, ItemInstance const&);
	ItemInstance* getItem(int) const;
};
