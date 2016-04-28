#pragma once
class Level;
class Timer;
namespace mce {
class TextureGroup;
} // namespace mce
class Mob;
class Options;
class AbstractScreen;
class ScreenChooser;
class GuiData;
class Minecraft {
public:
	Level* getLevel();
	Timer* getTimer();
};

class MinecraftClient {
public:
	Minecraft* getServer();
	Player* getLocalPlayer();
	mce::TextureGroup& getTextures() const;
	void setCameraTargetEntity(Entity*);
	Options* getOptions();
	AbstractScreen* getScreen();
	ScreenChooser& getScreenChooser() const;
	GuiData* getGuiData();
};
