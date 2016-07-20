#pragma once
class TextureUVCoordinateSet;
namespace Json {
	class Value;
};
class BlockGraphics {
public:
	void** vtable;
	char filler[324-4]; // 0
	BlockGraphics(std::string const&);
	~BlockGraphics();
	void setTextures(BlockGraphics&, Json::Value const&);
	void setBlockShape(BlockGraphics&, Json::Value const&);
	void setCarriedTextures(BlockGraphics&, Json::Value const&);
	void setTextureIsotropic(BlockGraphics&, Json::Value const&);
	Block* getBlock() const;
	TextureUVCoordinateSet const& getTexture(signed char, int);
	static TextureUVCoordinateSet getTextureUVCoordinateSet(std::string const&, int);
	static BlockGraphics* mBlocks[0x100];
};
static_assert(sizeof(BlockGraphics) == 324, "blockgraphics size");
