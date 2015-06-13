#pragma once
class Packet;
namespace RakNet {
	class RakNetGUID;
};
class PacketSender {
public:
	virtual ~PacketSender();
	virtual void send(Packet const&);
	virtual void send(RakNet::RakNetGUID const&, Packet const&);
	virtual void sendBroadcast(RakNet::RakNetGUID const&, Packet const&);
};

