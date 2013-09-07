#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>
#include <string>

#include "mcpelauncher.h"
#include "modscript.h"
#include "modscript_shared.h"

extern "C" {

static void (*bl_Gui_displayClientMessage)(void*, std::string const&);

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClientMessage
  (JNIEnv *env, jclass clazz, jstring text) {
	const char * utfChars = env->GetStringUTFChars(text, NULL);
	std::string mystr = std::string(utfChars);
	void* mygui = (void*) (((int) bl_minecraft) + 400);
	bl_Gui_displayClientMessage(mygui, mystr);
	env->ReleaseStringUTFChars(text, utfChars);
}

void bl_changeEntitySkin(void* entity, const char* newSkin) {
	std::string* newSkinString = new std::string(newSkin);
	std::string* ptrToStr = (std::string*) (((int) entity) + 2920);
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer: %p, %i, %s\n", ptrToStr, *((int*) ptrToStr), ptrToStr->c_str());
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "New string pointer: %s\n", newSkinString->c_str());
	(*ptrToStr) = (*newSkinString);
	std::string* ptrToStr2 = (std::string*) (((int) entity) + 2920);
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Str pointer again: %p, %i, %s\n", ptrToStr2, *((int*) ptrToStr2), ptrToStr2->c_str());
}

void bl_setuphooks_cppside() {
	bl_Gui_displayClientMessage = (void (*)(void*, const std::string&)) dlsym(RTLD_DEFAULT, "_ZN3Gui20displayClientMessageERKSs");
}

} //extern
