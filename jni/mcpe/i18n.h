#pragma once
#include <string>
#include <vector>

class Localization {
public:
	void* filler;
	std::map<std::string, std::string> map;
};

class I18n {
public:
	static std::string get(std::string const&, std::vector<std::string> const&);
	static Localization* getCurrentLanguage();

	static std::vector<Localization*> languages;
	static Localization* currentLanguage;
};
