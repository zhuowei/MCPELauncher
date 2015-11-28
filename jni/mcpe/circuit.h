#pragma once

class BlockPos;
class BlockSource;

class ConsumerComponent {
public:
	char filler[25];
	bool setToOne;
};

class CircuitSystem {
public:
	template <typename SomeComponent> SomeComponent* create(BlockPos const&, BlockSource*, signed char);
};
