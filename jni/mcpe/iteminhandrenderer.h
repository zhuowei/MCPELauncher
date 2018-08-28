#pragma once
class Actor;
class ItemInstance;
class ItemInHandRenderer {
public:
	void* renderItem(Actor&, ItemInstance const&, bool, float, bool, bool);
};
