#pragma once
#include <string>
class MinecraftGame;
class ClientInstance;
class SceneStack;
class SceneFactory;
class IClientInstance;
class IMinecraftGame;

class MinecraftScreenModel {
public:
	virtual ~MinecraftScreenModel();
	void updateTextBoxText(std::string const&);
};

class ClientInstanceScreenModel : public MinecraftScreenModel{
public:
	char filler[116-4];

	ClientInstanceScreenModel(IMinecraftGame&, IClientInstance&, SceneStack&, SceneFactory&);
	virtual ~ClientInstanceScreenModel();

	void executeCommand(std::string const&);
	void sendChatMessage(std::string const&);
};
// search for the constructor
// or std::__shared_ptr<ClientInstanceScreenModel
static_assert(sizeof(ClientInstanceScreenModel) == 116, "ClientInstanceScreenModel size");
