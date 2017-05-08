#pragma once
class Entity;
class ItemInstance;
class ItemInHandRenderer {
public:
	void* renderItem(Entity&, ItemInstance const&, bool, float, bool, bool);
};
