#pragma once
#include <memory>

enum EntityType {
};

class EntityDefinitionIdentifier {
public:
	std::string part1;
	std::string part2;
	std::string part3;
	EntityDefinitionIdentifier(EntityType);
};

class EntityFactory {
public:
	static std::unique_ptr<Entity> createSpawnedEntity(EntityDefinitionIdentifier const&, Entity*, Vec3 const&, Vec2 const&);
};
