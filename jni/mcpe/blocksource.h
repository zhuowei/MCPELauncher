#pragma once

#include "mcpe/blockstructs.h"

class BlockEntity;
class Biome;
class LevelChunk;
class Dimension;

class BlockSource {
public:
	void		setBlockAndData(int, int, int, FullBlock, int);
	BlockID		getBlockID(int, int, int) const;
	BlockID		getBlockID(BlockPos const&) const;
	int		getData(int, int, int);
	int		getData(BlockPos const&) const;
	BlockBrightness	getRawBrightness(int, int, int, bool);
	BlockEntity*	getBlockEntity(int, int, int);
	int		getGrassColor(BlockPos const&) const;
	void		setGrassColor(int, BlockPos const&, int);
	Biome*		getBiome(BlockPos const&);
	LevelChunk*	getChunk(int, int) const;
	void		fireBlockEvent(int, int, int, int, int);
	Dimension*	getDimension();
	bool		canSeeSky(int, int, int) const;
	Level*		getLevel();
	unsigned short	getExtraData(BlockPos const&);
	void		setExtraData(BlockPos const&, unsigned short);
};
#define TileSource BlockSource
