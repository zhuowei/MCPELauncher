#pragma once
#include <string>
#include "blockentity.h"
class UIProfanityContext;
class SignBlockEntity : public BlockEntity {
public:
	std::string const& getMessage(UIProfanityContext const&);
	void setMessage(std::string const&);
};
