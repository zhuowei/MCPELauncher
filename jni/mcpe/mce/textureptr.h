#pragma once
#include <string>
class TextureGroup;
namespace mce {
class Texture {
public:
	bool isLoaded() const;
};
class TexturePtr {
public:
	char filler[0x4]; // 0
	Texture* texture; // 4
	std::string textureName;
	TexturePtr();
	TexturePtr(TextureGroup&, std::string const&);
	TexturePtr(TexturePtr&&);
	~TexturePtr();
	TexturePtr& operator=(TexturePtr&&);
	void onGroupReloaded();
	static TexturePtr NONE;
};
}; // namespace mce
class TextureGroup {
public:
	mce::TexturePtr getTexture(std::string const&);
	void loadTexture(std::string const&, bool, bool, bool, bool);
};
