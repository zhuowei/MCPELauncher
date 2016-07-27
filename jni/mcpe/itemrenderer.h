#pragma once
class ItemInstance;
class ItemGraphics {
	char filler[16 /*sizeof(mce::TexturePtr)*/]; // 0
};
class ItemRenderer {
public:
	static mce::TexturePtr const& getGraphics(ItemInstance const&);
	static mce::TexturePtr const& getGraphics(Item const&);
	static std::vector<ItemGraphics> mItemGraphics;
};
