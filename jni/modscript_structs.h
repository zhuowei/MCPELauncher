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
#include "mcpe/hashstring.h"

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
// last update: 1.13.0b6
class Actor {
public:
	char filler2[176-4]; // 4
	float pitch; //176 Entity::setRot
	float yaw; //180
	float prevPitch; //184
	float prevYaw; //188

	char filler4[584-192]; //192
	std::vector<Entity*> riders; // 584

	char filler3[780-596]; // 596
	float x; //780 - Entity::setPos(Vec3 const&) or Actor.getPos
	float y; //784
	float z; //788
	// Actor::lerpMotion
	char filler5[804-792];
	float motionX; // 804 - Actor::push or PushableComponent::push
	float motionY; // 808
	float motionZ; // 812

	virtual ~Actor();
	BlockSource* getRegion() const;
	void setRot(Vec2 const&);
	EntityUniqueID const& getUniqueID() const;
	void setNameTag(std::string const&);
	SynchedEntityData* getEntityData();
	EntityUniqueID const& getTargetId();
	void setTarget(Entity*);
	void setStatusFlag(ActorFlags, bool);
	Level* getLevel();
	void hurt(ActorDamageSource const&, int, bool, bool);
	Entity* getRide() const;
	void setNameTagVisible(bool);
	void sendMotionPacketIfNeeded();
	void sendMotionToServer(bool);
	bool isAutoSendEnabled() const;
	void enableAutoSendPosRot(bool);
	void teleportTo(Vec3 const&, int, int);
	bool hasTeleported() const;
	ItemStack* getOffhandSlot() const;
	int getHealth() const;
	ItemStack* getArmor(ArmorSlot) const;
	AttributeInstance* getAttribute(Attribute const&) const;
	void setChanged();
	void addEffect(MobEffectInstance const&);
	void removeEffect(int);
	void removeAllEffects();
};
// Entity::getRiderIndex
static_assert(offsetof(Entity, riders) == 584, "Entity rider offset wrong");
static_assert(offsetof(Actor, pitch) == 176, "Actor pitch offset wrong");
static_assert(offsetof(Actor, x) == 780, "Actor x offset wrong");
static_assert(offsetof(Actor, motionX) == 804, "Actor motionX offset wrong");

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
class ItemStack;
class PlayerInventoryProxy {
public:
	bool add(ItemStack&, bool);
	void removeResource(ItemStack const&, bool, bool, int);
	void clearSlot(int, ContainerID id=ContainerIDInventory);
	ItemStack* getItem(int, ContainerID id=ContainerIDInventory) const;
	void setItem(int, ItemStack const&, ContainerID=ContainerIDInventory);
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
	char filler[8]; // 32 TextureUVCoordinateSet::TextureUVCoordinateSet
	TextureUVCoordinateSet(TextureUVCoordinateSet const& other) {
		*this = other; // yeah I know, bad memory management habit. Deal with it
	};
};
//static_assert(offsetof(TextureUVCoordinateSet, textureFile) == 24, "textureFile offset wrong");
static_assert(sizeof(TextureUVCoordinateSet) == 40, "TextureUVCoordinateSet size wrong");

namespace Json {
	class Value;
};
class TextureAtlas;

enum UseAnimation {
};

class ActorInfoRegistry;
class BlockDefinitionGroup;

// Updated 1.14.1
// see VanillaItems::initClientData; search for r2, #318 near it (is above it in b30)
// or  ItemRegistry::registerItemShared
class BaseGameVersion;
class Item {
public:
	//void** vtable; //0
	char filler0[64-4]; //4
	short itemId; //64
	char filler1[98-66]; // 66
	short flags; // 98
	char filler3[168-100]; // 100
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
	static void initCreativeItems(bool, ActorInfoRegistry*, BlockDefinitionGroup*, bool, BaseGameVersion const&, std::function<void (ActorInfoRegistry*, BlockDefinitionGroup*, bool)>);
};
static_assert(offsetof(Item, itemId) == 64, "Item ID offset");
static_assert(sizeof(Item) == 168, "item size is wrong");

class CompoundTag {
public:
	virtual ~CompoundTag();
};
class BlockLegacy;
class ItemStack;
class Block;
// 1.13.1
class ItemStackBase {
public:
	char filler1[16-4]; // 4
	short damage; // 16
	unsigned char count; //18
	char filler2[84-19]; // 19

	virtual ~ItemStackBase();
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
	void _setItem(int);
	int getDamageValue() const;
	void setBlock(Block const*);
	void setUserData(std::unique_ptr<CompoundTag> tag);
}; // see ItemInstance::fromTag for size
// or just use the shared_ptr constructor
// or look at ItemInstance::EMPTY_ITEM

class ItemInstance: public ItemStackBase {
public:
	ItemInstance();

	ItemInstance(int id, int count, int data) : ItemInstance() {
		init(id, count, data);
		_setItem(id);
		_bl_fixBlock(id, count, data);
	}
	ItemInstance(ItemStack const&);

	ItemInstance(ItemInstance const&);
	ItemInstance& operator=(ItemInstance const&);
	virtual ~ItemInstance();
	bool operator==(ItemInstance const&) const;
	bool operator!=(ItemInstance const&) const;
	void _bl_fixBlock(int id, int count, int data);
};

static_assert(offsetof(ItemInstance, count) == 18, "count wrong");
static_assert(sizeof(ItemInstance) == 84, "ItemInstance wrong");

enum CreativeItemCategory {
};

enum BlockProperty {
	BlockPropertyOpaque = 32, // from _istransparent
};
class AABB;
class Block;
typedef Block BlockAndData;
// last updated 1.14.1
class BlockLegacy {
public:
	void** vtable; //0
	std::string nameId; // 4
	std::string mappingId; // 8
	char filler1[24-12]; // 12
	int renderLayer; //24
	char filler2[96-28]; // 28
	float destroyTime; //96
	float explosionResistance; //100
	char filler3[120-104]; // 104
	unsigned char lightOpacity; // 120 from BlockLegacy::setLightBlock
	unsigned char lightEmission; // 121 from BlockLegacy::setLightEmission
	char filler4[136-122]; // 122
	unsigned short id; // 136
	char filler5[3192-138]; // 138

	float getDestroySpeed() const;
	float getFriction() const;
	void setFriction(float);
	void setSolid(bool);
	void setCategory(CreativeItemCategory);
	std::string const& getDescriptionId() const;
	int getRenderLayer() const;
	void* getMaterial() const;
	BlockAndData* getStateFromLegacyData(unsigned short) const;
	AABB& getVisualShape(BlockAndData const&, AABB&, bool) const;
};
// SharedPtr<BlockLegacy>::make
static_assert(sizeof(BlockLegacy) == 3192, "Block size is wrong");
static_assert(offsetof(BlockLegacy, renderLayer) == 24, "renderlayer is wrong");
static_assert(offsetof(BlockLegacy, explosionResistance) == 100, "explosionResistance is wrong");
static_assert(offsetof(BlockLegacy, lightEmission) == 121, "lightEmission is wrong");
static_assert(offsetof(BlockLegacy, id) == 136, "blockId is wrong");
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
// NOT updated 1.8
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
class ResourcePackManager;

class Recipes;
#if 0
// FIXME 1.13.1: Recipes::Type is now 28 bytes in 1.13.0.6 and probably 1.13.1
class Recipes {
public:

class Type {
public:
	Item* item; //0
	Tile* tile; //4
	ItemInstance itemInstance; //8
	char letter; //96
#ifdef __arm__
	char filler[7]; // 97
#else
	char filler[3]; // 97
#endif
};
	std::vector<Recipe*> recipes;
	void addShapelessRecipe(ItemInstance const&, std::vector<Type> const&, std::vector<Util::HashString> const&, int,
		std::function<std::unique_ptr<ShapelessRecipe>(std::vector<ItemInstance> const&, std::vector<ItemInstance> const&,
		Util::HashString)>);
	void addShapedRecipe(std::vector<ItemInstance> const&, std::vector<std::string> const&, std::vector<Type> const&,
		std::vector<Util::HashString> const&, int,
		std::function<std::unique_ptr<ShapedRecipe>(int, int, std::vector<ItemInstance> const&, std::vector<ItemInstance> const&,
		Util::HashString)>);
	void addFurnaceRecipeAuxData(short, short, ItemInstance const&, std::vector<Util::HashString> const&);
	void* loadRecipes(ResourcePackManager&);

}; // class Recipes

typedef Recipes::Type RecipesType;
// std::vector<Recipes::Type, std::allocator<Recipes::Type> > definition<ItemInstance>(char, ItemInstance)
static_assert(offsetof(RecipesType, letter) == 96, "RecipesType letter");
#ifdef __arm__
static_assert(sizeof(RecipesType) == 104, "RecipesType size");
#else
static_assert(sizeof(RecipesType) == 100, "RecipesType size");
#endif
#endif // if 0

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
	unsigned char side; //28
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
	char filler2[88-8]; //8
	int id; //88 from Biome::Biome
};
static_assert(offsetof(Biome, name) == 4, "Biome name");
static_assert(offsetof(Biome, id) == 88, "Biome ID");

enum AbilitiesIndex {
};

class Abilities {
public:
	bool getBool(AbilitiesIndex) const;
	void setAbility(AbilitiesIndex, bool);
	static AbilitiesIndex nameToAbilityIndex(std::string const&);
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
	int armorType; // 168
	int damageReduceAmount; // 172
	int renderIndex; // 176
	void* armorMaterial; // 180
	char fillerendarmor[200-184]; // 184
};

#ifdef __arm__
static_assert(sizeof(ArmorItem) == 200, "armor item size");
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
	void setSpawnPos(BlockPos const&);
	void setGameType(GameType);
	GameType getGameType() const;
};

class Spawner {
public:
	Entity* spawnItem(BlockSource&, ItemStack const&, Entity*, Vec3 const&, int);
};

enum ParticleType {
};
class BlockPalette;
class Level {
public:
	void** vtable; // 0
	char filler[44-4]; // 4
	std::vector<Player*> allPlayers; // 44

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
	BlockPalette* getGlobalBlockPalette() const;
};
// Level::getActivePlayerCount
static_assert(offsetof(Level, allPlayers) == 44, "allPlayers vec");

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
