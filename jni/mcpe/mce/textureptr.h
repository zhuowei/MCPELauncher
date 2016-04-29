#pragma once
#include <string>
enum TextureLocation {
	TEXTURE_LOCATION_INTERNAL, // 0
};
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
	TextureLocation myLocation; // 12
	TexturePtr();
	TexturePtr(TextureGroup&, std::string const&, TextureLocation);
	TexturePtr(TexturePtr&&);
	~TexturePtr();
	TexturePtr& operator=(TexturePtr&&);
	void onGroupReloaded();
	static TexturePtr NONE;
};
class TextureGroup {
public:
	TexturePtr getTexture(std::string const&);
	void loadTexture(std::string const&, TextureLocation, bool);
};
}; // namespace mce
