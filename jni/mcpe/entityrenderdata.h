#pragma once
class Actor;
class ActorRenderData {
public:
	Actor* entity; // 0
};
static_assert(offsetof(ActorRenderData, entity) == 0, "entity render data?");
