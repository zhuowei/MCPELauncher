#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <typeinfo>
#include <map>
#include <unordered_map>
#include <array>
#include <sstream>

#include "utf8proc.h"

#include "json/value.h"
#include "json/reader.h"

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"
#include "dobby_public.h"
#include "simpleuuid.h"
#include "logutil.h"

#define cppbool bool

#include "modscript_shared.h"

#include "minecraft_colors.h"

#include "mcpe/i18n.h"
#include "mcpe/entitydamagesource.h"
#include "mcpe/mobfactory.h"
#include "mcpe/synchedentitydata.h"
#include "mcpe/mobeffect.h"
#include "mcpe/packetsender.h"
#include "mcpe/servercommandparser.h"
#include "mcpe/attribute.h"
#include "mcpe/weather.h"
#include "mcpe/dimension.h"
#include "mcpe/util.h"
#include "mcpe/itemrenderer.h"
#include "mcpe/mce/textureptr.h"
#include "mcpe/circuit.h"
#include "mcpe/enchant.h"
#include "mcpe/inventory.h"
#include "mcpe/options.h"
#include "mcpe/screenchooser.h"
#include "mcpe/guidata.h"
#include "mcpe/blockgraphics.h"
#include "mcpe/minecraftcommands.h"
#include "mcpe/gameconnectioninfo.h"
#include "mcpe/textureatlas.h"
#include "mcpe/particle.h"

typedef void RakNetInstance;
typedef void Font;

// FIXME 0.11
//#define MINECRAFT_RAKNET_INSTANCE_OFFSET 76
//#define BLOCK_VTABLE_GET_AABB 14
// found in LocalPlayer::displayClientMessage, also before the first call to Gui constructor
//#define MINECRAFT_GUI_OFFSET 252
//#define MOB_TARGET_OFFSET 3156

// found in GameMode::initPlayer
// or look for Abilities::Abilities
#define PLAYER_ABILITIES_OFFSET 3776
// FIXME 0.11
//#define RAKNET_INSTANCE_VTABLE_OFFSET_SEND 15
// MinecraftClient::handleBack
// FIXME 0.14
//#define MINECRAFT_SCREENCHOOSER_OFFSET 252

// found in _Z13registerBlockI5BlockIRA8_KciS3_RK8MaterialEERT_DpOT0_; tile id 4
const size_t kTileSize = 120;
const size_t kLiquidBlockDynamicSize = 120;
const size_t kLiquidBlockStaticSize = 124;
// found in registerBlock
const size_t kBlockItemSize = 108;
// found in _Z12registerItemI4ItemIRA11_KciEERT_DpOT0_
const size_t kItemSize = 108;
// found in ItemEntity::_validateItem
const size_t kItemEntity_itemInstance_offset = 3244;
// found in TextPacket::handle
const size_t kClientNetworkHandler_vtable_offset_handleTextPacket = 17;
// ProjectileComponent::throwableHit
const size_t kProjectileComponent_entity_offset = 16;

static const char* const listOfRenderersToPatchTextures[] = {
"_ZTV11BatRenderer",
"_ZTV11MobRenderer",
"_ZTV11NpcRenderer",
"_ZTV11PigRenderer",
"_ZTV12WolfRenderer",
"_ZTV13BlazeRenderer",
"_ZTV13GhastRenderer",
"_ZTV13HorseRenderer",
"_ZTV13SheepRenderer",
"_ZTV13SlimeRenderer",
"_ZTV13SquidRenderer",
"_ZTV13StrayRenderer",
"_ZTV13WitchRenderer",
"_ZTV14OcelotRenderer",
"_ZTV14RabbitRenderer",
"_ZTV14SpiderRenderer",
"_ZTV15ChickenRenderer",
"_ZTV15CreeperRenderer",
"_ZTV15ShulkerRenderer",
"_ZTV16EnderManRenderer",
"_ZTV16GuardianRenderer",
"_ZTV16SkeletonRenderer",
"_ZTV16VillagerRenderer",
"_ZTV17EndermiteRenderer",
"_ZTV17IronGolemRenderer",
"_ZTV17LavaSlimeRenderer",
"_ZTV17PolarBearRenderer",
"_ZTV17SnowGolemRenderer",
"_ZTV18SilverfishRenderer",
"_ZTV18WitherBossRenderer",
"_ZTV19EnderDragonRenderer",
"_ZTV19HumanoidMobRenderer",
"_ZTV19MushroomCowRenderer",
"_ZTV22VillagerZombieRenderer"
};

#define AXIS_X 0
#define AXIS_Y 1
#define AXIS_Z 2

#define BLOCK_ID (0x10 + 0)
#define BLOCK_DATA (0x10 + 1)
#define BLOCK_SIDE (0x10 + 2)

#define TEXT_BOLD 1

typedef struct {
	//union {
	//	std::map<const int, int> pack;
		char filler[24];
	//}
} ItemPack;

class Recipe {
public:
	void** vtable; //4
	ItemPack itemPack; //4
	static bool isAnyAuxValue(int);
};

class Recipes {
public:
	std::vector<Recipe*> recipes;
};

typedef struct {
	void** vtable;//0
	ItemPack itemPack; //4
	std::vector<ItemInstance> output; //28
	int filler; //48
} ShapelessRecipe; //52 bytes long

typedef struct {
	void** vtable;//0
	ItemPack itemPack; //4
	char filler2[12]; //28
	ItemInstance* output; //40
	int filler; //48
} ShapedRecipe; //52 bytes long

class Packet {
public:
	virtual ~Packet();
};

// fixme 0.15: size?

class TextPacket : public Packet {
public:
	char filler[8-4]; // 4
	int shouldBeOne; // 8
	unsigned char type; //12
	char filler2[16-14]; // 14
	std::string username; // 16
	std::string message; // 20
	//std::vector<std::string> thevector; // 24
	//char filler3[36-28]; // 28
	char filler3[36-24]; // 24
	virtual ~TextPacket() override;
};

typedef struct {
	void** vtable; //0
	void* filler[2]; //4
	std::string message; //12
} ChatPacket;

typedef struct {
	void** vtable; //0
	void* filler[2]; //4
	int time;
	char started;
} SetTimePacket;

typedef struct {
	void** vtable; //0
	void* filler[2]; //4
	RakString* sender; //12
	RakString* message; //16
} MessagePacket;

class BaseMobSpawner {
public:
	int getSpawnTypeId() const;
};

struct bl_vtable_indexes_nextgen_cpp {
	int tile_get_second_part;
	int tile_vtable_size;
	int blockgraphics_vtable_size;
	int blockgraphics_get_carried_texture;
	int tile_get_color;
	int tile_get_color_data;
	int tile_get_visual_shape;
	//int raknet_instance_connect;
	int mobrenderer_get_skin_ptr;
	int tile_on_redstone_update;
	int tile_is_redstone_block;
	int tile_on_loaded;
	int tile_on_place;
	int mob_set_sneaking;
	int blockitem_vtable_size;
	int blockitem_get_level_data_for_aux_value;
	int item_vtable_size;
	int item_get_enchant_slot;
	int item_get_enchant_value;
	int level_set_difficulty;
	int appplatform_get_ui_scaling_rules;
	int appplatform_get_platform_type;
	int appplatform_get_edition;
	int appplatform_use_centered_gui;
//	int appplatform_use_metadata_driven_screens;
//	int entity_hurt;
	int mobrenderer_render;
	int snowball_item_vtable_size;
	int item_use;
	int item_dispense;
	int clientnetworkhandler_handle_text_packet;
};

static bl_vtable_indexes_nextgen_cpp vtable_indexes;

static void populate_vtable_indexes(void* mcpelibhandle) {
	vtable_indexes.tile_get_second_part = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block13getSecondPartER11BlockSourceRK8BlockPosRS2_");
	vtable_indexes.tile_vtable_size = dobby_elfsym(mcpelibhandle, "_ZTV5Block")->st_size;
	vtable_indexes.blockgraphics_vtable_size = dobby_elfsym(mcpelibhandle, "_ZTV13BlockGraphics")->st_size;
	vtable_indexes.blockgraphics_get_carried_texture = bl_vtableIndex(mcpelibhandle, "_ZTV13BlockGraphics",
		"_ZNK13BlockGraphics17getCarriedTextureEai");
	vtable_indexes.tile_get_color = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block8getColorER11BlockSourceRK8BlockPos");
	vtable_indexes.tile_get_color_data = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block8getColorEi");
	vtable_indexes.tile_get_visual_shape = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block14getVisualShapeEhR4AABBb");
	//vtable_indexes.raknet_instance_connect = bl_vtableIndex(mcpelibhandle, "_ZTV14RakNetInstance",
	//	"_ZN14RakNetInstance7connectEPKci");
	vtable_indexes.mobrenderer_get_skin_ptr = bl_vtableIndex(mcpelibhandle, "_ZTV11MobRenderer",
		"_ZNK11MobRenderer10getSkinPtrER6Entity");
	vtable_indexes.tile_on_redstone_update = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block16onRedstoneUpdateER11BlockSourceRK8BlockPosib");
	vtable_indexes.tile_is_redstone_block = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block15isRedstoneBlockEv");
	vtable_indexes.tile_on_loaded = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block8onLoadedER11BlockSourceRK8BlockPos");
	vtable_indexes.tile_on_place = bl_vtableIndex(mcpelibhandle, "_ZTV5Block",
		"_ZNK5Block7onPlaceER11BlockSourceRK8BlockPos");
	vtable_indexes.mob_set_sneaking = bl_vtableIndex(mcpelibhandle, "_ZTV3Mob",
		"_ZN3Mob11setSneakingEb") - 2;
	vtable_indexes.blockitem_vtable_size = dobby_elfsym(mcpelibhandle, "_ZTV9BlockItem")->st_size;
	vtable_indexes.blockitem_get_level_data_for_aux_value = bl_vtableIndex(mcpelibhandle, "_ZTV9BlockItem",
		"_ZNK4Item23getLevelDataForAuxValueEi");
	vtable_indexes.item_vtable_size = dobby_elfsym(mcpelibhandle, "_ZTV4Item")->st_size;
	vtable_indexes.item_get_enchant_slot = bl_vtableIndex(mcpelibhandle, "_ZTV4Item",
		"_ZNK4Item14getEnchantSlotEv");
	vtable_indexes.item_get_enchant_value = bl_vtableIndex(mcpelibhandle, "_ZTV4Item",
		"_ZNK4Item15getEnchantValueEv");
	vtable_indexes.level_set_difficulty = bl_vtableIndex(mcpelibhandle, "_ZTV5Level",
		"_ZN5Level13setDifficultyE10Difficulty");
	vtable_indexes.appplatform_get_ui_scaling_rules = bl_vtableIndex(mcpelibhandle, "_ZTV21AppPlatform_android23",
		"_ZNK19AppPlatform_android25getPlatformUIScalingRulesEv");
	vtable_indexes.appplatform_get_platform_type = bl_vtableIndex(mcpelibhandle, "_ZTV21AppPlatform_android23",
		"_ZNK19AppPlatform_android15getPlatformTypeEv");
	vtable_indexes.appplatform_get_edition = bl_vtableIndex(mcpelibhandle, "_ZTV21AppPlatform_android23",
		"_ZNK11AppPlatform10getEditionEv");
	vtable_indexes.appplatform_use_centered_gui = bl_vtableIndex(mcpelibhandle, "_ZTV21AppPlatform_android23",
		"_ZNK11AppPlatform14useCenteredGUIEv");
//	vtable_indexes.appplatform_use_metadata_driven_screens = bl_vtableIndex(mcpelibhandle, "_ZTV21AppPlatform_android23",
//		"_ZNK19AppPlatform_android24useMetadataDrivenScreensEv");
//	vtable_indexes.entity_hurt = bl_vtableIndex(mcpelibhandle, "_ZTV6Entity",
//		"_ZN6Entity4hurtERK18EntityDamageSourceibb");
	vtable_indexes.mobrenderer_render = bl_vtableIndex(mcpelibhandle, "_ZTV11MobRenderer",
		"_ZN11MobRenderer6renderER6EntityRK4Vec3ff");
	vtable_indexes.snowball_item_vtable_size = dobby_elfsym(mcpelibhandle, "_ZTV12SnowballItem")->st_size;
	vtable_indexes.item_use = bl_vtableIndex(mcpelibhandle, "_ZTV4Item",
		"_ZN4Item3useER12ItemInstanceR6Player");
	vtable_indexes.item_dispense = bl_vtableIndex(mcpelibhandle, "_ZTV4Item",
		"_ZN4Item8dispenseER11BlockSourceR9ContaineriRK4Vec3a");
	vtable_indexes.clientnetworkhandler_handle_text_packet = bl_vtableIndex(mcpelibhandle, "_ZTV20ClientNetworkHandler",
		"_ZN20ClientNetworkHandler6handleERK17NetworkIdentifierRK10TextPacket");
}

template <typename T>
std::string bl_to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}

bool bl_setArmorTexture(int, std::string const&);

extern "C" {

static void (*bl_MinecraftScreenModel_sendChatMessage_real)(MinecraftScreenModel*, std::string const&);
static void (*bl_MinecraftScreenModel_executeCommand_real)(MinecraftScreenModel*, std::string const&);

static void (*bl_Item_Item)(Item*, std::string const&, short);

static void** bl_Item_vtable;
static void** bl_Tile_vtable;
static void** bl_BlockGraphics_vtable;
static void** bl_BlockItem_vtable;
static void** bl_CustomBlockItem_vtable;
static Block** bl_Block_mBlocks;
static unsigned char* bl_Block_mLightEmission;
static unsigned char* bl_Block_mLightBlock;

static void (*bl_MinecraftClient_startLocalServer)(MinecraftClient*, std::string const&, std::string const&, void*);

static void (*bl_MinecraftClient_leaveGame)(MinecraftClient*, bool saveWorld);
static void (*bl_Minecraft_setLeaveGame)(Minecraft*);

//static void* (*bl_Level_getAllEntities)(Level*);

//static void (*bl_Level_addListener)(Level*, LevelListener*);

static void (*bl_RakNetInstance_connect_real)(RakNetInstance*, Social::GameConnectionInfo, Social::GameConnectionInfo);

#if 0

static void (*bl_Font_drawCached_real)(Font*, std::string const&, float, float, Color const&, bool, MaterialPtr*);

static int (*bl_Font_width)(Font*, std::string const&);

#endif

static void (*bl_Block_Block)(Block*, std::string const&, int, void*);
static void (*bl_BlockItem_BlockItem)(Item*, std::string const&, short);
static void (*bl_Block_setVisualShape)(Block*, Vec3 const&, Vec3 const&);
static void (*bl_Mob_setSneaking)(Entity*, bool);
static bool (*bl_Mob_isSneaking)(Entity*);

static void (*bl_Item_setIcon)(Item*, std::string const&, int);
static void (*bl_Item_setMaxStackSize)(Item*, unsigned char);

static void (*bl_Item_setMaxDamage)(Item*, int);

static Recipes* (*bl_Recipes_getInstance)();
static void (*bl_Recipes_addShapedRecipe)(Recipes*, std::vector<ItemInstance> const&, std::vector<std::string> const&, 
	std::vector<RecipesType> const&);
static FurnaceRecipes* (*bl_FurnaceRecipes_getInstance)();
static void (*bl_FurnaceRecipes_addFurnaceRecipe)(FurnaceRecipes*, int, ItemInstance const&);

static void** bl_ShapelessRecipe_vtable;

static void (*bl_RakNetInstance_send)(void*, void*);
static void** bl_SetTimePacket_vtable;
//static void (*bl_Packet_Packet)(void*);
static void (*bl_ClientNetworkHandler_handleTextPacket_real)(void*, void*, TextPacket*);
static void** bl_MessagePacket_vtable;

bool bl_text_parse_color_codes = true;

//custom blocks
void** bl_CustomBlock_vtable;
void** bl_CustomBlockGraphics_vtable;
void** bl_CustomLiquidBlockStatic_vtable;
void** bl_CustomLiquidBlockDynamic_vtable;
TextureUVCoordinateSet** bl_custom_block_textures[256];
bool bl_custom_block_opaque[256];
bool bl_custom_block_collisionDisabled[256];
int* bl_custom_block_colors[256];
AABB** bl_custom_block_visualShapes[256];
//end custom blocks
unsigned char bl_custom_block_redstone[256];

std::vector<short*> bl_creativeItems;

char bl_stonecutter_status[BL_ITEMS_EXPANDED_COUNT];

static Item** bl_Item_mItems;
#if 0
static void (*bl_CompoundTag_putString)(void*, std::string, std::string);
static std::string (*bl_CompoundTag_getString)(void*, std::string);
static void (*bl_CompoundTag_putLong)(void*, std::string const&, long long);
static int64_t (*bl_CompoundTag_getLong)(void*, std::string const&);
//static Tag* (*bl_CompoundTag_get)(void*, std::string const&);
static void (*bl_Entity_saveWithoutId_real)(Entity*, void*);
static int (*bl_Entity_load_real)(Entity*, void*);
#endif

//static std::map<int, std::array<unsigned char, 16> > bl_entityUUIDMap;

static void (*bl_Level_addParticle)(Level*, int, Vec3 const&, Vec3 const&, int);
static void (*bl_MinecraftClient_setScreen)(Minecraft*, void*);
static void (*bl_ProgressScreen_ProgressScreen)(void*);
static void (*bl_Minecraft_locateMultiplayer)(Minecraft*);
static bool (*bl_Level_addEntity_real)(Level*, BlockSource&, std::unique_ptr<Entity>);
static bool (*bl_MultiPlayerLevel_addEntity_real)(Level*, BlockSource&, std::unique_ptr<Entity>);
static bool (*bl_Level_addPlayer_real)(Level*, std::unique_ptr<Player>);
static void (*bl_Level_removeEntity_real)(Level*, std::unique_ptr<Entity>&&, bool);
static void (*bl_Level_explode_real)(Level*, TileSource*, Entity*, Vec3 const&, float, bool, bool, float);
static void (*bl_BlockSource_fireBlockEvent_real)(BlockSource* source, int x, int y, int z, int type, int data);
static AABB* (*bl_Block_getAABB)(Block*, BlockSource&, BlockPos const&, AABB&, int, bool, int);
static AABB* (*bl_ReedBlock_getAABB)(Block*, BlockSource&, BlockPos const&, AABB&, int, bool, int);

static void (*bl_LevelChunk_setBiome)(LevelChunk*, Biome const&, ChunkTilePos const&);
static Biome* (*bl_Biome_getBiome)(int);
static void (*bl_Entity_setSize)(Entity*, float, float);
static void (*bl_BaseMobSpawner_setEntityId)(BaseMobSpawner*, int);
static void (*bl_ArmorItem_ArmorItem)(ArmorItem*, std::string const&, int, void*, int, int);
static void (*bl_ScreenChooser_setScreen)(ScreenChooser*, int);
static void (*bl_Minecraft_hostMultiplayer)(Minecraft* minecraft, int port);
static void (*bl_Mob_die_real)(Entity*, EntityDamageSource const&);
static bool bl_forceController = false;
static bool (*bl_MinecraftClient_useController)(Minecraft*);
static std::string* (*bl_Entity_getNameTag)(Entity*);
static void (*bl_Item_addCreativeItem)(short, short);
static PacketSender* (*bl_Minecraft_getPacketSender)(Minecraft*);
static int (*bl_Mob_getHealth)(Entity*);
static AttributeInstance* (*bl_Mob_getAttribute)(Entity*, Attribute const&);
static void (*bl_Player_eat_real)(Entity*, int, float);
static int (*bl_Entity_getDimensionId)(Entity*);
static AABB& (*bl_Tile_getVisualShape)(Tile*, unsigned char, AABB&, bool);
static Attribute* bl_Player_HUNGER;
static Attribute* bl_Player_EXHAUSTION;
static Attribute* bl_Player_SATURATION;
static Attribute* bl_Player_LEVEL;
static Attribute* bl_Player_EXPERIENCE;
static void (*bl_Player_addExperience_real)(Player*, int);
static void (*bl_Player_addLevels_real)(Player*, int);
static void (*bl_Item_setCategory)(Item*, int);
static void (*bl_Item_initCreativeItems_real)();
static mce::TexturePtr const& (*bl_ItemRenderer_getGraphics_real)(ItemInstance const&);
static mce::TexturePtr const& (*bl_ItemRenderer_getGraphics_real_item)(Item*);
static mce::TexturePtr const& (*bl_MobRenderer_getSkinPtr_real)(MobRenderer* renderer, Entity& ent);
static void* (*bl_MobRenderer_render_real)(MobRenderer*, Entity&, Vec3 const&, float, float);
static void* (*bl_SkeletonRenderer_render_real)(MobRenderer*, Entity&, Vec3 const&, float, float);
static void (*bl_Block_onPlace)(Block*, BlockSource&, BlockPos const&);

static bool* bl_Block_mSolid;
static void** bl_LiquidBlockStatic_vtable;
static void** bl_LiquidBlockDynamic_vtable;
static void (*bl_LiquidBlockStatic_LiquidBlockStatic)
	(Block*, std::string const&, int, BlockID, void*, std::string const&, std::string const&);
static void (*bl_LiquidBlockDynamic_LiquidBlockDynamic)
	(Block*, std::string const&, int, void*, std::string const&, std::string const&);
static bool (*bl_Recipe_isAnyAuxValue_real)(int id);
static void (*bl_TntBlock_onLoaded)(Block*, BlockSource&, BlockPos const&);
static void*(*bl_MinecraftTelemetry_fireEventScreenChanged_real)(void*, std::string const&, std::unordered_map<std::string, std::string> const&);
static TextureUVCoordinateSet* (*bl_BlockGraphics_getTexture_real)(BlockGraphics*, signed char, int);
static TextureUVCoordinateSet* (*bl_BlockGraphics_getTexture_char_real)(BlockGraphics*, signed char);

#define STONECUTTER_STATUS_DEFAULT 0
#define STONECUTTER_STATUS_FORCE_FALSE 1
#define STONECUTTER_STATUS_FORCE_TRUE 2

#define ITEMID 0
#define DAMAGE 1
#define AMOUNT 2

#ifdef DLSYM_DEBUG

void* debug_dlsym(void* handle, const char* symbol);

#define dlsym debug_dlsym
#endif //DLSYM_DEBUG

void bl_forceTextureLoad(std::string const&);
void bl_dumpVtable(void**, size_t);
void bl_set_i18n(std::string const&, std::string const&);
static bool isLocalAddress(JNIEnv* env, jstring addr);
void* bl_marauder_translation_function(void* input);

__attribute__((__visibility__("hidden")))
bool bl_onLockDown = false;

int bl_item_id_count = 512;
Item* bl_items[BL_ITEMS_EXPANDED_COUNT];
unsigned char bl_anyAuxValue[BL_ITEMS_EXPANDED_COUNT];
void** bl_CustomItem_vtable;
static void** bl_CustomSnowballItem_vtable;
static void** bl_SnowballItem_vtable;
unsigned short bl_customItem_allowEnchantments[BL_ITEMS_EXPANDED_COUNT];
int bl_customItem_enchantValue[BL_ITEMS_EXPANDED_COUNT];
// was a new custom block added
static bool bl_customBlocksCreated = false;

enum CustomBlockRedstoneType {
	// this is a bitfield
	REDSTONE_CONSUMER = (1 << 0),
};

static void dumpEnchantNames() {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "enchant dump: %d", Enchant::mEnchants.size());
	for (Enchant* enchant: Enchant::mEnchants) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "enchant %d %s", enchant->id, enchant->description.c_str());
	}
}

Entity* bl_getEntityWrapper(Level* level, long long entityId) {
	if (bl_removedEntity != NULL && bl_removedEntity->getUniqueID() == entityId) {
		return bl_removedEntity;
	}
	if (bl_onLockDown || level == nullptr) return nullptr;
	return level->fetchEntity(entityId, false);
}

void bl_MinecraftScreenModel_sendChatMessage_hook(MinecraftScreenModel* chatScreen, std::string const& message) {
	const char* chatMessageChars = message.c_str();

	JNIEnv *env;
	preventDefaultStatus = false;
	bl_JavaVM->AttachCurrentThread(&env, NULL);

	jstring chatMessageJString = env->NewStringUTF(chatMessageChars);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "chatCallback", "(Ljava/lang/String;)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, chatMessageJString);

	bl_JavaVM->DetachCurrentThread();
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Chat message: %s preventDefault %d\n", chatMessageChars,
		(int) preventDefaultStatus);
	if (!preventDefaultStatus) {
		bl_MinecraftScreenModel_sendChatMessage_real(chatScreen, message);
	} else {
		//clear the chat string
		chatScreen->updateTextBoxText("");
	}
}

void bl_MinecraftScreenModel_executeCommand_hook(MinecraftScreenModel* chatScreen, std::string const& message) {
	const char* chatMessageChars = message.c_str();

	JNIEnv *env;
	preventDefaultStatus = false;
	bl_JavaVM->AttachCurrentThread(&env, NULL);

	jstring chatMessageJString = env->NewStringUTF(chatMessageChars);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "chatCallback", "(Ljava/lang/String;)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, chatMessageJString);

	bl_JavaVM->DetachCurrentThread();
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Command message: %s preventDefault %d\n", chatMessageChars,
		(int) preventDefaultStatus);
	if (!preventDefaultStatus) {
		bl_MinecraftScreenModel_executeCommand_real(chatScreen, message);
	} else {
		//clear the chat string
		chatScreen->updateTextBoxText("");
	}
}

void bl_RakNetInstance_connect_hook(RakNetInstance* rakNetInstance, Social::GameConnectionInfo remoteInfo, Social::GameConnectionInfo myInfo) {
	BL_LOG("Connecting to server!");
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jstring hostJString = env->NewStringUTF(remoteInfo.host.c_str());

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "rakNetConnectCallback", "(Ljava/lang/String;I)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, hostJString, remoteInfo.port);

	bl_onLockDown = !isLocalAddress(env, hostJString);

	env->DeleteLocalRef(hostJString);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_RakNetInstance_connect_real(rakNetInstance, remoteInfo, myInfo);
}

#if 0

void bl_Font_drawCached_hook(Font* font, std::string const& textStr, float xOffset, float yOffset, Color const& color, bool isShadow, MaterialPtr* material) {
	if (bl_text_parse_color_codes) {
		char const* currentTextBegin = textStr.c_str(); //the current coloured section
		int currentLength = 0; //length in bytes of the current coloured section
		const uint8_t* iteratePtr = (const uint8_t*) textStr.c_str(); //where we are iterating
		const uint8_t* endIteratePtr = (const uint8_t*) iteratePtr + textStr.length(); //once we reach here, stop iterating
		//int lengthOfStringRemaining = length;
		float substringOffset = xOffset; //where we draw the currently coloured section
		Color curColor = color; //the colour for the current section
		int flags = 0;
		//Loop through the string.
		//When we find the first colour control character:
		//call draw with text pointer at original position and length num of characters already scanned
		//increment text pointer with characters already drawn
		//length of scanned to 0
		//once we get to the end of the string,
		//call draw with text pointer and length num of characters

		//to loop, we use our embedded copy of utf8proc
		//which is the same library that MCPE uses

		while(iteratePtr < endIteratePtr) {
			int myChar = -1;
			int bytesAdvanced = utf8proc_iterate(iteratePtr, endIteratePtr - iteratePtr, &myChar);
			if (bytesAdvanced < 0 || myChar < 0) break;
			iteratePtr += bytesAdvanced;

			if (myChar == 0xA7 && iteratePtr < endIteratePtr) {
				//is chat colouring code
				bytesAdvanced = utf8proc_iterate(iteratePtr, endIteratePtr - iteratePtr, &myChar);
				if (bytesAdvanced < 0 || myChar < 0) break;
				iteratePtr += bytesAdvanced;

				int newColor = -1;
				bool newFlags = flags;
				if (myChar >= '0' && myChar <= '9') {
					newColor = bl_minecraft_colors[myChar - '0'];
				} else if (myChar >= 'a' && myChar <= 'f') {
					newColor = bl_minecraft_colors[myChar - 'a' + 10];
				} else if (myChar == 0x7caa) { // chinese character for "poo"
					newColor = 0x2d3b00; // veggie poop green
				} else if (myChar == 'l') {
					newFlags |= TEXT_BOLD;
				} else if (myChar == 'r') {
					newFlags = 0;
				}

				std::string cppStringPart = std::string(currentTextBegin, currentLength);

				bl_Font_drawCached_real(font, cppStringPart, substringOffset, yOffset, curColor, isShadow, material);
				if (flags & TEXT_BOLD) {
					bl_Font_drawCached_real(font, cppStringPart, substringOffset + 1, yOffset, curColor, isShadow, material);
				}

				substringOffset += bl_Font_width(font, cppStringPart);

				if (newColor != -1) curColor = {((newColor >> 16) & 0xff) / 255.0f, ((newColor >> 8) & 0xff) / 255.0f,
					(newColor & 0xff) / 255.0f, 1.0f};;
				flags = newFlags;
				currentTextBegin = (const char *) iteratePtr;
				currentLength = 0;

			} else {
				currentLength += bytesAdvanced;
			}
		}
		if (currentLength > 0) {
			std::string cppStringPart = std::string(currentTextBegin, currentLength);
			bl_Font_drawCached_real(font, cppStringPart, substringOffset, yOffset, curColor, isShadow, material);
			if (flags & TEXT_BOLD) {
				bl_Font_drawCached_real(font, cppStringPart, substringOffset + 1, yOffset, curColor, isShadow, material);
			}
		}
	} else {
		bl_Font_drawCached_real(font, textStr, xOffset, yOffset, color, isShadow, material);
		return;
	}
}

#endif

static void bl_Item_initCreativeItems_hook() {
	bl_Item_initCreativeItems_real();
	for (short*& pair: bl_creativeItems) {
		bl_Item_addCreativeItem(pair[0], pair[1]);
	}
}

const char* bl_getCharArr(void* str){
	std::string* mystr = static_cast<std::string*>(str);
	std::string mystr2 = *mystr;
	const char* cs = mystr2.c_str();
	return cs;
}

TextureUVCoordinateSet* bl_CustomBlockGraphics_getTextureHook(BlockGraphics* graphics, signed char side, int data) {
	int blockId = graphics->getBlock()->id;
	TextureUVCoordinateSet** ptrToBlockInfo = bl_custom_block_textures[blockId];
	if (ptrToBlockInfo == NULL || ptrToBlockInfo[0] == nullptr) {
		return bl_BlockGraphics_getTexture_real(graphics, side, data);
	}
	int myIndex = (data * 6) + side;
	if (myIndex < 0 || myIndex >= 16*6) {
		myIndex = side;
	}
	return ptrToBlockInfo[myIndex];
}

TextureUVCoordinateSet* bl_CustomBlockGraphics_getTexture_char_hook(BlockGraphics* graphics, signed char side) {
	return bl_CustomBlockGraphics_getTextureHook(graphics, side, 0);
}

TextureUVCoordinateSet* bl_CustomBlockGraphics_getCarriedTexture_hook(BlockGraphics* graphics, signed char side, int data) {
	return bl_CustomBlockGraphics_getTextureHook(graphics, side, data);
}

bool bl_CustomBlock_isCubeShapedHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_opaque[blockId];
}
AABB* bl_CustomBlock_getAABBHook(Block* block, BlockSource& blockSource, BlockPos const& pos, AABB& aabb, int int1, bool bool1, int int2) {
	int blockId = block->id;
	if (bl_custom_block_collisionDisabled[blockId]) return bl_ReedBlock_getAABB(block, blockSource, pos, aabb, int1, bool1, int2);
	return bl_Block_getAABB(block, blockSource, pos, aabb, int1, bool1, int2);
}

int bl_CustomBlock_getColorHook(Block* tile, BlockSource& blockSource, BlockPos const& pos) {
	int blockId = tile->id;
	int* myColours = bl_custom_block_colors[blockId];
	if (myColours == NULL || bl_level == NULL) return -1; //I see your true colours shining through
	int data = blockSource.getData(pos.x, pos.y, pos.z);
	return myColours[data];
}

// doesn't seem to be used but might as well hook it
int bl_CustomBlock_getColor_data_Hook(Tile* tile, int data) {
	int blockId = tile->id;
	int* myColours = bl_custom_block_colors[blockId];
	if (myColours == NULL || bl_level == NULL) return -1; //I see your true colours shining through
	return myColours[data];
}

AABB& bl_CustomBlock_getVisualShape_hook(Tile* tile, unsigned char data, AABB& aabb, bool someBool) {
	if (data == 0) return bl_Tile_getVisualShape(tile, data, aabb, someBool);
	int blockId = tile->id;
	AABB** aabbs = bl_custom_block_visualShapes[blockId];
	AABB* aabbout;
	if (aabbs == nullptr || (aabbout = aabbs[data-1]) == nullptr) return bl_Tile_getVisualShape(tile, data, aabb, someBool);
	return *aabbout;
}

bool bl_CustomBlock_isRedstoneBlock_hook(Block* block) {
	return bl_custom_block_redstone[block->id] != 0;
}

void bl_CustomBlock_onRedstoneUpdate_hook(Block* block, BlockSource& source, BlockPos const& pos, int newLevel, bool something) {
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	int blockId = source.getBlockID(pos);
	int blockData = source.getData(pos);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "redstoneUpdateCallback", "(IIIIZII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, pos.x, pos.y, pos.z, newLevel, something, blockId, blockData);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}

void bl_CustomBlock_onLoaded_hook(Block* block, BlockSource& source, BlockPos const& pos) {
	if (source.getLevel()->isClientSide()) return;
	if (bl_custom_block_redstone[block->id] & REDSTONE_CONSUMER) {
		bl_TntBlock_onLoaded(block, source, pos);
	}
}

void bl_CustomBlock_onPlace_hook(Block* block, BlockSource& source, BlockPos const& pos) {
	bl_Block_onPlace(block, source, pos);
	if (bl_custom_block_redstone[block->id]) {
		bl_CustomBlock_onLoaded_hook(block, source, pos);
	}
}

void bl_ClientNetworkHandler_handleTextPacket_hook(void* handler, void* ipaddress, TextPacket* packet) {
	if (!(packet->type == 0 || packet->type == 1)) { // text or client message
		// just pass it along
		bl_ClientNetworkHandler_handleTextPacket_real(handler, ipaddress, packet);
		return;
	}
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}
	if (packet->type == 0) { // client message
		jstring messageJString = env->NewStringUTF(packet->message.c_str());
		preventDefaultStatus = false;
		//Call back across JNI into the ScriptManager
		jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "handleChatPacketCallback", "(Ljava/lang/String;)V");

		env->CallStaticVoidMethod(bl_scriptmanager_class, mid, messageJString);
	} else if (packet->type == 1) {
		if (packet->username.length() == 0 && packet->message == "\xc2" "\xa7" "0BlockLauncher, enable scripts") {
			bl_onLockDown = false;
		}
		jstring senderJString = env->NewStringUTF(packet->username.c_str());
		jstring messageJString = env->NewStringUTF(packet->message.c_str());
		preventDefaultStatus = false;
		//Call back across JNI into the ScriptManager
		jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "handleMessagePacketCallback",
			"(Ljava/lang/String;Ljava/lang/String;)V");

		env->CallStaticVoidMethod(bl_scriptmanager_class, mid, senderJString, messageJString);
	}

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) {
		bl_ClientNetworkHandler_handleTextPacket_real(handler, ipaddress, packet);
	}
}

#if 0

void bl_Entity_saveWithoutId_hook(Entity* entity, void* compoundTag) {
	int entityId = entity->entityId;
	std::array<unsigned char, 16> uuidBytes = bl_getEntityUUID(entityId);
	int64_t* uuidLongs = (int64_t*) uuidBytes.data();
	bl_CompoundTag_putLong(compoundTag, "UUIDLeast", uuidLongs[0]);
	bl_CompoundTag_putLong(compoundTag, "UUIDMost", uuidLongs[1]);
	/*
	LongTag* leastTag = (LongTag*) bl_CompoundTag_get(compoundTag, "UUIDLeast");
	LongTag* mostTag = (LongTag*) bl_CompoundTag_get(compoundTag, "UUIDMost");
	leastTag->value = uuidLongs[0];
	mostTag->value = uuidLongs[1];
	*/
	bl_Entity_saveWithoutId_real(entity, compoundTag);
}

int bl_Entity_load_hook(Entity* entity, void* compoundTag) {
	int entityId = entity->entityId;
	/*int64_t msl = bl_CompoundTag_getLong(compoundTag, "UUIDMost");
	if (msl != 0) {
		std::array<unsigned char, 16> newuuid;
		int64_t* uuidLongs = (int64_t*) newuuid.data();
		uuidLongs[0] = bl_CompoundTag_getLong(compoundTag, "UUIDLeast");
		uuidLongs[1] = msl;
		bl_entityUUIDMap[entityId] = newuuid;
	}*/
	return bl_Entity_load_real(entity, compoundTag);
}
#endif

static void bl_CustomSnowball_processAddEntity(Entity*);

static bool bl_Level_addEntity_hook(Level* level, BlockSource& blockSource, std::unique_ptr<Entity> entityPtr) {
	JNIEnv *env;
	Entity* entity = entityPtr.get();

	bool retval = bl_Level_addEntity_real(level, blockSource, std::move(entityPtr));

	if (!retval) return retval; // entity wasn't added
	bl_CustomSnowball_processAddEntity(entity);

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->getUniqueID());

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return retval;
}

static uintptr_t bl_Level_addPlayer_hook(Level* level, std::unique_ptr<Player> entity) {
	jlong entityId = entity->getUniqueID();
	uintptr_t retval = bl_Level_addPlayer_real(level, std::move(entity));
	JNIEnv *env;

	if (!retval) return retval; // entity wasn't added

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entityId);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return retval;
}

static bool bl_MultiPlayerLevel_addEntity_hook(Level* level, BlockSource& blockSource, std::unique_ptr<Entity> entityPtr) {
	JNIEnv *env;

	Entity* entity = entityPtr.get();

	bool retval = bl_MultiPlayerLevel_addEntity_real(level, blockSource, std::move(entityPtr));

	if (!retval) return retval; // entity wasn't added
	bl_CustomSnowball_processAddEntity(entity);

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->getUniqueID());

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return retval;
}

static void bl_Level_removeEntity_hook(Level* level, std::unique_ptr<Entity>&& entity, bool arg2) {
	JNIEnv *env;

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	bl_removedEntity = entity.get();

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityRemovedCallback", "(J)V");
	BL_LOG("Entity removed: %lld", (long long) entity->getUniqueID());

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->getUniqueID());
	bl_removedEntity = nullptr;

	bl_Level_removeEntity_real(level, std::move(entity), arg2);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}

static void bl_Level_explode_hook(Level* level, TileSource* tileSource, Entity* entity, Vec3 const& p, float power, bool onFire, bool anotherBool, float something) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	preventDefaultStatus = false;

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "explodeCallback", "(JFFFFZ)V");

	long long id = entity != NULL? entity->getUniqueID().id: -1LL;

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, id, p.x, p.y, p.z, power, onFire);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) bl_Level_explode_real(level, tileSource, entity, p, power, onFire, anotherBool, something);
}

static void bl_BlockSource_fireBlockEvent_hook(BlockSource* source, int x, int y, int z, int type, int data) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "blockEventCallback", "(IIIII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, x, y, z, type, data);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_BlockSource_fireBlockEvent_real(source, x, y, z, type, data);
}

static void bl_Player_eat_hook(Entity* player, int hearts, float notHearts) {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "I'm EATING %d %f", hearts, notHearts);
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "eatCallback", "(IF)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, hearts);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_Player_eat_real(player, hearts, notHearts);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClientMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	bl_minecraft->getGuiData()->displayClientMessage(mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

JNIEXPORT int JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetBlockRenderShape
  (JNIEnv *env, jclass clazz, jint blockId) {
	// FIXME 0.15
/*
	Tile* tile = bl_Block_mBlocks[blockId];
	if(tile == NULL) return 0;
	
	return tile->renderType;//bl_CustomBlock_getRenderShapeHook(tile);
*/
	return 0;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetBlockRenderShape
  (JNIEnv *env, jclass clazz, jint blockId, jint renderType) {
	//bl_custom_block_renderShape[blockId] = renderType;
	// FIXME 0.15
/*
	Tile* tile = bl_Block_mBlocks[blockId];
	if (tile == nullptr) return;
	tile->renderType = renderType;
*/
}

static void bl_registerItem(Item* item, std::string const& name) {
	bl_Item_setCategory(item, 3 /* TOOL */);
	bl_Item_mItems[item->itemId] = item;
	//std::string lowercaseStr = Util::toLower(name);
	if (item->itemId > 0x200 && Item::mItemTextureAtlas != nullptr) {
		ResourceLocation location("atlas.items", 0);
		if (!(item->itemId < ItemRenderer::mItemGraphics.size())) {
			ItemRenderer::mItemGraphics.resize(item->itemId + 1);
		}
		ItemRenderer::mItemGraphics[item->itemId] = ItemGraphics(std::move(mce::TexturePtr(bl_minecraft->getTextures(), location)));
	}
	//Item::mItemLookupMap[lowercaseStr] = std::make_pair(lowercaseStr, std::unique_ptr<Item>(item));
}

void bl_repopulateItemGraphics() {
	for (int i = 0x200; i < bl_item_id_count; i++) {
		Item* item = bl_Item_mItems[i];
		if (!item) continue;
		bool needsSetting = false;
		if (item->itemId >= ItemRenderer::mItemGraphics.size()) {
			// outside the array, expand and populate
			ItemRenderer::mItemGraphics.resize(item->itemId + 1);
			needsSetting = true;
		} else if (!(((void**)&ItemRenderer::mItemGraphics[item->itemId])[1])) {
			// inside but not set, populate
			needsSetting = true;
		}
		if (needsSetting) {
			//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "populating graphics %d", item->itemId);
			if (Item::mItemTextureAtlas == nullptr) {
				BL_LOG("item texture atlas null when populating graphics?");
			}
			ResourceLocation location("atlas.items", 0);
			ItemRenderer::mItemGraphics[item->itemId] = ItemGraphics(std::move(mce::TexturePtr(bl_minecraft->getTextures(), location)));
		}
	}
}

void bl_cpp_selectLevel_hook() {
	if (Item::mItemTextureAtlas == nullptr) return;
	bl_repopulateItemGraphics();
}
Item* bl_constructItem(std::string const& name, int id) {
	Item* retval = (Item*) ::operator new(kItemSize);
	bl_Item_Item(retval, name, id - 0x100);
	*((void***)retval) = bl_CustomItem_vtable + 2;
	retval->setStackedByData(true);
	bl_registerItem(retval, name);
	return retval;
}

Item* bl_constructSnowballItem(std::string const& name, int id) {
	Item* retval = (Item*) ::operator new(kItemSize);
	bl_Item_Item(retval, name, id - 0x100);
	*((void***)retval) = bl_CustomSnowballItem_vtable + 2;
	retval->setStackedByData(true);
	bl_registerItem(retval, name);
	return retval;
}

static void bl_Item_setIcon_wrapper(Item* item, std::string const& iconName, int iconIndex);

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineItem
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jstring name, jint maxStackSize) {
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);

	Item* item = bl_constructItem(mystr, id);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon_wrapper(item, iconNameString, iconIndex);

	if (maxStackSize <= 0) {
		bl_Item_setMaxStackSize(item, 64);
	} else {
		bl_Item_setMaxStackSize(item, maxStackSize);
	}

	bl_set_i18n("item." + mystr + ".name", mystr);
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineArmor
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jstring name, jstring texture,
		jint damageReduceAmount, jint maxDamage, jint armorType) {
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);

	ArmorItem* item = new ArmorItem;
	bl_ArmorItem_ArmorItem(item, mystr, id - 0x100, ((ArmorItem*) bl_Item_mItems[310])->armorMaterial, 42, armorType);
	bl_registerItem(item, mystr);
	item->damageReduceAmount = damageReduceAmount;
	bl_Item_setMaxDamage(item, maxDamage);

	const char * textureUTFChars = env->GetStringUTFChars(texture, NULL);
	//if (bl_armorRenders[id] != nullptr) delete bl_armorRenders[id];
	//bl_armorRenders[id] = new mce::TexturePtr(bl_minecraft->getTextures(), ResourceLocation(textureUTFChars));
	bl_setArmorTexture(id, std::string(textureUTFChars));
	env->ReleaseStringUTFChars(name, textureUTFChars);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon_wrapper(item, iconNameString, iconIndex);

	bl_set_i18n("item." + mystr + ".name", mystr);
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineSnowballItem
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jstring name, jint maxStackSize) {
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);

	Item* item = bl_constructSnowballItem(mystr, id);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon_wrapper(item, iconNameString, iconIndex);

	if (maxStackSize <= 0) {
		bl_Item_setMaxStackSize(item, 64);
	} else {
		bl_Item_setMaxStackSize(item, maxStackSize);
	}

	bl_set_i18n("item." + mystr + ".name", mystr);
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetItemMaxDamage
  (JNIEnv *env, jclass clazz, jint id, jint maxDamage) {
	if (id <= 0 || id >= bl_item_id_count) return;
	Item* item = bl_Item_mItems[id];
	if(item == NULL) return;
	bl_Item_setMaxDamage(item, maxDamage);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemMaxDamage
  (JNIEnv *env, jclass clazz, jint id) {
	if (id <= 0 || id >= bl_item_id_count) return -1;
	Item* item = bl_Item_mItems[id];
	if(item == NULL) return -1;
	return item->getMaxDamage();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSelectLevel
  (JNIEnv *env, jclass clazz, jstring worlddir, jstring worldName) {
	const char * utfChars = env->GetStringUTFChars(worlddir, NULL);
	const char * nameUtfChars = env->GetStringUTFChars(worldName, NULL);
	std::string worlddirstr = std::string(utfChars);
	env->ReleaseStringUTFChars(worlddir, utfChars);
	LevelSettings levelSettings = {
		-1,
		-1,
		3,
		4,
		0,
		0,
		0,
	};

	bl_MinecraftClient_startLocalServer(bl_minecraft, worlddirstr, std::string(nameUtfChars), &levelSettings);
	env->ReleaseStringUTFChars(worlddir, utfChars);
	env->ReleaseStringUTFChars(worldName, nameUtfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLeaveGame
  (JNIEnv *env, jclass clazz, jboolean saveMultiplayerWorld) {
	//bl_Minecraft_setLeaveGame(bl_minecraft);
	// Is this boolean right?
	bl_MinecraftClient_leaveGame(bl_minecraft, true);
}

/*JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeJoinServer
  (JNIEnv *env, jclass clazz, jstring serverAddress, jint serverPort) {
	const char * utfChars = env->GetStringUTFChars(serverAddress, NULL);
	std::string addressstr = std::string(utfChars);
	env->ReleaseStringUTFChars(serverAddress, utfChars);
	bl_Minecraft_connectToMCOServer(bl_minecraft, addressstr, addressstr, serverPort);
}*/

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlaySound
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jstring sound, jfloat volume, jfloat pitch) {
	const char * utfChars = env->GetStringUTFChars(sound, NULL);
	std::string soundstr = std::string(utfChars);
	env->ReleaseStringUTFChars(sound, utfChars);
	bl_minecraft->play(soundstr, Vec3(x, y, z), volume, pitch);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeJoinServer
  (JNIEnv *env, jclass clazz, jstring host, jint port) {
/*	const char* hostChars = env->GetStringUTFChars(host, NULL);
	bl_Minecraft_locateMultiplayer(bl_minecraft); //to set up the client network handler
	int rakNetOffset = ((int) bl_minecraft) + MINECRAFT_RAKNET_INSTANCE_OFFSET;
	RakNetInstance* raknetInstance = *((RakNetInstance**) rakNetOffset);
	bl_RakNetInstance_connect_hook(raknetInstance, hostChars, port);
	env->ReleaseStringUTFChars(host, hostChars);
	// put up the progress screen (since otherwise Minecraft PE closes itself?!)
	void* progressScreen = operator new(224);
	bl_ProgressScreen_ProgressScreen(progressScreen);
	bl_Minecraft_setScreen(bl_minecraft, progressScreen);
*/
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetSignText
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint line) {
	if (bl_level == NULL) return NULL;
	SignBlockEntity* te = static_cast<SignBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return NULL;

	std::string const& lineStr = te->getMessage(line);

	jstring signJString = env->NewStringUTF(lineStr.c_str());
	return signJString;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSignText
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint line, jstring newText) {
	if (bl_level == NULL) return;
	SignBlockEntity* te = static_cast<SignBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return;

	const char * utfChars = env->GetStringUTFChars(newText, NULL);

	te->setMessage(std::string(utfChars), line);
	env->ReleaseStringUTFChars(newText, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTextParseColorCodes
  (JNIEnv *env, jclass clazz, jboolean colorText) {
	bl_text_parse_color_codes = colorText;
}

std::unordered_map<long long, mce::TexturePtr> bl_mobTexturesMap;

void bl_changeEntitySkin(Entity* entity, const char* newSkin) {
	if (strlen(newSkin) == 0) return; // WHAT
	bl_mobTexturesMap[entity->getUniqueID()] = mce::TexturePtr(bl_minecraft->getTextures(), ResourceLocation(newSkin));
	bl_forceTextureLoad(newSkin);
}
void bl_clearMobTextures() {
	bl_mobTexturesMap.clear();
}

void bl_attachLevelListener() {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSneaking
  (JNIEnv *env, jclass clazz, jlong entityId, jboolean doIt) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	void (*setSneaking)(Entity*, bool) = (void (*)(Entity*, bool)) entity->vtable[vtable_indexes.mob_set_sneaking];
	setSneaking(entity, doIt);
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeIsSneaking
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return false;
	return bl_Mob_isSneaking(entity);
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemName
  (JNIEnv *env, jclass clazz, jint itemId, jint itemDamage, jboolean raw) {
	if (itemId <= 0 || itemId >= bl_item_id_count) return nullptr;
	ItemInstance myStack(itemId, 1, itemDamage);
	if (myStack.getId() != itemId) return NULL;
	switch(itemId) {
		case 95:
		case 255:
			//these return blank strings. Blank strings will kill libstdc++ since we are not using the same blank string.
			return NULL;
	}
	Localization* bak;
	if (raw) {
		bak = I18n::mCurrentLanguage;
		I18n::mCurrentLanguage = nullptr;
	}
	std::string descriptionId = myStack.getName();
	if (raw) {
		I18n::mCurrentLanguage = bak;
	}
	if (descriptionId.length() <= 0) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "dead tile: %i\n", itemId);
	}
	//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Tile: %s\n", descriptionId.c_str());
	std::string returnVal = descriptionId;
	jstring returnValString = env->NewStringUTF(returnVal.c_str());
	return returnValString;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTextureCoordinatesForItem
  (JNIEnv *env, jclass clazz, jint itemId, jint itemDamage, jfloatArray outputArray) {
	if (itemId <= 0 || itemId >= bl_item_id_count) return false;
	ItemInstance myStack(itemId, 1, itemDamage);
	if (myStack.getId() != itemId) return false;
	TextureUVCoordinateSet* set = myStack.getIcon(0, true);
	if (set == NULL || set->bounds == NULL) return false;
	float lasttwo[] = {(float) set->size[0], (float) set->size[1]};
	env->SetFloatArrayRegion(outputArray, 0, 4, set->bounds);
	env->SetFloatArrayRegion(outputArray, 4, 2, lasttwo);
	return true;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTextureCoordinatesForBlock
  (JNIEnv *env, jclass clazz, jint itemId, jint itemDamage, jint side, jfloatArray outputArray) {
	if (itemId <= 0 || itemId >= 256) return false;
	// FIXME 0.15
/*
	BlockGraphics* block = BlockGraphics::mBlocks[itemId];
	if (!block) return false;
	TextureUVCoordinateSet* (*gettex)(BlockGraphics*, signed char, int) =
		(TextureUVCoordinateSet* (*)(BlockGraphics*, signed char, int))
		block->vtable[vtable_indexes.blockgraphics_get_texture_char_int - 2];
	TextureUVCoordinateSet* set = gettex(block, itemDamage, side);
	if (set == NULL || set->bounds == NULL) return false;
	float lasttwo[] = {(float) set->size[0], (float) set->size[1]};
	env->SetFloatArrayRegion(outputArray, 0, 4, set->bounds);
	env->SetFloatArrayRegion(outputArray, 4, 2, lasttwo);
	return true;
*/
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPlayerName
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return NULL;
	std::string* myName = bl_Entity_getNameTag(entity);
	jstring returnValString = env->NewStringUTF(myName->c_str());
	return returnValString;
}

static void bl_setBlockVtable(void** bl_CustomBlock_vtable) {
	bl_CustomBlock_vtable[vtable_indexes.tile_get_color] = (void*) &bl_CustomBlock_getColorHook;
	bl_CustomBlock_vtable[vtable_indexes.tile_get_color_data] = (void*) &bl_CustomBlock_getColor_data_Hook;
	bl_CustomBlock_vtable[vtable_indexes.tile_get_visual_shape] = (void*) &bl_CustomBlock_getVisualShape_hook;
	bl_CustomBlock_vtable[vtable_indexes.tile_is_redstone_block] = (void*) &bl_CustomBlock_isRedstoneBlock_hook;
	bl_CustomBlock_vtable[vtable_indexes.tile_on_redstone_update] = (void*) &bl_CustomBlock_onRedstoneUpdate_hook;
	bl_CustomBlock_vtable[vtable_indexes.tile_on_loaded] = (void*) &bl_CustomBlock_onLoaded_hook;
}

int bl_CustomBlockItem_getLevelDataForAuxValue_hook(Item* item, int value) {
	if (item->isStackedByData()) return value;
	return 0;
}

unsigned short bl_CustomItem_getEnchantSlot_hook(Item* item) {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Get enchant slot %d %d", item->itemId,
		bl_customItem_allowEnchantments[item->itemId]);
	return bl_customItem_allowEnchantments[item->itemId];
}

int bl_CustomItem_getEnchantValue_hook(Item* item) {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Get enchant value %d %d", item->itemId,
		bl_customItem_enchantValue[item->itemId]);
	return bl_customItem_enchantValue[item->itemId];
}
class Container;
static void* (*bl_CustomSnowballItem_use_real)(Item*, ItemInstance&, Player&);
static void* (*bl_CustomSnowballItem_dispense_real)(Item*, BlockSource&, Container&, int, Vec3 const&, signed char);
static int bl_activeSnowballId;

static void* bl_CustomSnowballItem_use_hook(Item* item, ItemInstance& stack, Player& player) {
	bl_activeSnowballId = item->itemId;
	void* retval = bl_CustomSnowballItem_use_real(item, stack, player);
	bl_activeSnowballId = 0;
	return retval;
}

static void* bl_CustomSnowballItem_dispense_hook(Item* item, BlockSource& blockSource, Container& container,
	int something, Vec3 const& vec3, signed char side) {
	bl_activeSnowballId = item->itemId;
	void* retval = bl_CustomSnowballItem_dispense_real(item, blockSource, container, something, vec3, side);
	bl_activeSnowballId = 0;
	return retval;
}

int bl_renderManager_renderTypeForItemSprite(int itemId);
int bl_getEntityTypeIdThroughVtable(Entity* entity);

Item** bl_getItemsArray() {
	return bl_Item_mItems;
}

static void bl_CustomSnowball_processAddEntity(Entity* entity) {
	if (bl_getEntityTypeIdThroughVtable(entity) != 81 /* snowball */) return;
	if (!bl_activeSnowballId) return;
	int renderType = bl_renderManager_renderTypeForItemSprite(bl_activeSnowballId);
	if (!renderType) return;
	bl_renderManager_setRenderType(entity, renderType);
}

void bl_initCustomBlockVtable() {
	//copy existing vtable
	bl_CustomBlock_vtable = (void**) ::operator new(vtable_indexes.tile_vtable_size);
	memcpy(bl_CustomBlock_vtable, bl_Tile_vtable, vtable_indexes.tile_vtable_size);
	bl_CustomBlockGraphics_vtable = (void**) ::operator new(vtable_indexes.blockgraphics_vtable_size);
	memcpy(bl_CustomBlockGraphics_vtable, bl_BlockGraphics_vtable, vtable_indexes.blockgraphics_vtable_size);

	//set the texture getter to our overridden version

	//bl_CustomBlockGraphics_vtable[vtable_indexes.blockgraphics_get_carried_texture] = (void*) &bl_CustomBlockGraphics_getCarriedTexture_hook;

	bl_Block_onPlace = (void (*)(Block*, BlockSource&, BlockPos const&))
		bl_CustomBlock_vtable[vtable_indexes.tile_on_place];
	bl_CustomBlock_vtable[vtable_indexes.tile_on_place] = (void*) &bl_CustomBlock_onPlace_hook;
	bl_setBlockVtable(bl_CustomBlock_vtable);

	bl_CustomLiquidBlockStatic_vtable = (void**) ::operator new(vtable_indexes.tile_vtable_size);
	memcpy(bl_CustomLiquidBlockStatic_vtable, bl_LiquidBlockStatic_vtable, vtable_indexes.tile_vtable_size);
	bl_setBlockVtable(bl_CustomLiquidBlockStatic_vtable);

	bl_CustomLiquidBlockDynamic_vtable = (void**) ::operator new(vtable_indexes.tile_vtable_size);
	memcpy(bl_CustomLiquidBlockDynamic_vtable, bl_LiquidBlockDynamic_vtable, vtable_indexes.tile_vtable_size);
	bl_setBlockVtable(bl_CustomLiquidBlockDynamic_vtable);

	bl_CustomBlockItem_vtable = (void**) ::operator new(vtable_indexes.blockitem_vtable_size);
	memcpy(bl_CustomBlockItem_vtable, bl_BlockItem_vtable, vtable_indexes.blockitem_vtable_size);
	bl_CustomBlockItem_vtable[vtable_indexes.blockitem_get_level_data_for_aux_value] =
		(void*) &bl_CustomBlockItem_getLevelDataForAuxValue_hook;
	bl_CustomBlockItem_vtable[vtable_indexes.item_get_enchant_slot] =
		(void*) &bl_CustomItem_getEnchantSlot_hook;
	bl_CustomBlockItem_vtable[vtable_indexes.item_get_enchant_value] =
		(void*) &bl_CustomItem_getEnchantValue_hook;

	bl_CustomItem_vtable = (void**) ::operator new(vtable_indexes.item_vtable_size);
	memcpy(bl_CustomItem_vtable, bl_Item_vtable, vtable_indexes.item_vtable_size);
	bl_CustomItem_vtable[vtable_indexes.item_get_enchant_slot] =
		(void*) &bl_CustomItem_getEnchantSlot_hook;
	bl_CustomItem_vtable[vtable_indexes.item_get_enchant_value] =
		(void*) &bl_CustomItem_getEnchantValue_hook;
	bl_CustomSnowballItem_vtable = (void**) ::operator new(vtable_indexes.snowball_item_vtable_size);
	memcpy(bl_CustomSnowballItem_vtable, bl_SnowballItem_vtable, vtable_indexes.snowball_item_vtable_size);
	bl_CustomSnowballItem_use_real = (void* (*)(Item*, ItemInstance&, Player&))
		bl_SnowballItem_vtable[vtable_indexes.item_use];
	bl_CustomSnowballItem_dispense_real = (void* (*)(Item*, BlockSource&, Container&, int, Vec3 const&, signed char))
		bl_SnowballItem_vtable[vtable_indexes.item_dispense];
	bl_CustomSnowballItem_vtable[vtable_indexes.item_use] =
		(void*) &bl_CustomSnowballItem_use_hook;
	bl_CustomSnowballItem_vtable[vtable_indexes.item_dispense] =
		(void*) &bl_CustomSnowballItem_dispense_hook;
}

void* bl_getMaterial(int materialType) {
	Tile* baseTile = bl_Block_mBlocks[materialType];
	if (baseTile == NULL) {
		baseTile = bl_Block_mBlocks[1];
	}
	return baseTile->material;
}

void bl_buildTextureArray(TextureUVCoordinateSet* output[], std::string textureNames[], int textureCoords[]) {
	for (int i = 0; i < 16*6; i++) {
		TextureUVCoordinateSet* mySet = new TextureUVCoordinateSet(BlockGraphics::getTextureUVCoordinateSet(
			textureNames[i], textureCoords[i]));
		//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Building %s %d\n", textureNames[i].c_str(), textureCoords[i]);
		output[i] = mySet;
	}
}

struct BLBlockBuildTextureRequest {
	int blockId;
	//std::string textureNames[16*6];
	//int textureCoords[16*6];
	std::array<std::string, 6> textureSides;
};

static std::vector<BLBlockBuildTextureRequest> buildTextureRequests;

static void bl_finishBlockBuildTextureRequests() {
	BL_LOG("finishBlockBuildTextureRequests");
	for (auto& request: buildTextureRequests) {
		auto bg = BlockGraphics::mBlocks[request.blockId];
		if (!bg) continue;
		//BL_LOG("Setting texs for %d: %s:%s:%s:%s:%s:%s", request.blockId,
		//	request.textureSides[0].c_str(), request.textureSides[1].c_str(), request.textureSides[2].c_str(),
		//	request.textureSides[3].c_str(), request.textureSides[4].c_str(), request.textureSides[5].c_str());
		bg->setTextureItem(request.textureSides[0], request.textureSides[1], request.textureSides[2],
			request.textureSides[3], request.textureSides[4], request.textureSides[5]);
		//if (!bl_custom_block_textures[request.blockId]) continue;
		//BL_LOG("finishBlockBuildTextureRequests for %d", request.blockId);
		//bl_buildTextureArray(bl_custom_block_textures[request.blockId], request.textureNames, request.textureCoords);
	}
}

class BLItemSetRequest {
public:
	int itemId;
	virtual ~BLItemSetRequest(){};
	virtual void setItem() = 0;
};

class BLItemSetIconRequest : public BLItemSetRequest {
public:
	std::string iconName;
	int iconIndex;
	virtual void setItem();
};

void BLItemSetIconRequest::setItem() {
	Item* item = bl_Item_mItems[this->itemId];
	if (!item) return;
	bl_Item_setIcon(item, this->iconName, this->iconIndex);
}

class BLItemSetJsonRequest : public BLItemSetRequest {
public:
	std::string value;
	virtual void setItem();
};

void BLItemSetJsonRequest::setItem() {
	Item* item = bl_Item_mItems[this->itemId];
	if (!item) return;
	Json::Value jsonValue;
	Json::Reader jsonReader;
	bool ret = false;
	if (jsonReader.parse(value, jsonValue)) {
		item->initServer(jsonValue);
		item->initClient(jsonValue, jsonValue);
	}
}

static std::vector<std::shared_ptr<BLItemSetRequest>> itemSetIconRequests;

static void bl_finishItemSetIconRequests() {
	for (auto& request: itemSetIconRequests) {
		request->setItem();
	}
	//itemSetIconRequests.clear();
}

static void bl_Item_setIcon_wrapper(Item* item, std::string const& iconName, int iconIndex) {
	if (Item::mItemTextureAtlas) {
		bl_Item_setIcon(item, iconName, iconIndex);
	} else {
		auto request = std::make_shared<BLItemSetIconRequest>();
		request->itemId = item->itemId;
		request->iconName = iconName;
		request->iconIndex = iconIndex;
		itemSetIconRequests.push_back(request);
	}
}

static void bl_Item_initServer_wrapper(Item* item, Json::Value& value) {
	if (Item::mItemTextureAtlas) {
		item->initServer(value);
	} else {
	}
}

static bool hasRepopulatedItemGraphics = false;

void bl_cpp_tick_hook() {
	//if (Item::mItemTextureAtlas && itemSetIconRequests.size() != 0) bl_finishItemSetIconRequests();
	if (!hasRepopulatedItemGraphics && Item::mItemTextureAtlas) {
		BL_LOG("repopulating item graphics");
		bl_repopulateItemGraphics();
		hasRepopulatedItemGraphics = true;
	}
}

Tile* bl_createBlock(int blockId, std::string textureNames[], int textureCoords[], int materialType, bool opaque, int renderShape, const char* name, int customBlockType) {
	if (blockId < 0 || blockId > 255) return NULL;
	if (bl_custom_block_textures[blockId] != NULL) {
		delete[] bl_custom_block_textures[blockId];
	}
	if (bl_custom_block_visualShapes[blockId]) {
		AABB** a = bl_custom_block_visualShapes[blockId];
		for (int i = 0; i < 15; i++) {
			if (a[i]) delete[] a[i];
		}
		delete[] a;
		bl_custom_block_visualShapes[blockId] = nullptr;
	}

	//bl_custom_block_opaque[blockId] = opaque;
#if 0
	if (customBlockType == 0 /* standard */) {
		bl_custom_block_textures[blockId] = new TextureUVCoordinateSet*[16*6];
		memset(bl_custom_block_textures[blockId], 0, sizeof(TextureUVCoordinateSet*)*16*6);
		if (BlockGraphics::mTerrainTextureAtlas) {
			bl_buildTextureArray(bl_custom_block_textures[blockId], textureNames, textureCoords);
		} else {
			// we're on the title screen; can't access textures
			BLBlockBuildTextureRequest request;
			request.blockId = blockId;
			for (int i = 0; i < 16*6; i++) {
				request.textureNames[i] = textureNames[i];
			}
			memcpy(request.textureCoords, textureCoords, sizeof(int)*16*6);
			buildTextureRequests.push_back(request);
		}
	} else {
		bl_custom_block_textures[blockId] = nullptr;
	}
#endif
	bl_custom_block_textures[blockId] = nullptr;	

	std::string realNameStr = std::string(name);
	std::string nameStr = realNameStr + "." + bl_to_string(blockId);

	//Allocate memory for the block
	// size found before the Tile::Tile constructor for tile ID #4
	Block* retval;
	BlockGraphics* retGraphics;
	if (customBlockType == 0 || true) { // FIXME 0.15
		retval = (Block*) ::operator new(kTileSize);
		bl_Block_Block(retval, nameStr, blockId, bl_getMaterial(materialType));
		retval->vtable = bl_CustomBlock_vtable + 2;
		// FIXME 0.15
		//retval->renderType = renderShape;
		retval->setSolid(opaque);
		//retval->blockProperties = (retval->blockProperties & ~BlockPropertyOpaque) | (opaque? BlockPropertyOpaque: 0);
		//Block::mTranslucency[blockId] = opaque? 0: 1;
		Block::mBlockLookupMap[Util::toLower(retval->mappingId)] = retval;
		// todo: graphics
	} else if (customBlockType == 1 /* liquid */ ) {
		// FIXME 0.15
		retval = (Block*) ::operator new(kLiquidBlockDynamicSize);
		bl_LiquidBlockDynamic_LiquidBlockDynamic(retval, nameStr, blockId, bl_getMaterial(materialType),
			textureNames[0], textureNames[1]);
		retval->vtable = bl_CustomLiquidBlockDynamic_vtable + 2;
		Block::mBlockLookupMap[Util::toLower(retval->mappingId)] = retval;
	} else if (customBlockType == 2 /* still liquid */ ) {
		// FIXME 0.15
		retval = (Block*) ::operator new(kLiquidBlockStaticSize);
		bl_LiquidBlockStatic_LiquidBlockStatic(retval, nameStr, blockId, blockId - 1, bl_getMaterial(materialType),
			textureNames[0], textureNames[1]);
		retval->vtable = bl_CustomLiquidBlockStatic_vtable + 2;
		Block::mBlockLookupMap[Util::toLower(retval->mappingId)] = retval;
	}
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "created block %s", retval->mappingId.c_str());
	retGraphics = new BlockGraphics(retval->mappingId);
	retGraphics->vtable = bl_CustomBlockGraphics_vtable + 2;
	BlockGraphics::mBlocks[blockId] = retGraphics;

	bl_set_i18n("tile." + nameStr + ".name", realNameStr);
	//add it to the global tile list
	bl_Block_mBlocks[blockId] = retval;
	// set default category
	retval->setCategory((CreativeItemCategory) 2 /* Decoration */);
	//now allocate the item
	Item* tileItem = (Item*) ::operator new(kBlockItemSize);
	bl_BlockItem_BlockItem(tileItem, nameStr, blockId - 0x100);
	*((void***)tileItem) = bl_CustomBlockItem_vtable + 2;
	tileItem->setMaxDamage(0);
	tileItem->setStackedByData(true);
	bl_registerItem(tileItem, nameStr);
	bl_Item_setCategory(tileItem, 2 /* Decoration */);
	if (BlockGraphics::mBlocks[blockId] == nullptr) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "BlockGraphics is null for %d", blockId);
		abort();
	}
	BLBlockBuildTextureRequest request;
	request.blockId = blockId;
	request.textureSides = {nameStr + "_down", nameStr + "_up", nameStr + "_north",
		nameStr + "_south", nameStr + "_west", nameStr + "_east"};
	if (BlockGraphics::mTerrainTextureAtlas) {
		retGraphics->setTextureItem(request.textureSides[0], request.textureSides[1], request.textureSides[2],
			request.textureSides[3], request.textureSides[4], request.textureSides[5]);
	}
	//BL_LOG("Created blockGraphics %p for blockId %d", retGraphics, blockId);
	buildTextureRequests.push_back(request);
	bl_customBlocksCreated = true;
	
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineBlock
  (JNIEnv *env, jclass clazz, jint blockId, jstring name, jobjectArray textureNames, 
		jintArray textureCoords, jint materialBlockId, jboolean opaque, jint renderType, jint customBlockType) {
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	int myIntArray[16*6];
	env->GetIntArrayRegion(textureCoords, 0, 16*6, myIntArray);
	std::string myStringArray[16*6];
	for (int i = 0; i < 16*6; i++) {
		jstring myString = (jstring) env->GetObjectArrayElement(textureNames, i);
		const char * myStringChars = env->GetStringUTFChars(myString, NULL);
		myStringArray[i] = myStringChars;
		env->ReleaseStringUTFChars(myString, myStringChars);
	}
	Tile* tile = bl_createBlock(blockId, myStringArray, myIntArray, materialBlockId, opaque, renderType, utfChars, customBlockType);
	env->ReleaseStringUTFChars(name, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetDestroyTime
  (JNIEnv *env, jclass clazz, jint blockId, jfloat time) {
	if (blockId < 0 || blockId > 255) return;
	Tile* tile = bl_Block_mBlocks[blockId];
	if (tile == NULL) {
		return;
	}
	tile->destroyTime = time;
        if (tile->explosionResistance < time * 5.0F) {
            tile->explosionResistance = time * 5.0F;
        }
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetExplosionResistance
  (JNIEnv *env, jclass clazz, jint blockId, jfloat resistance) {
	if (blockId < 0 || blockId > 255) return;
	Tile* tile = bl_Block_mBlocks[blockId];
	if (tile == NULL) {
		return;
	}
	tile->explosionResistance = resistance * 3.0f;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetShape
  (JNIEnv *env, jclass clazz, jint blockId, jfloat v1, jfloat v2, jfloat v3, jfloat v4, jfloat v5, jfloat v6, jint damage) {
	if (blockId < 0 || blockId > 255) return;
	Tile* tile = bl_Block_mBlocks[blockId];
	if (tile == NULL) {
		return;
	}
	if (damage == 0) {
		bl_Block_setVisualShape(tile, Vec3(v1, v2, v3), Vec3(v4, v5, v6));
	} else {
		AABB** aabbs = bl_custom_block_visualShapes[blockId];
		if (!aabbs) {
			bl_custom_block_visualShapes[blockId] = aabbs = new AABB*[15]();
		}
		AABB* theAABB = aabbs[damage - 1];
		if (!theAABB) aabbs[damage - 1] = theAABB = new AABB();
		theAABB->shouldBeFalse = false;
		theAABB->x1 = v1;
		theAABB->y1 = v2;
		theAABB->z1 = v3;
		theAABB->x2 = v4;
		theAABB->y2 = v5;
		theAABB->z2 = v6;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetStepSound
  (JNIEnv *env, jclass clazz, jint blockId, jint sourceBlockId) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetLightLevel
  (JNIEnv *env, jclass clazz, jint blockId, jint level) {
	if (blockId < 0 || blockId > 255) return;
	bl_Block_mLightEmission[blockId] = level;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetLightOpacity
  (JNIEnv *env, jclass clazz, jint blockId, jint level) {
	if (blockId < 0 || blockId > 255) return;
	bl_Block_mLightBlock[blockId] = level;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetColor
  (JNIEnv *env, jclass clazz, jint blockId, jintArray colours) {
	if (blockId < 0 || blockId > 255) return;
	int* myIntArray = bl_custom_block_colors[blockId];
	if (myIntArray == NULL) {
		myIntArray = new int[16];
		bl_custom_block_colors[blockId] = myIntArray;
	}
	env->GetIntArrayRegion(colours, 0, 16, myIntArray);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetRenderLayer
  (JNIEnv *env, jclass clazz, jint blockId, jint level) {
	if (blockId < 0 || blockId > 255) return;
	Block* tile = bl_Block_mBlocks[blockId];
	if (!tile) return;
	tile->renderLayer = level;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockGetRenderLayer
  (JNIEnv *env, jclass clazz, jint blockId) {
	if (blockId < 0 || blockId > 255) return 0;
	Block* tile = bl_Block_mBlocks[blockId];
	if (!tile) return 0;
	return tile->getRenderLayer();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemCreativeInv
  (JNIEnv *env, jclass clazz, jint id, jint count, jint damage) {
	for (short* item: bl_creativeItems) {
		if (item[0] == id && item[1] == damage) {
			// already added to the creative inventory; ignore.
			return;
		}
	}
	bl_Item_addCreativeItem((short) id, (short) damage);
	short* pair = new short[2];
	pair[0] = (short) id;
	pair[1] = (short) damage;
	bl_creativeItems.push_back(pair);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetI18NString
  (JNIEnv *env, jclass clazz, jstring key, jstring value) {

	const char * valueUTFChars = env->GetStringUTFChars(value, NULL);
	std::string valueNameString = std::string(valueUTFChars);

	const char * keyUTFChars = env->GetStringUTFChars(key, NULL);
	std::string keyString = std::string(keyUTFChars);
	bl_set_i18n(keyString, valueNameString);
	env->ReleaseStringUTFChars(key, keyUTFChars);
	env->ReleaseStringUTFChars(value, valueUTFChars);
}

bool bl_tryRemoveExistingRecipe(Recipes* recipeMgr, int itemId, int itemCount, int itemDamage, int ingredients[], int ingredientsCount) {
	std::vector<Recipe*>* recipesList = &recipeMgr->recipes;
	int recipesSize = recipesList->size();
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Recipes: %i", recipesSize);
	for (int i = recipesSize - 1; i >= 0; i--) { //TODO: inefficient?
		Recipe* recipe = (*recipesList)[i];
		if ((((uintptr_t) recipe->vtable) & ~0xf) != (((uintptr_t) bl_ShapelessRecipe_vtable) & ~0xf)) {
			//not a ShapelessRecipe
			continue;
		}
		ShapelessRecipe* shapeless = (ShapelessRecipe*) recipe;
		if (shapeless->output.size() != 1) {
			//no outputs?!
			continue;
		}
		ItemInstance* myitem = &shapeless->output[0];
		int myitemid = bl_ItemInstance_getId(myitem);
		if (myitemid == itemId && myitem->damage == itemDamage) {
			//TODO check existing recipe to see if recipes match.
			recipesList->erase(recipesList->begin() + i);
			return true;
		}
	}
	return false;
}

bool bl_lookForExistingRecipe(Recipes* recipeMgr, int itemId, int itemCount, int itemDamage, int ingredients[], int ingredientsCount) {
	std::vector<Recipe*>* recipesList = &recipeMgr->recipes;
	int recipesSize = recipesList->size();
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Recipes: %i", recipesSize);
	for (int i = recipesSize - 1; i >= 0; i--) { //TODO: inefficient?
		Recipe* recipe = (*recipesList)[i];
		ShapedRecipe* shaped = (ShapedRecipe*) recipe;
		ItemInstance* myitem = shaped->output;
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Recipe: id %d count %d damage %d",
			bl_ItemInstance_getId(myitem), myitem->count, myitem->damage);
	}
	return false;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddShapedRecipe
  (JNIEnv *env, jclass clazz, jint itemId, jint itemCount, jint itemDamage, jobjectArray shape, jintArray ingredientsArray) {
	std::vector<std::string> shapeVector;
	int shapeLength = env->GetArrayLength(shape);
	for (int i = 0; i < shapeLength; i++) {
		jstring myString = (jstring) env->GetObjectArrayElement(shape, i);
		const char * myStringChars = env->GetStringUTFChars(myString, NULL);
		shapeVector.emplace_back(myStringChars);
		env->ReleaseStringUTFChars(myString, myStringChars);
	}
	int ingredientsElemsCount = env->GetArrayLength(ingredientsArray);
	int ingredients[ingredientsElemsCount];
	env->GetIntArrayRegion(ingredientsArray, 0, ingredientsElemsCount, ingredients);

	ItemInstance outStack(itemId, itemCount, itemDamage);
	std::vector<ItemInstance> outStacks;
	outStacks.push_back(outStack);

	int ingredientsCount = ingredientsElemsCount / 3;
	std::vector<RecipesType> ingredientsList;
	for (int i = 0; i < ingredientsCount; i++) {
		RecipesType recipeType;
		int inputId = ingredients[i * 3 + 1];
		recipeType.tile = nullptr; //inputId < 0x100? bl_Block_mBlocks[inputId]: nullptr;
		recipeType.item = nullptr; //bl_Item_mItems[inputId];//nullptr;
		recipeType.itemInstance.damage = ingredients[i * 3 + 2];
		recipeType.itemInstance.count = 1;
		recipeType.itemInstance.tag = NULL;
		bl_ItemInstance_setId(&recipeType.itemInstance, inputId);
		recipeType.letter = (char) ingredients[i * 3];
		ingredientsList.push_back(recipeType);
	}
	Recipes* recipes = bl_Recipes_getInstance();
	//bl_tryRemoveExistingRecipe(recipes, itemId, itemCount, itemDamage, ingredients, ingredientsCount);
	bl_Recipes_addShapedRecipe(recipes, outStacks, shapeVector, ingredientsList);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddFurnaceRecipe
  (JNIEnv *env, jclass clazz, jint inputId, jint outputId, jint outputDamage) {
	// You don't need count, not sure how to omit it completely
  	ItemInstance outputStack(outputId, 1, outputDamage);
  	FurnaceRecipes* recipes = bl_FurnaceRecipes_getInstance();
	bl_FurnaceRecipes_addFurnaceRecipe(recipes, inputId, outputStack);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeShowTipMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	bl_minecraft->getGuiData()->showTipMessage(mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntitySetNameTag
  (JNIEnv *env, jclass clazz, jlong entityId, jstring name) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	const char * nameUtfChars = env->GetStringUTFChars(name, NULL);
	entity->setNameTagVisible(true);
	entity->setNameTag(std::string(nameUtfChars));
	env->ReleaseStringUTFChars(name, nameUtfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetStonecutterItem
  (JNIEnv *env, jclass clazz, jint itemId, jint status) {
	bl_stonecutter_status[itemId] = status;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetItemCategory
  (JNIEnv *env, jclass clazz, jint itemId, jint category, jint mystery1) {
	Item* myitem = bl_Item_mItems[itemId];
	bl_Item_setCategory(myitem, category);
	if (itemId < 256) {
		Block* myblock = bl_Block_mBlocks[itemId];
		myblock->setCategory((CreativeItemCategory)category);
	}
}

void bl_sendPacket(Packet* packet) {
	PacketSender* sender = bl_Minecraft_getPacketSender(bl_minecraft->getServer());
	sender->send(*packet);
/*
	void* bl_raknet = *((void**) (((uintptr_t) bl_minecraft) + MINECRAFT_RAKNET_INSTANCE_OFFSET));
	void* vtable = ((void***) bl_raknet)[0][RAKNET_INSTANCE_VTABLE_OFFSET_SEND];
	void (*fn)(void*, void*) = (void (*) (void*, void*)) vtable;
	fn(bl_raknet, packet);
*/
}

void bl_sendIdentPacket() {
	/*
	 * format of the ident packet: SUBJECT TO EXTREME CHANGE!!!1eleven
	 * Currently a SetTimePacket sent from client to server. Vanilla servers *should* ignore it, as well as old PocketMine.
	 * The magic time value is 0x1abe11ed, and the boolean sent is false.
	 */
	/* Disable in 1.6.8 pending crash investigation
	SetTimePacket packet;
	bl_Packet_Packet(&packet);
	packet.vtable = (void**) (((uintptr_t) bl_SetTimePacket_vtable) + 8);
	packet.time = 0x1abe11ed;
	packet.started = false;
	bl_sendPacket(&packet);
	*/
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSendChat
  (JNIEnv *env, jclass clazz, jstring message) {
#if 0
	const char * messageUtfChars = env->GetStringUTFChars(message, NULL);
	std::string* myName = bl_Entity_getNameTag(bl_localplayer);
	TextPacket textPacket;
	textPacket.type = 1;
	memset(&(textPacket.filler2), 0, sizeof(textPacket.filler2));
	memset(&(textPacket.filler3), 0, sizeof(textPacket.filler3));
	textPacket.shouldBeZero = 0;
	textPacket.shouldBeOne = 1;
	textPacket.username = *myName;
	textPacket.message = messageUtfChars;
	bl_sendPacket(&textPacket);
	env->ReleaseStringUTFChars(message, messageUtfChars);
#endif
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetNameTag
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == nullptr) return nullptr;
	std::string* mystr = bl_Entity_getNameTag(entity);
	jstring returnValString = env->NewStringUTF(mystr->c_str());
	return returnValString;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetRiding
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return -1;
	Entity* riding = entity->getRide();
	if (riding == NULL) return -1;
	return riding->getUniqueID();
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetRider
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return -1;
	Entity* rider = entity->rider;
	if (rider == NULL) return -1;
	return rider->getUniqueID();
}

static const char* bl_getOriginalSkin(Entity* entity) {
	const char* retval;
	// get the entity renderer and ask it for the skin
	EntityRenderer* renderer = EntityRenderDispatcher::getInstance().getRenderer(*entity);
	if (renderer != nullptr) {
		void** vtable = *((void***)renderer);
		mce::TexturePtr const& (*getSkinPtr)(EntityRenderer*, Entity*)
			= (mce::TexturePtr const& (*)(EntityRenderer*, Entity*))
			vtable[vtable_indexes.mobrenderer_get_skin_ptr - 2];
		mce::TexturePtr const& texPtr = getSkinPtr(renderer, entity);
		retval = texPtr.textureName.c_str();
	} else {
		retval = "";
	}
	return retval;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetMobSkin
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == nullptr) return nullptr;
	const char* retval;
	auto foundIter = bl_mobTexturesMap.find(entity->getUniqueID());
	if (foundIter != bl_mobTexturesMap.end()) {
		retval = foundIter->second.textureName.c_str();
	} else {
		retval = bl_getOriginalSkin(entity);
	}
	jstring returnValString = env->NewStringUTF(retval);
	return returnValString;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityHasCustomSkin
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == nullptr) return false;
	auto foundIter = bl_mobTexturesMap.find(entity->getUniqueID());
	return foundIter != bl_mobTexturesMap.end();
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetRenderType
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return -1;
	return bl_renderManager_getRenderType(entity);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCameraEntity
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_minecraft->setCameraEntity(entity);
}

JNIEXPORT jlongArray JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetUUID
  (JNIEnv *env, jclass clazz, jlong entityId) {
	return nullptr;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelAddParticle
  (JNIEnv *env, jclass clazz, jstring type, jfloat x, jfloat y, jfloat z, jfloat xVel, jfloat yVel, jfloat zVel, jint data) {
	if (!type) return;
	const char * typeChars = env->GetStringUTFChars(type, nullptr);
	std::string typeStr(typeChars);
	env->ReleaseStringUTFChars(type, typeChars);
	int particleType = ParticleTypeFromString(typeStr);
	if (particleType <= 0) return;
	Vec3 pos {x, y, z};
	Vec3 vel {xVel, yVel, zVel};
	bl_Level_addParticle(bl_level, particleType, pos, vel, data);
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelIsRemote
  (JNIEnv *env, jclass clazz) {
	if (!bl_level) return false;
	return bl_level->isClientSide();
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeIsBlockTextureAtlasLoaded
  (JNIEnv *env, jclass clazz) {
	return BlockGraphics::mTerrainTextureAtlas != nullptr;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefinePlaceholderBlocks
  (JNIEnv *env, jclass clazz) {
	for (int i = 1; i < 0x100; i++) {
		//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "mBlocks[%d] = %p", i, Block::mBlocks[i]);
		if (Block::mBlocks[i] == NULL) {
			char name[100];
			snprintf(name, sizeof(name), "Missing block ID: %d", i);
			std::string textureNames[16*6];
			for (int a = 0; a < 16*6; a++) {
				textureNames[a] = "cobblestone";
			}
			int textureCoords[16*6];
			memset(textureCoords, 0, sizeof(textureCoords));
			bl_createBlock(i, textureNames, textureCoords, 17 /* wood */, true, 0, (const char*) name, 0);
		}
	}
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetPointedEntity
  (JNIEnv *env, jclass clazz) {
	if (!bl_level) return -1;
	HitResult const& objectMouseOver = bl_level->getHitResult();
	if (objectMouseOver.type != HIT_RESULT_ENTITY) return -1;
	//Entity* hoverEntity = *((Entity**) ((uintptr_t) bl_level + MINECRAFT_HIT_ENTITY_OFFSET));
	Entity* hoverEntity = objectMouseOver.entity;
	if (hoverEntity == NULL) return -1;
	return hoverEntity->getUniqueID();
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetPointedBlock
  (JNIEnv *env, jclass clazz, jint axis) {
	if (!bl_level) return -1;
	HitResult const& objectMouseOver = bl_level->getHitResult();
	//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "hit %d %d %d %d", objectMouseOver->type,
	//	objectMouseOver->x, objectMouseOver->y, objectMouseOver->z);
	if (objectMouseOver.type != HIT_RESULT_BLOCK) return -1;
	switch (axis) {
		case AXIS_X:
			return objectMouseOver.x;
		case AXIS_Y:
			return objectMouseOver.y;
		case AXIS_Z:
			return objectMouseOver.z;
		case BLOCK_SIDE:
			return objectMouseOver.side;
		case BLOCK_ID:
			return bl_localplayer->getRegion()->getBlockID(
				objectMouseOver.x, objectMouseOver.y, objectMouseOver.z);
		case BLOCK_DATA:
			return bl_localplayer->getRegion()->getData(
				objectMouseOver.x, objectMouseOver.y, objectMouseOver.z);
		default:
			return -1;
	}
}

JNIEXPORT float JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetPointedVec
  (JNIEnv *env, jclass clazz, jint axis) {
	if (!bl_level) return -1;
	HitResult const& objectMouseOver = bl_level->getHitResult();
	switch (axis) {
		case AXIS_X:
			return objectMouseOver.hitVec.x;
		case AXIS_Y:
			return objectMouseOver.hitVec.y;
		case AXIS_Z:
			return objectMouseOver.hitVec.z;
		default:
			return -1;
	}
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetBiome
  (JNIEnv *env, jclass clazz, jint x, jint z) {
	if (bl_level == NULL) return 0;
	BlockPos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	Biome* biome = bl_localplayer->getRegion()->getBiome(pos);
	if (biome == NULL) return 0;
	return biome->id;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetBiomeName
  (JNIEnv *env, jclass clazz, jint x, jint z) {
	if (bl_level == NULL) return 0;
	BlockPos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	Biome* biome = bl_localplayer->getRegion()->getBiome(pos);
	if (biome == NULL) return NULL;
	jstring retval = env->NewStringUTF(biome->name.c_str());
	return retval;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetGrassColor
  (JNIEnv *env, jclass clazz, jint x, jint z) {
	if (bl_level == NULL) return 0;
	BlockPos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	return bl_localplayer->getRegion()->getGrassColor(pos);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelSetGrassColor
  (JNIEnv *env, jclass clazz, jint x, jint z, jint color) {
	if (bl_level == NULL) return;
	BlockPos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	bl_localplayer->getRegion()->setGrassColor(color, pos, 3); //if you recall, 3 = full block update
}

static Abilities* bl_getAbilities(Player* player) {
	return ((Abilities*) ((uintptr_t) player + PLAYER_ABILITIES_OFFSET));
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerIsFlying
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return false;
	return bl_getAbilities(bl_localplayer)->flying;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetFlying
  (JNIEnv *env, jclass clazz, jboolean val) {
	if (bl_localplayer == NULL) return;
	if (bl_onLockDown) return;
	bl_getAbilities(bl_localplayer)->flying = val;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerCanFly
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return false;
	return bl_getAbilities(bl_localplayer)->mayFly;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetCanFly
  (JNIEnv *env, jclass clazz, jboolean val) {
	if (bl_localplayer == NULL) return;
	if (bl_onLockDown) return;
	bl_getAbilities(bl_localplayer)->mayFly = val;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetCollisionEnabled
  (JNIEnv *env, jclass clazz, jint blockId, jboolean collide) {
	if (blockId < 0 || blockId > 255) return;
	bl_custom_block_collisionDisabled[blockId] = !collide;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelSetBiome
  (JNIEnv *env, jclass clazz, jint x, jint z, jint id) {
	if (bl_level == nullptr) return;
	LevelChunk* chunk = bl_localplayer->getRegion()->getChunk(x >> 4, z >> 4);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Chunk: %p", chunk);
	if (chunk == nullptr) return;
	ChunkTilePos pos;
	pos.x = x & 0xf;
	pos.y = 64;
	pos.z = z & 0xf;
	Biome* biome = bl_Biome_getBiome(id);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Biome: %p", biome);
	if (biome == nullptr) return;
	bl_LevelChunk_setBiome(chunk, *biome, pos);
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBiomeIdToName
  (JNIEnv *env, jclass clazz, jint id) {
	Biome* biome = bl_Biome_getBiome(id);
	if (biome == nullptr) return nullptr;
	jstring retval = env->NewStringUTF(biome->name.c_str());
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeForceCrash
  (JNIEnv *env, jclass clazz) {
	*((int*) 0xdeadfa11) = 0xd15ea5e;
};

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntitySetSize
  (JNIEnv *env, jclass clazz, jlong entityId, jfloat a, jfloat b) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setSize(entity, a, b);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetHandEquipped
  (JNIEnv *env, jclass clazz, jint id, jboolean handEquipped) {
	Item* item = bl_Item_mItems[id];
	if (item == nullptr) return;
	item->handEquipped = handEquipped;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSpawnerSetEntityType
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint entityTypeId) {
	if (bl_level == NULL) return;
	MobSpawnerBlockEntity* te = static_cast<MobSpawnerBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return;

	BaseMobSpawner* spawner = te->getSpawner();
	bl_BaseMobSpawner_setEntityId(spawner, entityTypeId);
	te->setChanged();
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSpawnerGetEntityType
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	if (bl_level == NULL) return 0;
	MobSpawnerBlockEntity* te = static_cast<MobSpawnerBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return 0;

	BaseMobSpawner* spawner = te->getSpawner();
	if (!spawner) return 0;
	return spawner->getSpawnTypeId();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeScreenChooserSetScreen
  (JNIEnv *env, jclass clazz, jint screen) {
	//bl_ScreenChooser_setScreen(*((ScreenChooser**) ((uintptr_t) bl_minecraft + MINECRAFT_SCREENCHOOSER_OFFSET)), screen);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeCloseScreen
  (JNIEnv *env, jclass clazz) {
/*
	//bl_MinecraftClient_setScreen(bl_minecraft, nullptr);
	AbstractScreen* screen = bl_minecraft->getScreen();
	if (!screen) return;
	bl_minecraft->getScreenChooser().popScreen(*screen, 1);
*/
}
JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeShowProgressScreen
  (JNIEnv *env, jclass clazz) {
	/* FIXME 0.11
	void* progress = operator new(224);
	bl_ProgressScreen_ProgressScreen(progress);
	bl_MinecraftClient_setScreen(bl_minecraft, progress);
	*/
}

void bl_Mob_die_hook(Entity* entity1, EntityDamageSource const& damageSource) {
	JNIEnv *env;
	preventDefaultStatus = false;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "mobDieCallback", "(JJ)V");
	long long victimId = entity1->getUniqueID();
	long long attackerId = -1;
	if (damageSource.isEntitySource()) {
		attackerId = damageSource.getEntity()->getUniqueID();
	}

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, attackerId, victimId);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_Mob_die_real(entity1, damageSource);
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSpawnEntity
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jint type, jstring skinPath) {
	//TODO: spawn entities, not just mobs
	Vec3 pos;
	pos.x = x;
	pos.y = y;
	pos.z = z;
	Vec2 rot {0, 0};
	EntityDefinitionIdentifier identifier((EntityType)type);
	std::unique_ptr<Entity> entity = EntityFactory::createSpawnedEntity(identifier, bl_localplayer, pos, rot);

	if (entity == nullptr) {
		//WTF?
		return -1;
	}
	Entity* e = entity.get();
	bl_Entity_setPos_helper(e, x, y, z);

	if (type != 93) {
		bl_level->addEntity(*bl_localplayer->getRegion(), std::move(entity));
	} else { // lightning
		bl_level->addGlobalEntity(*bl_localplayer->getRegion(), std::move(entity));
	}

	//skins
	if (skinPath != NULL && type < 64) {
		const char * skinUtfChars = env->GetStringUTFChars(skinPath, NULL);
		if (strcmp(bl_getOriginalSkin(e), skinUtfChars) != 0) {
			bl_changeEntitySkin(e, skinUtfChars);
		}
		env->ReleaseStringUTFChars(skinPath, skinUtfChars);
	}

	return e->getUniqueID();

}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDropItem
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jfloat range, jint id, jint count, jint damage) {

	ItemInstance instance(id, count, damage);

	Entity* entity = bl_level->getSpawner()->spawnItem(*bl_localplayer->getRegion(),
		instance, bl_localplayer, Vec3(x, y + range, z), 10);

	return entity->getUniqueID();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetUseController
  (JNIEnv *env, jclass clazz, jboolean use) {
	if (bl_forceController) return;
#ifdef __arm__
	unsigned char* ptr = (unsigned char*) bl_marauder_translation_function(
		(void*) (((uintptr_t) bl_MinecraftClient_useController) & ~1));
	*ptr = use;
#endif
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDumpVtable
  (JNIEnv *env, jclass clazz, jstring message, jint size) {
	const char * messageUtfChars = env->GetStringUTFChars(message, NULL);
	void* vtable = dlsym(RTLD_DEFAULT, messageUtfChars);
	if (vtable != nullptr) {
		bl_dumpVtable((void**) vtable, size);
	}
	env->ReleaseStringUTFChars(message, messageUtfChars);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_MobEffect_nativePopulate
  (JNIEnv *env, jclass clazz, jstring message) {
	const char * messageUtfChars = env->GetStringUTFChars(message, NULL);
	void* effect = dlsym(RTLD_DEFAULT, messageUtfChars);
	jint ret = -1;
	if (effect != nullptr) {
		ret = (*((MobEffect**) effect))->getId();
	}
	env->ReleaseStringUTFChars(message, messageUtfChars);
	return ret;
}

static void (*bl_Mob_addEffect)(Entity*, MobEffectInstance&);
static void (*bl_Mob_removeEffect)(Entity*, int);
static void (*bl_Mob_removeAllEffects)(Entity*);

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobAddEffect
  (JNIEnv* env, jclass clazz, jlong entityId, jint id, jint duration, jint amplifier, jboolean ambient, jboolean showParticles) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	MobEffectInstance inst(id, duration, amplifier, ambient, showParticles);
	bl_Mob_addEffect(entity, inst);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobRemoveEffect
  (JNIEnv* env, jclass clazz, jlong entityId, jint id) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Mob_removeEffect(entity, id);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobRemoveAllEffects
  (JNIEnv* env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Mob_removeAllEffects(entity);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemEntityItem
  (JNIEnv* env, jclass clazz, jlong entityId, jint type) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	ItemInstance* instance = (ItemInstance*) (((uintptr_t) entity) + kItemEntity_itemInstance_offset);
	switch (type) {
		case ITEMID:
			return bl_ItemInstance_getId(instance);
		case DAMAGE:
			return instance->damage;
		case AMOUNT:
			return instance->count;
		default:
			return 0;
	}
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetMobHealth
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	return bl_Mob_getHealth(entity);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetMobHealth
  (JNIEnv *env, jclass clazz, jlong entityId, jint halfhearts) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(entity, SharedAttributes::HEALTH);
	if (attrib) attrib->value = halfhearts;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetMobMaxHealth
  (JNIEnv *env, jclass clazz, jlong entityId, jint halfhearts) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(entity, SharedAttributes::HEALTH);
	if (attrib) attrib->setMaxValue(halfhearts);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetMobMaxHealth
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return -1;
	AttributeInstance* attrib = bl_Mob_getAttribute(entity, SharedAttributes::HEALTH);
	if (attrib) return attrib->getMaxValue();
	return -1;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetHunger
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return -1;
	AttributeInstance* attrib = bl_Mob_getAttribute(entity, *bl_Player_HUNGER);
	if (attrib) return attrib->value;
	return -1;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetHunger
  (JNIEnv *env, jclass clazz, jlong entityId, jfloat hunger) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(entity, *bl_Player_HUNGER);
	if (attrib) attrib->value = hunger;
}

static void generateBl(uint16_t* buffer, uintptr_t curpc, uintptr_t newpc) {
	unsigned int diff = newpc - curpc;
	unsigned int shiftdiff = (diff >> 1);
	unsigned int lowerHalf = shiftdiff & 0x7ff;
	unsigned int topHalf = (shiftdiff >> 11) & 0x7ff;
	unsigned int topInst = topHalf | 0xf000;
	unsigned int bottomInst = lowerHalf | 0xf800;
	buffer[0] = (uint16_t) topInst;
	buffer[1] = (uint16_t) bottomInst;
}

void bl_forceTextureLoad(std::string const& name) {
	if (name.length() == 0) return;
	BL_LOG("Forcing texture reload: %s canLoad: %s", name.c_str(), mce::TextureGroup::mCanLoadTextures? "yes": "no");
	bool old = mce::TextureGroup::mCanLoadTextures;
	mce::TextureGroup::mCanLoadTextures = true;
	mce::TexturePtr(bl_minecraft->getTextures(), ResourceLocation(name));
	mce::TextureGroup::mCanLoadTextures = old;
}
void bl_reload_armor_textures();
void bl_cppNewLevelInit() {
	//bl_entityUUIDMap.clear();
	bl_reload_armor_textures();
}

void bl_set_i18n(std::string const& key, std::string const& value) {
	(I18n::mCurrentLanguage->_getStrings())[key] = value;
}

static bool isLocalAddress(JNIEnv* env, jstring hostJString) {
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "isLocalAddress", "(Ljava/lang/String;)Z");

	return env->CallStaticBooleanMethod(bl_scriptmanager_class, mid, hostJString);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemIdCount
  (JNIEnv* env, jclass clazz) {
	return bl_item_id_count;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeIsValidItem
  (JNIEnv *env, jclass clazz, jint itemId) {
	if (itemId == 0) return true;
	if (itemId < 0 || itemId >= bl_item_id_count) return false;
	return bl_Item_mItems[itemId] != nullptr;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeIsValidCommand
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	MinecraftCommands* commands = bl_minecraft->getServer()->getCommands();
	if (!commands) return false;
	bool hasCommand = commands->getCommand(mystr, 0) != nullptr;
	//BL_LOG("command: %s hasCommand: %s", utfChars, hasCommand? "yes": "no");
	env->ReleaseStringUTFChars(text, utfChars);
	return hasCommand;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockGetSecondPart
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint axis) {
	if (!bl_localplayer) return -1;
	int blockId = bl_localplayer->getRegion()->getBlockID(x, y, z);
	if (blockId == 0) return -1;
	Tile* tile = bl_Block_mBlocks[blockId];
	if (!tile) return -1;
	void* methodPtr = tile->vtable[vtable_indexes.tile_get_second_part];
	bool (*getSecondPart)(Tile*, TileSource&, TilePos const&, TilePos&) =
		(bool (*)(Tile*, TileSource&, TilePos const&, TilePos&)) methodPtr;
	TilePos tilePos;
	tilePos.x = x;
	tilePos.y = y;
	tilePos.z = z;
	TilePos tilePosOut;
	bool ret = getSecondPart(tile, *(bl_localplayer->getRegion()), tilePos, tilePosOut);
	if (!ret) return -1;
	switch (axis) {
		case AXIS_X:
			return tilePosOut.x;
		case AXIS_Y:
			return tilePosOut.y;
		case AXIS_Z:
			return tilePosOut.z;
		default:
			abort();
	}
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetDimension
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return 0;
	return bl_Entity_getDimensionId(bl_localplayer);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetLightningLevel
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return 0;
	return bl_localplayer->getRegion()->getDimension()->getWeather()->getLightningLevel(0);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelSetLightningLevel
  (JNIEnv *env, jclass clazz, float amount) {
	if (!bl_localplayer) return;
	return bl_localplayer->getRegion()->getDimension()->getWeather()->setLightningLevel(amount);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetRainLevel
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return 0;
	return bl_localplayer->getRegion()->getDimension()->getWeather()->getRainLevel(0);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelSetRainLevel
  (JNIEnv *env, jclass clazz, float amount) {
	if (!bl_localplayer) return;
	return bl_localplayer->getRegion()->getDimension()->getWeather()->setRainLevel(amount);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetExhaustion
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return -1;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_EXHAUSTION);
	if (attrib) return attrib->value;
	return -1;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetExhaustion
  (JNIEnv *env, jclass clazz, jfloat value) {
	if (!bl_localplayer) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_EXHAUSTION);
	if (attrib) attrib->value = value;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetSaturation
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return -1;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_SATURATION);
	if (attrib) return attrib->value;
	return -1;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetSaturation
  (JNIEnv *env, jclass clazz, jfloat value) {
	if (!bl_localplayer) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_SATURATION);
	if (attrib) attrib->value = value;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetLevel
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return -1;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_LEVEL);
	if (attrib) return attrib->value;
	return -1;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetLevel
  (JNIEnv *env, jclass clazz, jint value) {
	if (!bl_localplayer) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_LEVEL);
	if (attrib) attrib->value = value;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetExperience
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return -1;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_EXPERIENCE);
	if (attrib) return attrib->value;
	return -1;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetExperience
  (JNIEnv *env, jclass clazz, jfloat value) {
	if (!bl_localplayer) return;
	AttributeInstance* attrib = bl_Mob_getAttribute(bl_localplayer, *bl_Player_EXPERIENCE);
	if (attrib) attrib->value = value;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerAddExperience
  (JNIEnv *env, jclass clazz, jint value) {
	if (!bl_localplayer) return;
	bl_Player_addExperience_real(bl_localplayer, value);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetScore
  (JNIEnv *env, jclass clazz) {
	if (!bl_localplayer) return 0;
	return bl_localplayer->getScore();
}

/*ItemGraphics const& bl_ItemRenderer_getGraphics_hook(ItemInstance const& itemStack) {
	if (itemStack.getId() >= 0x200) { // extended item ID
		return ItemRenderer::mItemGraphics[0x100];
	}
	return ItemRenderer::mItemGraphics[itemStack.getId()];
}

ItemGraphics const& bl_ItemRenderer_getGraphics_hook_item(Item* item) {
	if (item->itemId >= 0x200) { // extended item ID
		return ItemRenderer::mItemGraphics[0x100];
	}
	return ItemRenderer::mItemGraphics[item->itemId];
}
*/


mce::TexturePtr const& bl_MobRenderer_getSkinPtr_hook(MobRenderer* renderer, Entity& ent) {
	if (ent.getLevel() == nullptr) return bl_MobRenderer_getSkinPtr_real(renderer, ent);
	auto foundIter = bl_mobTexturesMap.find(ent.getUniqueID());
	if (foundIter != bl_mobTexturesMap.end()) {
		return foundIter->second;
	}
	return bl_MobRenderer_getSkinPtr_real(renderer, ent);
}

static void* bl_SkeletonRenderer_render_hook(MobRenderer* renderer, Entity& ent, Vec3 const& vec, float a, float b) {
	auto foundIter = bl_mobTexturesMap.find(ent.getUniqueID());
	if (foundIter != bl_mobTexturesMap.end()) {
		return bl_MobRenderer_render_real(renderer, ent, vec, a, b);
	}
	return bl_SkeletonRenderer_render_real(renderer, ent, vec, a, b);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockGetDestroyTime
  (JNIEnv *env, jclass clazz, jint blockId, jint damage) {
	Block* block = bl_Block_mBlocks[blockId];
	if (!block) return -1;
	return block->getDestroySpeed();
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockGetFriction
  (JNIEnv *env, jclass clazz, jint blockId) {
	Block* block = bl_Block_mBlocks[blockId];
	if (!block) return -1;
	return block->getFriction();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetFriction
  (JNIEnv *env, jclass clazz, jint blockId, jfloat friction) {
	Block* block = bl_Block_mBlocks[blockId];
	if (!block) return;
	block->setFriction(friction);
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelCanSeeSky
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	if (!bl_localplayer) return false;
	return bl_localplayer->getRegion()->canSeeSky(x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetRedstoneConsumer
  (JNIEnv *env, jclass clazz, jint blockId, jboolean enabled) {
	if (enabled) bl_custom_block_redstone[blockId] |= REDSTONE_CONSUMER;
	else bl_custom_block_redstone[blockId] &= ~REDSTONE_CONSUMER;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeItemSetProperties
  (JNIEnv *env, jclass clazz, jint itemId, jstring text) {
	Item* item = bl_Item_mItems[itemId];
	if (!item) return false;
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	Json::Value jsonValue;
	Json::Reader jsonReader;
	bool ret = false;
	if (jsonReader.parse(std::string(utfChars), jsonValue)) {
		ret = true;
		item->initServer(jsonValue);
		if (Item::mItemTextureAtlas != nullptr) item->initClient(jsonValue, jsonValue);
		auto request = std::make_shared<BLItemSetJsonRequest>();
		request->itemId = item->itemId;
		request->value = std::string(utfChars);
		itemSetIconRequests.push_back(request);
	}
	env->ReleaseStringUTFChars(text, utfChars);
	return ret;
}

static void* (*bl_Throwable_throwableHit_real)(void* component, HitResult const& hitResult);

void* bl_Throwable_throwableHit_hook(void* projectileComponent, HitResult const& hitResult) {
	Entity* entity = *((Entity**)(((uintptr_t)projectileComponent) + kProjectileComponent_entity_offset));
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "throwableHitCallback", "(JIIIIIFFFJ)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->getUniqueID(),
		hitResult.type, hitResult.side, hitResult.x, hitResult.y, hitResult.z,
		hitResult.hitVec.x, hitResult.hitVec.y, hitResult.hitVec.z,
		hitResult.type == HIT_RESULT_ENTITY? (jlong)hitResult.entity->getUniqueID(): (jlong)0);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	return bl_Throwable_throwableHit_real(projectileComponent, hitResult);
}

JNIEXPORT jstring Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetI18NString
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string const& returnVal = (I18n::getCurrentLanguage()->_getStrings())[utfChars];
	env->ReleaseStringUTFChars(text, utfChars);

	jstring returnValString = env->NewStringUTF(returnVal.c_str());
	return returnValString;
}

JNIEXPORT jstring Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetLanguageName
  (JNIEnv *env, jclass clazz) {
	std::string returnVal = I18n::getCurrentLanguage()->getFullLanguageCode();
	jstring returnValString = env->NewStringUTF(returnVal.c_str());
	return returnValString;
}

JNIEXPORT jint Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeItemGetUseAnimation
  (JNIEnv *env, jclass clazz, jint id) {
	if (id < 0 || id >= bl_item_id_count) return -1;
	Item* item = bl_Item_mItems[id];
	if (!item) return -1;
	return item->useAnimation;
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeItemSetUseAnimation
  (JNIEnv *env, jclass clazz, jint id, jint animation) {
	if (id < 0 || id >= bl_item_id_count) return;
	Item* item = bl_Item_mItems[id];
	if (!item) return;
	item->useAnimation = animation;
}

JNIEXPORT jboolean Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerEnchant
  (JNIEnv *env, jclass clazz, jint slot, jint enchantmentId, jint enchantmentLevel) {
	auto inventory = bl_localplayer->getSupplies();
	ItemInstance* itemInstance = inventory->getItem(slot);
	if (itemInstance == nullptr) return false;
	return EnchantUtils::applyEnchant(*itemInstance, enchantmentId, enchantmentLevel);
}

JNIEXPORT jintArray Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetEnchantments
  (JNIEnv *env, jclass clazz, jint slot) {
	auto inventory = bl_localplayer->getSupplies();
	ItemInstance* itemInstance = inventory->getItem(slot);
	if (itemInstance == nullptr) return nullptr;
	std::vector<EnchantmentInstance> enchantments = itemInstance->getEnchantsFromUserData().getAllEnchants();
	int arrLen = enchantments.size() * 2;
	int tempArr[arrLen];
	int i = 0;
	for (EnchantmentInstance& e: enchantments) {
		tempArr[i++] = e.type;
		tempArr[i++] = e.level;
	}
	jintArray returnVal = env->NewIntArray(arrLen);
	env->SetIntArrayRegion(returnVal, 0, arrLen, tempArr);
	return returnVal;
}

JNIEXPORT jstring Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetItemCustomName
  (JNIEnv *env, jclass clazz, jint slot) {
	auto inventory = bl_localplayer->getSupplies();
	ItemInstance* itemInstance = inventory->getItem(slot);
	if (itemInstance == nullptr) return nullptr;
	if (!itemInstance->hasCustomHoverName()) return nullptr;
	const char* name = itemInstance->getCustomName().c_str();
	return env->NewStringUTF(name);
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerSetItemCustomName
  (JNIEnv *env, jclass clazz, jint slot, jstring name) {
	auto inventory = bl_localplayer->getSupplies();
	ItemInstance* itemInstance = inventory->getItem(slot);
	if (itemInstance == nullptr) return;
	const char* nameUtf = env->GetStringUTFChars(name, nullptr);
	itemInstance->setCustomName(std::string(nameUtf));
	env->ReleaseStringUTFChars(name, nameUtf);
}

JNIEXPORT jstring Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobGetArmorCustomName
  (JNIEnv *env, jclass clazz, jlong entityId, jint slot) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return nullptr;
	//Geting the item
	ItemInstance* itemInstance = bl_Mob_getArmor(entity, slot);
	if (itemInstance == nullptr) return nullptr;
	if (!itemInstance->hasCustomHoverName()) return nullptr;
	const char* name = itemInstance->getCustomName().c_str();
	return env->NewStringUTF(name);
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobSetArmorCustomName
  (JNIEnv *env, jclass clazz, jlong entityId, jint slot, jstring name) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//Geting the item
	ItemInstance* itemInstance = bl_Mob_getArmor(entity, slot);
	if (itemInstance == nullptr) return;
	const char* nameUtf = env->GetStringUTFChars(name, nullptr);
	itemInstance->setCustomName(std::string(nameUtf));
	env->ReleaseStringUTFChars(name, nameUtf);
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeItemSetStackedByData
  (JNIEnv *env, jclass clazz, jint id, jboolean stacked) {
	if (id < 0 || id >= bl_item_id_count) return;
	Item* item = bl_Item_mItems[id];
	if (!item) return;
	item->setStackedByData(stacked);
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRecipeSetAnyAuxValue
  (JNIEnv *env, jclass clazz, jint id, jboolean anyAux) {
	if (id < 0 || id >= bl_item_id_count) return;
	bl_anyAuxValue[id] = anyAux? 2: 1;
}

bool bl_Recipe_isAnyAuxValue_hook(int id) {
	auto b = bl_anyAuxValue[id];
	if (b == 0) {
		return bl_Recipe_isAnyAuxValue_real(id);
	}
	return b == 2;
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetAllowEnchantments
  (JNIEnv* env, jclass clazz, jint id, jint mask, jint value) {
	if (id < 0 || id >= bl_item_id_count) return;
	bl_customItem_allowEnchantments[id] = mask;
	bl_customItem_enchantValue[id] = value;
}

JNIEXPORT int Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetDifficulty
  (JNIEnv* env, jclass clazz) {
	if (bl_level == nullptr) return 0;
	return bl_level->getDifficulty();
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelSetDifficulty
  (JNIEnv* env, jclass clazz, int difficulty) {
	if (bl_level == nullptr) return;
	void (*setDifficulty)(Level*, int) = (void (*)(Level*, int)) bl_level->vtable[vtable_indexes.level_set_difficulty - 2];
	setDifficulty(bl_level, difficulty);
}

static void** bl_AppPlatform_vtable;
static void* bl_AppPlatform_getUIScalingRules_real;
static void* bl_AppPlatform_getEdition_real;
static void* bl_AppPlatform_useCenteredGui_real;
static void* bl_AppPlatform_getPlatformType_real;
//static void* bl_AppPlatform_useMetadataDrivenScreens_real;

static int bl_AppPlatform_getPlatformType_hook(void* appPlatform) {
	return 0;
}

static int bl_AppPlatform_getUIScalingRules_hook(void* appPlatform) {
	return 0;
}

static bool bl_AppPlatform_useCenteredGui_hook(void* appPlatform) {
	return true;
}

static std::string bl_AppPlatform_getEdition_hook(void* appPlatform) {
	return "win10";
}

//static bool bl_AppPlatform_useMetadataDrivenScreens_hook(void* appPlatform) {
//	return true;
//}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeModPESetDesktopGui
  (JNIEnv* env, jclass clazz, jboolean desktop) {
	bl_AppPlatform_vtable[vtable_indexes.appplatform_get_ui_scaling_rules] = desktop?
		(void*) &bl_AppPlatform_getUIScalingRules_hook: bl_AppPlatform_getUIScalingRules_real;
	bl_AppPlatform_vtable[vtable_indexes.appplatform_get_platform_type] = desktop?
		(void*) &bl_AppPlatform_getPlatformType_hook: bl_AppPlatform_getPlatformType_real;
	bl_AppPlatform_vtable[vtable_indexes.appplatform_get_edition] = desktop?
		(void*) &bl_AppPlatform_getEdition_hook: bl_AppPlatform_getEdition_real;
	bl_AppPlatform_vtable[vtable_indexes.appplatform_use_centered_gui] = desktop?
		(void*) &bl_AppPlatform_useCenteredGui_hook: bl_AppPlatform_useCenteredGui_real;
//	bl_AppPlatform_vtable[vtable_indexes.appplatform_use_metadata_driven_screens] = desktop?
//		(void*) &bl_AppPlatform_useMetadataDrivenScreens_hook: bl_AppPlatform_useMetadataDrivenScreens_real;
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntitySetImmobile
  (JNIEnv* env, jclass clazz, jlong entityId, jboolean immobile) {
	if (bl_level == nullptr) return;
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == nullptr) return;
/*
	DataItem* synchedData = entity->getEntityData()->_get(0xf);
	if (synchedData == nullptr) return;
	synchedData->thevalue = (char) immobile;
*/
	entity->setStatusFlag(EntityFlagsImmobile, immobile);
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeModPESetRenderDebug
  (JNIEnv* env, jclass clazz, jboolean debug) {
/*
	Options* options = bl_minecraft->getOptions();
	if (options == nullptr) return;
	options->setDevRenderGoalState(debug);
	options->setDevRenderBoundingBoxes(debug);
	options->setDevRenderPaths(debug);
	options->setRenderDebug(debug);
*/
	ScreenView::setDebugRendering(debug);
}

JNIEXPORT jlong Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetTarget
  (JNIEnv* env, jclass clazz, jlong entityId) {
	if (bl_level == nullptr) return -1;
	Mob* entity = static_cast<Mob*>(bl_getEntityWrapper(bl_level, entityId));
	if (entity == nullptr) return -1;
	return entity->getTargetId().id;
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntitySetTarget
  (JNIEnv* env, jclass clazz, jlong entityId, jlong targetId) {
	if (bl_level == nullptr) return;
	Mob* entity = static_cast<Mob*>(bl_getEntityWrapper(bl_level, entityId));
	if (entity == nullptr) return;
	if (targetId == -1) {
		entity->setTarget(nullptr);
	} else {
		Mob* target = static_cast<Mob*>(bl_getEntityWrapper(bl_level, targetId));
		if (target == nullptr) return;
		entity->setTarget(target);
	}
}

void (*bl_Entity_hurt_real)(Entity* entity, EntityDamageSource const& damageSource, int hearts, bool a, bool b);

void bl_Entity_hurt_hook(Entity* entity, EntityDamageSource const& damageSource, int hearts, bool a, bool b) {
	JNIEnv *env;
	preventDefaultStatus = false;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityHurtCallback", "(JJI)V");
	long long victimId = entity->getUniqueID();
	long long attackerId = -1;
	if (damageSource.isEntitySource()) {
		attackerId = damageSource.getEntity()->getUniqueID();
	}

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, attackerId, victimId, hearts);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) bl_Entity_hurt_real(entity, damageSource, hearts, a, b);
}

void bl_Player_addExperience_hook(Player* player, int experience) {
	JNIEnv *env;
	preventDefaultStatus = false;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jlong playerId = player->getUniqueID();

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "playerAddExperienceCallback", "(JI)V");
	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, playerId, experience);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) bl_Player_addExperience_real(player, experience);
}

void bl_Player_addLevels_hook(Player* player, int experience) {
	JNIEnv *env;
	preventDefaultStatus = false;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jlong playerId = player->getUniqueID();

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "playerAddLevelsCallback", "(JI)V");
	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, playerId, experience);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) bl_Player_addLevels_real(player, experience);
}

JNIEXPORT jint Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeItemGetMaxStackSize
  (JNIEnv* env, jclass clazz, jint id) {
	if (id < 0 || id >= bl_item_id_count || !bl_Item_mItems[id]) return -1;
	ItemInstance stack(id, 1, 0);
	return stack.getMaxStackSize();
}

void* bl_MinecraftTelemetry_fireEventScreenChanged_hook(void* a, std::string const& s1, std::unordered_map<std::string, std::string> const& theMap) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "screenChangeCallback", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	jstring s1j = env->NewStringUTF(s1.c_str());
	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, s1j, nullptr, nullptr);
	env->DeleteLocalRef(s1j);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return bl_MinecraftTelemetry_fireEventScreenChanged_real(a, s1, theMap);
}
void bl_armorInit_postLoad();
void (*bl_MinecraftClient_onResourcesLoaded_real)(MinecraftClient*);
void bl_MinecraftClient_onResourcesLoaded_hook(MinecraftClient* client) {
	bl_MinecraftClient_onResourcesLoaded_real(client);
	bl_finishBlockBuildTextureRequests();
	bl_finishItemSetIconRequests();
	bl_repopulateItemGraphics();
	bl_armorInit_postLoad();
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeNewLevelCallbackStarted
  (JNIEnv* env, jclass clazz) {
	bl_customBlocksCreated = false;
}

JNIEXPORT void Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeNewLevelCallbackEnded
  (JNIEnv* env, jclass clazz) {
	if (bl_customBlocksCreated) {
		// some derp created a custom block in newLevel
		BlockGraphics::mTerrainTextureAtlas->loadMetaFile();
		BlockGraphics::initBlocks();
		bl_finishBlockBuildTextureRequests();
	}
}

static bool bl_patchAllItemInstanceConstructors(soinfo2* mcpelibhandle) {
	const char* const toPatch[] = {
"_ZN12ItemInstanceC1EPK4Item",
"_ZN12ItemInstanceC1EPK4Itemi",
"_ZN12ItemInstanceC1EPK4Itemii",
"_ZN12ItemInstanceC1EPK4ItemiiPK11CompoundTag",
"_ZN12ItemInstanceC1Eiii",
"_ZN12ItemInstanceC1EiiiPK11CompoundTag",
"_ZN12ItemInstanceC1ERKS_",
"_ZN12ItemInstance4initEiii",
"_ZN12ItemInstance4loadERK11CompoundTag",
"_ZN12ItemInstance8_setItemEi",
nullptr
	};
	for (int j = 0; ; j++) {
		const char* theName = toPatch[j];
		if (!theName) return true;
		void* ItemInstance_init = (void*) dlsym(mcpelibhandle, theName);
		unsigned char* itemInitCode = (unsigned char*)
			bl_marauder_translation_function((void*)(((uintptr_t) ItemInstance_init) & ~1));
		bool hasSet = false;
#ifdef __arm__
		for (int i = 0; i < (j == 8? 0x900: 0x100); i += 2) {
			// f5b? 7f00
			if ((itemInitCode[i] & 0xf0) == 0xb0 && itemInitCode[i+1] == 0xf5 &&
				itemInitCode[i+2] == 0x00 && itemInitCode[i+3] == 0x7f) {
				itemInitCode[i+2] = 0x80; itemInitCode[i+3] = 0x5f;
				hasSet = true;
				break;
			}
		}
#else
		for (int i = 0; i < (j == 8? 0x900: 0x100); i++) {
			// x86: 81 f? 00 02 00 00
			if (itemInitCode[i] == 0x81 && (itemInitCode[i+1] & 0xf0) == 0xf0
				&& itemInitCode[i+2] == 0x00 && itemInitCode[i+3] == 0x02
				&& itemInitCode[i+4] == 0x00 && itemInitCode[i+5] == 0x00) {
				itemInitCode[i+2] = (BL_ITEMS_EXPANDED_COUNT & 0xff); itemInitCode[i+3] = (BL_ITEMS_EXPANDED_COUNT>>8) & 0xff;
				hasSet = true;
				break;
			}
		}
#endif
		if (!hasSet) {
			__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: can't patch %s",
				theName);
			return false;
		}
	}
	return false; // WHAT
}

static bool bl_patchItemEntityChecks(soinfo2* mcpelibhandle) {
	const char* const toPatch[] = {
"_ZN10ItemEntity15reloadHardcodedEN6Entity20InitializationMethodERK20VariantParameterList",
"_ZN10ItemEntity22readAdditionalSaveDataERK11CompoundTag",
nullptr
	};
	for (int j = 0; ; j++) {
		const char* theName = toPatch[j];
		if (!theName) return true;
		void* ItemInstance_init = (void*) dlsym(mcpelibhandle, theName);
		unsigned char* itemInitCode = (unsigned char*)
			bl_marauder_translation_function((void*)(((uintptr_t) ItemInstance_init) & ~1));
		bool hasSet = false;
#ifdef __arm__
		for (int i = 0; i < 0x400; i += 2) {
			// ebb? 2f50 d1??
			if ((itemInitCode[i] & 0xf0) == 0xb0 && itemInitCode[i+1] == 0xeb &&
				itemInitCode[i+2] == 0x50 && itemInitCode[i+3] == 0x2f && itemInitCode[i+5] == 0xd1) {
				itemInitCode[i+4] = 0x00; itemInitCode[i+5] = 0xbf;
				hasSet = true;
				break;
			}
		}
#else
		// FIXME 0.17 x86
		for (int i = 0; i < 0x400; i++) {
			// x86: 3d ff 01 00 00
			if (itemInitCode[i] == 0x3d && itemInitCode[i+1] == 0xff
				&& itemInitCode[i+2] == 0x01 && itemInitCode[i+3] == 0x00
				&& itemInitCode[i+4] == 0x00) {
				itemInitCode[i+1] = (BL_ITEMS_EXPANDED_COUNT & 0xff); itemInitCode[i+2] = (BL_ITEMS_EXPANDED_COUNT>>8) & 0xff;
				hasSet = true;
				break;
			}
		}
#endif
		if (!hasSet) {
			__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: can't patch %s",
				theName);
			return false;
		}
	}
	return false; // WHAT
}

void bl_prepatch_fakeassets(soinfo2* mcpelibhandle);

void bl_prepatch_cppside(void* mcpelibhandle_) {
	populate_vtable_indexes(mcpelibhandle_);
	soinfo2* mcpelibhandle = (soinfo2*) mcpelibhandle_;
	void* originalItemsAddress = dlsym(mcpelibhandle, "_ZN4Item6mItemsE");
	// Edit the dlsym for Item::items
	Elf_Sym* itemsSym = (Elf_Sym*) bl_marauder_translation_function(
		(void*) dobby_elfsym((void*) mcpelibhandle, "_ZN4Item6mItemsE"));
	// since value + base = addr:
	Elf_Addr oldValue = itemsSym->st_value;
	itemsSym->st_value = ((uintptr_t) &bl_items) - mcpelibhandle->base;
	Elf_Word oldSize = itemsSym->st_size;
	itemsSym->st_size = sizeof(bl_items);
	// now check if the address matches
	void* newItemsAddress = dlsym(mcpelibhandle, "_ZN4Item6mItemsE");
	if (newItemsAddress != &bl_items) {
		// poor man's assert
		// restore stuff
		itemsSym->st_value = oldValue;
		itemsSym->st_size = oldSize;
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: expected %p got %p original %p",
			&bl_items, newItemsAddress, originalItemsAddress);
		return;
	}

	if (!bl_patch_got(mcpelibhandle, originalItemsAddress, (void*) &bl_items)) {
		itemsSym->st_value = oldValue;
		itemsSym->st_size = oldSize;
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: can't find got");
		return;
	}

	if (!bl_patchAllItemInstanceConstructors(mcpelibhandle)) return;
	bl_patchItemEntityChecks(mcpelibhandle);

	bl_item_id_count = BL_ITEMS_EXPANDED_COUNT;
/*
	mcpelauncher_hook((void*) static_cast<mce::TexturePtr const& (*)(ItemInstance const&)>(&ItemRenderer::getGraphics),
		(void*) &bl_ItemRenderer_getGraphics_hook,
		(void**) &bl_ItemRenderer_getGraphics_real);
	mcpelauncher_hook((void*) static_cast<mce::TexturePtr const& (*)(Item const&)>(&ItemRenderer::getGraphics),
		(void*) &bl_ItemRenderer_getGraphics_hook_item,
		(void**) &bl_ItemRenderer_getGraphics_real_item);
*/

	bl_AppPlatform_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV21AppPlatform_android23");
	bl_AppPlatform_getUIScalingRules_real = bl_AppPlatform_vtable[vtable_indexes.appplatform_get_ui_scaling_rules];
	bl_AppPlatform_getEdition_real = bl_AppPlatform_vtable[vtable_indexes.appplatform_get_edition];
	bl_AppPlatform_useCenteredGui_real = bl_AppPlatform_vtable[vtable_indexes.appplatform_use_centered_gui];
	bl_AppPlatform_getPlatformType_real = bl_AppPlatform_vtable[vtable_indexes.appplatform_get_platform_type];
//	bl_AppPlatform_useMetadataDrivenScreens_real = bl_AppPlatform_vtable[vtable_indexes.appplatform_use_metadata_driven_screens];
	bl_prepatch_fakeassets(mcpelibhandle);
}
#define bl_patch_got_wrap(a, b, c) do{if (!bl_patch_got(a, b, c)) {\
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "can't patch GOT: " #b);\
}}while(false)

void bl_setuphooks_cppside() {
	soinfo2* mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);

	bl_MinecraftScreenModel_sendChatMessage_real = (void (*)(MinecraftScreenModel*, std::string const&)) dlsym(mcpelibhandle, "_ZN20MinecraftScreenModel15sendChatMessageERKSs");
	bl_patch_got(mcpelibhandle, (void*)bl_MinecraftScreenModel_sendChatMessage_real, (void*)bl_MinecraftScreenModel_sendChatMessage_hook);

	bl_MinecraftScreenModel_executeCommand_real = (void (*)(MinecraftScreenModel*, std::string const&))  dlsym(mcpelibhandle, "_ZN20MinecraftScreenModel14executeCommandERKSs");
	bl_patch_got(mcpelibhandle, (void*)bl_MinecraftScreenModel_executeCommand_real, (void*)bl_MinecraftScreenModel_executeCommand_hook);

	bl_Item_Item = (void (*)(Item*, std::string const&, short)) dlsym(RTLD_DEFAULT, "_ZN4ItemC1ERKSss");

	// FIXME 0.11
	bl_MinecraftClient_startLocalServer = (void (*) (MinecraftClient*, std::string const&, std::string const&, void*))
		dlsym(mcpelibhandle, "_ZN15MinecraftClient16startLocalServerESsSs13LevelSettings");
	bl_MinecraftClient_leaveGame = (void (*) (MinecraftClient*, bool))
		dlsym(mcpelibhandle, "_ZN15MinecraftClient9leaveGameEb"); //hooked - just pull whichever version MCPE uses
	bl_Minecraft_setLeaveGame = (void (*) (Minecraft*)) dlsym(mcpelibhandle, "_ZN9Minecraft12setLeaveGameEv");

	//bl_Level_getAllEntities = (void* (*)(Level*))
	//	dlsym(RTLD_DEFAULT, "_ZN5Level14getAllEntitiesEv");

	//bl_Level_addListener = (void (*) (Level*, LevelListener*))
	//	dlsym(RTLD_DEFAULT, "_ZN5Level11addListenerEP13LevelListener");

	void* raknet_connect = dlsym(mcpelibhandle, "_ZN14RakNetInstance7connectEN6Social18GameConnectionInfoES1_");

	mcpelauncher_hook(raknet_connect, (void*) &bl_RakNetInstance_connect_hook, (void**) &bl_RakNetInstance_connect_real);

	bl_Item_vtable = (void**) ((uintptr_t) dobby_dlsym((void*) mcpelibhandle, "_ZTV4Item"));
	bl_SnowballItem_vtable = (void**) dobby_dlsym((void*) mcpelibhandle, "_ZTV12SnowballItem");
	//I have no idea why I have to subtract 24 (or add 8).
	//tracing out the original vtable seems to suggest this.

#if 0

	void* fontDrawCached = dlsym(RTLD_DEFAULT, "_ZN4Font10drawCachedERKSsffRK5ColorbP11MaterialPtr");
	mcpelauncher_hook(fontDrawCached, (void*) &bl_Font_drawCached_hook, (void**) &bl_Font_drawCached_real);

	bl_Font_width = (int (*) (Font*, std::string const&))
		dlsym(RTLD_DEFAULT, "_ZN4Font5widthERKSs");

#endif

	bl_Tile_vtable = (void**) ((uintptr_t) dobby_dlsym((void*) mcpelibhandle, "_ZTV5Block"));
	bl_BlockGraphics_vtable = (void**) dobby_dlsym((void*) mcpelibhandle, "_ZTV13BlockGraphics");
	//bl_dumpVtable(bl_Tile_vtable, 0x100);

	bl_Block_Block = (void (*)(Block*, std::string const&, int, void*)) dlsym(RTLD_DEFAULT, "_ZN5BlockC1ERKSsiRK8Material");
	bl_BlockItem_BlockItem = (void (*)(Item*, std::string const&, short)) dobby_dlsym(mcpelibhandle, "_ZN9BlockItemC1ERKSsi");
	bl_Block_setVisualShape = (void (*)(Block*, Vec3 const&, Vec3 const&))
		dlsym(RTLD_DEFAULT, "_ZN5Block14setVisualShapeERK4Vec3S2_");
	bl_Block_mBlocks = (Block**) dlsym(RTLD_DEFAULT, "_ZN5Block7mBlocksE");
	bl_Block_mLightEmission = (unsigned char*) dlsym(RTLD_DEFAULT, "_ZN5Block14mLightEmissionE");
	bl_Block_mLightBlock = (unsigned char*) dlsym(RTLD_DEFAULT, "_ZN5Block11mLightBlockE");

#if 0
#define CHECKVTABLE(actualfn) \
	if (vtable[i] == actualfn) { \
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Found "#actualfn" at %d", i); \
	}
	{
	void** vtable = bl_Tile_vtable;
	void* getTexture = dlsym(mcpelibhandle, "_ZN4Tile10getTextureEai");
	void* getColor = dlsym(mcpelibhandle, "_ZN4Tile8getColorEP10TileSourceiii");

	for (int i = 0; i < BLOCK_VTABLE_SIZE / 4; i++) {
		CHECKVTABLE(getTexture);
		CHECKVTABLE(getColor);
	}
	}
#endif

	bl_LiquidBlockStatic_vtable = (void**) dlsym(mcpelibhandle, "_ZTV17LiquidBlockStatic");
	bl_LiquidBlockDynamic_vtable = (void**) dlsym(mcpelibhandle, "_ZTV18LiquidBlockDynamic");
	// FIXME 0.15
	//bl_LiquidBlockStatic_LiquidBlockStatic = (void (*)(Block*, std::string const&, int, BlockID, void*,
	//	std::string const&, std::string const&))
	//	dlsym(mcpelibhandle, "_ZN17LiquidBlockStaticC1ERKSsi7BlockIDRK8MaterialS1_S1_");
	//bl_LiquidBlockDynamic_LiquidBlockDynamic = (void (*)(Block*, std::string const&, int, void*,
	//	std::string const&, std::string const&))
	//	dlsym(mcpelibhandle, "_ZN18LiquidBlockDynamicC1ERKSsiRK8MaterialS1_S1_");
	bl_BlockItem_vtable = (void**) dlsym(mcpelibhandle, "_ZTV9BlockItem");

	bl_initCustomBlockVtable();

	bl_Item_setIcon = (void (*)(Item*, std::string const&, int)) dlsym(mcpelibhandle, "_ZN4Item7setIconERKSsi");

	bl_Mob_setSneaking = (void (*)(Entity*, bool)) dlsym(mcpelibhandle, "_ZN3Mob11setSneakingEb");
	bl_Mob_isSneaking = (bool (*)(Entity*)) dlsym(mcpelibhandle, "_ZNK3Mob10isSneakingEv");

	bl_Recipes_getInstance = (Recipes* (*)()) dlsym(mcpelibhandle, "_ZN7Recipes11getInstanceEv");
	bl_Recipes_addShapedRecipe = (void (*)(Recipes*, std::vector<ItemInstance> const&, std::vector<std::string> const&, 
		std::vector<RecipesType> const&)) dlsym(mcpelibhandle,
		"_ZN7Recipes15addShapedRecipeERKSt6vectorI12ItemInstanceSaIS1_EERKS0_ISsSaISsEERKS0_INS_4TypeESaISA_EE");
	bl_FurnaceRecipes_getInstance = (FurnaceRecipes* (*)()) dlsym(mcpelibhandle, "_ZN14FurnaceRecipes11getInstanceEv");
	bl_FurnaceRecipes_addFurnaceRecipe = (void (*)(FurnaceRecipes*, int, ItemInstance const&))
		dlsym(mcpelibhandle, "_ZN14FurnaceRecipes16addFurnaceRecipeEiRK12ItemInstance");
	
	bl_Item_setMaxStackSize = (void (*)(Item*, unsigned char)) dlsym(mcpelibhandle, "_ZN4Item15setMaxStackSizeEh");
	bl_Item_setMaxDamage = (void (*)(Item*, int)) dlsym(mcpelibhandle, "_ZN4Item12setMaxDamageEi");

	bl_Item_mItems = (Item**) dlsym(RTLD_DEFAULT, "_ZN4Item6mItemsE");
	// FIXME 0.11
	//void* handleTextPacket = dlsym(mcpelibhandle, "_ZN20ClientNetworkHandler6handleERKN6RakNet10RakNetGUIDEP10TextPacket");
	//mcpelauncher_hook(handleTextPacket, (void*) &bl_ClientNetworkHandler_handleTextPacket_hook,
	//	(void**) &bl_ClientNetworkHandler_handleTextPacket_real);
	void** clientNetworkHandlerVtable = (void**) dlsym(mcpelibhandle, "_ZTV20ClientNetworkHandler") + 2;
	// first two entries are removed
	bl_ClientNetworkHandler_handleTextPacket_real = (void (*)(void*, void*, TextPacket*))
		clientNetworkHandlerVtable[vtable_indexes.clientnetworkhandler_handle_text_packet];
	clientNetworkHandlerVtable[vtable_indexes.clientnetworkhandler_handle_text_packet] =
		(void*) &bl_ClientNetworkHandler_handleTextPacket_hook;
	void** legacyClientNetworkHandlerVtable = (void**) dlsym(mcpelibhandle, "_ZTV26LegacyClientNetworkHandler") + 2;
	legacyClientNetworkHandlerVtable[vtable_indexes.clientnetworkhandler_handle_text_packet] =
		(void*) &bl_ClientNetworkHandler_handleTextPacket_hook;
	bl_SetTimePacket_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV13SetTimePacket");
	// FIXME 0.11
	//bl_RakNetInstance_send = (void (*) (void*, void*)) dlsym(mcpelibhandle, "_ZN14RakNetInstance4sendER6Packet");
	//bl_Packet_Packet = (void (*) (void*)) dlsym(mcpelibhandle, "_ZN6PacketC2Ev");
	// FIXME 0.11
	//void* handleMessagePacket = dlsym(mcpelibhandle, "_ZN24ClientSideNetworkHandler6handleERKN6RakNet10RakNetGUIDEP13MessagePacket");
	//mcpelauncher_hook(handleMessagePacket, (void*) &bl_ClientSideNetworkHandler_handleMessagePacket_hook,
	//	(void**) &bl_ClientSideNetworkHandler_handleMessagePacket_real);
	// FIXME 0.11
	//bl_MessagePacket_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV13MessagePacket");
#if 0
	bl_CompoundTag_putString = (void (*)(void*, std::string, std::string))
		dobby_dlsym(mcpelibhandle, "_ZN11CompoundTag9putStringERKSsS1_");
	bl_CompoundTag_getString = (std::string (*)(void*, std::string))
		dobby_dlsym(mcpelibhandle, "_ZNK11CompoundTag9getStringERKSs");
	bl_CompoundTag_putLong = (void (*)(void*, std::string const&, long long))
		dobby_dlsym(mcpelibhandle, "_ZN11CompoundTag7putLongERKSsx");
	/*
	bl_CompoundTag_getLong = (int64_t (*)(void*, std::string const&))
		dobby_dlsym(mcpelibhandle, "_ZNK11CompoundTag7getLongERKSs");
	bl_CompoundTag_get = (Tag* (*)(void*, std::string const&))
		dobby_dlsym(mcpelibhandle, "_ZNK11CompoundTag3getERKSs");
	*/
	void* entitySaveWithoutId = dlsym(mcpelibhandle, "_ZN6Entity13saveWithoutIdER11CompoundTag");
	//mcpelauncher_hook(entitySaveWithoutId, (void*) &bl_Entity_saveWithoutId_hook, (void**) &bl_Entity_saveWithoutId_real);
	void* entityLoad = dlsym(mcpelibhandle, "_ZN6Entity4loadER11CompoundTag");
	//mcpelauncher_hook(entityLoad, (void*) &bl_Entity_load_hook, (void**) &bl_Entity_load_real);
#endif
	bl_Level_addParticle = (void (*)(Level*, int, Vec3 const&, Vec3 const&, int))
		dlsym(mcpelibhandle, "_ZN5Level11addParticleE12ParticleTypeRK4Vec3S3_i");
	//bl_MinecraftClient_setScreen = (void (*)(Minecraft*, void*)) dlsym(mcpelibhandle, "_ZN15MinecraftClient9setScreenEP6Screen");
	//bl_ProgressScreen_ProgressScreen = (void (*)(void*)) dlsym(mcpelibhandle, "_ZN14ProgressScreenC1Ev");
	//bl_Minecraft_locateMultiplayer = (void (*)(Minecraft*)) dlsym(mcpelibhandle, "_ZN9Minecraft17locateMultiplayerEv");
	void* addEntity = dlsym(mcpelibhandle, "_ZN5Level9addEntityER11BlockSourceSt10unique_ptrI6EntitySt14default_deleteIS3_EE");
	mcpelauncher_hook(addEntity, (void*) &bl_Level_addEntity_hook, (void**) &bl_Level_addEntity_real);
	void* mpAddEntity = dlsym(mcpelibhandle,
		"_ZN16MultiPlayerLevel9addEntityER11BlockSourceSt10unique_ptrI6EntitySt14default_deleteIS3_EE");
	mcpelauncher_hook(mpAddEntity, (void*) &bl_MultiPlayerLevel_addEntity_hook, (void**) &bl_MultiPlayerLevel_addEntity_real);
	void* addPlayer = dlsym(mcpelibhandle, "_ZN5Level9addPlayerESt10unique_ptrI6PlayerSt14default_deleteIS1_EE");
	mcpelauncher_hook(addPlayer, (void*) &bl_Level_addPlayer_hook, (void**) &bl_Level_addPlayer_real);
	void* onEntityRemoved = dlsym(mcpelibhandle, "_ZN5Level18queueEntityRemovalEOSt10unique_ptrI6EntitySt14default_deleteIS1_EEb");
	mcpelauncher_hook(onEntityRemoved, (void*) &bl_Level_removeEntity_hook, (void**) &bl_Level_removeEntity_real);

	mcpelauncher_hook((void*) &Level::explode, (void*) &bl_Level_explode_hook, (void**) &bl_Level_explode_real);

	mcpelauncher_hook((void*) &BlockSource::fireBlockEvent, (void*) &bl_BlockSource_fireBlockEvent_hook,
		(void**) &bl_BlockSource_fireBlockEvent_real);

	bl_Block_mSolid = (bool*) dlsym(RTLD_DEFAULT, "_ZN5Block6mSolidE");
	bl_Block_getAABB = (AABB* (*)(Block*, BlockSource&, BlockPos const&, AABB&, int, bool, int))
		dlsym(mcpelibhandle, "_ZNK5Block7getAABBER11BlockSourceRK8BlockPosR4AABBibi");
	bl_ReedBlock_getAABB = (AABB* (*)(Block*, BlockSource&, BlockPos const&, AABB&, int, bool, int))
		dlsym(mcpelibhandle, "_ZNK9ReedBlock7getAABBER11BlockSourceRK8BlockPosR4AABBibi");

	bl_LevelChunk_setBiome = (void (*)(LevelChunk*, Biome const&, ChunkTilePos const&))
		dlsym(mcpelibhandle, "_ZN10LevelChunk8setBiomeERK5BiomeRK13ChunkBlockPos");
	bl_Biome_getBiome = (Biome* (*)(int))
		dlsym(mcpelibhandle, "_ZN5Biome8getBiomeEi");
	bl_Entity_setSize = (void (*)(Entity*, float, float))
		dlsym(mcpelibhandle, "_ZN6Entity7setSizeEff");
	bl_BaseMobSpawner_setEntityId = (void (*)(BaseMobSpawner*, int))
		dlsym(mcpelibhandle, "_ZN14BaseMobSpawner11setEntityIdE10EntityType");
	bl_ArmorItem_ArmorItem = (void (*)(ArmorItem*, std::string const&, int, void*, int, int))
		dlsym(mcpelibhandle, "_ZN9ArmorItemC1ERKSsiRKNS_13ArmorMaterialEi9ArmorSlot");
	//bl_ScreenChooser_setScreen = (void (*)(ScreenChooser*, int))
	//	dlsym(mcpelibhandle, "_ZN13ScreenChooser9setScreenE8ScreenId");
	// FIXME 0.11
	//bl_Minecraft_hostMultiplayer = (void (*)(Minecraft*, int))
	//	dlsym(mcpelibhandle, "_ZN9Minecraft15hostMultiplayerEi");

	void* mobDie = dlsym(RTLD_DEFAULT, "_ZN3Mob3dieERK18EntityDamageSource");
	mcpelauncher_hook(mobDie, (void*) &bl_Mob_die_hook, (void**) &bl_Mob_die_real);

	bl_MinecraftClient_useController = (bool (*) (Minecraft*))
		dlsym(mcpelibhandle, "_ZN15MinecraftClient13useControllerEv");

#ifdef __arm__
	unsigned char* useControllerPtr = (unsigned char*) (((uintptr_t) bl_MinecraftClient_useController) & ~1);
	bl_forceController = *useControllerPtr;
#endif

	bl_Entity_getNameTag = (std::string* (*)(Entity*))
		dlsym(mcpelibhandle, "_ZNK6Entity10getNameTagEv");
	bl_Mob_addEffect = (void (*)(Entity*, MobEffectInstance&))
		dlsym(mcpelibhandle, "_ZN3Mob9addEffectERK17MobEffectInstance");
	bl_Mob_removeEffect = (void (*)(Entity*, int))
		dlsym(mcpelibhandle, "_ZN3Mob12removeEffectEi");
	bl_Mob_removeAllEffects = (void (*)(Entity*))
		dlsym(mcpelibhandle, "_ZN3Mob16removeAllEffectsEv");

	bl_Item_addCreativeItem = (void (*)(short, short))
		dlsym(mcpelibhandle, "_ZN4Item15addCreativeItemEss");
	bl_Minecraft_getPacketSender = (PacketSender* (*)(Minecraft*))
		dlsym(mcpelibhandle, "_ZN9Minecraft15getPacketSenderEv");
/* FIXME 0.13
	bl_Mob_getTexture = (std::string* (*)(Entity*))
		dlsym(mcpelibhandle, "_ZN3Mob10getTextureEv");
*/
	bl_Mob_getHealth = (int (*)(Entity*))
		dlsym(mcpelibhandle, "_ZNK3Mob9getHealthEv");
	bl_Mob_getAttribute = (AttributeInstance* (*)(Entity*, Attribute const&))
		dlsym(mcpelibhandle, "_ZNK3Mob12getAttributeERK9Attribute");
	void* playerEat = dlsym(mcpelibhandle, "_ZN6Player3eatEif");
	mcpelauncher_hook((void*) playerEat, (void*) &bl_Player_eat_hook,
		(void**) &bl_Player_eat_real);
	bl_Entity_getDimensionId = (int (*)(Entity*))
		dlsym(mcpelibhandle, "_ZNK6Entity14getDimensionIdEv");
	bl_Tile_getVisualShape = (AABB& (*)(Tile*, unsigned char, AABB&, bool))
		dlsym(mcpelibhandle, "_ZNK5Block14getVisualShapeEhR4AABBb");

	bl_Player_HUNGER = (Attribute*)
		dlsym(mcpelibhandle, "_ZN6Player6HUNGERE");
	bl_Player_EXHAUSTION = (Attribute*)
		dlsym(mcpelibhandle, "_ZN6Player10EXHAUSTIONE");
	bl_Player_SATURATION = (Attribute*)
		dlsym(mcpelibhandle, "_ZN6Player10SATURATIONE");
	bl_Player_LEVEL = (Attribute*)
		dlsym(mcpelibhandle, "_ZN6Player5LEVELE");
	bl_Player_EXPERIENCE = (Attribute*)
		dlsym(mcpelibhandle, "_ZN6Player10EXPERIENCEE");

	mcpelauncher_hook((void*) &Player::addExperience, (void*) &bl_Player_addExperience_hook,
		(void**) &bl_Player_addExperience_real);
	mcpelauncher_hook((void*) &Player::addLevels, (void*) &bl_Player_addLevels_hook,
		(void**) &bl_Player_addLevels_real);

	bl_Item_setCategory = (void (*)(Item*, int))
		dlsym(mcpelibhandle, "_ZN4Item11setCategoryE20CreativeItemCategory");
	void* initCreativeItems =
		dlsym(mcpelibhandle, "_ZN4Item17initCreativeItemsEv");
	mcpelauncher_hook(initCreativeItems, (void*) &bl_Item_initCreativeItems_hook,
		(void**) &bl_Item_initCreativeItems_real);
	bl_MobRenderer_getSkinPtr_real = (mce::TexturePtr const& (*)(MobRenderer*, Entity&))
		dlsym(mcpelibhandle, "_ZNK11MobRenderer10getSkinPtrER6Entity");
	void* throwableHit = dlsym(mcpelibhandle, "_ZN19ProjectileComponent5onHitERK9HitResult");
	mcpelauncher_hook(throwableHit, (void*) &bl_Throwable_throwableHit_hook,
		(void**) &bl_Throwable_throwableHit_real);
	mcpelauncher_hook((void*)&Recipe::isAnyAuxValue, (void*)&bl_Recipe_isAnyAuxValue_hook,
		(void**) &bl_Recipe_isAnyAuxValue_real);
	bl_TntBlock_onLoaded = (void (*)(Block*, BlockSource&, BlockPos const&))
		dlsym(mcpelibhandle, "_ZNK8TntBlock8onLoadedER11BlockSourceRK8BlockPos");

	for (unsigned int i = 0; i < sizeof(listOfRenderersToPatchTextures) / sizeof(const char*); i++) {
		void** vtable = (void**) dobby_dlsym(mcpelibhandle, listOfRenderersToPatchTextures[i]);
#if 0
		Dl_info info;
		if (dladdr(vtable[vtable_indexes.mobrenderer_get_skin_ptr], &info)) {
			__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Patching %s in %s", info.dli_sname, listOfRenderersToPatchTextures[i]);
		}
#endif
		vtable[vtable_indexes.mobrenderer_get_skin_ptr] = (void*) &bl_MobRenderer_getSkinPtr_hook;
	}
	void** skeletonRendererVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV16SkeletonRenderer");
	bl_SkeletonRenderer_render_real = (void* (*)(MobRenderer*, Entity&, Vec3 const&, float, float))
		skeletonRendererVtable[vtable_indexes.mobrenderer_render];
	bl_MobRenderer_render_real = (void* (*)(MobRenderer*, Entity&, Vec3 const&, float, float))
		dlsym(mcpelibhandle, "_ZN11MobRenderer6renderER6EntityRK4Vec3ff");
	skeletonRendererVtable[vtable_indexes.mobrenderer_render] = (void*) &bl_SkeletonRenderer_render_hook;

	bl_MinecraftTelemetry_fireEventScreenChanged_real = (void* (*)(void*, std::string const&, std::unordered_map<std::string, std::string> const&))
		dlsym(mcpelibhandle, "_ZN17MinecraftEventing22fireEventScreenChangedERKSsRKSt13unordered_mapISsSsSt4hashISsESt8equal_toISsESaISt4pairIS0_SsEEE");
	bl_patch_got(mcpelibhandle, (void*)bl_MinecraftTelemetry_fireEventScreenChanged_real, 
		(void*)bl_MinecraftTelemetry_fireEventScreenChanged_hook);
/*
	bl_BlockGraphics_getTexture_real = (TextureUVCoordinateSet* (*)(BlockGraphics*, signed char, int))
		dlsym(mcpelibhandle, "_ZNK13BlockGraphics10getTextureEai");
	bl_patch_got_wrap(mcpelibhandle, (void*)bl_BlockGraphics_getTexture_real, (void*) bl_CustomBlockGraphics_getTextureHook);
	bl_BlockGraphics_getTexture_char_real = (TextureUVCoordinateSet* (*)(BlockGraphics*, signed char))
		dlsym(mcpelibhandle, "_ZNK13BlockGraphics10getTextureEa");
	bl_patch_got_wrap(mcpelibhandle, (void*)bl_BlockGraphics_getTexture_char_real, (void*) bl_CustomBlockGraphics_getTexture_char_hook);
*/

	mcpelauncher_hook((void*)&MinecraftClient::onResourcesLoaded, (void*)bl_MinecraftClient_onResourcesLoaded_hook, (void**)&bl_MinecraftClient_onResourcesLoaded_real);
	mcpelauncher_hook((void*)&Entity::hurt, (void*)bl_Entity_hurt_hook, (void**)&bl_Entity_hurt_real);

	//bl_entity_hurt_hook_init(mcpelibhandle);

	bl_renderManager_init(mcpelibhandle);
}

} //extern
