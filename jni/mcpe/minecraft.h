#pragma once
class Level;
class Minecraft {
public:
	Level* getLevel();
};

class MinecraftClient : public Minecraft {
public:
	Player* getLocalPlayer();
};
