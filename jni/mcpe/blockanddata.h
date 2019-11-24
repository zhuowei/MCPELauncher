#pragma once
#include "sharedptr.h"
class Block;
class BlockLegacy;

class Block {
public:
	unsigned char blockData; // 4 from Block::Block
	WeakPtr<BlockLegacy> blockBase; // 8 from Block::Block
	char filler[44-12]; // 12 from Block::Block
	char unknown1; // 44

	Block(unsigned char, BlockLegacy const*);
	virtual ~Block();
};
typedef Block BlockAndData;
static_assert(offsetof(Block, blockBase) == 8, "Block blockBase");
// Block::setRuntimeId
static_assert(sizeof(Block) == 48, "Block size");
