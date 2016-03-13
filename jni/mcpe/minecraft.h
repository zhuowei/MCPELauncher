#pragma once
class Level;
class Timer;
class TextureGroup;
class Mob;
class Options;
class AbstractScreen;
class ScreenChooser;
class Minecraft {
public:
	Level* getLevel();
	Timer* getTimer();
};

class MinecraftClient {
public:
	Minecraft* getServer();
	Player* getLocalPlayer();
	TextureGroup& getTextures() const;
	void setCameraTargetEntity(Entity*);
	Options* getOptions();
	AbstractScreen* getScreen();
	ScreenChooser& getScreenChooser() const;
};
