#include <jni.h>
#include <stdbool.h>
#include <dlfcn.h>
#include "dobby_public.h"
#include "mcpelauncher.h"

void* bl_marauder_translation_function(void* input);

static int bl_hasinit_prepatch = 0;

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

#ifndef MCPELAUNCHER_LITE
	bl_prepatch_cside(mcpelibhandle, env, clazz, signalhandler, activity, limitedPrepatch);
#endif
	bl_hasinit_prepatch = 1;
}
