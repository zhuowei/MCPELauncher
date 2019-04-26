#pragma once
// updated 1.2.5b15
class Minecraft;
class Player;
class ServerLevel;
class CommandOrigin {
public:
	CommandOrigin();
	virtual ~CommandOrigin();
};
class ServerCommandOrigin : public CommandOrigin {
public:
	ServerCommandOrigin(std::string const&, ServerLevel&);
	virtual ~ServerCommandOrigin();
	char filler[32-4]; // 4 from ServerCommandOrigin::clone()
};
static_assert(sizeof(ServerCommandOrigin) == 32, "dedicated server command size");

class DevConsoleCommandOrigin : public CommandOrigin {
public:
	DevConsoleCommandOrigin(Player&);
	virtual ~DevConsoleCommandOrigin();
	char filler[200-4]; // 4 from DevConsoleCommandOrigin::clone()
};
static_assert(sizeof(DevConsoleCommandOrigin) == 200, "dev console command size");

class CommandVersion {
public:
	static int CurrentVersion;
};
