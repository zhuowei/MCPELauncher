#pragma once
class Command;
class CommandOrigin;
// this is returned in r0 on ARM and passed as hidden first arg on x86
struct MCRESULT {
	int filler;
};
class MinecraftCommands {
public:
	std::shared_ptr<Command> getCommand(std::string const&, int) const;
	MCRESULT requestCommandExecution(std::unique_ptr<CommandOrigin>, std::string const&, std::string&) const;
};
