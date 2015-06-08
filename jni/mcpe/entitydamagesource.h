#pragma once
#include <string>
class EntityDamageSource {
public:
	int source;
	virtual ~EntityDamageSource();
	virtual bool isChildEntitySource();
	virtual bool isEntitySource();
	virtual bool isTileSource();
	virtual std::string getDeathMessage(std::string, Entity*);
};

class EntityDamageByEntitySource : public EntityDamageSource {
public:
	Entity* entity;
	virtual ~EntityDamageByEntitySource();
	virtual bool isEntitySource() override;
	virtual std::string getDeathMessage(std::string, Entity*) override;
	virtual bool isChildEntitySource() override;
};
