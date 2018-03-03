#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[292];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
static_assert(sizeof(ItemSpriteRenderer) == 292, "item sprite renderer size");
