#pragma once
class ItemInstance;
class BlockPos;
class Vec3;
class ItemUseCallback;
class Player;
class GameMode {
public:
	virtual ~GameMode();
	Player* player; // 4
	void _destroyBlockInternal(BlockPos const&, unsigned char);
	float getDestroyProgress();
	void useItemOn(ItemInstance&, BlockPos const&, signed char, Vec3 const&, ItemUseCallback*);
	void destroyBlock(BlockPos const&, unsigned char);
};
static_assert(offsetof(GameMode, player) == 4, "GameMode player");
