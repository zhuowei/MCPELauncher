#include <jni.h>
#include <stdbool.h>
#include <dlfcn.h>
#include <android/log.h>
#include "dobby_public.h"
#include "mcpelauncher.h"

extern JavaVM* bl_JavaVM;

jclass bl_scriptmanager_class;

typedef void Minecraft;

void* bl_marauder_translation_function(void* input);

static int bl_hasinit_prepatch = 0;

static void (*bl_Minecraft_leaveGame_real)(Minecraft*, int);

void bl_Minecraft_leaveGame_hook(Minecraft* minecraft, int thatotherboolean) {
	JNIEnv *env;
	bl_Minecraft_leaveGame_real(minecraft, thatotherboolean);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Leave game callback");

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = (*bl_JavaVM)->GetEnv(bl_JavaVM, (void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "leaveGameCallback", "(Z)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, thatotherboolean);

	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	}

}

static void setupIsModded(void* mcpelibhandle) {
#ifdef __arm__
	uintptr_t isModdedAddr = ((uintptr_t) bl_marauder_translation_function(
		dobby_dlsym(mcpelibhandle, "_ZN9Minecraft8isModdedEv"))) & ~1;
	unsigned char* isModdedArray = (unsigned char*) isModdedAddr;
	isModdedArray[0] = 1;
#endif
}

extern void bl_prepatch_cside(void* mcpelibhandle, JNIEnv *env, jclass clazz,
	jboolean signalhandler, jobject activity, jboolean limitedPrepatch);

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePrePatch
  (JNIEnv *env, jclass clazz, jboolean signalhandler, jobject activity, jboolean limitedPrepatch) {
	if (bl_hasinit_prepatch) return;
	void* mcpelibhandle = dlopen("libminecraftpe.so", RTLD_LAZY);
	void* readAssetFile = (void*) dobby_dlsym(mcpelibhandle, "_ZN19AppPlatform_android13readAssetFileERKSs");
	void* readAssetFileToHook = (void*) dobby_dlsym(mcpelibhandle, "_ZN21AppPlatform_android2313readAssetFileERKSs");
	void* tempPtr;
	mcpelauncher_hook(readAssetFileToHook, readAssetFile, &tempPtr);

	setupIsModded(mcpelibhandle);

	jclass clz = (*env)->FindClass(env, "net/zhuoweizhang/mcpelauncher/ScriptManager");

	bl_scriptmanager_class = (*env)->NewGlobalRef(env, clz);
	//get a callback when the level is exited
	void* leaveGame = dlsym(RTLD_DEFAULT, "_ZN9Minecraft9leaveGameEb");
	if (!leaveGame) leaveGame = dlsym(RTLD_DEFAULT, "_ZN15MinecraftClient9leaveGameEb");
	mcpelauncher_hook(leaveGame, &bl_Minecraft_leaveGame_hook, (void**) &bl_Minecraft_leaveGame_real);

#ifndef MCPELAUNCHER_LITE
	bl_prepatch_cside(mcpelibhandle, env, clazz, signalhandler, activity, limitedPrepatch);
#endif
	bl_hasinit_prepatch = 1;
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
#ifdef MCPELAUNCHER_LITE
JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetExitEnabled
  (JNIEnv* env, jclass clazz, jboolean p) {
}
#endif
