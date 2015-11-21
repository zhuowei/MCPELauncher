#pragma once
#ifdef __cplusplus
#include <memory>
#endif
class Entity;
typedef Entity Player;
#include "mcpe/minecraft.h"
#include "mcpe/blocksource.h"

#ifdef __cplusplus
#define cppstr std::string
#else
typedef struct {
	char* contents;
} cppstr;
#endif

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
class EntityUniqueID {
public:
	long long id;
	EntityUniqueID(long long id) : id(id) {
	}

	operator long long() const {
		return this->id;
	}
};
#else
typedef struct {
	long long id;
} EntityUniqueID;
#endif

struct Vec3 {
	float x;
	float y;
	float z;
	Vec3(float x_, float y_, float z_): x(x_), y(y_), z(z_) {
	};
	Vec3(): x(0), y(0), z(0) {
	};
};

struct Vec2 {
	float x;
	float y;
	Vec2(float x_, float y_): x(x_), y(y_) {
	};
};

class Entity {
public:
	void** vtable; //0
	int filler3[5];//4
	float x; //24
	float y; //28
	float z; //32
	char filler2[48-36]; //36
	TileSource* blockSource; // 48
	int dimension; // 52
	char filler2_[76-56]; // 56
	float motionX; //76 found in Entity::rideTick(); should be set to 0 there
	float motionY; //80
	float motionZ; //84
	float yaw; //88
	float pitch; //92
	float prevYaw; //96
	float prevPitch; //100
	char filler4[268-104]; //104
	int renderType; //268
	char filler5[284-272]; // 272
	struct Entity* rider; //284
	struct Entity* riding; //288
	char filler6[320-292]; //292
#ifdef __cplusplus
	EntityUniqueID entityId; // 320
#else
	long long entityId; // 320
#endif

	BlockSource* getRegion() const;
	void setRot(Vec2 const&);
};

#ifdef __cplusplus
struct TextureUVCoordinateSet {
	float bounds[6];
	int idunno;
	void* textureFile;
	TextureUVCoordinateSet(TextureUVCoordinateSet const& other) {
		*this = other; // yeah I know, bad memory management habit. Deal with it
	};
};
#else
typedef struct {
	float bounds[6];
	int idunno;
	void* textureFile;
} TextureUVCoordinateSet;
#endif

typedef struct {
	void** vtable; //0
	unsigned char maxStackSize; //4
	char filler0[24-5]; //5
	short itemId; //24
	short maxDamage; //26
	char filler[60-28]; //28
	int category1; //60
	bool handEquipped; //64
	bool stackedByData; //65
	char filler1[72-66]; //66
} Item;

typedef void CompoundTag;

typedef struct {
	unsigned char count; //0
	short damage; //2
	CompoundTag* tag; // 4
	Item* item; // 8
	void* block; //12
	bool wtf; //16
} ItemInstance; // see ServerCommandParser::give for size

typedef struct {
	void** vtable; //0
	float idunnofloat1; //4
	char aabbfiller[28]; //8
	TextureUVCoordinateSet texture; //36
	unsigned char id; //68
	char filler[80-69]; //69 (insert rude joke here)
	int renderPass; //80
	int renderType; //84
	char filler2[108-88]; //88
	void* material; //108
	int filler3; //112
	float destroyTime; //116
	float explosionResistance; //120
	int category1; //124
	char filler4[8]; //128
	cppstr descriptionId; //136
} Block;
#define Tile Block

typedef struct {
	char filler0[664]; //0
} Cube;

typedef struct {
	char filler[12]; // 0
} MaterialPtr;

// from ModelPart::setPos, ModelPart::setTexSize
class ModelPart {
public:
	float offsetX; //0
	float offsetY; //4
	float offsetZ; //8
	float rotateAngleX; // 12
	float rotateAngleY; // 16
	char filler0[25-20]; // 20
	bool showModel; // 25 from HumanoidMobRenderer::prepareArmor
	char filler1[52-26]; //26
	float textureWidth; //52
	float textureHeight; //56
	MaterialPtr* material; //60 from ModelPart::draw
	int textureOffsetX; // 64
	int textureOffsetY; // 68
	bool neverRender; // 72
	char filler3[152-73];//73
	void* model; // 152

	void addBox(Vec3 const&, Vec3 const&, float);
}; // 156 bytes

// from HumanoidModel::render

typedef struct {
	void** vtable; //0
	char filler[13-4]; // 4
	bool riding; // 13
	char filler1[16-14]; // 14
	MaterialPtr* activeMaterial; // 16
	MaterialPtr materialNormal; // 20
	MaterialPtr materialAlphaTest; // 32
	MaterialPtr materialAlphaBlend; // 44
	MaterialPtr materialStatic; // 56
	MaterialPtr materialEmissive; // 68
	MaterialPtr materialEmissiveAlpha; // 80
	MaterialPtr materialChangeColor; // 92
	MaterialPtr materialGlint; // 104
	MaterialPtr materialAlphaTestGlint; // 116
	char filler2[140-128]; // 128
	ModelPart bipedHead;//140
	ModelPart bipedHeadwear;//296
	ModelPart bipedBody;//452
	ModelPart bipedRightArm;//608
	ModelPart bipedLeftArm;//764
	ModelPart bipedRightLeg;//920
	ModelPart bipedLeftLeg;//1076
} HumanoidModel;

#ifdef __cplusplus
static_assert(sizeof(ModelPart) == 156, "ModelPart size");
#endif

typedef struct {
	Item* item; //0
	Tile* tile; //4
	ItemInstance itemInstance; //8
	char letter; //28
} RecipesType;

typedef struct {
} FurnaceRecipes;

typedef struct {
	char filler[16]; //0
	char* text; //16;
} RakString;

typedef void EntityRenderer;

typedef struct {
	void** vtable; //0
	char filler[108-4]; //4
	void* model; // 108 (from MobRenderer::MobRenderer)
} MobRenderer;

typedef void Tag;

typedef struct {
	void** vtable; //0
	char filler[12]; //4
	int64_t value; //16
} LongTag;

#define HIT_RESULT_BLOCK 0
#define HIT_RESULT_ENTITY 1
#define HIT_RESULT_NONE 2

typedef struct {
	int type; //0
	int side; //4
	int x; //8
	int y; //12
	int z; //16
	Vec3 hitVec; //20
	Entity* entity; //32
	unsigned char filler1; //36
} HitResult;

class Biome {
public:
	void** vtable; //0
	char filler[56-4]; //4
	cppstr name; //56 from Biome::setName
	char filler2[148-60]; //60
	int id; //148 from Biome::Biome
};

typedef struct {
	bool invulnerable; // from Desktop Edition's PlayerAbilities and also CreativeMode::initAbilities
	bool flying;
	bool mayFly;
	bool instabuild;
} Abilities;

// from LocalServerListItemElement::serverMainPressed 25c7d4
typedef struct {
	int wtf1; // 0 always negative 1?
	int wtf2; // 4 always negative 1?
	int wtf3; // 8 always 3
	int wtf4; // 12 always 4
	int wtf5; // 16 always 0
	int wtf6; // 16 always 0
	int wtf7; // 16 always 0
} LevelSettings;

typedef struct {
	float r;
	float g;
	float b;
	float a;
} Color;

typedef struct {
	bool shouldBeFalse; // 0
	float x1; // 4
	float y1; // 8
	float z1; // 12
	float x2; // 16
	float y2; // 20
	float z2; // 24
} AABB;

typedef struct {
	unsigned char x;
	unsigned char z;
	unsigned char y;
} ChunkTilePos;

class LevelChunk;

typedef void ModelRenderer;

typedef void ScreenChooser;

#ifdef __cplusplus
struct ArmorItem : public Item {
	int armorType; // 76
	int damageReduceAmount; // 80
	int renderIndex; // 84
	void* armorMaterial; // 88
};

#ifdef __arm__
static_assert(sizeof(ArmorItem) == 88, "armor item size");
#endif

struct HumanoidMobRenderer : public MobRenderer {
	char filler[144-112]; // 112
	HumanoidModel* modelArmor; // 144
	HumanoidModel* modelArmorChestplate; // 148
};
#endif

#ifdef __cplusplus
}
#endif

enum GameType {
};

class LevelData {
public:
	void setSpawn(BlockPos const&);
	void setGameType(GameType);
	GameType getGameType() const;
};

class Level {
public:
	void** vtable; //0
	char filler[12-4]; //4
	bool isRemote; //12 PrimedTnT::normalTick
	char filler2[2908-13];//13

	Entity* getEntity(EntityUniqueID, bool) const;
	void addEntity(std::unique_ptr<Entity>);
	void explode(BlockSource&, Entity*, float, float, float, float, bool);
	void setNightMode(bool);
	void setTime(int);
	int getTime() const;
	LevelData* getLevelData();
};

#include "mcpe/blockentity/chestblockentity.h"
#include "mcpe/blockentity/furnaceblockentity.h"
