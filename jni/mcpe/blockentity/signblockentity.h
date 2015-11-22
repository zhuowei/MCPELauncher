#pragma once
#include <string>
#include "blockentity.h"
class SignBlockEntity : public BlockEntity {
public:
	std::string const& getMessage(int) const;
	void setMessage(std::string const&, int);
};
