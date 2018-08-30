#pragma once
class NewBlockID {
public:
	~NewBlockID() {
	};
	short id;
};
class BlockPalette {
public:
	Block* getBlockFromLegacyData(NewBlockID, unsigned int) const;
};
