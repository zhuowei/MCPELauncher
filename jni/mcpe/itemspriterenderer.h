#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[740];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
static_assert(sizeof(ItemSpriteRenderer) == 740, "item sprite renderer size");
