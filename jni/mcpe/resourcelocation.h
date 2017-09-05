#pragma once
#include <string>
class ResourceLocation {
public:
	std::string str1; // 0
	int position; // 4
	/*ResourceLocation(std::string const& _str1) : ResourceLocation() {
		*((void**)(&str1)) = *((void**)&_str1);
		position = 0;
	}*/
	ResourceLocation(std::string const& _str1, int _position=0) : str1(_str1), position(_position) {
	}
	ResourceLocation();
	~ResourceLocation();
};
// GuiData::getAtlasTex
static_assert(sizeof(ResourceLocation) == 8, "ResourceLocation size");
