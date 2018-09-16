#pragma once
#include "mce/textureptr.h"
class ItemInstance;
class BaseActorRenderContext;
class ActorRenderData;
class ItemRenderer {
public:
	//char filler[680];
	//std::unordered_map<short, ItemGraphics> itemGraphics; // 680
	void _loadItemGraphics();
	void* getAtlasPos(ItemInstance const&);
	void render(BaseActorRenderContext&, ActorRenderData&);

	static mce::TexturePtr const& getGraphics(ItemInstance const&);
	static mce::TexturePtr const& getGraphics(Item const&);
};

