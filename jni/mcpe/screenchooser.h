#pragma once

class AbstractScene;
class ScreenChooser {
public:
	void popScreen(AbstractScene&, int);
};

class SceneStack {
public:
	void _popScreens(int&, bool);
	void pushScreen(std::shared_ptr<AbstractScene>, bool);
	std::string getScreenName() const;
	void* update();
};
