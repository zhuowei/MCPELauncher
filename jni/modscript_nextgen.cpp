#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <typeinfo>

#include "utf8proc.h"

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"

#define cppbool bool

#include "modscript_shared.h"

#include "modscript_ScriptLevelListener.hpp"

#include "minecraft_colors.h"

typedef void RakNetInstance;
typedef void Font;

#define RAKNET_INSTANCE_VTABLE_OFFSET_CONNECT 5
#define MINECRAFT_RAKNET_INSTANCE_OFFSET 3104
#define SIGN_TILE_ENTITY_LINE_OFFSET 68
#define BLOCK_VTABLE_SIZE 0x120
#define BLOCK_VTABLE_GET_TEXTURE_OFFSET 10
#define BLOCK_VTABLE_IS_CUBE_SHAPED 4
#define BLOCK_VTABLE_GET_RENDER_SHAPE 5
#define BLOCK_VTABLE_GET_NAME 57
#define BLOCK_VTABLE_GET_DESCRIPTION_ID 58

extern "C" {

static void (*bl_ChatScreen_sendChatMessage_real)(void*);

static void (*bl_Gui_displayClientMessage)(void*, std::string const&);

static void (*bl_Item_Item)(Item*, int);

static void** bl_FoodItem_vtable;
static void** bl_Item_vtable;
static void** bl_Tile_vtable;
static void** bl_TileItem_vtable;
static Tile** bl_Tile_tiles;

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

bool bl_text_parse_color_codes = true;

//custom blocks
void* bl_CustomBlock_vtable[BLOCK_VTABLE_SIZE];
int* bl_custom_block_textures[256];
bool bl_custom_block_opaque[256];
int bl_custom_block_renderShape[256];
//end custom blocks

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

const char* bl_getCharArr(void* str){
	std::string* mystr = static_cast<std::string*>(str);
	std::string mystr2 = *mystr;
	const char* cs = mystr2.c_str();
	return cs;
}

int bl_CustomBlock_getTextureHook(Tile* tile, int side, int data) {
	int blockId = tile->id;
	int* ptrToBlockInfo = bl_custom_block_textures[blockId];
	if (ptrToBlockInfo == NULL) {
		return 0;
	}
	return ptrToBlockInfo[(data * 6) + side];
}

bool bl_CustomBlock_isCubeShapedHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_opaque[blockId];
}

int bl_CustomBlock_getRenderShapeHook(Tile* tile) {
	int blockId = tile->id;
	return bl_custom_block_renderShape[blockId];
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClientMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	void* mygui = (void*) (((int) bl_minecraft) + 400);
	bl_Gui_displayClientMessage(mygui, mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

Item* bl_constructItem(int id) {
	Item* retval = (Item*) ::operator new((std::size_t) 36);
	bl_Item_Item(retval, id - 0x100);
	return retval;
}

Item* bl_constructFoodItem(int id, int hearts, float timetoeat) {
	Item* retval = (Item*) ::operator new((std::size_t) 48);
	bl_Item_Item(retval, id - 0x100);
	retval->vtable = bl_FoodItem_vtable;
	((int*)retval)[9] = hearts;
	((float*) retval)[10] = timetoeat; //time to eat
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineItem
  (JNIEnv *env, jclass clazz, jint id, jint icon, jstring name) {
	Item* item = bl_constructItem(id);
	item->icon = icon;
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	bl_Item_setDescriptionId(item, mystr);
	env->ReleaseStringUTFChars(name, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineFoodItem
  (JNIEnv *env, jclass clazz, jint id, jint icon, jint halfhearts, jstring name) {
	Item* item = bl_constructFoodItem(id, halfhearts, 0.3f);
	item->icon = icon;
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	std::string mystr = std::string(utfChars);
	bl_Item_setDescriptionId(item, mystr);
	env->ReleaseStringUTFChars(name, utfChars);
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
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "%x, %x\n", ptr, *((void**)(ptr)));
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
	std::string* ptrToStr = (std::string*) (((int) entity) + 2920);
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer: %p, %i, %s\n", ptrToStr, *((int*) ptrToStr), ptrToStr->c_str());
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "New string pointer: %s\n", newSkinString->c_str());
	(*ptrToStr) = (*newSkinString);
	std::string* ptrToStr2 = (std::string*) (((int) entity) + 2920);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer again: %p, %i, %s\n", ptrToStr2, *((int*) ptrToStr2), ptrToStr2->c_str());
}

void bl_attachLevelListener() {
	ScriptLevelListener* listener = new ScriptLevelListener();
	bl_Level_addListener(bl_level, listener);
}

void bl_initCustomBlockVtable() {
	//copy existing vtable
	memcpy(bl_CustomBlock_vtable, bl_Tile_vtable, BLOCK_VTABLE_SIZE);
	//set the texture getter to our overridden version
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_TEXTURE_OFFSET] = (void*) &bl_CustomBlock_getTextureHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_IS_CUBE_SHAPED] = (void*) &bl_CustomBlock_isCubeShapedHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_RENDER_SHAPE] = (void*) &bl_CustomBlock_getRenderShapeHook;
	bl_CustomBlock_vtable[BLOCK_VTABLE_GET_NAME] = bl_CustomBlock_vtable[BLOCK_VTABLE_GET_DESCRIPTION_ID];
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "The material is %x\n", bl_Material_dirt);
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "The material vtable is %x\n", *((int*) bl_Material_dirt));
}

void* bl_getMaterial(int materialType) {
	return bl_Tile_tiles[1]->material;
}

Tile* bl_createBlock(int blockId, int texture[], int materialType, bool opaque, int renderShape, const char* name) {
	if (blockId < 0 || blockId > 255) return NULL;
	if (bl_custom_block_textures[blockId] != NULL) return NULL;
	bl_custom_block_opaque[blockId] = opaque;
	bl_custom_block_textures[blockId] = texture;
	bl_custom_block_renderShape[blockId] = renderShape;
	//Allocate memory for the block
	Tile* retval = (Tile*) ::operator new((std::size_t) 92);
	retval->vtable = bl_CustomBlock_vtable;
	bl_Tile_Tile(retval, blockId, bl_getMaterial(materialType));
	retval->vtable = bl_CustomBlock_vtable;
	retval->material = bl_getMaterial(materialType);
	std::string nameStr = std::string(name);
	bl_Tile_setDescriptionId(retval, nameStr);
	//add it to the global tile list
	bl_Tile_tiles[blockId] = retval;
	//now allocate the item
	Item* tileItem = (Item*) ::operator new((std::size_t) 40);
	tileItem->vtable = bl_TileItem_vtable;
	bl_TileItem_TileItem(tileItem, blockId - 0x100);
	tileItem->vtable = bl_TileItem_vtable;
	return retval;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDefineBlock
  (JNIEnv *env, jclass clazz, jint blockId, jstring name, jintArray textures, jboolean opaque, jint renderType) {
	const char * utfChars = env->GetStringUTFChars(name, NULL);
	int* myIntArray = new int[16*6];
	env->GetIntArrayRegion(textures, 0, 16*6, myIntArray);
	Tile* tile = bl_createBlock(blockId, myIntArray, 1, opaque, renderType, utfChars);
	if (tile == NULL) delete[] myIntArray;
	env->ReleaseStringUTFChars(name, utfChars);
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
	int foodItemVtableOffset = 0x291a50;
	bl_FoodItem_vtable = (void**) (mcpelibhandle->base + foodItemVtableOffset + 8); //I have no idea why I have to add 8.
	bl_Item_vtable = (void**) (mcpelibhandle->base + 0x2923d0 + 8); //tracing out the original vtable seems to suggest this.

	void* fontDrawSlow = dlsym(RTLD_DEFAULT, "_ZN4Font8drawSlowEPKciffib");
	mcpelauncher_hook(fontDrawSlow, (void*) &bl_Font_drawSlow_hook, (void**) &bl_Font_drawSlow_real);

	bl_Font_width = (int (*) (Font*, std::string const&))
		dlsym(RTLD_DEFAULT, "_ZN4Font5widthERKSs");

	bl_Tile_vtable = (void**) ((int) dlsym(RTLD_DEFAULT, "_ZTV4Tile") + 8);
	bl_Material_dirt = (void*) dlsym(RTLD_DEFAULT, "_ZN8Material4dirtE");

	bl_Tile_Tile = (void (*)(Tile*, int, void*)) dlsym(RTLD_DEFAULT, "_ZN4TileC1EiPK8Material");
	bl_TileItem_TileItem = (void (*)(Item*, int)) (mcpelibhandle->base + 0x187559); //TODO amazon dlsym(RTLD_DEFAULT, "_ZN8TileItemC2Ei");
	bl_Tile_setDescriptionId = (void (*)(Tile*, const std::string&))
		dlsym(RTLD_DEFAULT, "_ZN4Tile16setDescriptionIdERKSs");
	bl_TileItem_vtable = (void**) (mcpelibhandle->base + 0x294e88);//dlsym(RTLD_DEFAULT, "_ZTV8TileItem");
	bl_Tile_tiles = (Tile**) dlsym(RTLD_DEFAULT, "_ZN4Tile5tilesE");

	bl_initCustomBlockVtable();
}

} //extern
