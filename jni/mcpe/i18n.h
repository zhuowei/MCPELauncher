#pragma once
#include <string>
#include <vector>

class Localization {
public:
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
