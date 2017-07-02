#pragma once
#include <string>
class GuiData {
public:
	void displayClientMessage(std::string const&);
	void showTipMessage(std::string const&);
	mce::TexturePtr const& getGuiTex();
};
