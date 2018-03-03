#pragma once
#include <string>
class MinecraftGame;
class ClientInstance;
class SceneStack;
class SceneFactory;

class MinecraftScreenModel {
public:
	virtual ~MinecraftScreenModel();
	void updateTextBoxText(std::string const&);
};

class ClientInstanceScreenModel : public MinecraftScreenModel{
public:
	char filler[60-4];

	ClientInstanceScreenModel(MinecraftGame&, ClientInstance&, SceneStack&, SceneFactory&);
	virtual ~ClientInstanceScreenModel();

	void executeCommand(std::string const&);
	void sendChatMessage(std::string const&);
};
// search for the constructor
static_assert(sizeof(ClientInstanceScreenModel) == 60, "ClientInstanceScreenModel size");
