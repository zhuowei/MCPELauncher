#pragma once

namespace Util {

class HashString {
public:
	// basically std::string content, long hash;
	char filler[16]; // 0

	HashString(std::string const&);
	~HashString();
	std::string const& getString() const;
	HashString& operator=(Util::HashString const&);
};
static_assert(sizeof(HashString) == 16, "HashString size");

}; // namespace Util
