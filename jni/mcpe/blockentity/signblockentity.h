#pragma once
#include <string>
#include "blockentity.h"
class UIProfanityContext;
class SignBlockActor : public BlockActor {
public:
	char filler[140-0]; // 0
	std::string message; // 140
	//std::string const& getMessage(UIProfanityContext const&);
	void setMessage(std::string, std::string);
};
// SignBlockActor::setMessage, argument 1. Be careful.
static_assert(offsetof(SignBlockActor, message) == 140, "SignBlockActor message offset");
