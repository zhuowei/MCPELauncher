#pragma once
#include <memory>
class Level;

enum ActorType {
};
typedef ActorType EntityType;
#include "actordefinitionidentifier.h"

class ActorFactory {
public:
	Level* level; // 0
	char filler[12-4]; // 4
	ActorFactory(Level&);
	std::unique_ptr<Actor> createSpawnedEntity(ActorDefinitionIdentifier const&, Actor*, Vec3 const&, Vec2 const&);
};
// ActorFactory::ActorFactory
static_assert(sizeof(ActorFactory) == 12, "ActorFactory size");
