#pragma once
class AbstractScene;
class ScreenChooser {
public:
	void popScreen(AbstractScene&, int);
};

class SceneStack {
public:
	void _popScreen(bool);
	void pushScreen(std::shared_ptr<AbstractScene>, bool);
	std::string getScreenName() const;
};
