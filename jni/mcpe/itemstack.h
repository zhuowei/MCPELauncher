#pragma once
class ItemStack {
public:
	// this is actually the same layout as ItemInstance
	char filler1[14-0]; // 0
	unsigned char count; // 14
	char filler2[88-15]; // 15
	ItemStack();
	ItemStack(ItemInstance const&);
	ItemStack(int id, int count, int data) : ItemStack() {
		init(id, count, data);
		_setItem(id);
	}
	ItemStack& operator=(ItemStack const&);
	~ItemStack();
	void init(int, int, int);
	ItemEnchants getEnchantsFromUserData() const;
	int getId() const;
	void _setItem(int);
	int getDamageValue() const;
	bool hasCustomHoverName() const;
	std::string getCustomName() const;
	void setCustomName(std::string const&);
};
static_assert(sizeof(ItemStack) == 88, "ItemStack size");
static_assert(offsetof(ItemStack, count) == 14, "ItemStack count");
