#pragma once
#include <string>
#include "blockentity.h"
class UIProfanityContext;
class SignBlockActor : public BlockActor {
public:
	char filler[136-0]; // 0
	std::string message; // 136
	//std::string const& getMessage(UIProfanityContext const&);
	void setMessage(std::string, std::string);
};
// SignBlockActor::setMessage, argument 1. Be careful.
static_assert(offsetof(SignBlockActor, message) == 136, "SignBlockActor message offset");
