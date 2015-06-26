#pragma once
#include <string>
#include <unordered_map>

class ServerCommandParser {
public:
	class Command {
	};

	std::string what; // 0
	std::unordered_multimap<std::string, std::unique_ptr<Command> > commands; // 4

};
