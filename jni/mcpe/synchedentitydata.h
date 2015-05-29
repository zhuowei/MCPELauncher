#pragma once
#include <string>

class DataItem {
public:
	char filler[8];
};

template <class T> class DataItem2 : public DataItem {
public:
	T data; // 8
};

class SynchedEntityData {
public:
	std::string* getString(unsigned char) const;
	DataItem* _find(unsigned char) const;
};
