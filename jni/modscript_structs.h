#pragma once
#ifdef __cplusplus
#include <memory>
#include <unordered_map>
#endif
class Entity;
class Player;
#include "mcpe/minecraft.h"
#include "mcpe/blocksource.h"
#include "mcpe/enchant.h"
#include "mcpe/synchedentitydata.h"

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

// last update: 0.14.0b7
class Entity {
public:
	void** vtable; //0
	int filler3;//4
	float x; //8
	float y; //12
	float z; //16
	char filler2[44-20]; // 20
	float motionX; //44 found in Entity::rideTick(); should be set to 0 there
	float motionY; //48
	float motionZ; //52
	float pitch; //56 Entity::setRot
	float yaw; //60
	float prevPitch; //64
	float prevYaw; //68

	char filler4[244-72]; //72
	int renderType; //244
	char filler5[260-248]; // 248
	struct Entity* rider; //260
	struct Entity* riding; //264 from Entity::getRide

	BlockSource* getRegion() const;
	void setRot(Vec2 const&);
	EntityUniqueID const& getUniqueID() const;
	void setNameTag(std::string const&);
	SynchedEntityData* getEntityData();
};
static_assert(offsetof(Entity, renderType) == 244, "renderType offset wrong");

class Mob: public Entity {
public:
	Mob* getTarget();
	void setTarget(Mob*);
};

class Player: public Mob {
public:
	int getScore();
};

struct TextureUVCoordinateSet {
	float bounds[4];
	unsigned short size[2]; // 16
	void* filler; // 20
	void* textureFile; // 24
	TextureUVCoordinateSet(TextureUVCoordinateSet const& other) {
		*this = other; // yeah I know, bad memory management habit. Deal with it
	};
};
static_assert(offsetof(TextureUVCoordinateSet, textureFile) == 24, "textureFile offset wrong");

namespace Json {
	class Value;
};

// Updated 0.14.0b7
// see _Z12registerItemI4ItemIRA11_KciEERT_DpOT0_
// for useAnimation see setUseAnimation
class Item {
public:
	//void** vtable; //0
	char filler0[18-4]; //4
	short itemId; //18
	char filler1[26-20]; // 20
	unsigned char attribs; // 26
	char filler[32-27]; //27
	unsigned char useAnimation; // 32
	char filler3[68-33]; // 33
	virtual ~Item();

	static std::unordered_map<std::string, std::pair<std::string, std::unique_ptr<Item>>> mItemLookupMap;
	void init(Json::Value&);
	void setStackedByData(bool);
	bool isStackedByData() const;
	void setMaxDamage(int);
	int getMaxDamage();
};

#define ITEM_HAND_EQUIPPED (1 << 1)

class CompoundTag {
public:
	~CompoundTag();
};

class ItemInstance {
public:
	unsigned char count; //0
	short damage; //2
	unsigned char something; // 4
	char filler[8-5]; // 5
	CompoundTag* tag; // 8
	Item* item; // 12
	void* block; //16

	ItemInstance();
	ItemInstance(int, int, int);
	ItemInstance(ItemInstance const&);
	ItemInstance& operator=(ItemInstance const&);
	bool operator==(ItemInstance const&) const;
	bool operator!=(ItemInstance const&) const;
	ItemEnchants getEnchantsFromUserData() const;
	bool hasCustomHoverName() const;
	std::string getCustomName() const;
	void setCustomName(std::string const&);
	void init(int, int, int);
	int getId() const;
	TextureUVCoordinateSet* getIcon(int, bool) const;
	std::string getName() const;
}; // see ItemInstance::fromTag for size
static_assert(offsetof(ItemInstance, tag) == 8, "tag offset wrong");
static_assert(sizeof(ItemInstance) == 20, "ItemInstance wrong");

enum CreativeItemCategory {
};

class Block {
public:
	void** vtable; //0
	unsigned char id; // 4
	char filler0[16-5]; //5
	TextureUVCoordinateSet texture; //16
	char filler[52-44]; //48 (insert rude joke here)
	int renderLayer; //52
	int renderType; //56 Block::getBlockShape()
	char filler2[84-60]; //60
	void* material; //84
	char filler3[112-88]; // 88
	float destroyTime; //112
	float explosionResistance; //116

	float getDestroySpeed();
	float getFriction();
	void setFriction(float);
	void setSolid(bool);
	void setCategory(CreativeItemCategory);
	static TextureUVCoordinateSet getTextureUVCoordinateSet(std::string const&, int);
};

static_assert(offsetof(Block, renderLayer) == 52, "renderlayer is wrong");
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
	char padding[4]; // 156

	void addBox(Vec3 const&, Vec3 const&, float);
}; // 160 bytes
static_assert(sizeof(ModelPart) == 160, "modelpart size wrong");

namespace mce {
	class TexturePtr;
};

// from HumanoidModel::render

class HumanoidModel {
public:
	void** vtable; //0
	char filler[13-4]; // 4
	bool riding; // 13
	char filler1[16-14]; // 14
	MaterialPtr* activeMaterial; // 16
	mce::TexturePtr* activeTexture; // 20
	MaterialPtr materialNormal; // 24
	MaterialPtr materialAlphaTest; // 36
	MaterialPtr materialAlphaBlend; // 48
	MaterialPtr materialStatic; // 60
	MaterialPtr materialEmissive; // 72
	MaterialPtr materialEmissiveAlpha; // 84
	MaterialPtr materialChangeColor; // 96
	MaterialPtr materialGlint; // 108
	MaterialPtr materialAlphaTestGlint; // 120
	char filler2[180-132]; // 132
	ModelPart bipedHead;//180
	ModelPart bipedHeadwear;//340
	ModelPart bipedBody;//500
	ModelPart bipedRightArm;//660
	ModelPart bipedLeftArm;//820
	ModelPart bipedRightLeg;//980
	ModelPart bipedLeftLeg;//1140
	char mystery2[2]; // 1300
	char filler3[1312-1302]; // 1302
	HumanoidModel(float, float, int, int);
};

static_assert(sizeof(HumanoidModel) == 1312, "HumanoidModel size");
static_assert(offsetof(HumanoidModel, activeTexture) == 20, "active texture");
static_assert(offsetof(HumanoidModel, materialAlphaTest) == 36, "active texture");
static_assert(offsetof(HumanoidModel, bipedHead) == 180, "HumanoidModel bipedHead");

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
	char filler[132-4]; //4
	void* model; // 132 (from MobRenderer::MobRenderer)
	char filler2[180-136]; // 136
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
	int wtf6; // 20 always 0
	int wtf7; // 24 always 0
	int wtf8; // 28
	int wtf9; // 32
	int wtf10; // 36
} LevelSettings;

typedef struct {
	float r;
	float g;
	float b;
	float a;
} Color;

typedef struct {
	float x1; // 0
	float y1; // 4
	float z1; // 8
	float x2; // 12
	float y2; // 16
	float z2; // 20
	bool shouldBeFalse; // 24
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
	int armorType; // 68
	int damageReduceAmount; // 72
	int renderIndex; // 76
	void* armorMaterial; // 80
};

#ifdef __arm__
static_assert(sizeof(ArmorItem) == 84, "armor item size");
#endif

struct HumanoidMobRenderer : public MobRenderer {
	int something; // 180
	HumanoidModel* modelArmor; // 184
	HumanoidModel* modelArmorChestplate; // 188
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
	void addGlobalEntity(std::unique_ptr<Entity>);
	void explode(BlockSource&, Entity*, Vec3 const&, float, bool);
	void setNightMode(bool);
	void setTime(int);
	int getTime() const;
	LevelData* getLevelData();
	void playSound(Vec3 const&, std::string const&, float, float);
	bool isClientSide() const;
	HitResult const& getHitResult();
	int getDifficulty() const;
};

class EntityRenderDispatcher {
public:
	EntityRenderer* getRenderer(Entity&);
	static EntityRenderDispatcher& getInstance();
};

#include "mcpe/blockentity/chestblockentity.h"
#include "mcpe/blockentity/furnaceblockentity.h"
#include "mcpe/blockentity/mobspawnerblockentity.h"
#include "mcpe/blockentity/signblockentity.h"
