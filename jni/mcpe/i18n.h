#pragma once
#include <string>
#include <vector>

class ResourcePackManager;
class SkinRepository;

class Localization {
public:
	char filler[4];
	std::string fullLanguageCode; // 4 - getFullLanguageCode from 1.13.0.6
	std::map<std::string, std::string> strings; // 8 - _getStrings from 1.13.0.6
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
