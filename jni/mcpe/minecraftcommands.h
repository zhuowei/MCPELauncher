#pragma once
class Command;
class CommandOrigin;
class MinecraftCommands {
public:
	std::shared_ptr<Command> getCommand(std::string const&, int) const;
	int requestCommandExecution(std::unique_ptr<CommandOrigin>, std::string const&, std::string&) const;
};
