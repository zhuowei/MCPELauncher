#pragma once
// updated 1.2.0b2
class Minecraft;
class Player;
class CommandOrigin {
public:
	CommandOrigin();
	virtual ~CommandOrigin();
};
class DedicatedServerCommandOrigin : public CommandOrigin {
public:
	DedicatedServerCommandOrigin(std::string const&, Minecraft&);
	virtual ~DedicatedServerCommandOrigin();
	char filler[12-4]; // 4 from DedicatedServerCommandOrigin::clone()
};
static_assert(sizeof(DedicatedServerCommandOrigin) == 12, "dedicated server command size");

class DevConsoleCommandOrigin : public CommandOrigin {
public:
	DevConsoleCommandOrigin(Player&);
	virtual ~DevConsoleCommandOrigin();
	char filler[184-4]; // 4 from DevConsoleCommandOrigin::clone()
};
static_assert(sizeof(DevConsoleCommandOrigin) == 184, "dev console command size");
