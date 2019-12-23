#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[872];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
// search for ItemSpriteRenderer::ItemSpriteRenderer
static_assert(sizeof(ItemSpriteRenderer) == 872, "item sprite renderer size");
