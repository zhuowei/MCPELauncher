#pragma once
#include <string>
#include <vector>

class Localization {
public:
	char filler[12]; // 0
	std::map<std::string, std::string> stringsMap; // 12
	std::map<std::string, std::string>& _getStrings() const;
	std::string getFullLanguageCode() const;
};

class I18n {
public:
	static std::string get(std::string const&, std::vector<std::string> const&);
	static Localization* getCurrentLanguage();

	static std::vector<Localization*> languages;
	static Localization* mCurrentLanguage;
};
