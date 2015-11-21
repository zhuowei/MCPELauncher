#pragma once

#include "mcpe/blockstructs.h"

class BlockEntity;

class BlockSource {
public:
	void		setBlockAndData(int, int, int, FullBlock, int);
	unsigned char	getBlock(int, int, int);
	int		getData(int, int, int);
	BlockBrightness	getRawBrightness(int, int, int, bool);
	BlockEntity*	getBlockEntity(int, int, int);
	void		setGrassColor(int, BlockPos const&, int);
};
#define TileSource BlockSource
#define tileSource blockSource
