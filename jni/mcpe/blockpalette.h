#pragma once
class BlockPalette {
public:
	static BlockPalette* getInstance();
	void createLegacyBlockStates(BlockLegacy const&);
	void registerBlockLegacy(BlockAndData const**, BlockLegacy const&);
};
