#pragma once
#include <string>
class TextureGroup;
namespace mce {
class TexturePtr {
public:
	char filler[0x8]; // 0
	std::string textureName;
	TexturePtr();
	TexturePtr(TextureGroup&, std::string const&);
	TexturePtr(TexturePtr&&);
	~TexturePtr();
	TexturePtr& operator=(TexturePtr&&);
	static TexturePtr NONE;
};
}; // namespace mce
