#pragma once
class TextureUVCoordinateSet;
class TextureAtlas;
namespace Json {
	class Value;
};
class BlockGraphics {
public:
	void** vtable;
	char filler[324-4]; // 4
	BlockGraphics(std::string const&);
	~BlockGraphics();
	void setTextures(BlockGraphics&, Json::Value const&);
	void setBlockShape(BlockGraphics&, Json::Value const&);
	void setCarriedTextures(BlockGraphics&, Json::Value const&);
	void setTextureIsotropic(BlockGraphics&, Json::Value const&);
	Block* getBlock() const;
	TextureUVCoordinateSet const& getTexture(signed char, int);
	void setTextureItem(std::string const&, std::string const&, std::string const&, std::string const&, std::string const&, std::string const&);
	static TextureUVCoordinateSet getTextureUVCoordinateSet(std::string const&, int);
	static BlockGraphics* mBlocks[0x100];
	static std::unordered_map<std::string, BlockGraphics*> mBlockLookupMap;
	static std::vector<std::unique_ptr<BlockGraphics>> mOwnedBlocks;
	static TextureAtlas* mTerrainTextureAtlas;
};
static_assert(sizeof(BlockGraphics) == 324, "blockgraphics size");
