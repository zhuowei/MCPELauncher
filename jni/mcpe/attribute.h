#pragma once

class Attribute {
public:
	char filler[8];
};

class SharedAttributes {
public:
	static Attribute ABSORPTION;
	static Attribute ATTACK_DAMAGE;
	static Attribute MOVEMENT_SPEED;
	static Attribute KNOCKBACK_RESISTANCE;
	static Attribute FOLLOW_RANGE;
	static Attribute HEALTH;
};

class AttributeInstance {
public:
	char filler[76-0]; // 0
	float value; // 76
	float getMaxValue() const;
	void setMaxValue(float);
};
