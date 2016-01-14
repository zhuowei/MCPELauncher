#pragma once
#include <vector>
#include <string>
class ItemInstance;

class Enchant {
public:
	void* vtable; // 0
	int id; // 4
	char filler[24-8]; // 8
	std::string description; // 24
	static std::vector<Enchant*> mEnchants;
};

class EnchantUtils {
public:
	static void applyEnchant(ItemInstance&, int, int);
};

class EnchantmentInstance {
public:
	int type;
	int level;
};

class ItemEnchants {
public:
	char filler[40]; // ItemEnchants::ItemEnchants
	int count() const;
	std::vector<EnchantmentInstance> getAllEnchants() const;
};
