#pragma once

#include "mcpe/blockstructs.h"

class BlockEntity;
class Biome;
class LevelChunk;
class Dimension;

class BlockSource {
public:
	void		setBlockAndData(int, int, int, FullBlock, int);
	BlockID		getBlockID(int, int, int);
	BlockID		getBlockID(BlockPos const&);
	int		getData(int, int, int);
	int		getData(BlockPos const&);
	BlockBrightness	getRawBrightness(int, int, int, bool);
	BlockEntity*	getBlockEntity(int, int, int);
	int		getGrassColor(BlockPos const&);
	void		setGrassColor(int, BlockPos const&, int);
	Biome*		getBiome(BlockPos const&);
	LevelChunk*	getChunk(int, int);
	void		fireBlockEvent(int, int, int, int, int);
	Dimension*	getDimension();
	bool		canSeeSky(int, int, int);
	Level*		getLevel();
};
#define TileSource BlockSource
