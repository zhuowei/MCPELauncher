#pragma once
#include "mce/textureptr.h"
class ItemInstance;
class BaseActorRenderContext;
class ActorRenderData;
class ItemGraphics {
public:
	//char filler[16 /*sizeof(mce::TexturePtr)*/]; // 0
	mce::TexturePtr texturePtr;
	ItemGraphics(mce::TexturePtr&& _texturePtr) {
		texturePtr = std::move(_texturePtr);
	}
	ItemGraphics() {
	}
};
static_assert(sizeof(ItemGraphics) == 24, "itemGraphics size");
class ItemRenderer {
public:
	char filler[680];
	std::unordered_map<short, ItemGraphics> itemGraphics; // 680
	void _loadItemGraphics();
	void* getAtlasPos(ItemInstance const&);
	void render(BaseActorRenderContext&, ActorRenderData&);

	static mce::TexturePtr const& getGraphics(ItemInstance const&);
	static mce::TexturePtr const& getGraphics(Item const&);
};
static_assert(offsetof(ItemRenderer, itemGraphics) == 680, "itemrenderer offset wrong");
