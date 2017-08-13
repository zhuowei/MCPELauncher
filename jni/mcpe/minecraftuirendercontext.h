#pragma once
#include "mce/textureptr.h"
class ResourceLocation;
class MinecraftUIRenderContext {
public:
	mce::TexturePtr getTexture(ResourceLocation const&, bool) const;
};
