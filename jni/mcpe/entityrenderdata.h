#pragma once
class Entity;
class EntityRenderData {
public:
	Entity* entity; // 0
};
static_assert(offsetof(EntityRenderData, entity) == 0, "entity render data?");
