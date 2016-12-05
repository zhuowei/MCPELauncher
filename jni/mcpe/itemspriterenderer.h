#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[240];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
static_assert(sizeof(ItemSpriteRenderer) == 240, "item sprite renderer size");
