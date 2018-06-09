#pragma once
#ifdef __cplusplus
#include <memory>
#include <unordered_map>
#endif
class Entity;
class Player;
class BlockPos;
class BlockSource;
#include "mcpe/minecraft.h"
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

class AgeableComponent;

enum EntityFlags {
	EntityFlagsImmobile = 16,
};
class PlayerInventoryProxy;
class EntityDamageSource;
// last update: 1.2.20b1
class Entity {
public:
	void** vtable; //0
	char filler3[136-4]; // 4
	float x; //136 - Entity::setPos(Vec3 const&)
	float y; //140
	float z; //144
	char filler2[172-148]; // 148
	float motionX; //172 found in Entity::rideTick(); should be set to 0 there
	float motionY; //176
	float motionZ; //180
	float pitch; //184 Entity::setRot
	float yaw; //188
	float prevPitch; //192
	float prevYaw; //196

	char filler4[340-200]; //200
	int renderType; //340
	char filler5[580-344]; // 328
	std::vector<Entity*> riders; // 580

	~Entity();
	BlockSource* getRegion() const;
	void setRot(Vec2 const&);
	EntityUniqueID const& getUniqueID() const;
	void setNameTag(std::string const&);
	SynchedEntityData* getEntityData();
	EntityUniqueID const& getTargetId();
	void setTarget(Entity*);
	void setStatusFlag(EntityFlags, bool);
	Level* getLevel();
	AgeableComponent* getAgeableComponent() const;
	void hurt(EntityDamageSource const&, int, bool, bool);
	Entity* getRide() const;
	void setNameTagVisible(bool);
	void sendMotionPacketIfNeeded();
	void sendMotionToServer(bool);
	bool isAutoSendEnabled() const;
	void enableAutoSendPosRot(bool);
	void teleportTo(Vec3 const&, int, int);
	bool hasTeleported() const;
};
static_assert(offsetof(Entity, renderType) == 340, "renderType offset wrong");
// Entity::getRiderIndex
static_assert(offsetof(Entity, riders) == 580, "Entity rider offset wrong");

class Mob: public Entity {
public:
	ItemInstance* getOffhandSlot() const;
};
enum ContainerID : unsigned char {
	ContainerIDInventory = 0,
};

struct PlayerInventorySlot {
	ContainerID containerID;
	int slot;
};

class PlayerInventoryProxy {
public:
	bool add(ItemInstance&, bool);
	void removeResource(ItemInstance const&, bool, bool, int);
	void clearSlot(int, ContainerID id=ContainerIDInventory);
	ItemInstance* getItem(int, ContainerID id=ContainerIDInventory) const;
	void setItem(int, ItemInstance const&, ContainerID=ContainerIDInventory);
	PlayerInventorySlot getSelectedSlot() const;
	void selectSlot(int, ContainerID=ContainerIDInventory);
};
class Player: public Mob {
public:
	int getScore();
	void addExperience(int);
	void addLevels(int);
	PlayerInventoryProxy* getSupplies() const;
};

class ServerPlayer : public Player {
public:
	void push(Vec3 const&);
	void sendInventory(bool);
};

struct TextureUVCoordinateSet {
	float bounds[4];
	float extra;
	unsigned short size[2]; // 20
	ResourceLocation location;
	TextureUVCoordinateSet(TextureUVCoordinateSet const& other) {
		*this = other; // yeah I know, bad memory management habit. Deal with it
	};
};
//static_assert(offsetof(TextureUVCoordinateSet, textureFile) == 24, "textureFile offset wrong");
static_assert(sizeof(TextureUVCoordinateSet) == 32, "TextureUVCoordinateSet size wrong");

namespace Json {
	class Value;
};
class TextureAtlas;

enum UseAnimation {
};

// Updated 1.2b11
// see _Z12registerItemI4ItemIRA11_KciEERT_DpOT0_
class Item {
public:
	//void** vtable; //0
	char filler0[18-4]; //4
	short itemId; //18
	char filler1[33-20]; // 20
	bool handEquipped; // 33
	char filler3[124-34]; // 34
	virtual ~Item();

	static std::unordered_map<std::string, std::pair<std::string, std::unique_ptr<Item>>> mItemLookupMap;
	// this one loads textures
	void initClient(Json::Value&, Json::Value&);
	// this one doesn't
	void initServer(Json::Value&);
	void setStackedByData(bool);
	bool isStackedByData() const;
	void setMaxDamage(int);
	int getMaxDamage() const;
	void setAllowOffhand(bool);
	void setUseAnimation(UseAnimation);
	static void initCreativeItems(bool);
	static Item* getItem(short);
	static Item* mExtraItems[512];

	static std::shared_ptr<TextureAtlas> mItemTextureAtlas;
};
static_assert(offsetof(Item, itemId) == 18, "Item ID offset");
static_assert(sizeof(Item) == 124, "item size is wrong");

class CompoundTag {
public:
	~CompoundTag();
};
class BlockLegacy;
class ItemInstance {
public:
	Item* item; // 0
	CompoundTag* tag; // 4
	short damage; // 8
	unsigned char count; //10
	char filler2[72-11]; // 11

	ItemInstance();
	ItemInstance(int, int, int);
	ItemInstance(ItemInstance const&);
	ItemInstance& operator=(ItemInstance const&);
	~ItemInstance();
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
	bool isArmorItem() const;
	UseAnimation getUseAnimation() const;
}; // see ItemInstance::fromTag for size
// or just use the shared_ptr constructor
// or look at ItemInstance::EMPTY_ITEM
static_assert(offsetof(ItemInstance, tag) == 4, "tag offset wrong");
static_assert(offsetof(ItemInstance, item) == 0, "item offset wrong");
static_assert(sizeof(ItemInstance) == 72, "ItemInstance wrong");

enum CreativeItemCategory {
};

enum BlockProperty {
	BlockPropertyOpaque = 32, // from _istransparent
};
class AABB;
class Block;
typedef Block BlockAndData;
// last updated 1.2.13 / WIP!
class BlockLegacy {
public:
	void** vtable; //0
	std::string nameId; // 4
	std::string mappingId; // 8
	char filler1[16-12]; // 12
	int renderLayer; //16
	char filler2[88-20]; // 20
	float destroyTime; //88
	float explosionResistance; //92
	char filler3[112-96]; // 96
	unsigned char lightOpacity; // 112 from BlockLegacy::setLightBlock
	unsigned char lightEmission; // 113 from BlockLegacy::setLightEmission
	char filler4[124-114]; // 114
	unsigned short id; // 124
	char filler5[784-126]; // 126

	float getDestroySpeed() const;
	float getFriction() const;
	void setFriction(float);
	void setSolid(bool);
	void setCategory(CreativeItemCategory);
	std::string const& getDescriptionId() const;
	int getRenderLayer() const;
	void* getMaterial() const;
	BlockAndData* getBlockStateFromLegacyData(unsigned char) const;
	AABB& getVisualShape(BlockAndData const&, AABB&, bool) const;

	static std::unordered_map<std::string, BlockLegacy*> mBlockLookupMap;
	static BlockLegacy* mBlocks[0x100];
	static bool mSolid[0x100];
	static float mTranslucency[0x100];
};
static_assert(sizeof(BlockLegacy) == 784, "Block size is wrong");
static_assert(offsetof(BlockLegacy, renderLayer) == 16, "renderlayer is wrong");
static_assert(offsetof(BlockLegacy, explosionResistance) == 92, "explosionResistance is wrong");
static_assert(offsetof(BlockLegacy, lightEmission) == 113, "lightEmission is wrong");
static_assert(offsetof(BlockLegacy, id) == 124, "blockId is wrong");
#define Tile BlockLegacy

typedef struct {
	char filler0[176]; //0: from ModelPart::addBox
} Cube;

typedef struct {
	char filler[12]; // 0
} MaterialPtr; // actually mce::MaterialPtr

struct Color {
	float r;
	float g;
	float b;
	float a;
};

// from ModelPart::setPos, ModelPart::setTexSize
class ModelPart {
public:
	float offsetX; //0
	float offsetY; //4
	float offsetZ; //8
	float rotateAngleX; // 12
	float rotateAngleY; // 16
	char filler0[53-20]; // 20
	bool showModel; // 53 from HumanoidMobRenderer::prepareArmor
	char filler1[80-54]; //54
	float textureWidth; //80
	float textureHeight; //84
	MaterialPtr* material; //88 from ModelPart::draw
	int textureOffsetX; // 92
	int textureOffsetY; // 96
	char filler2[232-100]; // 100

	void addBox(Vec3 const&, Vec3 const&, float, Color const& = Color{0, 0, 0, 0});
}; // 232 bytes
static_assert(sizeof(ModelPart) == 232, "modelpart size wrong");

namespace mce {
	class TexturePtr;
};
class GeometryPtr;
// from HumanoidModel::render
// last updated 1.1.3.1
class HumanoidModel {
public:
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
	ModelPart bipedHead;//96
	ModelPart bipedHeadwear;//328
	ModelPart bipedBody;//560
	ModelPart bipedRightArm;//792
	ModelPart bipedLeftArm;//1024
	ModelPart bipedRightLeg;//1256
	ModelPart bipedLeftLeg;//1488
	char filler3[4552-1720]; // 1720 - more model parts
	HumanoidModel(float, float, int, int);
	HumanoidModel(GeometryPtr const&);
	virtual ~HumanoidModel();
};

static_assert(sizeof(HumanoidModel) == 4552, "HumanoidModel size");
static_assert(offsetof(HumanoidModel, activeTexture) == 32, "active texture");
static_assert(offsetof(HumanoidModel, materialAlphaTest) == 48, "material alpha test");
static_assert(offsetof(HumanoidModel, bipedHead) == 96, "HumanoidModel bipedHead");
static_assert(offsetof(HumanoidModel, bipedLeftLeg) == 1488, "HumanodModel bipedLeftLeg");

class Recipe;
class ShapelessRecipe;
class ShapedRecipe;

class Recipes {
public:

class Type {
public:
	Item* item; //0
	Tile* tile; //4
	ItemInstance itemInstance; //8
	char letter; //80
#ifdef __arm__
	char filler[7]; // 81
#else
	char filler[3]; // 81
#endif
};
	std::vector<Recipe*> recipes;
	void addShapelessRecipe(ItemInstance const&, std::vector<Type> const&,
		std::function<std::unique_ptr<ShapelessRecipe>(std::vector<ItemInstance> const&, std::vector<ItemInstance> const&)>);
	void addShapedRecipe(std::vector<ItemInstance> const&, std::vector<std::string> const&, std::vector<Type> const&,
		std::function<std::unique_ptr<ShapedRecipe>(int, int, std::vector<ItemInstance> const&, std::vector<ItemInstance> const&)>);
}; // class Recipes

typedef Recipes::Type RecipesType;
// std::vector<Recipes::Type, std::allocator<Recipes::Type> > definition<ItemInstance>(char, ItemInstance)
static_assert(offsetof(RecipesType, letter) == 80, "RecipesType letter");
#ifdef __arm__
static_assert(sizeof(RecipesType) == 88, "RecipesType size");
#else
static_assert(sizeof(RecipesType) == 84, "RecipesType size");
#endif

class FurnaceRecipes {
public:
	void addFurnaceRecipe(Item const&, ItemInstance const&);
};

typedef struct {
	char filler[16]; //0
	char* text; //16;
} RakString;

typedef void EntityRenderer;

class MobRenderer {
public:
	void** vtable; //0
	char filler[164-4]; //4
	void* model; // 164 (from MobRenderer::MobRenderer)
	char filler2[668-168]; // 172
	mce::TexturePtr const& getSkinPtr(Entity&) const;
};
static_assert(offsetof(MobRenderer, model) == 164, "mobrenderer model offset");
static_assert(sizeof(MobRenderer) == 668, "mobrenderer");

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
	char filler0[24-0];
	int type; //24
	int side; //28
	int x; //32
	int y; //36
	int z; //40
	Vec3 hitVec; //44
	Entity* entity; //56
	unsigned char filler1[89-60]; //60
} HitResult;

class Biome {
public:
	void** vtable; //0
	char filler[200-4]; //4
	cppstr name; //200 from Biome::setName
	char filler2[300-204]; //204
	int id; //300 from Biome::Biome
};
static_assert(offsetof(Biome, name) == 200, "Biome name");
static_assert(offsetof(Biome, id) == 300, "Biome ID");

class Abilities {
public:
	bool getBool(std::string const& name) const;
	void setAbility(std::string const&, bool);
	static std::string FLYING;
	static std::string MAYFLY;
};

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

class AABB {
public:
	float x1; // 0
	float y1; // 4
	float z1; // 8
	float x2; // 12
	float y2; // 16
	float z2; // 20
	bool shouldBeFalse; // 24
};

typedef struct {
	unsigned char x;
	unsigned char z;
	unsigned char y;
} ChunkTilePos;

class LevelChunk;

typedef void ModelRenderer;

#ifdef __cplusplus
struct ArmorItem : public Item {
	int armorType; // 124
	int damageReduceAmount; // 128
	int renderIndex; // 132
	void* armorMaterial; // 136
	char fillerendarmor[160-140]; // 140
};

#ifdef __arm__
static_assert(sizeof(ArmorItem) == 160, "armor item size");
#endif



struct HumanoidMobRenderer : public MobRenderer {
	int something; // 668
	HumanoidModel* modelArmor; // 672
	HumanoidModel* modelArmorChestplate; // 676
	char hmr_filler1[724-680]; // 680
	HumanoidMobRenderer(std::unique_ptr<HumanoidModel>, std::unique_ptr<HumanoidModel>,
		std::unique_ptr<HumanoidModel>, mce::TexturePtr, Vec2 const&, Vec3 const&);
};
#ifdef __arm__
static_assert(offsetof(HumanoidMobRenderer, modelArmor) == 672, "armour model offset");
static_assert(sizeof(HumanoidMobRenderer) == 724, "humanoid mob renderer size");
#endif
#endif // ifdef __cplusplus

#ifdef __cplusplus
} // extern "C"
#endif

class LevelData {
public:
	void setSpawn(BlockPos const&);
	void setGameType(GameType);
	GameType getGameType() const;
};

class Spawner {
public:
	Entity* spawnItem(BlockSource&, ItemInstance const&, Entity*, Vec3 const&, int);
};

enum ParticleType {
};

class Level {
public:
	void** vtable; // 0
	char filler[28-4]; // 4
	std::vector<Player*> allPlayers; // 28

	Entity* fetchEntity(EntityUniqueID, bool) const;
	void addEntity(BlockSource&, std::unique_ptr<Entity>);
	void addGlobalEntity(BlockSource&, std::unique_ptr<Entity>);
	void explode(BlockSource&, Entity*, Vec3 const&, float, bool, bool, float, bool);
	void setNightMode(bool);
	void setTime(int);
	int getTime() const;
	LevelData* getLevelData();
	void playSound(Vec3 const&, std::string const&, float, float);
	bool isClientSide() const;
	HitResult const& getHitResult();
	int getDifficulty() const;
	Spawner* getSpawner() const;
	Player* getPlayer(EntityUniqueID) const;
	Player* getPrimaryLocalPlayer() const;
	void addParticle(ParticleType, Vec3 const&, Vec3 const&, int, CompoundTag const*, bool);
	Abilities* getPlayerAbilities(EntityUniqueID const&);
};
// Level::getActivePlayerCount
static_assert(offsetof(Level, allPlayers) == 28, "allPlayers vec");

class MinecraftCommands;
class ServerLevel : public Level {
public:
	MinecraftCommands* getCommands();
};

class EntityRuntimeID {
public:
	long long id;
	EntityRuntimeID(long long id) : id(id) {
	}

	operator long long() const {
		return this->id;
	}
};

class MultiPlayerLevel : public Level {
public:
	void* putEntity(BlockSource&, EntityUniqueID, EntityRuntimeID, std::unique_ptr<Entity>);
};

class EntityRenderDispatcher {
public:
	EntityRenderer* getRenderer(Entity&);
};

void addItem(Player&, ItemInstance&);

#include "mcpe/blockentity/chestblockentity.h"
#include "mcpe/blockentity/furnaceblockentity.h"
#include "mcpe/blockentity/mobspawnerblockentity.h"
#include "mcpe/blockentity/signblockentity.h"
#include "mcpe/blocksource.h"
