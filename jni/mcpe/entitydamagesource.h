#pragma once
#include <string>
class EntityDamageSource {
public:
	int source;
	virtual ~EntityDamageSource();
	virtual bool isEntitySource() const;
	virtual bool isChildEntitySource() const;
	virtual bool isBlockSource() const;
	virtual std::string getDeathMessage(std::string, Entity*) const;
	virtual bool getIsCreative() const;
	virtual bool getIsWorldBuilder() const;
	virtual EntityUniqueID getEntityUniqueID() const;
	virtual int getEntityType() const;
	virtual int getEntityCategories() const;
	virtual bool getDamagingEntityIsCreative() const;
	virtual EntityUniqueID getDamagingEntityUniqueID() const;
	virtual int getDamagingEntityType() const;
	virtual int getDamagingEntityCategories() const;
};

class EntityDamageByEntitySource : public EntityDamageSource {
public:
	Entity* entity;
	virtual ~EntityDamageByEntitySource();
	virtual bool isEntitySource() const override;
	virtual std::string getDeathMessage(std::string, Entity*) const override;
	virtual bool isChildEntitySource() const override;
};
