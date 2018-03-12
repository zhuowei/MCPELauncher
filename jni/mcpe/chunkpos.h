#pragma once

class BlockPos;

class ChunkPos {
public:
	int x; // 0
	int y; // 4
	int z; // 8
	ChunkPos(BlockPos const&);
};

static_assert(sizeof(ChunkPos) == 12, "ChunkPos size");
