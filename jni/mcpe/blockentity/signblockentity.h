#pragma once
#include <string>
#include "blockentity.h"
class UIProfanityContext;
class SignBlockActor : public BlockActor {
public:
	char filler[112-0]; // 0
	std::string message; // 112
	//std::string const& getMessage(UIProfanityContext const&);
	void setMessage(std::string const&, std::string const&);
};
// SignBlockActor::setMessage
static_assert(offsetof(SignBlockActor, message) == 112, "SignBlockActor message offset");
