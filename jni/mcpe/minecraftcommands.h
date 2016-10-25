#pragma once
class Command;
class MinecraftCommands {
public:
	std::shared_ptr<Command> getCommand(std::string const&, int) const;
};
