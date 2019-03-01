#pragma once
class ItemStack {
public:
	char filler[88];
	// todo: what's the layout?
	unsigned char count;
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
