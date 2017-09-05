#pragma once
namespace Social {
class GameConnectionInfo {
public:
	// see GameConnectionInfo copy constructor
	short connectionType; // 0
	std::string host; // 4
	char filler[4]; // 8
	int port; // 12
	std::string rakNetGuid; // 16
	char thirdPartyInfo[37]; // 20
};
}; // namespace GameConnectionInfo
