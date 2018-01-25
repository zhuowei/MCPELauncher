#pragma once
class PlayerName {
public:
	char filler[12];
	std::string const& getPrimaryName() const;
	std::string getDisplayName() const;
};

static_assert(sizeof(PlayerName) == 12, "playername");
