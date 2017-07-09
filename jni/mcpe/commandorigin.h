#pragma once
class Minecraft;
class CommandOrigin {
public:
	CommandOrigin();
	virtual ~CommandOrigin();
};
class DedicatedServerCommandOrigin : public CommandOrigin {
public:
	DedicatedServerCommandOrigin(std::string const&, Minecraft&);
	virtual ~DedicatedServerCommandOrigin();
	char filler[168-4]; // 4 from DedicatedServerCommandOrigin::clone()
};
static_assert(sizeof(DedicatedServerCommandOrigin) == 168, "dedicated server command size");

class DevConsoleCommandOrigin : public CommandOrigin {
public:
	DevConsoleCommandOrigin(EntityUniqueID const&, Level*);
	virtual ~DevConsoleCommandOrigin();
	char filler[176-4]; // 4 from DevConsoleCommandOrigin::clone()
};
static_assert(sizeof(DevConsoleCommandOrigin) == 176, "dev console command size");
