#pragma once
#include <string>
class ResourceLocation {
public:
	std::string str1; // 0
	std::string str2; // 4
	ResourceLocation(std::string const&);
	ResourceLocation();
	~ResourceLocation();
};

