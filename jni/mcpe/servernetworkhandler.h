#pragma once
#include <memory>
class Player;
namespace RakNet {
	class RakNetGUID;
}
class LoginPacket;
class ServerNetworkHandler {
public:
	std::unique_ptr<Player> createNewPlayer(RakNet::RakNetGUID const& guid, LoginPacket* packet);
};
