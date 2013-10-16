#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <typeinfo>

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"

#define cppbool bool

#include "modscript_shared.h"

#include "modscript_ScriptLevelListener.hpp"

typedef void RakNetInstance;

#define RAKNET_INSTANCE_VTABLE_OFFSET_CONNECT 5
#define MINECRAFT_RAKNET_INSTANCE_OFFSET 3104

extern "C" {

static void (*bl_ChatScreen_sendChatMessage_real)(void*);

static void (*bl_Gui_displayClientMessage)(void*, std::string const&);

static void (*bl_Item_Item)(Item*, int);

static void** bl_FoodItem_vtable;
static void** bl_Item_vtable;

static void (*bl_Item_setDescriptionId)(Item*, std::string const&);

static void (*bl_Minecraft_selectLevel)(Minecraft*, std::string const&, std::string const&, void*);

static void (*bl_Minecraft_leaveGame)(Minecraft*, bool saveWorld);

static void (*bl_Minecraft_connectToMCOServer)(Minecraft*, std::string const&, std::string const&, unsigned short);

static void (*bl_Level_playSound)(Level*, float, float, float, std::string const&, float, float);

static void* (*bl_Level_getAllEntities)(Level*);

static void (*bl_Level_addListener)(Level*, LevelListener*);

static void (*bl_RakNetInstance_connect_real)(RakNetInstance*, char const*, int);

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

const char* bl_getCharArr(void* str){
	std::string* mystr = static_cast<std::string*>(str);
	std::string mystr2 = *mystr;
	const char* cs = mystr2.c_str();
	return cs;
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
	bl_Minecraft_leaveGame(bl_minecraft, saveMultiplayerWorld);
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

void bl_setuphooks_cppside() {
	bl_Gui_displayClientMessage = (void (*)(void*, const std::string&)) dlsym(RTLD_DEFAULT, "_ZN3Gui20displayClientMessageERKSs");

	void* sendChatMessage = dlsym(RTLD_DEFAULT, "_ZN10ChatScreen15sendChatMessageEv");
	mcpelauncher_hook(sendChatMessage, (void*) &bl_ChatScreen_sendChatMessage_hook, (void**) &bl_ChatScreen_sendChatMessage_real);

	bl_Item_Item = (void (*)(Item*, int)) dlsym(RTLD_DEFAULT, "_ZN4ItemC2Ei");
	bl_Item_setDescriptionId = (void (*)(Item*, std::string const&)) dlsym(RTLD_DEFAULT, "_ZN4Item16setDescriptionIdERKSs");

	bl_Minecraft_selectLevel = (void (*) (Minecraft*, std::string const&, std::string const&, void*)) 
		dlsym(RTLD_DEFAULT, "_ZN9Minecraft11selectLevelERKSsS1_RK13LevelSettings");
	bl_Minecraft_leaveGame = (void (*) (Minecraft*, bool)) dlsym(RTLD_DEFAULT, "_ZN9Minecraft9leaveGameEb"); //hooked via vtable; we use the unhooked version here

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
}

} //extern
