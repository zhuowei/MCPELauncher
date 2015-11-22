#pragma once
#include "blockentity.h"

class BaseMobSpawner;

class MobSpawnerBlockEntity : public BlockEntity {
public:
	BaseMobSpawner* getSpawner();
};
