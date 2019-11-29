#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[1480];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
// search for ItemSpriteRenderer::ItemSpriteRenderer
static_assert(sizeof(ItemSpriteRenderer) == 1480, "item sprite renderer size");
