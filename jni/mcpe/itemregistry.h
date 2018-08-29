#pragma once
class Item;
class ItemRegistry {
public:
	static Item* getItem(short);
	static void _setItem(short, Item const*);
	static ItemRegistry* mItemRegistry;
};
