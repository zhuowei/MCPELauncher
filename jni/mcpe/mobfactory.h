#pragma once
#include <memory>

class MobFactory {
public:
	static std::unique_ptr<Entity> CreateMob(int, TileSource&, Vec3 const&, Vec2*);
};

class EntityFactory {
public:
	static std::unique_ptr<Entity> CreateEntity(int, TileSource&);
};
