#pragma once
#include <string>
class EntityDamageSource {
public:
	int source;
	virtual ~EntityDamageSource();
	virtual bool isEntitySource() const;
	virtual Entity* getEntity() const;
	virtual bool isChildEntitySource() const;
	virtual Entity* getChildEntity() const;
	virtual bool isTileSource() const;
	virtual std::string getDeathMessage(std::string, Entity*) const;
};

class EntityDamageByEntitySource : public EntityDamageSource {
public:
	Entity* entity;
	virtual ~EntityDamageByEntitySource();
	virtual bool isEntitySource() const override;
	virtual std::string getDeathMessage(std::string, Entity*) const override;
	virtual bool isChildEntitySource() const override;
};
