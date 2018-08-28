#pragma once
class Item;
class ItemRegistry {
public:
	Item* getItem(short);
	void _setItem(short, Item const*);
	static ItemRegistry* mItemRegistry;
};
