#pragma once
#include "sharedptr.h"
class Item;
class ItemRegistry {
public:
	static SharedPtr<Item> getItem(short);
	static SharedPtr<Item> getItem(std::string const&);
	static void _setItem(short, Item const*);
	static void registerItem(SharedPtr<Item>&&);
	static ItemRegistry* mItemRegistry;
	static std::unordered_map<std::string, std::pair<std::string const, Item const*>> mItemLookupMap;
};
