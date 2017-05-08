#pragma once
#include "blockentity.h"

class FurnaceBlockEntity : public BlockEntity {
public:
	void setItem(int, ItemInstance const&);
	ItemInstance* getItem(int) const;
};
