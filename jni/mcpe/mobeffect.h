#pragma once
#include <string>

class MobEffect {
public:
	static MobEffect** mobEffects;
	static MobEffect* saturation;
	static MobEffect* absorption;
	static MobEffect* healthBoost;
	static MobEffect* wither;
	static MobEffect* poison;
	static MobEffect* weakness;
	static MobEffect* hunger;
	static MobEffect* nightVision;
	static MobEffect* blindness;
	static MobEffect* invisibility;
	static MobEffect* waterBreathing;
	static MobEffect* fireResistance;
	static MobEffect* damageResistance;
	static MobEffect* regeneration;
	static MobEffect* confusion;
	static MobEffect* jump;
	static MobEffect* harm;
	static MobEffect* heal;
	static MobEffect* damageBoost;
	static MobEffect* digSlowdown;
	static MobEffect* digSpeed;
	static MobEffect* movementSlowdown;
	static MobEffect* movementSpeed;
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
	MobEffectInstance(int potionId, int duration, int amplifier, bool isAmbient, bool alwaysTrue);

};
