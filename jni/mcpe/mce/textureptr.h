#pragma once
#include <string>
enum TextureLocation {
	TEXTURE_LOCATION_INTERNAL, // 0
};
class ResourceLocation;
namespace mce {
class TextureGroup;
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
	std::string textureName; // 8
	std::string something; // 12
	char filler2[4]; // 16
	TexturePtr();
	TexturePtr(TextureGroup&, ResourceLocation const&);
	TexturePtr(TexturePtr&&);
	~TexturePtr();
	TexturePtr& operator=(TexturePtr&&);
	void onGroupReloaded();
	TexturePtr clone() const;
	static TexturePtr NONE;
};

static_assert(sizeof(TexturePtr) == 20, "textureptr size");
class TextureGroup {
public:
	TexturePtr getTexture(std::string const&);
	void loadTexture(ResourceLocation const&, bool);
	static bool mCanLoadTextures;
};
}; // namespace mce
