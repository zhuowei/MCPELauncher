#pragma once
class UIControl;
class ClientInstance;
class InventoryItemRenderer {
public:
	// FIXME 1.1
	char filler[48]; // 0
	int itemId; // 48; from render
	char filler2[80-52]; // 52
	std::string atlasName; // 80 from update
	void update(ClientInstance&, UIControl&);
};
