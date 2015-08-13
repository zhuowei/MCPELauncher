#pragma once
#include <memory>

enum EntityType {
};

class MobFactory {
public:
	static std::unique_ptr<Entity> CreateMob(EntityType, TileSource&, Vec3 const&, Vec2 const&);
};

class EntityFactory {
public:
	static std::unique_ptr<Entity> CreateEntity(EntityType, TileSource&);
};
