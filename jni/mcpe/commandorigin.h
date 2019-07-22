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

enum CommandPermissionLevel {
	CommandPermissionLevelDefault = 0,
	CommandPermissionLevelHigher = 1, // does this seem right?
};

class ServerCommandOrigin : public CommandOrigin {
public:
	ServerCommandOrigin(std::string const&, ServerLevel&, CommandPermissionLevel);
	virtual ~ServerCommandOrigin();
	char filler[40-4]; // 4 from ServerCommandOrigin::clone()
};
static_assert(sizeof(ServerCommandOrigin) == 40, "dedicated server command size");

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
