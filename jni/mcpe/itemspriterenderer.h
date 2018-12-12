#pragma once
class Item;
namespace mce {
	class TextureGroup;
}
class ItemSpriteRenderer {
public:
	char filler[736];
	ItemSpriteRenderer(mce::TextureGroup&, Item*, bool);
};
// search for ItemSpriteRenderer::ItemSpriteRenderer
static_assert(sizeof(ItemSpriteRenderer) == 736, "item sprite renderer size");
