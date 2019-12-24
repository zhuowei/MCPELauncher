#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <elf.h>

#include "dobby_public.h"
#include "dl_internal.h"
#include "mcpelauncher.h"
#include "fmod_hdr.h"

#include "modscript_shared.h"
#include <stdlib.h>
#include <fcntl.h>

extern "C" {

const int kSupportedVersionCode = 941140105; // 1.14.1.5

jclass bl_scriptmanager_class;

void* bl_marauder_translation_function(void* input);

static int bl_hasinit_prepatch = 0;

//static void (*bl_Minecraft_leaveGame_real)(Minecraft*, int);
static void (*bl_Minecraft_stopGame_real)(Minecraft*, bool);
static ServerNetworkHandler* (*bl_Minecraft_getServerNetworkHandler)(Minecraft*);

#ifndef DLSYM_DEBUG
void* bl_fakeSyms_dlsym(const char* symbolName);
void* prepatch_dlsym(void* handle, const char* symbolName) {
	void* fakeSyms = bl_fakeSyms_dlsym(symbolName);
	if (fakeSyms) {
		return fakeSyms;
	}
	return dlsym(handle, symbolName);
}
#define dlsym prepatch_dlsym
#endif

//void bl_Minecraft_leaveGame_hook(Minecraft* minecraft, int thatotherboolean) {
void bl_Minecraft_stopGame_hook(Minecraft* minecraft, bool localServer) {
	JNIEnv *env;
	//bl_Minecraft_leaveGame_real(minecraft, thatotherboolean);
	bl_Minecraft_stopGame_real(minecraft, localServer);
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Leave game callback: %p %s %p", minecraft,
		localServer? "yes": "no", bl_Minecraft_getServerNetworkHandler(minecraft));
	// we only trigger on the client Minecraft instance
	if (bl_Minecraft_getServerNetworkHandler(minecraft)) {
		// we're a server
		return;
	}

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
#if 0 // 1.13
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
#endif
}

static bool bl_AppPlatform_supportsScripting_hook(void* self) {
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "scripting hook");
	return true;
}
static bool (*bl_ScriptEngine_isScriptingEnabled_real)();
static bool bl_ScriptEngine_isScriptingEnabled_hook() {
	bool realret = bl_ScriptEngine_isScriptingEnabled_real();
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "script engine hook: overriding %s with true", realret? "true" : "false");
	//v8Init();
	return true;
}
static bool (*bl_FeatureToggles_isEnabled_real)(void* toggles, int id);
static bool bl_FeatureToggles_isEnabled_hook(void* toggles, int id) {
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "feature enabled? %d", id);
	if (id == 9) return true;
	if (id == 7) return true;
	return bl_FeatureToggles_isEnabled_real(toggles, id);
}

static bool bl_hbui_Feature_isEnabled_hook(void* feature) {
	//__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "hbui feature enabled");
	return true;
}

static int bl_vtableIndexLocal(void* si, const char* vtablename, const char* name) {
	void* needle = dobby_dlsym(si, name);
	//Elf_Sym* vtableSym = dobby_elfsym(si, vtablename);
	void** vtable = (void**) dobby_dlsym(si, vtablename);
	for (unsigned int i = 0; i < 512 /*(vtableSym->st_size / sizeof(void*))*/; i++) {
		if (vtable[i] == needle) {
			return i;
		}

	}
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "cannot find vtable entry %s in %s", name, vtablename);
	return 0;
}

static void outputExtraLog() {
	int outputfd = open("/sdcard/bl_stdout_output.txt", O_WRONLY | O_CREAT | O_TRUNC, 0666);
	dup2(outputfd, 1);
	dup2(outputfd, 2);
}

static void setupMojangScripting(void* mcpelibhandle) {
	void** vtable = (void**)dlsym(mcpelibhandle, "_ZTV21AppPlatform_android23");
	if (!vtable) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't find appplatform vtable for scripting");
		return;
	}
	int supportsScriptingIndex = bl_vtableIndexLocal(mcpelibhandle, "_ZTV11AppPlatform",
		"_ZNK11AppPlatform17supportsScriptingEv");
	if (supportsScriptingIndex <= 0) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't find method for scripting");
		return;
	}
	vtable[supportsScriptingIndex] = (void*)&bl_AppPlatform_supportsScripting_hook;
	void* scriptingEngineOrig = dlsym(mcpelibhandle, "_ZN12ScriptEngine18isScriptingEnabledEv");
	bl_ScriptEngine_isScriptingEnabled_real = (bool (*)())scriptingEngineOrig;
	bl_patch_got((soinfo2*)mcpelibhandle, scriptingEngineOrig, (void*) &bl_ScriptEngine_isScriptingEnabled_hook);
	outputExtraLog();

	mcpelauncher_hook(dlsym(mcpelibhandle, "_ZNK14FeatureToggles9isEnabledE15FeatureOptionID"),
		(void*)&bl_FeatureToggles_isEnabled_hook,
		(void**)&bl_FeatureToggles_isEnabled_real);
	void* hbui = dlsym(mcpelibhandle, "_ZNK4hbui7Feature9isEnabledEv");
	bl_patch_got((soinfo2*)mcpelibhandle, (void*)hbui, (void*)&bl_hbui_Feature_isEnabled_hook);
}

/* FMOD */

FMOD_RESULT bl_FMOD_System_init_hook(FMOD::System* system, int maxchannels, FMOD_INITFLAGS flags, void *extradriverdata);

bool bl_patch_got(soinfo2* mcpelibhandle, void* original, void* newptr) {
	if (original == nullptr || newptr == nullptr) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Invalid bl_patch got! %p: %p", original, newptr);
		return false;
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
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "can't find the GOT");
		return false;
	}
	bool got_success = false;
	void** got_rw = (void**) bl_marauder_translation_function((void*) got);
	for (int i = 0; i < 0x40000; i++) {
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

#ifdef __arm__
void bl_fakeSyms_init(uintptr_t mcpe_base);
static void setupFakesym(uintptr_t mcpe_base, int versionCode) {
	if (versionCode != kSupportedVersionCode) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Wrong version: expected %d, got %d", kSupportedVersionCode, versionCode);
		return;
	}
	bl_fakeSyms_init(mcpe_base);
}
#else
// stub out fakeSyms
void* bl_fakeSyms_dlsym(const char* symbolName) {
}
#endif

extern void bl_prepatch_cside(void* mcpelibhandle, JNIEnv *env, jclass clazz,
	jboolean signalhandler, jobject activity, jboolean limitedPrepatch);
extern void bl_prepatch_fakeassets(soinfo2* mcpelibhandle);

void bl_setmcpelibhandle(void* _mcpelibhandle);
static void* hacked_dlopen(const char *filename, int flag);

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePrePatch
  (JNIEnv *env, jclass clazz, jboolean signalhandler, jobject activity, jboolean limitedPrepatch, jint versionCode) {
	if (bl_hasinit_prepatch) return;

	jclass clz = env->FindClass("net/zhuoweizhang/mcpelauncher/ScriptManager");

	bl_scriptmanager_class = (jclass) env->NewGlobalRef(clz);

	void* mcpelibhandle = hacked_dlopen("libminecraftpe.so", RTLD_LAZY);
#ifndef MCPELAUNCHER_LITE
	bl_setmcpelibhandle(mcpelibhandle);
#endif
#ifdef __arm__
	setupFakesym(((soinfo2*)mcpelibhandle)->base, versionCode);
#endif
	void* readAssetFile = (void*)dobby_dlsym(mcpelibhandle, "_ZN19AppPlatform_android13readAssetFileERKN4Core4PathE");
	void* readAssetFileToHook = (void*)dobby_dlsym(mcpelibhandle, "_ZN21AppPlatform_android2313readAssetFileERKN4Core4PathE");
	void* tempPtr;
	mcpelauncher_hook(readAssetFileToHook, readAssetFile, &tempPtr);

	setupIsModded(mcpelibhandle);
	setupMojangScripting(mcpelibhandle);

	//get a callback when the level is exited
	//void* leaveGame = dlsym(RTLD_DEFAULT, "_ZN15MinecraftClient9leaveGameEb");
	//mcpelauncher_hook(leaveGame, (void*) &bl_Minecraft_leaveGame_hook, (void**) &bl_Minecraft_leaveGame_real);
	void* stopGame = dlsym(mcpelibhandle, "_ZN9Minecraft14startLeaveGameEb");
	mcpelauncher_hook(stopGame, (void*) &bl_Minecraft_stopGame_hook, (void**) &bl_Minecraft_stopGame_real);
	bl_Minecraft_getServerNetworkHandler = (ServerNetworkHandler* (*)(Minecraft*))
		dlsym(mcpelibhandle, "_ZN9Minecraft23getServerNetworkHandlerEv");

	bl_prepatch_fmod((soinfo2*) mcpelibhandle);

#ifndef MCPELAUNCHER_LITE
	bl_prepatch_cside(mcpelibhandle, env, clazz, signalhandler, activity, limitedPrepatch);
#else
	bl_prepatch_fakeassets((soinfo2*) mcpelibhandle);
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

void bl_set_target_sdk_version(uint32_t version) {
	JNIEnv *env;
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "setTargetSdkVersion", "(I)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, version);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
}

// Android Nougat and above uses a randomly generated handle for dlopen. We need the real address. I'm so, so sorry for this.
static void* hacked_dlopen(const char *filename, int flag) {
	// first try a regular dlopen.
	void* handle = dlopen(filename, flag);
	if (!handle) return nullptr;
	// ok, is this Android Nougat or above?
	bool needsHax = (((uintptr_t)handle) & 1) != 0;
	if (!needsHax) return handle;
	// terrible hax time
	void* libchandle = dlopen("libc.so", RTLD_LAZY | RTLD_LOCAL);
	if (!libchandle) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Impossible: can't open: %s", dlerror());
		abort();
	}
	void (*android_set_application_target_sdk_version)(uint32_t) = (void (*)(uint32_t))
		dlsym(libchandle, "android_set_application_target_sdk_version");
	if (!android_set_application_target_sdk_version) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "We're on Android Q: can't get first needed: %s", dlerror());
		android_set_application_target_sdk_version = bl_set_target_sdk_version;
	}
	uint32_t (*android_get_application_target_sdk_version)() = (uint32_t (*)())
		dlsym(libchandle, "android_get_application_target_sdk_version");
	if (!android_get_application_target_sdk_version) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Impossible: can't get second needed: %s", dlerror());
		abort();
	}
	// grab our target SDK
	uint32_t target = android_get_application_target_sdk_version();
	// set it to 23 (Marshmallow MR1) temporarily
	android_set_application_target_sdk_version(23);

	if (android_get_application_target_sdk_version() != 23) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "set sdk failed");
		abort();
	}

	void* newhandle = dlopen(filename, flag);

	// restore as soon as possible
	android_set_application_target_sdk_version(target);

	if (android_get_application_target_sdk_version() != target) {
		abort();
	}

	// did we get it?

	if (!newhandle || (((uintptr_t)newhandle) & 1) != 0) {
		__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "Impossible: thwarted: %s", dlerror());
		abort();
	}
	return newhandle;
}

} // extern "C"
