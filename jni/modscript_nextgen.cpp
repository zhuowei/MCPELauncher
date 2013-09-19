#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>
#include <string>

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"
#include "modscript_shared.h"

extern "C" {

static void (*bl_ChatScreen_sendChatMessage_real)(void*);

static void (*bl_Gui_displayClientMessage)(void*, std::string const&);

static void (*bl_Item_Item)(Item*, int);

static void** bl_FoodItem_vtable;

static void (*bl_Item_setDescriptionId)(Item*, std::string const&);

static void (*bl_Minecraft_selectLevel)(Minecraft*, std::string const&, std::string const&, void*);

static void (*bl_Minecraft_leaveGame)(Minecraft*, bool saveWorld);

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

Item* bl_constructFoodItem(int id, int hearts) {
	Item* retval = (Item*) ::operator new((std::size_t) 48);
	bl_Item_Item(retval, id - 0x100);
	retval->vtable = bl_FoodItem_vtable;
	((int*)retval)[9] = hearts;
	((float*) retval)[10] = 0.3f; //time to eat
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
	Item* item = bl_constructFoodItem(id, halfhearts);
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
	bl_Minecraft_leaveGame(bl_minecraft, false);
	bl_Minecraft_selectLevel(bl_minecraft, worlddirstr, worlddirstr, NULL);
}

void bl_changeEntitySkin(void* entity, const char* newSkin) {
	std::string* newSkinString = new std::string(newSkin);
	std::string* ptrToStr = (std::string*) (((int) entity) + 2920);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer: %p, %i, %s\n", ptrToStr, *((int*) ptrToStr), ptrToStr->c_str());
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "New string pointer: %s\n", newSkinString->c_str());
	(*ptrToStr) = (*newSkinString);
	std::string* ptrToStr2 = (std::string*) (((int) entity) + 2920);
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer again: %p, %i, %s\n", ptrToStr2, *((int*) ptrToStr2), ptrToStr2->c_str());
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

	soinfo2* mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);
	int foodItemVtableOffset = 0x291a18;
	bl_FoodItem_vtable = (void**) (mcpelibhandle->base + foodItemVtableOffset);
}

} //extern
