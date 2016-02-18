#pragma once

class BlockPos;
class BlockSource;

class ConsumerComponent {
public:
	char filler[40];
	bool setToOne;

	ConsumerComponent();
};

class CircuitSystem {
public:
	template <typename SomeComponent> SomeComponent* create(BlockPos const&, BlockSource*, signed char);
};
