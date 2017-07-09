#pragma once
class AbstractScreen;
class ScreenChooser {
public:
	void popScreen(AbstractScreen&, int);
};

class ScreenStack {
public:
	void _popScreen(bool);
	void pushScreen(std::shared_ptr<AbstractScreen>, bool);
	std::string getScreenName() const;
};
