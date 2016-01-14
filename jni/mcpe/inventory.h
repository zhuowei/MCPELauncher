#pragma once
class ItemInstance;
class FillingContainer {
public:
	ItemInstance* getItem(int slot) const;
	int getLinkedSlotsCount() const;
	int getLinkedSlot(int slot) const;
	ItemInstance* getLinked(int slot) const;
	int** getLinkedSlots();
};

class Inventory : public FillingContainer {
public:
	int getLinkedSlotForItem(int slot);
};
