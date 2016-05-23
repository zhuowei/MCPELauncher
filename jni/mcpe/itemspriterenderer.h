#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[236];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
static_assert(sizeof(ItemSpriteRenderer) == 236, "item sprite renderer size");
