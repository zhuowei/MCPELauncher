#pragma once
namespace Social {
class GameConnectionInfo {
public:
	// see GameConnectionInfo copy constructor
	short connectionType; // 0
	std::string host; // 4
	int port; // 8
	std::string rakNetGuid; // 12
	char thirdPartyInfo[13]; // 16
};
}; // namespace GameConnectionInfo
