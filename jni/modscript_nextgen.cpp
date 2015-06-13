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
#include <array>

#include "utf8proc.h"

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"
#include "dobby_public.h"
#include "simpleuuid.h"

#define cppbool bool

#include "modscript_shared.h"

#include "modscript_ScriptLevelListener.hpp"

#include "minecraft_colors.h"

#include "mcpe/i18n.h"
#include "mcpe/entitydamagesource.h"
#include "mcpe/mobfactory.h"
#include "mcpe/synchedentitydata.h"
#include "mcpe/mobeffect.h"
#include "mcpe/packetsender.h"

typedef void RakNetInstance;
typedef void Font;

#define RAKNET_INSTANCE_VTABLE_OFFSET_CONNECT 5
// After the call to RakNetInstance::RakNetInstance
// FIXME 0.11
//#define MINECRAFT_RAKNET_INSTANCE_OFFSET 76
// from SignTileEntity::save(CompoundTag*)
#define SIGN_TILE_ENTITY_LINE_OFFSET 96
#define BLOCK_VTABLE_SIZE 0x118
#define BLOCK_VTABLE_GET_TEXTURE_OFFSET 8
#define BLOCK_VTABLE_GET_COLOR 45
//#define BLOCK_VTABLE_GET_AABB 14
// Mob::getTexture
#define MOB_TEXTURE_OFFSET 2996
// found in LocalPlayer::displayClientMessage, also before the first call to Gui constructor
//#define MINECRAFT_GUI_OFFSET 252
//#define MOB_TARGET_OFFSET 3156
// found in both GameRenderer::moveCameraToPlayer and Minecraft::setLevel
#define MINECRAFT_CAMERA_ENTITY_OFFSET 328
// found in ChatScreen::setTextboxText
#define CHATSCREEN_TEXTBOX_TEXT_OFFSET 200

#ifdef __i386
// FIXME 0.11
#define MINECRAFT_TEXTURES_OFFSET 308
#else
// found in StartMenuScreen::render, or search for getTextureData
#define MINECRAFT_TEXTURES_OFFSET 308
#endif

// found way, way inside GameRenderer::updateFreeformPickDirection, around the call to HitResult::HitResult(Entity*)
// (look for the stack allocation, then see where it's copied)
#define MINECRAFT_HIT_RESULT_OFFSET 2680
// found in TouchInputMove::interactWithEntity
#define MINECRAFT_HIT_ENTITY_OFFSET 2712
// found in GameMode::initPlayer
#define PLAYER_ABILITIES_OFFSET 3196
// FIXME 0.11
//#define RAKNET_INSTANCE_VTABLE_OFFSET_SEND 15
#define PLAYER_RENDER_TYPE 21
// MobSpawnerTileEntity constructor 0x60
#define MOB_SPAWNER_OFFSET 96
// MinecraftClient::handleBack
#define MINECRAFT_SCREENCHOOSER_OFFSET 252

// found in Tile::initTiles; tile id 4
const size_t kTileSize = 0x8c;
// found before TileItem::TileItem
const size_t kTileItemSize = 80;
// found in Item::initItems item with id 6
const size_t kItemSize = 72;
// found in Item::initItems item with id 4
const size_t kFoodItemSize = 100;
// found in Entity::spawnAtLocation
const size_t kItemEntitySize = 360;
// found in Entity::spawnAtLocation
const size_t kItemEntity_pickupDelay_offset = 344;
// found in ItemEntity::_validateItem
const size_t kItemEntity_itemInstance_offset = 324;
// found in TextPacket::handle
const int kClientNetworkHandler_vtable_offset_handleTextPacket = 13;

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

typedef struct {
	void** vtable; //4
	ItemPack itemPack; //4
} Recipe;

typedef struct {
	std::vector<Recipe*> recipes;
} Recipes;

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
	Packet();
	virtual ~Packet();
};

class TextPacket : public Packet {
public:
	TextPacket() {
	}
	char filler[13-4]; // 4
	unsigned char type; //13
	char filler2[16-14]; // 14
	std::string username; // 16
	std::string message; // 20
	std::vector<std::string> thevector; // 24
	char filler3[36-28]; // 28
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

struct BaseMobSpawner {
};

extern "C" {

static void (*bl_ChatScreen_sendChatMessage_real)(void*);

static void (*bl_Gui_displayClientMessage)(void*, std::string const&);

static void (*bl_Item_Item)(Item*, int);

static void** bl_FoodItem_vtable;
static void** bl_Item_vtable;
static void** bl_Tile_vtable;
static void** bl_TileItem_vtable;
static Tile** bl_Tile_tiles;
static unsigned char* bl_Tile_lightEmission;
static unsigned char* bl_Tile_lightBlock;

static void (*bl_Item_setNameID)(Item*, std::string const&);

static void (*bl_Minecraft_selectLevel)(Minecraft*, std::string const&, std::string const&, void*);

static void (*bl_MinecraftClient_leaveGame)(Minecraft*, bool saveWorld);
static void (*bl_Minecraft_setLeaveGame)(Minecraft*);

static void (*bl_Minecraft_connectToMCOServer)(Minecraft*, std::string const&, std::string const&, unsigned short);

static void (*bl_Level_playSound)(Level*, float, float, float, std::string const&, float, float);

//static void* (*bl_Level_getAllEntities)(Level*);

//static void (*bl_Level_addListener)(Level*, LevelListener*);

static void (*bl_RakNetInstance_connect_real)(RakNetInstance*, char const*, int);

#if 0

static void (*bl_Font_drawCached_real)(Font*, std::string const&, float, float, Color const&, bool, MaterialPtr*);

static int (*bl_Font_width)(Font*, std::string const&);

#endif

static void* bl_Material_dirt;

static void (*bl_Tile_Tile)(Tile*, int, void*);
static void (*bl_TileItem_TileItem)(Item*, int);
static void (*bl_Tile_setNameId)(Tile*, const std::string&);
static void (*bl_Tile_setShape)(Tile*, float, float, float, float, float, float);
static void (*bl_Mob_setSneaking)(Entity*, bool);
static bool (*bl_Mob_isSneaking)(Entity*);

static void (*bl_Item_setIcon)(Item*, std::string const&, int);
static void (*bl_Item_setMaxStackSize)(Item*, unsigned char);

static void (*bl_Item_setMaxDamage)(Item*, int);

static std::string const (*bl_ItemInstance_getName)(ItemInstance*);
//static TextureUVCoordinateSet* (*bl_ItemInstance_getIcon)(ItemInstance*, int, bool);
static TextureUVCoordinateSet* (*bl_Tile_getTexture)(Tile*, signed char, int);
static TextureUVCoordinateSet (*bl_Tile_getTextureUVCoordinateSet)(Tile*, std::string const&, int);
static Recipes* (*bl_Recipes_getInstance)();
static void (*bl_Recipes_addShapedRecipe)(Recipes*, std::vector<ItemInstance> const&, std::vector<std::string> const&, 
	std::vector<RecipesType> const&);
static FurnaceRecipes* (*bl_FurnaceRecipes_getInstance)();
static void (*bl_FurnaceRecipes_addFurnaceRecipe)(FurnaceRecipes*, int, ItemInstance const&);
static void (*bl_Gui_showTipMessage)(void*, std::string const&);
static bool (*bl_CraftingFilters_isStonecutterItem_real)(ItemInstance const&);

static void** bl_ShapelessRecipe_vtable;

static void (*bl_RakNetInstance_send)(void*, void*);
static void** bl_SetTimePacket_vtable;
static void (*bl_Packet_Packet)(void*);
static void (*bl_ClientNetworkHandler_handleTextPacket_real)(void*, void*, TextPacket*);
static void** bl_MessagePacket_vtable;

bool bl_text_parse_color_codes = true;

//custom blocks
void* bl_CustomBlock_vtable[BLOCK_VTABLE_SIZE];
TextureUVCoordinateSet** bl_custom_block_textures[256];
bool bl_custom_block_opaque[256];
bool bl_custom_block_collisionDisabled[256];
int* bl_custom_block_colors[256];
uint8_t bl_custom_block_renderLayer[256];
//end custom blocks

int bl_addItemCreativeInvRequest[256][4];
int bl_addItemCreativeInvRequestCount = 0;

std::map <int, std::string> bl_nametag_map;
char bl_stonecutter_status[BL_ITEMS_EXPANDED_COUNT];

static Item** bl_Item_items;
#if 0
static void (*bl_CompoundTag_putString)(void*, std::string, std::string);
static std::string (*bl_CompoundTag_getString)(void*, std::string);
static void (*bl_CompoundTag_putLong)(void*, std::string const&, long long);
static int64_t (*bl_CompoundTag_getLong)(void*, std::string const&);
//static Tag* (*bl_CompoundTag_get)(void*, std::string const&);
static void (*bl_Entity_saveWithoutId_real)(Entity*, void*);
static int (*bl_Entity_load_real)(Entity*, void*);
#endif

static std::map<int, std::array<unsigned char, 16> > bl_entityUUIDMap;

static void (*bl_Level_addParticle)(Level*, int, Vec3 const&, Vec3 const&, int);
static void (*bl_MinecraftClient_setScreen)(Minecraft*, void*);
static void (*bl_ProgressScreen_ProgressScreen)(void*);
static void (*bl_Minecraft_locateMultiplayer)(Minecraft*);
static void* (*bl_Textures_getTextureData)(void*, std::string const&);
static bool (*bl_Level_addEntity_real)(Level*, std::unique_ptr<Entity>);
static bool (*bl_MultiPlayerLevel_addEntity_real)(Level*, std::unique_ptr<Entity>);
static bool (*bl_Level_addPlayer_real)(Level*, Entity*);
static void (*bl_Level_removeEntity_real)(Level*, Entity*);
static void (*bl_Level_explode_real)(Level*, Entity*, float, float, float, float, bool);
static Biome* (*bl_TileSource_getBiome)(TileSource*, TilePos&);
static int (*bl_TileSource_getGrassColor)(TileSource*, TilePos&);
static void (*bl_TileSource_setGrassColor)(TileSource*, int, TilePos&, int);
static void (*bl_TileSource_fireTileEvent_real)(TileSource* source, int x, int y, int z, int type, int data);
static AABB* (*bl_Tile_getAABB)(Tile*, TileSource*, int, int, int, AABB&, int, bool, int);
static AABB* (*bl_ReedTile_getAABB)(Tile*, TileSource*, int, int, int, AABB&, int, bool, int);

static LevelChunk* (*bl_TileSource_getChunk)(TileSource*, int, int);
static void (*bl_LevelChunk_setBiome)(LevelChunk*, Biome const&, ChunkTilePos const&);
static Biome* (*bl_Biome_getBiome)(int);
static void (*bl_Entity_setSize)(Entity*, float, float);
static FullTile (*bl_TileSource_getTile_raw)(TileSource*, int, int, int);
static void* (*bl_MinecraftClient_getGui)(Minecraft* minecraft);
static void (*bl_BaseMobSpawner_setEntityId)(BaseMobSpawner*, int);
static void (*bl_TileEntity_setChanged)(TileEntity*);
static void (*bl_ArmorItem_ArmorItem)(ArmorItem*, int, void*, int, int);
static void (*bl_ScreenChooser_setScreen)(ScreenChooser*, int);
static void (*bl_Minecraft_hostMultiplayer)(Minecraft* minecraft, int port);
static void (*bl_Mob_die_real)(Entity*, EntityDamageSource&);
static bool bl_forceController = false;
static bool (*bl_MinecraftClient_useController)(Minecraft*);
static std::string* (*bl_Entity_getNameTag)(Entity*);
static void (*bl_ItemEntity_ItemEntity)(Entity*, TileSource&, float, float, float, ItemInstance&);
static void (*bl_FoodItem_FoodItem)(Item*, int, int, bool, float);
static void (*bl_Item_addCreativeItem)(short, short);
static PacketSender* (*bl_Minecraft_getPacketSender)(Minecraft*);

static bool* bl_Tile_solid;

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

__attribute__((__visibility__("hidden")))
bool bl_onLockDown = false;

static int bl_item_id_count = 512;
Item* bl_items[BL_ITEMS_EXPANDED_COUNT];

Entity* bl_getEntityWrapper(Level* level, long long entityId) {
	if (bl_removedEntity != NULL && bl_removedEntity->entityId == entityId) {
		return bl_removedEntity;
	}
	if (bl_onLockDown) return nullptr;
	return level->getEntity(entityId, 0 /* false */);
}

void bl_ChatScreen_sendChatMessage_hook(void* chatScreen) {
	std::string* chatMessagePtr = (std::string*) ((uintptr_t) chatScreen + CHATSCREEN_TEXTBOX_TEXT_OFFSET);
	//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Chat message: %s\n", chatMessagePtr->c_str());
	/*int chatMessagePtr = *(*((int**) ((int) chatScreen + 84))) - 12; 
	char* chatMessageChars = *((char**) chatMessagePtr);*/
	const char* chatMessageChars = chatMessagePtr->c_str();

	JNIEnv *env;
	preventDefaultStatus = false;
	bl_JavaVM->AttachCurrentThread(&env, NULL);

	jstring chatMessageJString = env->NewStringUTF(chatMessageChars);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "chatCallback", "(Ljava/lang/String;)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, chatMessageJString);

	bl_JavaVM->DetachCurrentThread();
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Chat message: %s preventDefault %d\n", chatMessagePtr->c_str(),
		(int) preventDefaultStatus);
	if (!preventDefaultStatus) {
		bl_ChatScreen_sendChatMessage_real(chatScreen);
	} else {
		//clear the chat string
		chatMessagePtr->clear();
	}
}

void bl_RakNetInstance_connect_hook(RakNetInstance* rakNetInstance, char const* host, int port) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jstring hostJString = env->NewStringUTF(host);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "rakNetConnectCallback", "(Ljava/lang/String;I)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, hostJString, port);

	bl_onLockDown = !isLocalAddress(env, hostJString);

	env->DeleteLocalRef(hostJString);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_RakNetInstance_connect_real(rakNetInstance, host, port);
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

/*
void bl_CreativeInventryScreen_populateTile_hook(Tile* tile, int count, int damage){
	int index = bl_addItemCreativeInvRequestCount;
	for(int i = index; i > 0; i--){
		if (!bl_addItemCreativeInvRequest[i][3]) continue;
		bl_addItemCreativeInvRequest[i][3] = 0;

		int id = bl_addItemCreativeInvRequest[i][0];

		if (id >= 0 && id <= 255) {
			Tile* addTile = bl_Tile_tiles[id];
			if (addTile == NULL) continue;

			bl_CreativeInventryScreen_populateTile_real(addTile, bl_addItemCreativeInvRequest[i][1], bl_addItemCreativeInvRequest[i][2]);
		} else if (id > 0 && id < bl_item_id_count) {
			ItemInstance* instance = bl_newItemInstance(id, 1, 0);
			if (instance == NULL) continue;
			Item* addItem = instance->item;

			bl_CreativeInventryScreen_populateItem_real(addItem, bl_addItemCreativeInvRequest[i][1], bl_addItemCreativeInvRequest[i][2]);
		}
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Add item: %d (%d)\n", id, i);
	}

	bl_CreativeInventryScreen_populateTile_real(tile, count, damage);
}

void bl_CreativeInventryScreen_populateItem_hook(Item* item, int count, int damage){
	bl_CreativeInventryScreen_populateItem_real(item, count, damage);
}
*/

const char* bl_getCharArr(void* str){
	std::string* mystr = static_cast<std::string*>(str);
	std::string mystr2 = *mystr;
	const char* cs = mystr2.c_str();
	return cs;
}

TextureUVCoordinateSet* bl_CustomBlock_getTextureHook(Tile* tile, signed char side, int data) {
	int blockId = tile->id;
	TextureUVCoordinateSet** ptrToBlockInfo = bl_custom_block_textures[blockId];
	if (ptrToBlockInfo == NULL) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Block pointer IS NULL! %d\n", blockId);
		return &tile->texture;
	}
	int myIndex = (data * 6) + side;
	if (myIndex < 0 || myIndex >= 16*6) {
		myIndex = side;
	}
	return ptrToBlockInfo[myIndex];
}

bool bl_CustomBlock_isCubeShapedHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_opaque[blockId];
}
AABB* bl_CustomBlock_getAABBHook(Tile* tile, TileSource* tileSource, int x, int y, int z, AABB& aabb, int int1, bool bool1, int int2) {
	int blockId = tile->id;
	if (bl_custom_block_collisionDisabled[blockId]) return bl_ReedTile_getAABB(tile, tileSource, x, y, z, aabb, int1, bool1, int2);
	return bl_Tile_getAABB(tile, tileSource, x, y, z, aabb, int1, bool1, int2);
}

int bl_CustomBlock_getColorHook(Tile* tile, TileSource* tileSource, int x, int y, int z) {
	int blockId = tile->id;
	int* myColours = bl_custom_block_colors[blockId];
	if (myColours == NULL || bl_level == NULL) return 0xffffff; //I see your true colours shining through
	int data = bl_TileSource_getData(tileSource, x, y, z);
	return myColours[data];
}

int bl_CustomBlock_getRenderLayerHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_renderLayer[blockId];
}

void bl_clearNameTags() {
	bl_nametag_map.clear();
}

bool bl_CraftingFilters_isStonecutterItem_hook(ItemInstance const& myitem) {
	int itemId = bl_ItemInstance_getId((ItemInstance*) &myitem);
	char itemStatus = bl_stonecutter_status[itemId];
	if (itemStatus == STONECUTTER_STATUS_DEFAULT) return bl_CraftingFilters_isStonecutterItem_real(myitem);
	return itemStatus == STONECUTTER_STATUS_FORCE_TRUE;
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

std::array<unsigned char, 16> bl_getEntityUUID(int entityId) {
	if (bl_entityUUIDMap.count(entityId) != 0) {
		return bl_entityUUIDMap[entityId];
	}
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Generating uuid for entity %d\n", entityId);
	// generate a new uuid
	std::array<unsigned char, 16> newuuid;
	uuid_t uuidT;
	uuid_generate_random(uuidT);
	memcpy(newuuid.data(), uuidT, sizeof(uuidT));
	bl_entityUUIDMap[entityId] = newuuid;
	return newuuid;
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

static bool bl_Level_addEntity_hook(Level* level, std::unique_ptr<Entity> entityPtr) {
	JNIEnv *env;
	Entity* entity = entityPtr.get();

	bool retval = bl_Level_addEntity_real(level, std::move(entityPtr));

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->entityId);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return retval;
}

static uintptr_t bl_Level_addPlayer_hook(Level* level, Player* entity) {
	JNIEnv *env;

	uintptr_t retval = bl_Level_addPlayer_real(level, entity);

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->entityId);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return retval;
}

static bool bl_MultiPlayerLevel_addEntity_hook(Level* level, std::unique_ptr<Entity> entityPtr) {
	JNIEnv *env;

	Entity* entity = entityPtr.get();

	bool retval = bl_MultiPlayerLevel_addEntity_real(level, std::move(entityPtr));

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityAddedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->entityId);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	return retval;
}

static void bl_Level_removeEntity_hook(Level* level, Entity* entity) {
	JNIEnv *env;

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	bl_removedEntity = NULL;

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "entityRemovedCallback", "(J)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, entity->entityId);

	bl_Level_removeEntity_real(level, entity);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}

static void bl_Level_explode_hook(Level* level, Entity* entity, float x, float y, float z, float power, bool onFire) {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	preventDefaultStatus = false;

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "explodeCallback", "(JFFFFZ)V");

	long long id = entity != NULL? entity->entityId.id: -1LL;

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, id, x, y, z, power, onFire);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) bl_Level_explode_real(level, entity, x, y, z, power, onFire);
}

static void bl_TileSource_fireTileEvent_hook(TileSource* source, int x, int y, int z, int type, int data) {
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

	bl_TileSource_fireTileEvent_real(source, x, y, z, type, data);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClientMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	void* mygui = bl_MinecraftClient_getGui(bl_minecraft);
	bl_Gui_displayClientMessage(mygui, mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

JNIEXPORT int JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetBlockRenderShape
  (JNIEnv *env, jclass clazz, jint blockId) {
	Tile* tile = bl_Tile_tiles[blockId];
	if(tile == NULL) return 0;
	
	return tile->renderType;//bl_CustomBlock_getRenderShapeHook(tile);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetBlockRenderShape
  (JNIEnv *env, jclass clazz, jint blockId, jint renderType) {
	//bl_custom_block_renderShape[blockId] = renderType;
	Tile* tile = bl_Tile_tiles[blockId];
	if (tile == nullptr) return;
	tile->renderType = renderType;
}

Item* bl_constructItem(int id) {
	Item* retval = (Item*) ::operator new(kItemSize);
	bl_Item_Item(retval, id - 0x100);
	retval->category1 = 2; //tool
	//retval->category2 = 0;
	return retval;
}

Item* bl_constructFoodItem(int id, int hearts, float timetoeat) {
	Item* retval = (Item*) ::operator new(kFoodItemSize);
	bl_FoodItem_FoodItem(retval, id - 0x100, hearts, false, timetoeat);
	retval->category1 = 4; //food
	//retval->category2 = 0;
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineItem
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jstring name, jint maxStackSize) {
	Item* item = bl_constructItem(id);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon(item, iconNameString, iconIndex);

	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	if (maxStackSize <= 0) {
		bl_Item_setMaxStackSize(item, 64);
	} else {
		bl_Item_setMaxStackSize(item, maxStackSize);
	}
	bl_Item_setNameID(item, mystr);
	bl_set_i18n("item." + mystr + ".name", mystr);
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineFoodItem
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jint halfhearts, jstring name, jint maxStackSize) {
	Item* item = bl_constructFoodItem(id, halfhearts, 0.3f);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon(item, iconNameString, iconIndex);

	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	if (maxStackSize <= 0) {
		bl_Item_setMaxStackSize(item, 64);
	} else {
		bl_Item_setMaxStackSize(item, maxStackSize);
	}
	bl_Item_setNameID(item, mystr);
	bl_set_i18n("item." + mystr + ".name", mystr);
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineArmor
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jstring name, jstring texture,
		jint damageReduceAmount, jint maxDamage, jint armorType) {
	ArmorItem* item = new ArmorItem;
	bl_ArmorItem_ArmorItem(item, id - 0x100, ((ArmorItem*) bl_Item_items[310])->armorMaterial, 42, armorType);
	item->damageReduceAmount = damageReduceAmount;
	item->maxDamage = maxDamage;

	const char * textureUTFChars = env->GetStringUTFChars(texture, NULL);
	bl_armorRenders[id] = textureUTFChars;
	env->ReleaseStringUTFChars(name, textureUTFChars);


	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon(item, iconNameString, iconIndex);

	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	bl_Item_setNameID(item, mystr);
	bl_set_i18n("item." + mystr + ".name", mystr);
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetItemMaxDamage
  (JNIEnv *env, jclass clazz, jint id, jint maxDamage) {
	if (id <= 0 || id >= bl_item_id_count) return;
	Item* item = bl_Item_items[id];
	if(item == NULL) return;
	bl_Item_setMaxDamage(item, maxDamage);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSelectLevel
  (JNIEnv *env, jclass clazz, jstring worlddir) {
/* FIXME 0.11
	const char * utfChars = env->GetStringUTFChars(worlddir, NULL);
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

	bl_Minecraft_selectLevel(bl_minecraft, worlddirstr, worlddirstr, &levelSettings);
	bl_Minecraft_hostMultiplayer(bl_minecraft, 19132);
*/
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLeaveGame
  (JNIEnv *env, jclass clazz, jboolean saveMultiplayerWorld) {
	//bl_Minecraft_setLeaveGame(bl_minecraft);
	// Is this boolean right?
	bl_MinecraftClient_leaveGame(bl_minecraft, saveMultiplayerWorld);
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
	bl_Level_playSound(bl_level, x, y, z, soundstr, volume, pitch);
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
	void* te = bl_TileSource_getTileEntity(bl_level->tileSource, x, y, z);
	if (te == NULL) return NULL;
	//line offsets: 68, 72, 76, 80
	std::string* lineStr = (std::string*) (((int) te) + (SIGN_TILE_ENTITY_LINE_OFFSET + (line * 4)));
	if (lineStr == NULL) return NULL;

	jstring signJString = env->NewStringUTF(lineStr->c_str());
	return signJString;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSignText
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint line, jstring newText) {
	if (bl_level == NULL) return;
	void* te = bl_TileSource_getTileEntity(bl_level->tileSource, x, y, z);
	if (te == NULL) return;

	const char * utfChars = env->GetStringUTFChars(newText, NULL);

	//line offsets: 68, 72, 76, 80
	std::string* lineStr = (std::string*) (((int) te) + (SIGN_TILE_ENTITY_LINE_OFFSET + (line * 4)));
	if (lineStr == NULL || lineStr->length() == 0) {
		//Workaround for C++ standard library's empty string optimization failing across libraries
		//search FULLY_DYNAMIC_STRING
		std::string* mystr = new std::string(utfChars);
		*((void**) lineStr) = *((void**) mystr);
	} else {
		lineStr->assign(utfChars);
	}
	env->ReleaseStringUTFChars(newText, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTextParseColorCodes
  (JNIEnv *env, jclass clazz, jboolean colorText) {
	bl_text_parse_color_codes = colorText;
}

void bl_changeEntitySkin(void* entity, const char* newSkin) {
	std::string newSkinString(newSkin);
	std::string* ptrToStr = (std::string*) (((uintptr_t) entity) + MOB_TEXTURE_OFFSET);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer: %p, %i, %s\n", ptrToStr, *((int*) ptrToStr), ptrToStr->c_str());
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "New string pointer: %s\n", newSkinString->c_str());
	*ptrToStr = newSkinString;
	bl_forceTextureLoad(newSkinString);
}

void bl_attachLevelListener() {
	/* FIXME
	ScriptLevelListener* listener = new ScriptLevelListener();
	bl_Level_addListener(bl_level, listener);
	*/
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSneaking
  (JNIEnv *env, jclass clazz, jlong entityId, jboolean doIt) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Mob_setSneaking(entity, doIt);
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
	ItemInstance* myStack = bl_newItemInstance(itemId, 1, itemDamage);
	if (myStack == NULL || bl_ItemInstance_getId(myStack) != itemId) return NULL;
	switch(itemId) {
		case 95:
		case 255:
			//these return blank strings. Blank strings will kill libstdc++ since we are not using the same blank string.
			return NULL;
	}
	std::string descriptionId = bl_ItemInstance_getName(myStack);
	if (descriptionId.length() <= 0) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "dead tile: %i\n", itemId);
	}
	//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Tile: %s\n", descriptionId.c_str());
	std::string returnVal = descriptionId;
	if (!raw) {
		std::vector<std::string> tempList;
		returnVal = I18n::get(descriptionId, tempList);
	}
	jstring returnValString = env->NewStringUTF(returnVal.c_str());
	return returnValString;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTextureCoordinatesForItem
  (JNIEnv *env, jclass clazz, jint itemId, jint itemDamage, jfloatArray outputArray) {
#if 0
	if (itemId <= 0 || itemId >= 512) return false;
	ItemInstance* myStack = bl_newItemInstance(itemId, 1, itemDamage);
	if (myStack == NULL || bl_ItemInstance_getId(myStack) != itemId) return false;
	TextureUVCoordinateSet* set = bl_ItemInstance_getIcon(myStack, 0, true);
	if (set == NULL || set->bounds == NULL) return false;
	env->SetFloatArrayRegion(outputArray, 0, 6, set->bounds);
	return true;
#endif
	return false;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPlayerName
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return NULL;
	std::string* myName = bl_Entity_getNameTag(entity);
	jstring returnValString = env->NewStringUTF(myName->c_str());
	return returnValString;
}

void bl_initCustomBlockVtable() {
	//copy existing vtable
	memcpy(bl_CustomBlock_vtable, bl_Tile_vtable, BLOCK_VTABLE_SIZE);

	//set the texture getter to our overridden version
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_TEXTURE_OFFSET] = (void*) &bl_CustomBlock_getTextureHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_COLOR] = (void*) &bl_CustomBlock_getColorHook;
	//bl_CustomBlock_vtable[BLOCK_VTABLE_GET_AABB] = (void*) &bl_CustomBlock_getAABBHook;
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "The material is %x\n", bl_Material_dirt);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "The material vtable is %x\n", *((int*) bl_Material_dirt));
}

void* bl_getMaterial(int materialType) {
	Tile* baseTile = bl_Tile_tiles[materialType];
	if (baseTile == NULL) {
		baseTile = bl_Tile_tiles[1];
	}
	return baseTile->material;
}

void bl_buildTextureArray(TextureUVCoordinateSet* output[], std::string textureNames[], int textureCoords[]) {
	Tile* sacrificialTile = bl_Tile_tiles[1]; //Oh, little Cobblestone Galatti, please sing for me again!
	for (int i = 0; i < 16*6; i++) {
		TextureUVCoordinateSet* mySet = new TextureUVCoordinateSet(bl_Tile_getTextureUVCoordinateSet(
			sacrificialTile, textureNames[i], textureCoords[i]));
		//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Building %s %d\n", textureNames[i].c_str(), textureCoords[i]);

		output[i] = mySet;
	}
}

Tile* bl_createBlock(int blockId, std::string textureNames[], int textureCoords[], int materialType, bool opaque, int renderShape, const char* name) {
	if (blockId < 0 || blockId > 255) return NULL;
	if (bl_custom_block_textures[blockId] != NULL) {
		delete[] bl_custom_block_textures[blockId];
	}
	//bl_custom_block_opaque[blockId] = opaque;
	bl_custom_block_textures[blockId] = new TextureUVCoordinateSet*[16*6];
	bl_buildTextureArray(bl_custom_block_textures[blockId], textureNames, textureCoords);
	//Allocate memory for the block
	// size found before the Tile::Tile constructor for tile ID #4
	Tile* retval = (Tile*) ::operator new(kTileSize);
	retval->vtable = bl_CustomBlock_vtable;
	bl_Tile_Tile(retval, blockId, bl_getMaterial(materialType));
	retval->vtable = bl_CustomBlock_vtable;
	retval->material = bl_getMaterial(materialType);
	std::string nameStr = std::string(name);
	bl_Tile_setNameId(retval, nameStr);

	bl_set_i18n("tile." + nameStr + ".name", nameStr);
	retval->renderType = renderShape;
	bl_Tile_solid[blockId] = opaque;
	//add it to the global tile list
	bl_Tile_tiles[blockId] = retval;
	retval->category1 = 1;
	//now allocate the item
	Item* tileItem = (Item*) ::operator new(kTileItemSize);
	tileItem->vtable = bl_TileItem_vtable;
	bl_TileItem_TileItem(tileItem, blockId - 0x100);
	tileItem->vtable = bl_TileItem_vtable;
	tileItem->category1 = 1;
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineBlock
  (JNIEnv *env, jclass clazz, jint blockId, jstring name, jobjectArray textureNames, 
		jintArray textureCoords, jint materialBlockId, jboolean opaque, jint renderType) {
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
	Tile* tile = bl_createBlock(blockId, myStringArray, myIntArray, materialBlockId, opaque, renderType, utfChars);
	env->ReleaseStringUTFChars(name, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetDestroyTime
  (JNIEnv *env, jclass clazz, jint blockId, jfloat time) {
	if (blockId < 0 || blockId > 255) return;
	Tile* tile = bl_Tile_tiles[blockId];
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
	Tile* tile = bl_Tile_tiles[blockId];
	if (tile == NULL) {
		return;
	}
	tile->explosionResistance = resistance * 3.0f;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetShape
  (JNIEnv *env, jclass clazz, jint blockId, jfloat v1, jfloat v2, jfloat v3, jfloat v4, jfloat v5, jfloat v6) {
	if (blockId < 0 || blockId > 255) return;
	Tile* tile = bl_Tile_tiles[blockId];
	if (tile == NULL) {
		return;
	}
	bl_Tile_setShape(tile, v1, v2, v3, v4, v5, v6);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetStepSound
  (JNIEnv *env, jclass clazz, jint blockId, jint sourceBlockId) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetLightLevel
  (JNIEnv *env, jclass clazz, jint blockId, jint level) {
	if (blockId < 0 || blockId > 255) return;
	bl_Tile_lightEmission[blockId] = level;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeBlockSetLightOpacity
  (JNIEnv *env, jclass clazz, jint blockId, jint level) {
	if (blockId < 0 || blockId > 255) return;
	bl_Tile_lightBlock[blockId] = level;
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
	bl_custom_block_renderLayer[blockId] = (uint8_t) level;
	Tile* tile = bl_Tile_tiles[blockId];
	tile->renderPass = level;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemCreativeInv
  (JNIEnv *env, jclass clazz, jint id, jint count, jint damage) {
	bl_Item_addCreativeItem((short) id, (short) damage);
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

	ItemInstance outStack;
	bl_setItemInstance(&outStack, itemId, itemCount, itemDamage);
	std::vector<ItemInstance> outStacks;
	outStacks.push_back(outStack);

	int ingredientsCount = ingredientsElemsCount / 3;
	std::vector<RecipesType> ingredientsList;
	for (int i = 0; i < ingredientsCount; i++) {
		RecipesType recipeType;
		recipeType.wtf2 = 0;
		recipeType.item = NULL;
		recipeType.itemInstance.damage = ingredients[i * 3 + 2];
		recipeType.itemInstance.count = 1;
		bl_ItemInstance_setId(&recipeType.itemInstance, ingredients[i * 3 + 1]);
		recipeType.letter = (char) ingredients[i * 3];
		ingredientsList.push_back(recipeType);
	}
	Recipes* recipes = bl_Recipes_getInstance();
	//bl_tryRemoveExistingRecipe(recipes, itemId, itemCount, itemDamage, ingredients, ingredientsCount);
	bl_Recipes_addShapedRecipe(recipes, outStacks, shapeVector, ingredientsList);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddFurnaceRecipe
  (JNIEnv *env, jclass clazz, jint inputId, jint outputId, jint outputDamage) {
  	ItemInstance outputStack;
	bl_setItemInstance(&outputStack, outputId, 1, outputDamage); // Should this be null? You don't need count, not sure how to omit it completely
  	FurnaceRecipes* recipes = bl_FurnaceRecipes_getInstance();
  	bl_FurnaceRecipes_addFurnaceRecipe(recipes, inputId, outputStack);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeShowTipMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	void* mygui = bl_MinecraftClient_getGui(bl_minecraft);
	bl_Gui_showTipMessage(mygui, mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntitySetNameTag
  (JNIEnv *env, jclass clazz, jlong entityId, jstring name) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	const char * nameUtfChars = env->GetStringUTFChars(name, NULL);
	SynchedEntityData* entityData = (SynchedEntityData*) (((uintptr_t) entity) + 8);
	DataItem2<std::string>* dataItem = static_cast<DataItem2<std::string >*>(entityData->_find(2));
	if (dataItem == nullptr) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "dataItem is null");
		env->ReleaseStringUTFChars(name, nameUtfChars);
		return;
	}
	std::string* lineStr = &(dataItem->data);
	if (lineStr == NULL || lineStr->length() == 0) {
		//Workaround for C++ standard library's empty string optimization failing across libraries
		//search FULLY_DYNAMIC_STRING
		// (I no longer remember how this works)
		std::string* mystr = new std::string(nameUtfChars);
		*((void**) lineStr) = *((void**) mystr);
	} else {
		lineStr->assign(nameUtfChars);
	}
	env->ReleaseStringUTFChars(name, nameUtfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetStonecutterItem
  (JNIEnv *env, jclass clazz, jint itemId, jint status) {
	bl_stonecutter_status[itemId] = status;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetItemCategory
  (JNIEnv *env, jclass clazz, jint itemId, jint category, jint mystery1) {
	Item* myitem = bl_Item_items[itemId];
	myitem->category1 = category;
	//myitem->category2 = mystery1;
}

void bl_sendPacket(Packet* packet) {
	PacketSender* sender = bl_Minecraft_getPacketSender(bl_minecraft);
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
	const char * messageUtfChars = env->GetStringUTFChars(message, NULL);
	std::string* myName = bl_Entity_getNameTag(bl_localplayer);
	TextPacket textPacket;
	textPacket.type = 1;
	memset(&(textPacket.filler2), 0, sizeof(textPacket.filler2));
	textPacket.username = *myName;
	textPacket.message = messageUtfChars;
	bl_sendPacket(&textPacket);
	env->ReleaseStringUTFChars(message, messageUtfChars);
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
	Entity* riding = entity->riding;
	if (riding == NULL) return -1;
	return riding->entityId;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetRider
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return -1;
	Entity* rider = entity->rider;
	if (rider == NULL) return -1;
	return rider->entityId;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetMobSkin
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return NULL;
	std::string* mystr = (std::string*) (((int) entity) + MOB_TEXTURE_OFFSET);
	jstring returnValString = env->NewStringUTF(mystr->c_str());
	return returnValString;
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
	Entity** camera = (Entity**) (((uintptr_t) bl_minecraft) + MINECRAFT_CAMERA_ENTITY_OFFSET);
	*camera = entity;
}

JNIEXPORT jlongArray JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntityGetUUID
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return NULL;
	std::array<unsigned char, 16> uuidBytes = bl_getEntityUUID(entityId);
	jlong* uuidLongs = (jlong*) uuidBytes.data();
	jlongArray uuidArray = env->NewLongArray(2);
	env->SetLongArrayRegion(uuidArray, 0, 2, uuidLongs);
	return uuidArray;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelAddParticle
  (JNIEnv *env, jclass clazz, jint type, jfloat x, jfloat y, jfloat z, jfloat xVel, jfloat yVel, jfloat zVel, jint data) {
	Vec3 pos {x, y, z};
	Vec3 vel {xVel, yVel, zVel};
	bl_Level_addParticle(bl_level, type, pos, vel, data);
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelIsRemote
  (JNIEnv *env, jclass clazz) {
	return bl_level->isRemote;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefinePlaceholderBlocks
  (JNIEnv *env, jclass clazz) {
#ifdef __arm__
	for (int i = 1; i < 0x100; i++) {
		if (bl_Tile_tiles[i] == NULL) {
			char name[100];
			snprintf(name, sizeof(name), "Missing block ID: %d", i);
			std::string textureNames[16*6];
			for (int a = 0; a < 16*6; a++) {
				textureNames[a] = "missing_tile";
			}
			int textureCoords[16*6];
			memset(textureCoords, 0, sizeof(textureCoords));
			bl_createBlock(i, textureNames, textureCoords, 17 /* wood */, true, 0, (const char*) name);
		}
	}
#endif
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetPointedEntity
  (JNIEnv *env, jclass clazz) {
	HitResult* objectMouseOver = (HitResult*) ((uintptr_t) bl_level + MINECRAFT_HIT_RESULT_OFFSET);
	if (objectMouseOver->type != HIT_RESULT_ENTITY) return -1;
	Entity* hoverEntity = *((Entity**) ((uintptr_t) bl_level + MINECRAFT_HIT_ENTITY_OFFSET));
	if (hoverEntity == NULL) return -1;
	return hoverEntity->entityId;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePlayerGetPointedBlock
  (JNIEnv *env, jclass clazz, jint axis) {
	HitResult* objectMouseOver = (HitResult*) ((uintptr_t) bl_level + MINECRAFT_HIT_RESULT_OFFSET);
	if (objectMouseOver->type != HIT_RESULT_BLOCK) return -1;
	switch (axis) {
		case AXIS_X:
			return objectMouseOver->x;
		case AXIS_Y:
			return objectMouseOver->y;
		case AXIS_Z:
			return objectMouseOver->z;
		case BLOCK_SIDE:
			return objectMouseOver->side;
		case BLOCK_ID:
			return bl_TileSource_getTile(bl_level->tileSource,
				objectMouseOver->x, objectMouseOver->y, objectMouseOver->z);
		case BLOCK_DATA:
			return bl_TileSource_getData(bl_level->tileSource,
				objectMouseOver->x, objectMouseOver->y, objectMouseOver->z);
		default:
			return -1;
	}
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetBiome
  (JNIEnv *env, jclass clazz, jint x, jint z) {
	if (bl_level == NULL) return 0;
	TilePos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	Biome* biome = bl_TileSource_getBiome(bl_level->tileSource, pos);
	if (biome == NULL) return 0;
	return biome->id;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetBiomeName
  (JNIEnv *env, jclass clazz, jint x, jint z) {
	if (bl_level == NULL) return NULL;
	TilePos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	Biome* biome = bl_TileSource_getBiome(bl_level->tileSource, pos);
	if (biome == NULL) return NULL;
	jstring retval = env->NewStringUTF(biome->name.c_str());
	return retval;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelGetGrassColor
  (JNIEnv *env, jclass clazz, jint x, jint z) {
	if (bl_level == NULL) return 0;
	TilePos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	return bl_TileSource_getGrassColor(bl_level->tileSource, pos);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLevelSetGrassColor
  (JNIEnv *env, jclass clazz, jint x, jint z, jint color) {
	if (bl_level == NULL) return;
	TilePos pos;
	pos.x = x;
	pos.y = 64;
	pos.z = z;
	bl_TileSource_setGrassColor(bl_level->tileSource, color, pos, 3); //if you recall, 3 = full block update
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
	LevelChunk* chunk = bl_TileSource_getChunk(bl_level->tileSource, x >> 4, z >> 4);
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

enum {
	BL_ARCH_ARM = 0,
	BL_ARCH_I386
};

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetArch
  (JNIEnv *env, jclass clazz) {
#ifdef __i386
	return BL_ARCH_I386;
#else
	return BL_ARCH_ARM;
#endif
};

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetHandEquipped
  (JNIEnv *env, jclass clazz, jint id, jboolean handEquipped) {
	Item* item = bl_Item_items[id];
	if (item == nullptr) return;
	item->handEquipped = handEquipped;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSpawnerSetEntityType
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint entityTypeId) {
	if (bl_level == NULL) return;
	void* te = bl_TileSource_getTileEntity(bl_level->tileSource, x, y, z);
	if (te == NULL) return;

	BaseMobSpawner* spawner = *((BaseMobSpawner**) (((uintptr_t) te) + MOB_SPAWNER_OFFSET));
	bl_BaseMobSpawner_setEntityId(spawner, entityTypeId);
	bl_TileEntity_setChanged(te);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeScreenChooserSetScreen
  (JNIEnv *env, jclass clazz, jint screen) {
	//bl_ScreenChooser_setScreen(*((ScreenChooser**) ((uintptr_t) bl_minecraft + MINECRAFT_SCREENCHOOSER_OFFSET)), screen);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeCloseScreen
  (JNIEnv *env, jclass clazz) {
	//bl_MinecraftClient_setScreen(bl_minecraft, nullptr);
}
JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeShowProgressScreen
  (JNIEnv *env, jclass clazz) {
	/* FIXME 0.11
	void* progress = operator new(224);
	bl_ProgressScreen_ProgressScreen(progress);
	bl_MinecraftClient_setScreen(bl_minecraft, progress);
	*/
}

void bl_Mob_die_hook(Entity* entity1, EntityDamageSource& damageSource) {
	JNIEnv *env;
	preventDefaultStatus = false;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "mobDieCallback", "(JJ)V");
	long long victimId = entity1->entityId;
	long long attackerId = -1;
	if (damageSource.isEntitySource()) {
		attackerId = static_cast<EntityDamageByEntitySource&>(damageSource).entity->entityId;
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
	std::unique_ptr<Entity> entity;
	Vec3 pos;
	pos.x = x;
	pos.y = y;
	pos.z = z;
	if (type < 64) {
		entity = MobFactory::CreateMob(type, *bl_level->tileSource, pos, nullptr); //the last two vec3s are pos and rot
	} else {
		entity = EntityFactory::CreateEntity(type, *bl_level->tileSource);
	}

	if (entity == nullptr) {
		//WTF?
		return -1;
	}
	Entity* e = entity.get();
	bl_Entity_setPos_helper(e, x, y, z);

	//skins
	if (skinPath != NULL && type < 64) {
		const char * skinUtfChars = env->GetStringUTFChars(skinPath, NULL);
		bl_changeEntitySkin((void*) e, skinUtfChars);
		env->ReleaseStringUTFChars(skinPath, skinUtfChars);
	}

	bl_level->addEntity(std::move(entity));

	return e->entityId;

}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDropItem
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jfloat range, jint id, jint count, jint damage) {

	ItemInstance* instance = bl_newItemInstance(id, count, damage);

	Entity* entity = (Entity*) ::operator new(kItemEntitySize);
	bl_ItemEntity_ItemEntity(entity, *(bl_level->tileSource), x, y + range, z, *instance);

	*((int*) (((uintptr_t) entity) + kItemEntity_pickupDelay_offset)) = 10; // 0.5 seconds

	bl_level->addEntity(std::unique_ptr<Entity>(entity));

	return entity->entityId;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetUseController
  (JNIEnv *env, jclass clazz, jboolean use) {
	if (bl_forceController) return;
#ifdef __arm__
	unsigned char* ptr = (unsigned char*) (((uintptr_t) bl_MinecraftClient_useController) & ~1);
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

unsigned char bl_TileSource_getTile(TileSource* source, int x, int y, int z) {
	FullTile retval = bl_TileSource_getTile_raw(source, x, y, z);
	return retval.id;
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

void* bl_marauder_translation_function(void* input);
static void patchUnicodeFont(void* mcpelibhandle) {
	void* setUnicodeTexture = dlsym(mcpelibhandle, "_ZN4Font17setUnicodeTextureEi");
	if (setUnicodeTexture == NULL) return;
	void* loadTexture = dlsym(mcpelibhandle, "_ZN8Textures11loadTextureERKSsbb");
	void* getTextureData = dlsym(mcpelibhandle, "_ZN8Textures14getTextureDataERKSs");
	uint16_t* setUnicodeTexture_b = (uint16_t*) ((uintptr_t) setUnicodeTexture & ~1);
	int offset = 0x267eb0 - 0x267e74;
	uint16_t buf[2];
	generateBl(buf, ((uintptr_t) setUnicodeTexture) + offset + 4, (uintptr_t) loadTexture);
	if (buf[0] != setUnicodeTexture_b[offset >> 1] || buf[1] != setUnicodeTexture_b[(offset >> 1) + 1]) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "not buf: %x %x %x %x",
			(int) buf[0], (int) buf[1], (int) setUnicodeTexture_b[offset >> 1], (int) setUnicodeTexture_b[(offset >> 1) + 1]);
		return;
	}
	generateBl(buf, ((uintptr_t) setUnicodeTexture) + offset + 4, (uintptr_t) getTextureData);
	uint16_t* setUnicodeTexture_m = (uint16_t*) bl_marauder_translation_function((void*) setUnicodeTexture_b);
	setUnicodeTexture_m[offset >> 1] = buf[0];
	setUnicodeTexture_m[(offset >> 1) + 1] = buf[1];
}

void bl_forceTextureLoad(std::string const& name) {
	void* textures = *((void**) ((uintptr_t) bl_minecraft + MINECRAFT_TEXTURES_OFFSET));
	bl_Textures_getTextureData(textures, name);
}

void bl_cppNewLevelInit() {
	bl_entityUUIDMap.clear();
}

void bl_set_i18n(std::string const& key, std::string const& value) {
	(I18n::getCurrentLanguage()->map)[key] = value;
}

static bool isLocalAddress(JNIEnv* env, jstring hostJString) {
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "isLocalAddress", "(Ljava/lang/String;)Z");

	return env->CallStaticBooleanMethod(bl_scriptmanager_class, mid, hostJString);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemIdCount
  (JNIEnv* env, jclass clazz) {
	return bl_item_id_count;
}

void bl_prepatch_cppside(void* mcpelibhandle_) {
	soinfo2* mcpelibhandle = (soinfo2*) mcpelibhandle_;
	void* originalItemsAddress = dlsym(mcpelibhandle, "_ZN4Item5itemsE");
	// Edit the dlsym for Item::items
	Elf_Sym* itemsSym = (Elf_Sym*) bl_marauder_translation_function(
		(void*) dobby_elfsym((void*) mcpelibhandle, "_ZN4Item5itemsE"));
	// since value + base = addr:
	Elf_Addr oldValue = itemsSym->st_value;
	itemsSym->st_value = ((uintptr_t) &bl_items) - mcpelibhandle->base;
	Elf_Word oldSize = itemsSym->st_size;
	itemsSym->st_size = sizeof(bl_items);
	// now check if the address matches
	void* newItemsAddress = dlsym(mcpelibhandle, "_ZN4Item5itemsE");
	if (newItemsAddress != &bl_items) {
		// poor man's assert
		// restore stuff
		itemsSym->st_value = oldValue;
		itemsSym->st_size = oldSize;
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: expected %p got %p original %p",
			&bl_items, newItemsAddress, originalItemsAddress);
		return;
	}

	// now edit the GOT
	// for got, got_end_addr[got_entry] = addr
	// FIND THE .GOT SECTION OR DIE TRYING
	void** got = nullptr;
	for (int i = 0; i < mcpelibhandle->phnum; i++) {
		const Elf_Phdr* phdr = mcpelibhandle->phdr + i;
		if (phdr->p_type == PT_DYNAMIC) { // .got always comes after .dynamic in every Android lib I've seen
			got = (void**) (((uintptr_t) mcpelibhandle->base) + phdr->p_vaddr + phdr->p_memsz);
			break;
		}
	}
	if (got == nullptr) {
		itemsSym->st_value = oldValue;
		itemsSym->st_size = oldSize;
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: can't find the GOT");
		return;
	}
	bool got_success = false;
	void** got_rw = (void**) bl_marauder_translation_function((void*) got);
	for (int i = 0; i < 5000; i++) {
		if (got[i] == originalItemsAddress) {
			got_rw[i] = &bl_items;
			got_success = true;
			break;
		}
	}
	if (!got_success) {
		itemsSym->st_value = oldValue;
		itemsSym->st_size = oldSize;
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: couldn't find Item::items pointer in GOT");
		return;
	}
	void* ItemInstance__setItem = dlsym(mcpelibhandle, "_ZN12ItemInstance8_setItemEi");
	unsigned char* setItemCode = (unsigned char*)
		bl_marauder_translation_function((void*)(((uintptr_t) ItemInstance__setItem) & ~1));
	if (setItemCode[4] == 0x00 && setItemCode[5] == 0x7f) {
		setItemCode[4] = 0x80; setItemCode[5] = 0x5f;
	} else if (setItemCode[2] == 0x00 && setItemCode[3] == 0x7f) {
		setItemCode[2] = 0x80; setItemCode[3] = 0x5f;
	} else {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Failed to expand item array: can't patch setItem");
		return;
	}
	bl_item_id_count = BL_ITEMS_EXPANDED_COUNT;
}

void bl_setuphooks_cppside() {
	soinfo2* mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);

	bl_Gui_displayClientMessage = (void (*)(void*, const std::string&)) dlsym(RTLD_DEFAULT, "_ZN3Gui20displayClientMessageERKSs");

	void* sendChatMessage = dlsym(RTLD_DEFAULT, "_ZN10ChatScreen15sendChatMessageEv");
	mcpelauncher_hook(sendChatMessage, (void*) &bl_ChatScreen_sendChatMessage_hook, (void**) &bl_ChatScreen_sendChatMessage_real);

	bl_Item_Item = (void (*)(Item*, int)) dlsym(RTLD_DEFAULT, "_ZN4ItemC2Ei");
	bl_Item_setNameID = (void (*)(Item*, std::string const&)) dlsym(RTLD_DEFAULT, "_ZN4Item9setNameIDERKSs");

	// FIXME 0.11
	bl_Minecraft_selectLevel = (void (*) (Minecraft*, std::string const&, std::string const&, void*)) 
		dlsym(RTLD_DEFAULT, "_ZN9Minecraft11selectLevelERKSsS1_RK13LevelSettings");
	bl_MinecraftClient_leaveGame = (void (*) (Minecraft*, bool)) dlsym(RTLD_DEFAULT, "_ZN15MinecraftClient9leaveGameEb"); //hooked - just pull whichever version MCPE uses
	bl_Minecraft_setLeaveGame = (void (*) (Minecraft*)) dlsym(RTLD_DEFAULT, "_ZN9Minecraft12setLeaveGameEv");

	//bl_Minecraft_connectToMCOServer = (void (*) (Minecraft*, std::string const&, std::string const&, unsigned short))
	//	dlsym(RTLD_DEFAULT, "_ZN9Minecraft18connectToMCOServerERKSsS1_t");

	bl_Level_playSound = (void (*) (Level*, float, float, float, std::string const&, float, float))
		dlsym(RTLD_DEFAULT, "_ZN5Level9playSoundEfffRKSsff");

	//bl_Level_getAllEntities = (void* (*)(Level*))
	//	dlsym(RTLD_DEFAULT, "_ZN5Level14getAllEntitiesEv");

	//bl_Level_addListener = (void (*) (Level*, LevelListener*))
	//	dlsym(RTLD_DEFAULT, "_ZN5Level11addListenerEP13LevelListener");

	bl_RakNetInstance_connect_real = (void (*) (RakNetInstance*, char const*, int))
		dlsym(RTLD_DEFAULT, "_ZN14RakNetInstance7connectEPKci");

	void** raknetVTable = (void**) dobby_dlsym((void*) mcpelibhandle, "_ZTV14RakNetInstance");
	bl_dumpVtable(raknetVTable, 0x100);
	raknetVTable[RAKNET_INSTANCE_VTABLE_OFFSET_CONNECT] = (void*) &bl_RakNetInstance_connect_hook;

	bl_Item_vtable = (void**) ((uintptr_t) dobby_dlsym((void*) mcpelibhandle, "_ZTV4Item")) + 8;
	//I have no idea why I have to subtract 24 (or add 8).
	//tracing out the original vtable seems to suggest this.

#if 0

	void* fontDrawCached = dlsym(RTLD_DEFAULT, "_ZN4Font10drawCachedERKSsffRK5ColorbP11MaterialPtr");
	mcpelauncher_hook(fontDrawCached, (void*) &bl_Font_drawCached_hook, (void**) &bl_Font_drawCached_real);

	bl_Font_width = (int (*) (Font*, std::string const&))
		dlsym(RTLD_DEFAULT, "_ZN4Font5widthERKSs");

#endif

	bl_Tile_vtable = (void**) ((uintptr_t) dobby_dlsym((void*) mcpelibhandle, "_ZTV4Tile") + 8);
	bl_dumpVtable(bl_Tile_vtable, 0x100);
	bl_Material_dirt = (void*) dlsym(RTLD_DEFAULT, "_ZN8Material4dirtE");

	bl_Tile_Tile = (void (*)(Tile*, int, void*)) dlsym(RTLD_DEFAULT, "_ZN4TileC1EiPK8Material");
	bl_TileItem_TileItem = (void (*)(Item*, int)) dobby_dlsym(mcpelibhandle, "_ZN8TileItemC2Ei");
	bl_Tile_setNameId = (void (*)(Tile*, const std::string&))
		dlsym(RTLD_DEFAULT, "_ZN4Tile9setNameIdERKSs");
	bl_Tile_setShape = (void (*)(Tile*, float, float, float, float, float, float))
		dlsym(RTLD_DEFAULT, "_ZN4Tile8setShapeEffffff");
	bl_TileItem_vtable = (void**) ((uintptr_t) dobby_dlsym((void*) mcpelibhandle, "_ZTV8TileItem") + 8);
	bl_Tile_tiles = (Tile**) dlsym(RTLD_DEFAULT, "_ZN4Tile5tilesE");
	bl_Tile_lightEmission = (unsigned char*) dlsym(RTLD_DEFAULT, "_ZN4Tile13lightEmissionE");
	bl_Tile_lightBlock = (unsigned char*) dlsym(RTLD_DEFAULT, "_ZN4Tile10lightBlockE");

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

	bl_initCustomBlockVtable();

	bl_Item_setIcon = (void (*)(Item*, std::string const&, int)) dlsym(mcpelibhandle, "_ZN4Item7setIconERKSsi");

	bl_Mob_setSneaking = (void (*)(Entity*, bool)) dlsym(mcpelibhandle, "_ZN3Mob11setSneakingEb");
	bl_Mob_isSneaking = (bool (*)(Entity*)) dlsym(mcpelibhandle, "_ZN3Mob10isSneakingEv");

	bl_ItemInstance_getName = (std::string const (*) (ItemInstance*)) dlsym(mcpelibhandle, "_ZNK12ItemInstance7getNameEv");
	//bl_ItemInstance_getIcon = (TextureUVCoordinateSet* (*) (ItemInstance*, int, bool)) dlsym(mcpelibhandle, "_ZNK12ItemInstance7getIconEib");

	bl_Tile_getTexture = (TextureUVCoordinateSet* (*)(Tile*, signed char, int)) dlsym(mcpelibhandle, "_ZN4Tile10getTextureEai");
	bl_Tile_getTextureUVCoordinateSet = (TextureUVCoordinateSet (*)(Tile*, std::string const&, int))
		dlsym(mcpelibhandle, "_ZN4Tile25getTextureUVCoordinateSetERKSsi");
	bl_Recipes_getInstance = (Recipes* (*)()) dlsym(mcpelibhandle, "_ZN7Recipes11getInstanceEv");
	bl_Recipes_addShapedRecipe = (void (*)(Recipes*, std::vector<ItemInstance> const&, std::vector<std::string> const&, 
		std::vector<RecipesType> const&)) dlsym(mcpelibhandle,
		"_ZN7Recipes15addShapedRecipeERKSt6vectorI12ItemInstanceSaIS1_EERKS0_ISsSaISsEERKS0_INS_4TypeESaISA_EE");
	bl_FurnaceRecipes_getInstance = (FurnaceRecipes* (*)()) dlsym(mcpelibhandle, "_ZN14FurnaceRecipes11getInstanceEv");
	bl_FurnaceRecipes_addFurnaceRecipe = (void (*)(FurnaceRecipes*, int, ItemInstance const&))
		dlsym(mcpelibhandle, "_ZN14FurnaceRecipes16addFurnaceRecipeEiRK12ItemInstance");
	bl_Gui_showTipMessage = (void (*)(void*, const std::string&)) dlsym(RTLD_DEFAULT, "_ZN3Gui14showTipMessageERKSs");
	
	bl_Item_setMaxStackSize = (void (*)(Item*, unsigned char)) dlsym(mcpelibhandle, "_ZN4Item15setMaxStackSizeEh");
	bl_Item_setMaxDamage = (void (*)(Item*, int)) dlsym(mcpelibhandle, "_ZN4Item12setMaxDamageEi");

	void* isStonecutterItem = dlsym(mcpelibhandle, "_ZN15CraftingFilters17isStonecutterItemERK12ItemInstance");
	//mcpelauncher_hook(isStonecutterItem, (void*) &bl_CraftingFilters_isStonecutterItem_hook, 
	//	(void**) &bl_CraftingFilters_isStonecutterItem_real);
	bl_Item_items = (Item**) dlsym(RTLD_DEFAULT, "_ZN4Item5itemsE");
	// FIXME 0.11
	//void* handleTextPacket = dlsym(mcpelibhandle, "_ZN20ClientNetworkHandler6handleERKN6RakNet10RakNetGUIDEP10TextPacket");
	//mcpelauncher_hook(handleTextPacket, (void*) &bl_ClientNetworkHandler_handleTextPacket_hook,
	//	(void**) &bl_ClientNetworkHandler_handleTextPacket_real);
	void** clientNetworkHandlerVtable = (void**) dlsym(mcpelibhandle, "_ZTV20ClientNetworkHandler") + 2;
	// first two entries are removed
	bl_ClientNetworkHandler_handleTextPacket_real = (void (*)(void*, void*, TextPacket*))
		clientNetworkHandlerVtable[kClientNetworkHandler_vtable_offset_handleTextPacket];
	clientNetworkHandlerVtable[kClientNetworkHandler_vtable_offset_handleTextPacket] =
		(void*) &bl_ClientNetworkHandler_handleTextPacket_hook;
	void** legacyClientNetworkHandlerVtable = (void**) dlsym(mcpelibhandle, "_ZTV26LegacyClientNetworkHandler") + 2;
	legacyClientNetworkHandlerVtable[kClientNetworkHandler_vtable_offset_handleTextPacket] =
		(void*) &bl_ClientNetworkHandler_handleTextPacket_hook;
	bl_SetTimePacket_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV13SetTimePacket");
	// FIXME 0.11
	//bl_RakNetInstance_send = (void (*) (void*, void*)) dlsym(mcpelibhandle, "_ZN14RakNetInstance4sendER6Packet");
	bl_Packet_Packet = (void (*) (void*)) dlsym(mcpelibhandle, "_ZN6PacketC2Ev");
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
	bl_MinecraftClient_setScreen = (void (*)(Minecraft*, void*)) dlsym(mcpelibhandle, "_ZN15MinecraftClient9setScreenEP6Screen");
	//bl_ProgressScreen_ProgressScreen = (void (*)(void*)) dlsym(mcpelibhandle, "_ZN14ProgressScreenC1Ev");
	//bl_Minecraft_locateMultiplayer = (void (*)(Minecraft*)) dlsym(mcpelibhandle, "_ZN9Minecraft17locateMultiplayerEv");
	bl_Textures_getTextureData = (void* (*)(void*, std::string const&))
		dlsym(mcpelibhandle, "_ZN8Textures14getTextureDataERKSs");
	void* addEntity = dlsym(mcpelibhandle, "_ZN5Level9addEntityESt10unique_ptrI6EntitySt14default_deleteIS1_EE");
	mcpelauncher_hook(addEntity, (void*) &bl_Level_addEntity_hook, (void**) &bl_Level_addEntity_real);
	void* mpAddEntity = dlsym(mcpelibhandle, "_ZN16MultiPlayerLevel9addEntityESt10unique_ptrI6EntitySt14default_deleteIS1_EE");
	mcpelauncher_hook(mpAddEntity, (void*) &bl_MultiPlayerLevel_addEntity_hook, (void**) &bl_MultiPlayerLevel_addEntity_real);
	void* addPlayer = dlsym(mcpelibhandle, "_ZN5Level9addPlayerEP6Player");
	mcpelauncher_hook(addPlayer, (void*) &bl_Level_addPlayer_hook, (void**) &bl_Level_addPlayer_real);
	void* onEntityRemoved = dlsym(mcpelibhandle, "_ZN5Level12removeEntityER6Entity");
	mcpelauncher_hook(onEntityRemoved, (void*) &bl_Level_removeEntity_hook, (void**) &bl_Level_removeEntity_real);

	void* explode = dlsym(mcpelibhandle, "_ZN5Level7explodeEP6Entityffffb");
	mcpelauncher_hook(explode, (void*) &bl_Level_explode_hook, (void**) &bl_Level_explode_real);

	bl_TileSource_getBiome = (Biome* (*)(TileSource*, TilePos&)) dlsym(mcpelibhandle, "_ZN10TileSource8getBiomeERK7TilePos");
	bl_TileSource_getGrassColor = (int (*)(TileSource*, TilePos&)) dlsym(mcpelibhandle, "_ZN10TileSource13getGrassColorERK7TilePos");
	bl_TileSource_setGrassColor = (void (*)(TileSource*, int, TilePos&, int))
		dlsym(mcpelibhandle, "_ZN10TileSource13setGrassColorEiRK7TilePosi");

	void* fireTileEvent = dlsym(mcpelibhandle, "_ZN10TileSource13fireTileEventEiiiii");
	mcpelauncher_hook(fireTileEvent, (void*) &bl_TileSource_fireTileEvent_hook, (void**) &bl_TileSource_fireTileEvent_real);

	bl_Tile_solid = (bool*) dlsym(RTLD_DEFAULT, "_ZN4Tile5solidE");
	bl_Tile_getAABB = (AABB* (*)(Tile*, TileSource*, int, int, int, AABB&, int, bool, int))
		dlsym(mcpelibhandle, "_ZN4Tile7getAABBEP10TileSourceiiiR4AABBibi");
	bl_ReedTile_getAABB = (AABB* (*)(Tile*, TileSource*, int, int, int, AABB&, int, bool, int))
		dlsym(mcpelibhandle, "_ZN8ReedTile7getAABBEP10TileSourceiiiR4AABBibi");

	bl_TileSource_getChunk = (LevelChunk* (*)(TileSource*, int, int))
		dlsym(mcpelibhandle, "_ZN10TileSource8getChunkEii");
	bl_LevelChunk_setBiome = (void (*)(LevelChunk*, Biome const&, ChunkTilePos const&))
		dlsym(mcpelibhandle, "_ZN10LevelChunk8setBiomeERK5BiomeRK12ChunkTilePos");
	bl_Biome_getBiome = (Biome* (*)(int))
		dlsym(mcpelibhandle, "_ZN5Biome8getBiomeEi");
	bl_Entity_setSize = (void (*)(Entity*, float, float))
		dlsym(mcpelibhandle, "_ZN6Entity7setSizeEff");
	bl_TileSource_getTile_raw = (FullTile (*)(TileSource*, int, int, int))
		dlsym(mcpelibhandle, "_ZN10TileSource7getTileEiii");
	bl_MinecraftClient_getGui = (void* (*)(Minecraft*))
		dlsym(mcpelibhandle, "_ZN15MinecraftClient6getGuiEv");
	bl_BaseMobSpawner_setEntityId = (void (*)(BaseMobSpawner*, int))
		dlsym(mcpelibhandle, "_ZN14BaseMobSpawner11setEntityIdEi");
	bl_TileEntity_setChanged = (void (*)(TileEntity*))
		dlsym(mcpelibhandle, "_ZN10TileEntity10setChangedEv");
	bl_ArmorItem_ArmorItem = (void (*)(ArmorItem*, int, void*, int, int))
		dlsym(mcpelibhandle, "_ZN9ArmorItemC1EiRKNS_13ArmorMaterialEii");
	bl_ScreenChooser_setScreen = (void (*)(ScreenChooser*, int))
		dlsym(mcpelibhandle, "_ZN13ScreenChooser9setScreenE8ScreenId");
	// FIXME 0.11
	//bl_Minecraft_hostMultiplayer = (void (*)(Minecraft*, int))
	//	dlsym(mcpelibhandle, "_ZN9Minecraft15hostMultiplayerEi");

	void* mobDie = dlsym(RTLD_DEFAULT, "_ZN3Mob3dieER18EntityDamageSource");
	mcpelauncher_hook(mobDie, (void*) &bl_Mob_die_hook, (void**) &bl_Mob_die_real);

	bl_MinecraftClient_useController = (bool (*) (Minecraft*))
		dlsym(mcpelibhandle, "_ZN15MinecraftClient13useControllerEv");

#ifdef __arm__
	unsigned char* useControllerPtr = (unsigned char*) (((uintptr_t) bl_MinecraftClient_useController) & ~1);
	bl_forceController = *useControllerPtr;
#endif

	bl_Entity_getNameTag = (std::string* (*)(Entity*))
		dlsym(mcpelibhandle, "_ZN6Entity10getNameTagEv");
	bl_Mob_addEffect = (void (*)(Entity*, MobEffectInstance&))
		dlsym(mcpelibhandle, "_ZN3Mob9addEffectERK17MobEffectInstance");
	bl_Mob_removeEffect = (void (*)(Entity*, int))
		dlsym(mcpelibhandle, "_ZN3Mob12removeEffectEi");
	bl_Mob_removeAllEffects = (void (*)(Entity*))
		dlsym(mcpelibhandle, "_ZN3Mob16removeAllEffectsEv");

	bl_FoodItem_FoodItem = (void (*)(Item*, int, int, bool, float))
		dlsym(mcpelibhandle, "_ZN8FoodItemC1Eiibf");

	bl_ItemEntity_ItemEntity = (void (*)(Entity*, TileSource&, float, float, float, ItemInstance&))
		dlsym(mcpelibhandle, "_ZN10ItemEntityC1ER10TileSourcefffRK12ItemInstance");
	bl_Item_addCreativeItem = (void (*)(short, short))
		dlsym(mcpelibhandle, "_ZN4Item15addCreativeItemEss");
	bl_Minecraft_getPacketSender = (PacketSender* (*)(Minecraft*))
		dlsym(mcpelibhandle, "_ZN9Minecraft15getPacketSenderEv");

	//patchUnicodeFont(mcpelibhandle);
	bl_renderManager_init(mcpelibhandle);
}

} //extern
