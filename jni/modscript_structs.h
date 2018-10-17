#pragma once
#ifdef __cplusplus
#include <memory>
#include <unordered_map>
#endif
class Actor;
class Player;
class BlockPos;
class BlockSource;
typedef Actor Entity;
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

class ActorUniqueID {
public:
	long long id;
	ActorUniqueID(long long id) : id(id) {
	}

	operator long long() const {
		return this->id;
	}
};

typedef ActorUniqueID EntityUniqueID;

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

enum ActorFlags {
	ActorFlagsImmobile = 16,
};
enum ArmorSlot {
};
class PlayerInventoryProxy;
class ActorDamageSource;
class AttributeInstance;
class Attribute;
class MobEffectInstance;
// last update: 1.2.20b1
class Actor {
public:
	void** vtable; //0
	char filler2[152-4]; // 4
	float pitch; //152 Entity::setRot
	float yaw; //156
	float prevPitch; //160
	float prevYaw; //164

	char filler4[300-168]; //168
	int renderType; //300
	char filler5[516-304]; // 304
	std::vector<Entity*> riders; // 516

	char filler3[3372-528]; // 528
	float motionX; // 3372 - Actor::push
	float motionY; // 3376
	float motionZ; // 3380
	float x; //3384 - Entity::setPos(Vec3 const&) or Actor.getPos
	float y; //3388
	float z; //3392

	~Actor();
	BlockSource* getRegion() const;
	void setRot(Vec2 const&);
	EntityUniqueID const& getUniqueID() const;
	void setNameTag(std::string const&);
	SynchedEntityData* getEntityData();
	EntityUniqueID const& getTargetId();
	void setTarget(Entity*);
	void setStatusFlag(ActorFlags, bool);
	Level* getLevel();
	AgeableComponent* getAgeableComponent() const;
	void hurt(ActorDamageSource const&, int, bool, bool);
	Entity* getRide() const;
	void setNameTagVisible(bool);
	void sendMotionPacketIfNeeded();
	void sendMotionToServer(bool);
	bool isAutoSendEnabled() const;
	void enableAutoSendPosRot(bool);
	void teleportTo(Vec3 const&, int, int);
	bool hasTeleported() const;
	ItemInstance* getOffhandSlot() const;
	int getHealth() const;
	ItemInstance* getArmor(ArmorSlot) const;
	AttributeInstance* getAttribute(Attribute const&) const;
	void setChanged();
	void addEffect(MobEffectInstance const&);
	void removeEffect(int);
	void removeAllEffects();
};
static_assert(offsetof(Entity, renderType) == 300, "renderType offset wrong");
// Entity::getRiderIndex
static_assert(offsetof(Entity, riders) == 516, "Entity rider offset wrong");
static_assert(offsetof(Actor, pitch) == 152, "Actor pitch offset wrong");
static_assert(offsetof(Actor, x) == 3384, "Actor x offset wrong");

class Mob: public Entity {
public:
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

// Updated 1.6.0b30
// see VanillaItems::initClientData; search for r2, #318 near it (is above it in b30)
class Item {
public:
	//void** vtable; //0
	char filler0[64-4]; //4
	short itemId; //64
	char filler1[83-66]; // 66
	bool handEquipped; // 83
	char filler3[136-84]; // 84
	virtual ~Item();

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
	static void initCreativeItems(bool, std::function<void ()>);

	static std::shared_ptr<TextureAtlas> mItemTextureAtlas;
};
static_assert(offsetof(Item, itemId) == 64, "Item ID offset");
static_assert(sizeof(Item) == 136, "item size is wrong");

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
	char filler4[128-114]; // 114
	unsigned short id; // 128
	char filler5[2184-130]; // 130

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
};
static_assert(sizeof(BlockLegacy) == 2184, "Block size is wrong");
static_assert(offsetof(BlockLegacy, renderLayer) == 16, "renderlayer is wrong");
static_assert(offsetof(BlockLegacy, explosionResistance) == 92, "explosionResistance is wrong");
static_assert(offsetof(BlockLegacy, lightEmission) == 113, "lightEmission is wrong");
static_assert(offsetof(BlockLegacy, id) == 128, "blockId is wrong");
#define Tile BlockLegacy

typedef struct {
	char filler0[188]; //0: from ModelPart::addBox or std::vector<Cube, std::allocator<Cube> >::_M_emplace_back_aux
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
/*
	float offsetX; //0
	float offsetY; //4
	float offsetZ; //8
	float rotateAngleX; // 12
	float rotateAngleY; // 16
*/
	char filler0[105-0]; // 0
	bool showModel; // 105 from HumanoidMobRenderer::prepareArmor or ModelPart::setVisible
	char filler1[108-106]; //102
	std::vector<Cube> cubeVector; // 108 - from ModelPart::addBox
	char filler3[132-120]; // 120
	//MaterialPtr* material; //88 from ModelPart::draw
	float textureWidth; //132
	float textureHeight; //136
	int textureOffsetX; // 140
	int textureOffsetY; // 144
	char filler2[416-148]; // 148

	void addBox(Vec3 const&, Vec3 const&, float, Color const& = Color{0, 0, 0, 0});
}; // 416 bytes
static_assert(offsetof(ModelPart, cubeVector) == 108, "modelpart cubeVector");
static_assert(offsetof(ModelPart, textureOffsetY) == 144, "modelpart textureOffsetY");
static_assert(sizeof(ModelPart) == 416, "modelpart size wrong");

namespace mce {
	class TexturePtr;
};
class GeometryPtr;
// from HumanoidModel::render
// last updated 1.1.3.1
class HumanoidModel {
public:
	char filler[25-4]; // 4
	bool riding; // 25, MobRenderer::renderModel
	char filler1[184-26]; // 26
/*
	MaterialPtr* activeMaterial; // 28
	MaterialPtr materialNormal; // 36
	MaterialPtr materialAlphaTest; // 48
	MaterialPtr materialAlphaBlend; // 60
	MaterialPtr materialStatic; // 72
	MaterialPtr materialEmissive; // 84
	MaterialPtr material96; // 96
*/
	ModelPart bipedHead;//184
	ModelPart bipedHeadwear;//600
	ModelPart bipedBody;//1016
	ModelPart bipedRightArm;//1432
	ModelPart bipedLeftArm;//1848
	ModelPart bipedRightLeg;//2264
	ModelPart bipedLeftLeg;//2680
	char filler3[8136-3096]; // 3096 - more model parts
	HumanoidModel(float, float, int, int);
	HumanoidModel(GeometryPtr const&);
	virtual ~HumanoidModel();
};

static_assert(sizeof(HumanoidModel) == 8136, "HumanoidModel size");
//static_assert(offsetof(HumanoidModel, activeTexture) == 32, "active texture");
//static_assert(offsetof(HumanoidModel, materialAlphaTest) == 48, "material alpha test");
static_assert(offsetof(HumanoidModel, bipedHead) == 184, "HumanoidModel bipedHead");
static_assert(offsetof(HumanoidModel, bipedLeftLeg) == 2680, "HumanodModel bipedLeftLeg");

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
	char filler[132-4]; //4
	void* model; // 132 (from ActorRenderer::ActorRenderer)
	char filler2[632-136]; // 136
	mce::TexturePtr const& getSkinPtr(Entity&) const;
};
static_assert(offsetof(MobRenderer, model) == 132, "mobrenderer model offset");
static_assert(sizeof(MobRenderer) == 632, "mobrenderer");

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
	cppstr name; //4 from Biome::setName
	char filler2[96-8]; //8
	int id; //96 from Biome::Biome
};
static_assert(offsetof(Biome, name) == 4, "Biome name");
static_assert(offsetof(Biome, id) == 96, "Biome ID");

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
// look for id #298 above VanillaItems::initClientData
struct ArmorItem : public Item {
	int armorType; // 136
	int damageReduceAmount; // 140
	int renderIndex; // 144
	void* armorMaterial; // 148
	char fillerendarmor[172-152]; // 152
};

#ifdef __arm__
static_assert(sizeof(ArmorItem) == 172, "armor item size");
#endif



struct HumanoidMobRenderer : public MobRenderer {
	int something; // 632
	HumanoidModel* modelArmor; // 636
	HumanoidModel* modelArmorChestplate; // 640
	char hmr_filler1[688-644]; // 644
	HumanoidMobRenderer(std::unique_ptr<HumanoidModel>, std::unique_ptr<HumanoidModel>,
		std::unique_ptr<HumanoidModel>, mce::TexturePtr, Vec2 const&, Vec3 const&);
};
#ifdef __arm__
static_assert(offsetof(HumanoidMobRenderer, modelArmor) == 636, "armour model offset");
static_assert(sizeof(HumanoidMobRenderer) == 688, "humanoid mob renderer size");
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
class BlockPalette;
class Level {
public:
	void** vtable; // 0
	char filler[40-4]; // 4
	std::vector<Player*> allPlayers; // 40

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
	Recipes* getRecipes() const;
	FurnaceRecipes* getFurnaceRecipes() const;
	BlockPalette* getGlobalBlockPalette() const;
};
// Level::getActivePlayerCount
static_assert(offsetof(Level, allPlayers) == 40, "allPlayers vec");

class MinecraftCommands;
class ServerLevel : public Level {
public:
	MinecraftCommands* getCommands();
};

class ActorRuntimeID {
public:
	long long id;
	ActorRuntimeID(long long id) : id(id) {
	}

	operator long long() const {
		return this->id;
	}
};

class MultiPlayerLevel : public Level {
public:
	void* putEntity(BlockSource&, ActorUniqueID, ActorRuntimeID, std::unique_ptr<Actor>);
};

class ActorRenderDispatcher {
public:
	EntityRenderer* getRenderer(Actor&);
};

void addItem(Player&, ItemInstance&);

#include "mcpe/blockentity/chestblockentity.h"
#include "mcpe/blockentity/furnaceblockentity.h"
#include "mcpe/blockentity/mobspawnerblockentity.h"
#include "mcpe/blockentity/signblockentity.h"
typedef BlockActor BlockEntity;
typedef ChestBlockActor ChestBlockEntity;
typedef FurnaceBlockActor FurnaceBlockEntity;
typedef MobSpawnerBlockActor MobSpawnerBlockEntity;
typedef SignBlockActor SignBlockEntity;
#include "mcpe/blocksource.h"
