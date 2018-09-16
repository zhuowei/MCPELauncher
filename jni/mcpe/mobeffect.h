#pragma once
#include <string>

class MobEffect {
public:
	static MobEffect** mobEffects;
	int getId() const;
};

class MobEffectInstance {
public:
	int potionId; //0
	int duration; // 4
	int amplifier; // 8
	bool isAmbient; // 13
	bool noCounter; // 14
	bool something; // 15
	MobEffectInstance(unsigned int potionId, int duration, int amplifier, bool isAmbient, bool alwaysTrue);

};
