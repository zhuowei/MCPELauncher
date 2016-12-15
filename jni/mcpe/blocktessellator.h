#pragma once
class BlockOccluder;
class BlockTessellator {
public:
	void tessellateInWorld(Block const&, BlockPos const&, unsigned char, bool);
	BlockSource* getRegion() const;
};
