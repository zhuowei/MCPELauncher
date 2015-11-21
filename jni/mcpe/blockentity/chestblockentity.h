#pragma once
#include "blockentity.h"

class ChestBlockEntity : public BlockEntity {
public:
	void setItem(int, ItemInstance*);
	ItemInstance* getItem(int) const;
};
