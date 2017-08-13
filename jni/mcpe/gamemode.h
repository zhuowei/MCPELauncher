#pragma once
class ItemInstance;
class BlockPos;
class Vec3;
class ItemUseCallback;
class Player;
class GameMode {
public:
	Player* player;
	void _destroyBlockInternal(BlockPos const&, signed char);
	float getDestroyProgress();
	void useItemOn(ItemInstance&, BlockPos const&, signed char, Vec3 const&, ItemUseCallback*);
};
