#pragma once
#include <memory>

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
	static std::unique_ptr<Actor> createSpawnedEntity(ActorDefinitionIdentifier const&, Actor*, Vec3 const&, Vec2 const&);
};
