#pragma once
#include <memory>
class Level;

enum ActorType {
};
typedef ActorType EntityType;

class ActorDefinitionIdentifier {
public:
/*
	std::string part1;
	std::string part2;
	std::string part3;
*/
	char filler[12]; // FIXME 0.16: yes this will leak memory sigh
	ActorDefinitionIdentifier(ActorType);
};

class ActorFactory {
public:
	Level* level; // 0
	ActorFactory(Level&);
	std::unique_ptr<Actor> createSpawnedEntity(ActorDefinitionIdentifier const&, Actor*, Vec3 const&, Vec2 const&);
};
static_assert(sizeof(ActorFactory) == 4, "ActorFactory size");
