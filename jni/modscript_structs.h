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
#include "mcpe/resourcelocation.h"

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

// last update: 0.15.1
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

	char filler4[264-72]; //72
	int renderType; //264
	char filler5[292-268]; // 268
	struct Entity* rider; //292
	struct Entity* riding; //296 from Entity::getRide

	BlockSource* getRegion() const;
	void setRot(Vec2 const&);
	EntityUniqueID const& getUniqueID() const;
	void setNameTag(std::string const&);
	SynchedEntityData* getEntityData();
};
static_assert(offsetof(Entity, renderType) == 264, "renderType offset wrong");

class Mob: public Entity {
public:
	Mob* getTarget();
	void setTarget(Mob*);
};

class Player: public Mob {
public:
	int getScore();
	void addExperience(int);
	void addLevels(int);
};

struct TextureUVCoordinateSet {
	float bounds[4];
	unsigned short size[2]; // 16
	ResourceLocation location;
	TextureUVCoordinateSet(TextureUVCoordinateSet const& other) {
		*this = other; // yeah I know, bad memory management habit. Deal with it
	};
};
//static_assert(offsetof(TextureUVCoordinateSet, textureFile) == 24, "textureFile offset wrong");

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
	char filler1[27-20]; // 20
	bool handEquipped; // 27
	char filler[37-28]; //28
	unsigned char useAnimation; // 37
	char filler3[68-38]; // 38
	virtual ~Item();

	static std::unordered_map<std::string, std::pair<std::string, std::unique_ptr<Item>>> mItemLookupMap;
	void init(Json::Value&);
	void setStackedByData(bool);
	bool isStackedByData() const;
	void setMaxDamage(int);
	int getMaxDamage();
};
static_assert(sizeof(Item) == 68, "item size is wrong");

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
	int getMaxStackSize() const;
	void remove(int);
}; // see ItemInstance::fromTag for size
// or just use the shared_ptr constructor
static_assert(offsetof(ItemInstance, tag) == 8, "tag offset wrong");
static_assert(offsetof(ItemInstance, item) == 12, "item offset wrong");
static_assert(sizeof(ItemInstance) == 20, "ItemInstance wrong");

enum CreativeItemCategory {
};

class Block {
public:
	void** vtable; //0
	unsigned char id; // 4
	char filler0[20-5]; //5
	int renderLayer; //20
	char filler2[52-24]; //24
	void* material; //52
	char filler3[112-56]; // 56
	float destroyTime; //80
	float explosionResistance; //84

	float getDestroySpeed();
	float getFriction();
	void setFriction(float);
	void setSolid(bool);
	void setCategory(CreativeItemCategory);
	static std::unordered_map<std::string, Block*> mBlockLookupMap;
};

static_assert(offsetof(Block, renderLayer) == 20, "renderlayer is wrong");
#define Tile Block

typedef struct {
	char filler0[664]; //0
} Cube;

typedef struct {
	char filler[12]; // 0
} MaterialPtr; // actually mce::MaterialPtr

// from ModelPart::setPos, ModelPart::setTexSize
class ModelPart {
public:
	float offsetX; //0
	float offsetY; //4
	float offsetZ; //8
	float rotateAngleX; // 12
	float rotateAngleY; // 16
	char filler0[37-20]; // 20
	bool showModel; // 37 from HumanoidMobRenderer::prepareArmor
	char filler1[64-38]; //38
	float textureWidth; //64
	float textureHeight; //68
	MaterialPtr* material; //72 from ModelPart::draw
	int textureOffsetX; // 76
	int textureOffsetY; // 80
	char filler2[184-84]; // 84

	void addBox(Vec3 const&, Vec3 const&, float);
}; // 184 bytes
static_assert(sizeof(ModelPart) == 184, "modelpart size wrong");

namespace mce {
	class TexturePtr;
};

// from HumanoidModel::render

class HumanoidModel {
public:
	void** vtable; //0
	char filler[25-4]; // 4
	bool riding; // 25
	char filler1[28-26]; // 26
	MaterialPtr* activeMaterial; // 28
	mce::TexturePtr* activeTexture; // 32 from MobRenderer::renderModel
	MaterialPtr materialNormal; // 36
	MaterialPtr materialAlphaTest; // 48
	MaterialPtr materialAlphaBlend; // 60
	MaterialPtr materialStatic; // 72
	MaterialPtr materialEmissive; // 84
	MaterialPtr materialEmissiveAlpha; // 96
	MaterialPtr materialChangeColor; // 108
	MaterialPtr materialGlint; // 120
	MaterialPtr materialAlphaTestGlint; // 132
	MaterialPtr materialChargedCreeper; // 144
	MaterialPtr materialAlphaTestChangeColor; // 156
	MaterialPtr materialAlphaTestChangeColorGlint; // 168
	MaterialPtr materialMultitexture; // 180
	MaterialPtr materialMultitextureColorMask; // 192
	MaterialPtr materialMultitextureAlphaTest; // 204
	MaterialPtr materialMultitextureAlphaTestColorMask; // 216
	char filler2[240-228]; // 228
	ModelPart bipedHead;//240
	ModelPart bipedHeadwear;//424
	ModelPart bipedBody;//608
	ModelPart bipedRightArm;//792
	ModelPart bipedLeftArm;//976
	ModelPart bipedRightLeg;//1160
	ModelPart bipedLeftLeg;//1344
	char mystery2[2]; // 1528
	char filler3[1540-1530]; // 1530
	HumanoidModel(float, float, int, int);
};

static_assert(sizeof(HumanoidModel) == 1540, "HumanoidModel size");
static_assert(offsetof(HumanoidModel, activeTexture) == 32, "active texture");
static_assert(offsetof(HumanoidModel, materialAlphaTest) == 48, "material alpha test");
static_assert(offsetof(HumanoidModel, bipedHead) == 240, "HumanoidModel bipedHead");

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

class MobRenderer {
public:
	void** vtable; //0
	char filler[132-4]; //4
	void* model; // 132 (from MobRenderer::MobRenderer)
	char filler2[612-136]; // 136
	mce::TexturePtr const& getSkinPtr(Entity&) const;
};
static_assert(sizeof(MobRenderer) == 612, "mobrenderer");

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
	int something; // 612
	HumanoidModel* modelArmor; // 616
	HumanoidModel* modelArmorChestplate; // 620
};
#ifdef __arm__
static_assert(offsetof(HumanoidMobRenderer, modelArmor) == 616, "armour model offset");
#endif
#endif // ifdef __cplusplus

#ifdef __cplusplus
} // extern "C"
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
	void** vtable;

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
