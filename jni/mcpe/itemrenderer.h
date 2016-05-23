#pragma once
class ItemInstance;
namespace mce {
	class TexturePtr;
};
class ItemRenderer {
public:
	static mce::TexturePtr const& getGraphics(ItemInstance const&);
	static mce::TexturePtr const& getGraphics(Item const&);
};
