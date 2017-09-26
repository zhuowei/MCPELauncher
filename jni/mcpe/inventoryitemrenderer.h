#pragma once
class UIControl;
class ClientInstance;

class MinecraftUIRenderContext;
class RectangleArea;

class InventoryItemRenderer {
public:
	char filler[52]; // 0
	int itemId; // 52; from render
	char filler2[92-56]; // 56
	std::string atlasName; // 92 from update
	void update(ClientInstance&, UIControl&);
	void render(MinecraftUIRenderContext&, ClientInstance&, UIControl&, int, RectangleArea&);
};
static_assert(offsetof(InventoryItemRenderer, itemId) == 52, "itemId offset");
