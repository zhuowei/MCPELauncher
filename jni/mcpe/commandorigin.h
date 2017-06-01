#pragma once
class Minecraft;
class CommandOrigin {
public:
	CommandOrigin();
	virtual ~CommandOrigin();
};
class DedicatedServerCommandOrigin : public CommandOrigin {
public:
	DedicatedServerCommandOrigin(Minecraft&);
	virtual ~DedicatedServerCommandOrigin();
	char filler[168-4]; // 4 from DedicatedServerCommandOrigin::clone()
};
static_assert(sizeof(DedicatedServerCommandOrigin) == 168, "dedicated server command size");
