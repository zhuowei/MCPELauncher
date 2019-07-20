#pragma once
#include <vector>
#include <string>
class ItemInstance;
class ItemStack;

class Enchant {
public:
	void* vtable; // 0
	int id; // 4
	char filler[24-8]; // 8
	std::string description; // 24
	static std::vector<Enchant*> mEnchants;
	enum Type {
	};
};

class EnchantUtils {
public:
	static bool applyEnchant(ItemInstance&, Enchant::Type, int, bool);
	static bool applyEnchant(ItemStack&, Enchant::Type, int, bool);
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
