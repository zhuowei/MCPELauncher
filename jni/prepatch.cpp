#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <elf.h>

#include "dobby_public.h"
#include "dl_internal.h"
#include "mcpelauncher.h"
#include "fmod_hdr.h"

#include "modscript_shared.h"

extern "C" {

jclass bl_scriptmanager_class;

void* bl_marauder_translation_function(void* input);

static int bl_hasinit_prepatch = 0;

//static void (*bl_Minecraft_leaveGame_real)(Minecraft*, int);
static void (*bl_Minecraft_stopGame_real)(Minecraft*);

//void bl_Minecraft_leaveGame_hook(Minecraft* minecraft, int thatotherboolean) {
void bl_Minecraft_stopGame_hook(Minecraft* minecraft) {
	JNIEnv *env;
	//bl_Minecraft_leaveGame_real(minecraft, thatotherboolean);
	bl_Minecraft_stopGame_real(minecraft);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Leave game callback");

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "leaveGameCallback", "(Z)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, true /*thatotherboolean*/);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

}

static void setupIsModded(void* mcpelibhandle) {
#ifdef __arm__
	uintptr_t isModdedAddr = ((uintptr_t) bl_marauder_translation_function(
		dobby_dlsym(mcpelibhandle, "_ZN9Minecraft8isModdedEv"))) & ~1;
	unsigned char* isModdedArray = (unsigned char*) isModdedAddr;
	isModdedArray[0] = 1;
#endif
#ifdef __i386
	uintptr_t isModdedAddr = ((uintptr_t) bl_marauder_translation_function(
		dobby_dlsym(mcpelibhandle, "_ZN9Minecraft8isModdedEv")));
	unsigned char* isModdedArray = (unsigned char*) isModdedAddr;
	isModdedArray[6] = 0xb0;
	isModdedArray[7] = 0x01;
#endif
}

/* FMOD */

FMOD_RESULT bl_FMOD_System_init_hook(FMOD::System* system, int maxchannels, FMOD_INITFLAGS flags, void *extradriverdata);

bool bl_patch_got(soinfo2* mcpelibhandle, void* original, void* newptr) {
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
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't find the GOT");
		return false;
	}
	bool got_success = false;
	void** got_rw = (void**) bl_marauder_translation_function((void*) got);
	for (int i = 0; i < 0x10000; i++) {
		if (got[i] == original) {
			got_rw[i] = newptr;
			got_success = true;
			break;
		}
	}
	if (!got_success) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't find pointer in GOT");
		return false;
	}
	return true;
}

static void bl_prepatch_fmod(soinfo2* mcpelibhandle) {
	// another got edit
	void* originalAddress = (void*) &FMOD::System::init;
	bl_patch_got(mcpelibhandle, originalAddress, (void*) &bl_FMOD_System_init_hook);
}


/* end FMOD */

extern void bl_prepatch_cside(void* mcpelibhandle, JNIEnv *env, jclass clazz,
	jboolean signalhandler, jobject activity, jboolean limitedPrepatch);

void bl_setmcpelibhandle(void* _mcpelibhandle);

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePrePatch
  (JNIEnv *env, jclass clazz, jboolean signalhandler, jobject activity, jboolean limitedPrepatch) {
	if (bl_hasinit_prepatch) return;
	void* mcpelibhandle = dlopen("libminecraftpe.so", RTLD_LAZY);
#ifndef MCPELAUNCHER_LITE
	bl_setmcpelibhandle(mcpelibhandle);
#endif
	void* readAssetFile = (void*) dobby_dlsym(mcpelibhandle, "_ZN19AppPlatform_android13readAssetFileERKSs");
	void* readAssetFileToHook = (void*) dobby_dlsym(mcpelibhandle, "_ZN21AppPlatform_android2313readAssetFileERKSs");
	void* tempPtr;
	mcpelauncher_hook(readAssetFileToHook, readAssetFile, &tempPtr);

	setupIsModded(mcpelibhandle);

	jclass clz = env->FindClass("net/zhuoweizhang/mcpelauncher/ScriptManager");

	bl_scriptmanager_class = (jclass) env->NewGlobalRef(clz);
	//get a callback when the level is exited
	//void* leaveGame = dlsym(RTLD_DEFAULT, "_ZN15MinecraftClient9leaveGameEb");
	//mcpelauncher_hook(leaveGame, (void*) &bl_Minecraft_leaveGame_hook, (void**) &bl_Minecraft_leaveGame_real);
	void* stopGame = dlsym(mcpelibhandle, "_ZN9Minecraft8stopGameEv");
	//mcpelauncher_hook(stopGame, (void*) &bl_Minecraft_stopGame_hook, (void**) &bl_Minecraft_stopGame_real);
	bl_patch_got((soinfo2*)mcpelibhandle, stopGame, (void*)bl_Minecraft_stopGame_hook);
	bl_Minecraft_stopGame_real = (void (*)(Minecraft*)) stopGame;

	bl_prepatch_fmod((soinfo2*) mcpelibhandle);

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

} // extern "C"
