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

#include "utf8proc.h"

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"
#include "dobby_public.h"

#define cppbool bool

#include "modscript_shared.h"

#include "modscript_ScriptLevelListener.hpp"

#include "minecraft_colors.h"

typedef void RakNetInstance;
typedef void Font;

#define RAKNET_INSTANCE_VTABLE_OFFSET_CONNECT 5
#define MINECRAFT_RAKNET_INSTANCE_OFFSET 3104
#define SIGN_TILE_ENTITY_LINE_OFFSET 92
#define BLOCK_VTABLE_SIZE 0x144
#define BLOCK_VTABLE_GET_TEXTURE_OFFSET 13
#define BLOCK_VTABLE_IS_CUBE_SHAPED 4
#define BLOCK_VTABLE_GET_RENDER_SHAPE 5
#define BLOCK_VTABLE_GET_DESCRIPTION_ID 58
#define BLOCK_VTABLE_GET_COLOR 55
#define BLOCK_VTABLE_GET_RENDER_LAYER 46
#define BLOCK_VTABLE_IS_SOLID_RENDER 19
#define MOB_TEXTURE_OFFSET 2948
#define PLAYER_NAME_OFFSET 3200
#define ENTITY_VTABLE_OFFSET_IS_PLAYER 44
#define MINECRAFT_GUI_OFFSET 416
#define ENTITY_RENDERER_OFFSET_RENDER_NAME 6

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

extern "C" {

static void (*bl_ChatScreen_sendChatMessage_real)(void*);

static void (*bl_Gui_displayClientMessage)(void*, std::string const&);

static void (*bl_Item_Item)(Item*, int);

static void** bl_FoodItem_vtable;
static void** bl_Item_vtable;
static void** bl_Tile_vtable;
static void** bl_TileItem_vtable;
static Tile** bl_Tile_tiles;
static int* bl_Tile_lightEmission;

static void (*bl_Item_setDescriptionId)(Item*, std::string const&);

static void (*bl_Minecraft_selectLevel)(Minecraft*, std::string const&, std::string const&, void*);

static void (*bl_Minecraft_leaveGame)(Minecraft*, bool saveWorld, bool thatotherboolean);

static void (*bl_Minecraft_connectToMCOServer)(Minecraft*, std::string const&, std::string const&, unsigned short);

static void (*bl_Level_playSound)(Level*, float, float, float, std::string const&, float, float);

static void* (*bl_Level_getAllEntities)(Level*);

static void (*bl_Level_addListener)(Level*, LevelListener*);

static void (*bl_RakNetInstance_connect_real)(RakNetInstance*, char const*, int);

static void (*bl_Font_drawSlow_real)(Font*, char const*, int, float, float, int, bool);

static int (*bl_Font_width)(Font*, std::string const&);

static void* bl_Material_dirt;

static void (*bl_Tile_Tile)(Tile*, int, void*);
static void (*bl_TileItem_TileItem)(Item*, int);
static void (*bl_Tile_setDescriptionId)(Tile*, const std::string&);
static void (*bl_Tile_setShape)(Tile*, float, float, float, float, float, float);
static std::string (*bl_Tile_getDescriptionId)(Tile*);
static void (*bl_Mob_setSneaking)(Entity*, bool);

static void (*bl_Item_setIcon)(Item*, std::string const&, int);
static void (*bl_CreativeInventryScreen_populateTile_real)(Tile*, int, int);
static void (*bl_CreativeInventryScreen_populateItem_real)(Item*, int, int);

//static Item** bl_Item_items;
static std::string const (*bl_ItemInstance_getDescriptionId)(ItemInstance*);
static TextureUVCoordinateSet* (*bl_ItemInstance_getIcon)(ItemInstance*, int, bool);
static TextureUVCoordinateSet* (*bl_Tile_getTexture)(Tile*, int, int);
static void (*bl_Tile_getTextureUVCoordinateSet)(TextureUVCoordinateSet*, Tile*, std::string const&, int);
static Recipes* (*bl_Recipes_getInstance)();
static void (*bl_Recipes_addShapelessRecipe)(Recipes*, ItemInstance const&, std::vector<RecipesType> const&);
static FurnaceRecipes* (*bl_FurnaceRecipes_getInstance)();
static void (*bl_FurnaceRecipes_addFurnaceRecipe)(FurnaceRecipes*, int, ItemInstance const&);
static void (*bl_Gui_showTipMessage)(void*, std::string const&);
static void (*bl_PlayerRenderer_renderName)(void*, Entity*, float);
static bool (*bl_CraftingFilters_isStonecutterItem_real)(ItemInstance const&);

static void** bl_ShapelessRecipe_vtable;

static void (*bl_RakNetInstance_send)(void*, void*);
static void** bl_SetTimePacket_vtable;
static void (*bl_Packet_Packet)(void*);
static void (*bl_ClientSideNetworkHandler_handleMessagePacket_real)(void*, void*, MessagePacket*);
static void** bl_MessagePacket_vtable;

bool bl_text_parse_color_codes = true;

//custom blocks
void* bl_CustomBlock_vtable[BLOCK_VTABLE_SIZE];
TextureUVCoordinateSet** bl_custom_block_textures[256];
bool bl_custom_block_opaque[256];
int bl_custom_block_renderShape[256];
int* bl_custom_block_colors[256];
uint8_t bl_custom_block_renderLayer[256];
//end custom blocks

std::map <std::string, std::string>* bl_I18n_strings;

int bl_addItemCreativeInvRequest[256][4];
int bl_addItemCreativeInvRequestCount = 0;

std::map <int, std::string> bl_nametag_map;
char bl_stonecutter_status[512];

static Item** bl_Item_items;

#define STONECUTTER_STATUS_DEFAULT 0
#define STONECUTTER_STATUS_FORCE_FALSE 1
#define STONECUTTER_STATUS_FORCE_TRUE 2


void bl_ChatScreen_sendChatMessage_hook(void* chatScreen) {
	std::string* chatMessagePtr = (std::string*) ((int) chatScreen + 84);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Chat message: %s\n", chatMessagePtr->c_str());
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

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_RakNetInstance_connect_real(rakNetInstance, host, port);
}

void bl_Font_drawSlow_hook(Font* font, char const* text, int length, float xOffset, float yOffset, int color, bool isShadow) {
	if (bl_text_parse_color_codes) {
		char const* currentTextBegin = text; //the current coloured section
		int currentLength = 0; //length in bytes of the current coloured section
		const uint8_t* iteratePtr = (const uint8_t*) text; //where we are iterating
		const uint8_t* endIteratePtr = (const uint8_t*) text + length; //once we reach here, stop iterating
		//int lengthOfStringRemaining = length;
		float substringOffset = xOffset; //where we draw the currently coloured section
		int curColor = color; //the colour for the current section
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

				int newColor = color;
				if (myChar >= '0' && myChar <= '9') {
					newColor = bl_minecraft_colors[myChar - '0'];
				} else if (myChar >= 'a' && myChar <= 'f') {
					newColor = bl_minecraft_colors[myChar - 'a' + 10];
				}

				bl_Font_drawSlow_real(font, currentTextBegin, currentLength, substringOffset, yOffset, curColor, isShadow);
				std::string cppStringForWidth = std::string(currentTextBegin, currentLength);
				substringOffset += bl_Font_width(font, cppStringForWidth);

				curColor = newColor;
				currentTextBegin = (const char *) iteratePtr;
				currentLength = 0;

			} else {
				currentLength += bytesAdvanced;
			}
		}
		if (currentLength > 0) {
			bl_Font_drawSlow_real(font, currentTextBegin, currentLength, substringOffset, yOffset, curColor, isShadow);
		}
	} else {
		bl_Font_drawSlow_real(font, text, length, xOffset, yOffset, color, isShadow);
		return;
	}
}

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
		} else if (id > 0 && id < 512) {
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

const char* bl_getCharArr(void* str){
	std::string* mystr = static_cast<std::string*>(str);
	std::string mystr2 = *mystr;
	const char* cs = mystr2.c_str();
	return cs;
}

TextureUVCoordinateSet* bl_CustomBlock_getTextureHook(Tile* tile, int side, int data) {
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

int bl_CustomBlock_getRenderShapeHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_renderShape[blockId];
}

int bl_CustomBlock_getColorHook(Tile* tile, Level* level, int x, int y, int z) {
	int blockId = tile->id;
	int* myColours = bl_custom_block_colors[blockId];
	if (myColours == NULL || bl_level == NULL) return 0xffffff; //I see your true colours shining through
	int data = bl_Level_getData(bl_level, x, y, z);
	return myColours[data];
}

int bl_CustomBlock_getRenderLayerHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_renderLayer[blockId];
}

bool bl_CustomBlock_isSolidRenderHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_renderShape[blockId] == 0;
}

void bl_EntityRenderer_renderName_hook(void* renderer, Entity* entity, float scale) {
	int entityId = entity->entityId;
	if (bl_nametag_map.count(entityId) == 0) return;
	std::string mystr = bl_nametag_map[entityId];
	//back up the existing data at the nametag's spot, then overwrite it with our string. 
	//This is awful code, but it might work.
	void* ptrToName = (void*) (((uintptr_t) entity) + PLAYER_NAME_OFFSET);
	char backup[sizeof(std::string)];
	memcpy(backup, ptrToName, sizeof(std::string));
	memcpy(ptrToName, &mystr, sizeof(std::string));

	float* stance1 = (float*) ((uintptr_t) entity + 36); //y position
	float* stance2 = (float*) ((uintptr_t) entity + 196); //y at last tick
	float* entityHeightPtr = (float*) ((uintptr_t) entity + 180);
	float backupstance1 = *stance1;
	float backupstance2 = *stance2;
	float entityHeight = *entityHeightPtr;
	*stance1 = *stance1 + entityHeight;
	*stance2 = *stance2 + entityHeight;

	bl_PlayerRenderer_renderName(renderer, entity, scale);

	*stance1 = backupstance1;
	*stance2 = backupstance2;
	memcpy(ptrToName, backup, sizeof(std::string));
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

void bl_ClientSideNetworkHandler_handleChatPacket_hook(void* handler, void* ipaddress, ChatPacket* packet) {
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jstring messageJString = env->NewStringUTF(packet->message.c_str());
	preventDefaultStatus = false;
	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "handleChatPacketCallback", "(Ljava/lang/String;)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, messageJString);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) {
		void* mygui = (void*) (((int) bl_minecraft) + MINECRAFT_GUI_OFFSET);
		bl_Gui_displayClientMessage(mygui, packet->message);
	}
}

void bl_ClientSideNetworkHandler_handleMessagePacket_hook(void* handler, void* ipaddress, MessagePacket* packet) {
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	jstring senderJString = env->NewStringUTF(packet->sender->text);
	jstring messageJString = env->NewStringUTF(packet->message->text);
	preventDefaultStatus = false;
	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "handleMessagePacketCallback", "(Ljava/lang/String;Ljava/lang/String;)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, senderJString, messageJString);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!preventDefaultStatus) {
		bl_ClientSideNetworkHandler_handleMessagePacket_real(handler, ipaddress, packet);
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClientMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	void* mygui = (void*) (((int) bl_minecraft) + MINECRAFT_GUI_OFFSET);
	bl_Gui_displayClientMessage(mygui, mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

Item* bl_constructItem(int id) {
	Item* retval = (Item*) ::operator new((std::size_t) 72);
	bl_Item_Item(retval, id - 0x100);
	retval->category1 = 2; //tool
	retval->category2 = 0;
	return retval;
}

Item* bl_constructFoodItem(int id, int hearts, float timetoeat) {
	Item* retval = (Item*) ::operator new((std::size_t) 84);
	bl_Item_Item(retval, id - 0x100);
	retval->vtable = bl_FoodItem_vtable;
	((int*)retval)[18] = hearts;
	((float*) retval)[19] = timetoeat; //time to eat
	retval->category1 = 4; //food
	retval->category2 = 0;
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineItem
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jstring name) {
	Item* item = bl_constructItem(id);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon(item, iconNameString, iconIndex);

	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	bl_Item_setDescriptionId(item, mystr);
	(*bl_I18n_strings)["item." + mystr + ".name"] = mystr;
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineFoodItem
  (JNIEnv *env, jclass clazz, jint id, jstring iconName, jint iconIndex, jint halfhearts, jstring name) {
	Item* item = bl_constructFoodItem(id, halfhearts, 0.3f);

	const char * iconUTFChars = env->GetStringUTFChars(iconName, NULL);
	std::string iconNameString = std::string(iconUTFChars);
	bl_Item_setIcon(item, iconNameString, iconIndex);

	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	bl_Item_setDescriptionId(item, mystr);
	(*bl_I18n_strings)["item." + mystr + ".name"] = mystr;
	env->ReleaseStringUTFChars(name, utfChars);
	env->ReleaseStringUTFChars(iconName, iconUTFChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSelectLevel
  (JNIEnv *env, jclass clazz, jstring worlddir) {
	const char * utfChars = env->GetStringUTFChars(worlddir, NULL);
	std::string worlddirstr = std::string(utfChars);
	env->ReleaseStringUTFChars(worlddir, utfChars);
	bl_Minecraft_selectLevel(bl_minecraft, worlddirstr, worlddirstr, NULL);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeLeaveGame
  (JNIEnv *env, jclass clazz, jboolean saveMultiplayerWorld) {
	bl_Minecraft_leaveGame(bl_minecraft, saveMultiplayerWorld, true);
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

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetAllEntities
  (JNIEnv *env, jclass clazz) {
	void* ptr = bl_Level_getAllEntities(bl_level);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "%x, %x\n", ptr, *((void**)(ptr)));
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeJoinServer
  (JNIEnv *env, jclass clazz, jstring host, jint port) {
	const char* hostChars = env->GetStringUTFChars(host, NULL);
	int rakNetOffset = ((int) bl_minecraft) + MINECRAFT_RAKNET_INSTANCE_OFFSET;
	RakNetInstance* raknetInstance = *((RakNetInstance**) rakNetOffset);
	bl_RakNetInstance_connect_hook(raknetInstance, hostChars, port);
	env->ReleaseStringUTFChars(host, hostChars);
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetSignText
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint line) {
	if (bl_level == NULL) return NULL;
	void* te = bl_Level_getTileEntity(bl_level, x, y, z);
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
	void* te = bl_Level_getTileEntity(bl_level, x, y, z);
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
	std::string* newSkinString = new std::string(newSkin);
	std::string* ptrToStr = (std::string*) (((int) entity) + MOB_TEXTURE_OFFSET);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer: %p, %i, %s\n", ptrToStr, *((int*) ptrToStr), ptrToStr->c_str());
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "New string pointer: %s\n", newSkinString->c_str());
	(*ptrToStr) = (*newSkinString);
	std::string* ptrToStr2 = (std::string*) (((int) entity) + 2920);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer again: %p, %i, %s\n", ptrToStr2, *((int*) ptrToStr2), ptrToStr2->c_str());
}

void bl_attachLevelListener() {
	ScriptLevelListener* listener = new ScriptLevelListener();
	bl_Level_addListener(bl_level, listener);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSneaking
  (JNIEnv *env, jclass clazz, jint entityId, jboolean doIt) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//bl_Mob_setSneaking(entity, doIt);
	bool* movement = *((bool**) ((uintptr_t) entity + 3416));
	movement[14] = doIt;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemName
  (JNIEnv *env, jclass clazz, jint itemId, jint itemDamage, jboolean raw) {
	if (itemId <= 0 || itemId >= 512) return NULL;
	ItemInstance* myStack = bl_newItemInstance(itemId, 1, itemDamage);
	if (myStack == NULL || bl_ItemInstance_getId(myStack) != itemId) return NULL;
	switch(itemId) {
		case 95:
		case 255:
			//these return blank strings. Blank strings will kill libstdc++ since we are not using the same blank string.
			return NULL;
	}
	std::string descriptionId = bl_ItemInstance_getDescriptionId(myStack);
	if (descriptionId.length() <= 0) {
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "dead tile: %i\n", itemId);
	}
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Tile: %s\n", descriptionId.c_str());
	std::string returnVal = descriptionId;
	if (!raw) {
		returnVal = (*bl_I18n_strings)[descriptionId];
	}
	jstring returnValString = env->NewStringUTF(returnVal.c_str());
	return returnValString;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTextureCoordinatesForItem
  (JNIEnv *env, jclass clazz, jint itemId, jint itemDamage, jfloatArray outputArray) {
	if (itemId <= 0 || itemId >= 512) return false;
	ItemInstance* myStack = bl_newItemInstance(itemId, 1, itemDamage);
	if (myStack == NULL || bl_ItemInstance_getId(myStack) != itemId) return false;
	TextureUVCoordinateSet* set = bl_ItemInstance_getIcon(myStack, 0, true);
	if (set == NULL || set->bounds == NULL) return false;
	env->SetFloatArrayRegion(outputArray, 0, 6, set->bounds);
	return true;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPlayerName
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return NULL;
	std::string* myName = (std::string*) ((intptr_t) entity + PLAYER_NAME_OFFSET);
	jstring returnValString = env->NewStringUTF(myName->c_str());
	return returnValString;
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeIsPlayer
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	void* vtable = entity->vtable[ENTITY_VTABLE_OFFSET_IS_PLAYER];
	int (*fn)(Entity*) = (int (*) (Entity*)) vtable;
	return fn(entity) != 0;
}

void bl_initCustomBlockVtable() {
	//copy existing vtable
	memcpy(bl_CustomBlock_vtable, bl_Tile_vtable, BLOCK_VTABLE_SIZE);
	//set the texture getter to our overridden version
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_TEXTURE_OFFSET] = (void*) &bl_CustomBlock_getTextureHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_IS_CUBE_SHAPED] = (void*) &bl_CustomBlock_isCubeShapedHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_RENDER_SHAPE] = (void*) &bl_CustomBlock_getRenderShapeHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_COLOR] = (void*) &bl_CustomBlock_getColorHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_RENDER_LAYER] = (void*) &bl_CustomBlock_getRenderLayerHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_IS_SOLID_RENDER] = (void*) &bl_CustomBlock_isSolidRenderHook;
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
		TextureUVCoordinateSet* mySet = new TextureUVCoordinateSet;
		//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Building %s %d\n", textureNames[i].c_str(), textureCoords[i]);
		bl_Tile_getTextureUVCoordinateSet(mySet, sacrificialTile, textureNames[i], textureCoords[i]);
		output[i] = mySet;
	}
}

Tile* bl_createBlock(int blockId, std::string textureNames[], int textureCoords[], int materialType, bool opaque, int renderShape, const char* name) {
	if (blockId < 0 || blockId > 255) return NULL;
	if (bl_custom_block_textures[blockId] != NULL) {
		delete[] bl_custom_block_textures[blockId];
	}
	bl_custom_block_opaque[blockId] = opaque;
	bl_custom_block_textures[blockId] = new TextureUVCoordinateSet*[16*6];
	bl_buildTextureArray(bl_custom_block_textures[blockId], textureNames, textureCoords);
	bl_custom_block_renderShape[blockId] = renderShape;
	//Allocate memory for the block
	Tile* retval = (Tile*) ::operator new((std::size_t) 0x80);
	retval->vtable = bl_CustomBlock_vtable;
	bl_Tile_Tile(retval, blockId, bl_getMaterial(materialType));
	retval->vtable = bl_CustomBlock_vtable;
	retval->material = bl_getMaterial(materialType);
	std::string nameStr = std::string(name);
	bl_Tile_setDescriptionId(retval, nameStr);
	//std::string comeOut = bl_Tile_getDescriptionId(retval);
	//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Block added: %s\n", comeOut.c_str());
	(*bl_I18n_strings)["tile." + nameStr + ".name"] = nameStr;
	//add it to the global tile list
	bl_Tile_tiles[blockId] = retval;
	retval->category1 = 1;
	retval->category2 = 1;
	//now allocate the item
	Item* tileItem = (Item*) ::operator new((std::size_t) 76);
	tileItem->vtable = bl_TileItem_vtable;
	bl_TileItem_TileItem(tileItem, blockId - 0x100);
	tileItem->vtable = bl_TileItem_vtable;
	tileItem->category1 = 1;
	tileItem->category2 = 1;
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
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemCreativeInv
  (JNIEnv *env, jclass clazz, jint id, jint count, jint damage) {
	if (id < 0) return;

	bl_addItemCreativeInvRequestCount++;
	int index = bl_addItemCreativeInvRequestCount;
	bl_addItemCreativeInvRequest[index][0] = id;
	bl_addItemCreativeInvRequest[index][1] = count;
	bl_addItemCreativeInvRequest[index][2] = damage;
	bl_addItemCreativeInvRequest[index][3] = 1;
	//bl_CreativeInventryScreen_populateItem_real(tile, count, damage);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetI18NString
  (JNIEnv *env, jclass clazz, jstring key, jstring value) {

	const char * valueUTFChars = env->GetStringUTFChars(value, NULL);
	std::string valueNameString = std::string(valueUTFChars);

	const char * keyUTFChars = env->GetStringUTFChars(key, NULL);
	std::string keyString = std::string(keyUTFChars);
	(*bl_I18n_strings)[keyString] = valueNameString;
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

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddShapelessRecipe
  (JNIEnv *env, jclass clazz, jint itemId, jint itemCount, jint itemDamage, jintArray ingredientsArray) {
	int ingredientsElemsCount = env->GetArrayLength(ingredientsArray);
	int ingredients[ingredientsElemsCount];
	env->GetIntArrayRegion(ingredientsArray, 0, ingredientsElemsCount, ingredients);
	ItemInstance outStack;
	bl_setItemInstance(&outStack, itemId, itemCount, itemDamage);
	int ingredientsCount = ingredientsElemsCount / 2;
	std::vector<RecipesType> ingredientsList;
	for (int i = 0; i < ingredientsCount; i++) {
		RecipesType recipeType;
		recipeType.wtf2 = 0;
		recipeType.item = NULL;
		recipeType.itemInstance.damage = ingredients[i * 2 + 1];
		recipeType.itemInstance.count = 1;
		bl_ItemInstance_setId(&recipeType.itemInstance, ingredients[i * 2]);
		ingredientsList.push_back(recipeType);
	}
	Recipes* recipes = bl_Recipes_getInstance();
	bl_tryRemoveExistingRecipe(recipes, itemId, itemCount, itemDamage, ingredients, ingredientsCount);
	bl_Recipes_addShapelessRecipe(recipes, outStack, ingredientsList);
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
	void* mygui = (void*) (((int) bl_minecraft) + MINECRAFT_GUI_OFFSET);
	bl_Gui_showTipMessage(mygui, mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeEntitySetNameTag
  (JNIEnv *env, jclass clazz, jint entityId, jstring name) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	const char * nameUtfChars = env->GetStringUTFChars(name, NULL);
	std::string nameString = std::string(nameUtfChars);
	bl_nametag_map[entity->entityId] = nameString;
	env->ReleaseStringUTFChars(name, nameUtfChars);
}

/* List of all renderers that need name tag support */
static const char* renderersToPatch[] = {
"_ZTV15ChickenRenderer",
"_ZTV15CreeperRenderer",
"_ZTV11PigRenderer",
"_ZTV14SpiderRenderer",
"_ZTV19HumanoidMobRenderer",
"_ZTV11MobRenderer",
"_ZTV13SheepRenderer"
};


static void patchEntityRenderers(soinfo2* mcpelibhandle) {
	int renderersCount = sizeof(renderersToPatch) / sizeof(const char*);
	for (int i = 0; i < renderersCount; i++) {
		void** entityRendererVtable = (void**) dobby_dlsym(mcpelibhandle, renderersToPatch[i]);
		entityRendererVtable[ENTITY_RENDERER_OFFSET_RENDER_NAME] = (void*) &bl_EntityRenderer_renderName_hook;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetStonecutterItem
  (JNIEnv *env, jclass clazz, jint itemId, jint status) {
	bl_stonecutter_status[itemId] = status;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetItemCategory
  (JNIEnv *env, jclass clazz, jint itemId, jint category, jint mystery1) {
	Item* myitem = bl_Item_items[itemId];
	myitem->category1 = category;
	myitem->category2 = mystery1;
}

void bl_sendPacket(void* packet) {
	void* bl_raknet = *((void**) (((uintptr_t) bl_minecraft) + 3144));
	void* vtable = ((void***) bl_raknet)[0][14];
	void (*fn)(void*, void*) = (void (*) (void*, void*)) vtable;
	fn(bl_raknet, packet);
}

void bl_sendIdentPacket() {
	/*
	 * format of the ident packet: SUBJECT TO EXTREME CHANGE!!!1eleven
	 * Currently a SetTimePacket sent from client to server. Vanilla servers *should* ignore it, as well as old PocketMine.
	 * The magic time value is 0x1abe11ed, and the boolean sent is false.
	 */
	SetTimePacket packet;
	bl_Packet_Packet(&packet);
	packet.vtable = (void**) (((uintptr_t) bl_SetTimePacket_vtable) + 8);
	packet.time = 0x1abe11ed;
	packet.started = false;
	bl_sendPacket(&packet);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSendChat
  (JNIEnv *env, jclass clazz, jstring message) {
	const char * messageUtfChars = env->GetStringUTFChars(message, NULL);
	std::string* myName = (std::string*) ((intptr_t) bl_localplayer + PLAYER_NAME_OFFSET);
	MessagePacket packet;
	bl_Packet_Packet(&packet);
	packet.vtable = (void**) (((uintptr_t) bl_MessagePacket_vtable) + 8);
	packet.sender = new RakString;
	packet.sender->text = (char*) myName->c_str();
	packet.message = new RakString;
	packet.message->text = (char*) messageUtfChars;
	bl_sendPacket(&packet);
	delete packet.sender;
	delete packet.message;
	env->ReleaseStringUTFChars(message, messageUtfChars);
}

void bl_setuphooks_cppside() {
	bl_Gui_displayClientMessage = (void (*)(void*, const std::string&)) dlsym(RTLD_DEFAULT, "_ZN3Gui20displayClientMessageERKSs");

	void* sendChatMessage = dlsym(RTLD_DEFAULT, "_ZN10ChatScreen15sendChatMessageEv");
	mcpelauncher_hook(sendChatMessage, (void*) &bl_ChatScreen_sendChatMessage_hook, (void**) &bl_ChatScreen_sendChatMessage_real);

	bl_Item_Item = (void (*)(Item*, int)) dlsym(RTLD_DEFAULT, "_ZN4ItemC2Ei");
	bl_Item_setDescriptionId = (void (*)(Item*, std::string const&)) dlsym(RTLD_DEFAULT, "_ZN4Item16setDescriptionIdERKSs");

	bl_Minecraft_selectLevel = (void (*) (Minecraft*, std::string const&, std::string const&, void*)) 
		dlsym(RTLD_DEFAULT, "_ZN9Minecraft11selectLevelERKSsS1_RK13LevelSettings");
	bl_Minecraft_leaveGame = (void (*) (Minecraft*, bool, bool)) dlsym(RTLD_DEFAULT, "_ZN9Minecraft9leaveGameEbb"); //hooked - just pull whichever version MCPE uses

	bl_Minecraft_connectToMCOServer = (void (*) (Minecraft*, std::string const&, std::string const&, unsigned short))
		dlsym(RTLD_DEFAULT, "_ZN9Minecraft18connectToMCOServerERKSsS1_t");

	bl_Level_playSound = (void (*) (Level*, float, float, float, std::string const&, float, float))
		dlsym(RTLD_DEFAULT, "_ZN5Level9playSoundEfffRKSsff");

	bl_Level_getAllEntities = (void* (*)(Level*))
		dlsym(RTLD_DEFAULT, "_ZN5Level14getAllEntitiesEv");

	bl_Level_addListener = (void (*) (Level*, LevelListener*))
		dlsym(RTLD_DEFAULT, "_ZN5Level11addListenerEP13LevelListener");

	bl_RakNetInstance_connect_real = (void (*) (RakNetInstance*, char const*, int))
		dlsym(RTLD_DEFAULT, "_ZN14RakNetInstance7connectEPKci");

	int* raknetVTable = (int*) dlsym(RTLD_DEFAULT, "_ZTV14RakNetInstance");
	raknetVTable[RAKNET_INSTANCE_VTABLE_OFFSET_CONNECT] = (int) &bl_RakNetInstance_connect_hook;

	soinfo2* mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);
	bl_FoodItem_vtable = (void**) ((int) dobby_dlsym((void*) mcpelibhandle, "_ZTV8FoodItem") + 8);
	bl_Item_vtable = (void**) ((int) dlsym((void*) mcpelibhandle, "_ZTV4Item")) + 8;
	//I have no idea why I have to subtract 24 (or add 8).
	//tracing out the original vtable seems to suggest this.

	void* fontDrawSlow = dlsym(RTLD_DEFAULT, "_ZN4Font8drawSlowEPKciffib");
	mcpelauncher_hook(fontDrawSlow, (void*) &bl_Font_drawSlow_hook, (void**) &bl_Font_drawSlow_real);

	bl_Font_width = (int (*) (Font*, std::string const&))
		dlsym(RTLD_DEFAULT, "_ZN4Font5widthERKSs");

	bl_Tile_vtable = (void**) ((int) dlsym(RTLD_DEFAULT, "_ZTV4Tile") + 8);
	bl_Material_dirt = (void*) dlsym(RTLD_DEFAULT, "_ZN8Material4dirtE");

	bl_Tile_Tile = (void (*)(Tile*, int, void*)) dlsym(RTLD_DEFAULT, "_ZN4TileC1EiPK8Material");
	bl_TileItem_TileItem = (void (*)(Item*, int)) dobby_dlsym(mcpelibhandle, "_ZN8TileItemC2Ei");
	bl_Tile_setDescriptionId = (void (*)(Tile*, const std::string&))
		dlsym(RTLD_DEFAULT, "_ZN4Tile16setDescriptionIdERKSs");
	bl_Tile_setShape = (void (*)(Tile*, float, float, float, float, float, float))
		dlsym(RTLD_DEFAULT, "_ZN4Tile8setShapeEffffff");
	bl_TileItem_vtable = (void**) ((int) dobby_dlsym((void*) mcpelibhandle, "_ZTV8TileItem") + 8);
	bl_Tile_tiles = (Tile**) dlsym(RTLD_DEFAULT, "_ZN4Tile5tilesE");
	bl_Tile_lightEmission = (int*) dlsym(RTLD_DEFAULT, "_ZN4Tile13lightEmissionE");
	bl_Tile_getDescriptionId = (std::string (*)(Tile*))
		dlsym(RTLD_DEFAULT, "_ZNK4Tile16getDescriptionIdEv");

	bl_initCustomBlockVtable();

	bl_I18n_strings = (std::map <std::string, std::string> *) dlsym(RTLD_DEFAULT, "_ZN4I18n8_stringsE");
	bl_Item_setIcon = (void (*)(Item*, std::string const&, int)) dlsym(mcpelibhandle, "_ZN4Item7setIconERKSsi");

	bl_Mob_setSneaking = (void (*)(Entity*, bool)) dlsym(RTLD_DEFAULT, "_ZN3Mob11setSneakingEb");

	//bl_Item_items = (Item**) dlsym(mcpelibhandle, "_ZN4Item5itemsE");
	bl_ItemInstance_getDescriptionId = (std::string const (*) (ItemInstance*)) dlsym(mcpelibhandle, "_ZNK12ItemInstance16getDescriptionIdEv");
	bl_ItemInstance_getIcon = (TextureUVCoordinateSet* (*) (ItemInstance*, int, bool)) dlsym(mcpelibhandle, "_ZNK12ItemInstance7getIconEib");

	void* populateTile = dlsym(RTLD_DEFAULT, "_ZN23CreativeInventoryScreen12populateItemEP4Tileii");
	mcpelauncher_hook(populateTile, (void*) &bl_CreativeInventryScreen_populateTile_hook, (void**) &bl_CreativeInventryScreen_populateTile_real);

	void* populateItem = dlsym(RTLD_DEFAULT, "_ZN23CreativeInventoryScreen12populateItemEP4Itemii");
	mcpelauncher_hook(populateItem, (void*) &bl_CreativeInventryScreen_populateItem_hook, (void**) &bl_CreativeInventryScreen_populateItem_real);

	bl_Tile_getTexture = (TextureUVCoordinateSet* (*)(Tile*, int, int)) dlsym(mcpelibhandle, "_ZN4Tile10getTextureEii");
	bl_Tile_getTextureUVCoordinateSet = (void (*)(TextureUVCoordinateSet*, Tile*, std::string const&, int)) 
		dlsym(mcpelibhandle, "_ZN4Tile25getTextureUVCoordinateSetERKSsi");
	bl_Recipes_getInstance = (Recipes* (*)()) dlsym(mcpelibhandle, "_ZN7Recipes11getInstanceEv");
	bl_Recipes_addShapelessRecipe = (void (*)(Recipes*, ItemInstance const&, std::vector<RecipesType> const&)) 
		dlsym(mcpelibhandle, "_ZN7Recipes18addShapelessRecipeERK12ItemInstanceRKSt6vectorINS_4TypeESaIS4_EE");
	bl_FurnaceRecipes_getInstance = (FurnaceRecipes* (*)()) dlsym(mcpelibhandle, "_ZN14FurnaceRecipes11getInstanceEv");
	bl_FurnaceRecipes_addFurnaceRecipe = (void (*)(FurnaceRecipes*, int, ItemInstance const&))
		dlsym(mcpelibhandle, "_ZN14FurnaceRecipes16addFurnaceRecipeEiRK12ItemInstance");
	bl_Gui_showTipMessage = (void (*)(void*, const std::string&)) dlsym(RTLD_DEFAULT, "_ZN3Gui14showTipMessageERKSs");

	patchEntityRenderers(mcpelibhandle);
	bl_PlayerRenderer_renderName = (void (*)(void*, Entity*, float)) dlsym(mcpelibhandle, "_ZN14PlayerRenderer10renderNameEP6Entityf");
	bl_ShapelessRecipe_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV15ShapelessRecipe");

	void* isStonecutterItem = dlsym(mcpelibhandle, "_ZN15CraftingFilters17isStonecutterItemERK12ItemInstance");
	//mcpelauncher_hook(isStonecutterItem, (void*) &bl_CraftingFilters_isStonecutterItem_hook, 
	//	(void**) &bl_CraftingFilters_isStonecutterItem_real);
	bl_Item_items = (Item**) dlsym(RTLD_DEFAULT, "_ZN4Item5itemsE");
	void* handleChatPacket = dlsym(mcpelibhandle, "_ZN24ClientSideNetworkHandler6handleERKN6RakNet10RakNetGUIDEP10ChatPacket");
	void* handleReal;
	mcpelauncher_hook(handleChatPacket, (void*) &bl_ClientSideNetworkHandler_handleChatPacket_hook, &handleReal);
	bl_SetTimePacket_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV13SetTimePacket");
	bl_RakNetInstance_send = (void (*) (void*, void*)) dlsym(mcpelibhandle, "_ZN14RakNetInstance4sendER6Packet");
	bl_Packet_Packet = (void (*) (void*)) dlsym(mcpelibhandle, "_ZN6PacketC2Ev");
	void* handleMessagePacket = dlsym(mcpelibhandle, "_ZN24ClientSideNetworkHandler6handleERKN6RakNet10RakNetGUIDEP13MessagePacket");
	mcpelauncher_hook(handleMessagePacket, (void*) &bl_ClientSideNetworkHandler_handleMessagePacket_hook,
		(void**) &bl_ClientSideNetworkHandler_handleMessagePacket_real);
	bl_MessagePacket_vtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV13MessagePacket");
}

} //extern
