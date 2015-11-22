#pragma once
#define FullTile FullBlock
struct FullTile {
	unsigned char id;
	unsigned char data;
	FullTile(): id(0), data(0) {
	}
	FullTile(FullTile const& other): id(other.id), data(other.data) {
	}
};

struct BlockBrightness {
	unsigned char value;
	BlockBrightness(BlockBrightness const& other): value(other.value) {
	}
};

struct BlockPos {
	int x;
	int y;
	int z;
	BlockPos(BlockPos const& other) : x(other.x), y(other.y), z(other.z) {
	}
	BlockPos(): x(0), y(0), z(0) {
	}
	BlockPos(int x_, int y_, int z_): x(x_), y(y_), z(z_) {
	}
};

struct BlockID {
	unsigned char id;
	operator unsigned char() const {
		return id;
	}
	BlockID(BlockID const& other): id(other.id) {
	}
	BlockID(unsigned char id_): id(id_) {
	}
};

#define TilePos BlockPos
