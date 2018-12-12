#pragma once
#include <string>
#include "../resourcelocation.h"
enum TextureLocation {
	TEXTURE_LOCATION_INTERNAL, // 0
};
namespace mce {
class TextureGroup;
class TextureGroupBase;
class TextureDescription;
class Texture {
public:
	bool isLoaded() const;
	TextureDescription* getDescription() const;
};
class TexturePtr {
public:
	char filler[0x4]; // 0
	Texture* texture; // 4
	void* something8; // 8
	std::string textureName; // 12
	char filler2[20-16]; // 16
	TexturePtr();
	TexturePtr(TextureGroupBase&, ResourceLocation const&, bool a=false);
	TexturePtr(TexturePtr&&);
	~TexturePtr();
	TexturePtr& operator=(TexturePtr&&);
	void onGroupReloaded();
	TexturePtr clone() const;
	Texture* get() const;
	static TexturePtr NONE;
};

static_assert(sizeof(TexturePtr) == 20, "textureptr size");
class TextureGroupBase {
public:
};
class TextureGroup : public TextureGroupBase {
public:
	TexturePtr getTexture(ResourceLocation, bool);
	void loadTexture(ResourceLocation const&, bool);
	static bool mCanLoadTextures;
};
}; // namespace mce
