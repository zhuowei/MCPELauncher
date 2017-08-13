#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[252];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
static_assert(sizeof(ItemSpriteRenderer) == 252, "item sprite renderer size");
