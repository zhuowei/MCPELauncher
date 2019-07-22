#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[720];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
// search for ItemSpriteRenderer::ItemSpriteRenderer
static_assert(sizeof(ItemSpriteRenderer) == 720, "item sprite renderer size");
