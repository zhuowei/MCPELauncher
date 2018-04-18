#pragma once
class TextureUVCoordinateSet;
class TextureAtlas;
class BlockPos;

class Block;
typedef Block BlockAndData;

namespace Json {
	class Value;
};
class BlockGraphics {
public:
	void** vtable;
	char filler[124-4]; // 4
	BlockGraphics(std::string const&);
	~BlockGraphics();
	void setTextures(BlockGraphics&, Json::Value const&);
	void setBlockShape(BlockGraphics&, Json::Value const&);
	void setCarriedTextures(BlockGraphics&, Json::Value const&);
	void setTextureIsotropic(BlockGraphics&, Json::Value const&);
	BlockAndData* getBlock() const;
	TextureUVCoordinateSet const& getTexture(BlockPos const&, unsigned int, Block const&) const;
	TextureUVCoordinateSet const& getTexture(BlockPos const&, unsigned int, int) const;
	TextureUVCoordinateSet const& getTexture(unsigned int, Block const&) const;
	TextureUVCoordinateSet const& getTexture(unsigned int, int) const;
	void setTextureItem(std::string const&, std::string const&, std::string const&, std::string const&, std::string const&, std::string const&);

	static void initBlocks();
	static TextureUVCoordinateSet getTextureUVCoordinateSet(std::string const&, int, int);
	static BlockGraphics* mBlocks[0x100];
	static std::unordered_map<std::string, BlockGraphics*> mBlockLookupMap;
	static std::vector<std::unique_ptr<BlockGraphics>> mOwnedBlocks;
	static TextureAtlas* mTerrainTextureAtlas;
};
// BlockGraphics::registerBlockGraphics(Json::Value const&, std::string const&)
static_assert(sizeof(BlockGraphics) == 124, "blockgraphics size");
