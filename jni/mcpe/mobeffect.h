#pragma once
#include <string>

class MobEffect {
public:
	static MobEffect** mobEffects;

	char filler[4]; // 0
	int effectId; // 4
	//int getId() const;
};
static_assert(offsetof(MobEffect, effectId) == 4, "MobEffect effectId");

class MobEffectInstance {
public:
	char filler[28];
	MobEffectInstance(unsigned int potionId, int duration, int amplifier, bool isAmbient, bool alwaysTrue, bool textureSomething);

};

static_assert(sizeof(MobEffectInstance) == 28, "MobEffectInstance size");
