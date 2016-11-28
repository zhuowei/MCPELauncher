#pragma once
namespace Social {
class GameConnectionInfo {
public:
	short connectionType; // 0
	std::string host; // 4
	int port; // 8
	std::string rakNetGuid; // 12
};
}; // namespace GameConnectionInfo
