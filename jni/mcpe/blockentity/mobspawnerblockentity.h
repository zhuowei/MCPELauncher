#pragma once
#include "blockentity.h"

class BaseMobSpawner;

class MobSpawnerBlockActor : public BlockActor {
public:
	BaseMobSpawner* getSpawner();
};
