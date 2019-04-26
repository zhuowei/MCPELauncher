#pragma once
#include <string>

class MobEffect {
public:
	static MobEffect** mobEffects;
	int getId() const;
};

class MobEffectInstance {
public:
	char filler[28];
	MobEffectInstance(unsigned int potionId, int duration, int amplifier, bool isAmbient, bool alwaysTrue, bool textureSomething);

};

static_assert(sizeof(MobEffectInstance) == 28, "MobEffectInstance size");
