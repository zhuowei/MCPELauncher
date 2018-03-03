#pragma once
#include <string>
#include "blockentity.h"
class UIProfanityContext;
class SignBlockEntity : public BlockEntity {
public:
	char filler[116-0]; // 0
	std::string message; // 116
	//std::string const& getMessage(UIProfanityContext const&);
	void setMessage(std::string const&, std::string const&);
};
// SignBlockEntity::setMessage
static_assert(offsetof(SignBlockEntity, message) == 116, "SignBlockEntity message offset");
