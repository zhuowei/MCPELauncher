#pragma once
class Level;
class Timer;
namespace mce {
class TextureGroup;
class Texture;
} // namespace mce
class Mob;
class Options;
class AbstractScreen;
class ScreenChooser;
class GuiData;
class MinecraftCommands;
class LevelRenderer;
class Vec3;
class ResourcePackManager;
class ClientInstance;
class EntityRenderDispatcher;
class ItemRenderer;
class UIProfanityContext;
class ServerNetworkHandler;
class SceneStack;
class SceneFactory;

enum GameType {
};

class Minecraft {
public:
	Level* getLevel() const;
	Timer* getTimer();
	MinecraftCommands* getCommands();
	ResourcePackManager* getResourceLoader();
	void setGameModeReal(GameType);
};

// actually an App::App subclass. Technically equivalent to MinecraftClient <= 1.0, but most functions moved to ClientInstance
class IMinecraftGame {
};

class MinecraftGame : public IMinecraftGame {
public:
	ClientInstance* getPrimaryClientInstance();
	Level* getLocalServerLevel() const;
	void updateFoliageColors();
	ServerNetworkHandler* getServerNetworkHandler();
	Minecraft* getServerInstance();
};

class IClientInstance {
};

class ClientInstance : public IClientInstance {
public:
	MinecraftGame* getMinecraftGame() const;
	Minecraft* getServerData();
	Player* getLocalPlayer();
	mce::TextureGroup& getTextures() const;
	void setCameraEntity(Entity*);
	void setCameraTargetEntity(Entity*);
	Options* getOptions();
	AbstractScreen* getScreen();
	ScreenChooser& getScreenChooser() const;
	GuiData* getGuiData();
	void onResourcesLoaded();
	LevelRenderer* getLevelRenderer() const;
	void play(std::string const&, Vec3 const&, float, float);
	void _startLeaveGame();
	Level* getLevel();
	EntityRenderDispatcher& getEntityRenderDispatcher();
	ItemRenderer* getItemRenderer();
	UIProfanityContext const& getUIProfanityContext() const;
	mce::Texture const& getUITexture();
	SceneStack& getClientSceneStack() const;
	SceneFactory& getSceneFactory() const;
	Level* getLocalServerLevel();
};
#define MinecraftClient ClientInstance
