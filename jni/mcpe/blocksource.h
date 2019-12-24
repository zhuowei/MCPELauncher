#pragma once

#include "mcpe/blockstructs.h"
#include "mcpe/blockanddata.h"

class BlockActor;
class Biome;
class LevelChunk;
class Dimension;
class ChunkPos;

class BlockSource {
public:
	void setBlock(int, int, int, BlockAndData const&, int);
	int getBlockID(int x, int y, int z) const {
		return getBlockID({x, y, z});
	}
	int getBlockID(BlockPos const& blockPos) const {
		BlockAndData* state = getBlock(blockPos);
		if (!state) return 0;
		if (!state->blockBase.get()) return 0;
		return state->blockBase.get()->id;
	}
	int getData(int x, int y, int z) {
		return getData({x, y, z});
	}
	int getData(BlockPos const& blockPos) const {
		BlockAndData* state = getBlock(blockPos);
		if (!state) return 0;
		return state->blockData;
	}
	BlockBrightness	getRawBrightness(BlockPos const&, bool, bool) const;
	BlockActor*	getBlockEntity(int x, int y, int z) {
		return getBlockEntity({x, y, z});
	}
	BlockActor*	getBlockEntity(BlockPos const&);
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
	LevelChunk*	getChunk(ChunkPos const&) const;
	BlockAndData*	getBlock(BlockPos const&) const;
};
#define TileSource BlockSource
