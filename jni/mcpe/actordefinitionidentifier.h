#pragma once
class ActorDefinitionIdentifier {
public:

	// FIXME 0.16: yes this will leak memory sigh
	char filler[28]; // 0 from ActorDefinitionIdentifier constructor

	ActorDefinitionIdentifier();
	ActorDefinitionIdentifier(ActorType);
	ActorDefinitionIdentifier(ActorDefinitionIdentifier const&);
	ActorDefinitionIdentifier(std::string const&);

	bool operator==(ActorDefinitionIdentifier const&) const;
	ActorDefinitionIdentifier& operator=(ActorDefinitionIdentifier const&);

	ActorType _getLegacyActorType() const;
	std::string getCanonicalName() const;
};
static_assert(sizeof(ActorDefinitionIdentifier) == 28, "ActorDefinitionIdentifier size");
