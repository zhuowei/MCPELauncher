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
	ActorFactory(Level&);
	std::unique_ptr<Actor> createSpawnedEntity(ActorDefinitionIdentifier const&, Actor*, Vec3 const&, Vec2 const&);
};
static_assert(sizeof(ActorFactory) == 4, "ActorFactory size");
