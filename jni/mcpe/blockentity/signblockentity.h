#pragma once
#include <string>
#include "blockentity.h"
class UIProfanityContext;
class SignBlockEntity : public BlockEntity {
public:
	char filler[108-0]; // 0
	std::string message; // 108
	//std::string const& getMessage(UIProfanityContext const&);
	void setMessage(std::string const&);
};
// SignBlockEntity::setMessage
static_assert(offsetof(SignBlockEntity, message) == 108, "SignBlockEntity message offset");
