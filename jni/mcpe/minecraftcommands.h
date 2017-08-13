#pragma once
class Command;
class CommandOrigin;
// this is returned in r0 on ARM and passed as hidden first arg on x86
struct MCRESULT {
	int filler;
};

class CommandRegistry {
public:
	Command* findCommand(std::string const&) const;
};

class MinecraftCommands {
public:
	CommandRegistry* getRegistry();
	MCRESULT requestCommandExecution(std::unique_ptr<CommandOrigin>, std::string const&, int, bool) const;
};
