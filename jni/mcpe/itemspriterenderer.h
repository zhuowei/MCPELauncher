#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[708];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
// search for ItemSpriteRenderer::ItemSpriteRenderer
static_assert(sizeof(ItemSpriteRenderer) == 708, "item sprite renderer size");
