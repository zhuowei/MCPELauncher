#include <dlfcn.h>
#include <stdio.h>
#include <android/log.h>
#include <jni.h>

#include "mcpelauncher.h"
#include "modscript.h"

typedef void Level;
typedef void Entity;
typedef Entity Player;
typedef struct {
	int count;
	int id;
	int damage;
} ItemInstance;

typedef void Minecraft;

typedef void cppstr;

typedef Player LocalPlayer;

#define GAMEMODE_VTABLE_OFFSET_USE_ITEM_ON 13
#define GAMEMODE_VTABLE_OFFSET_ATTACK 19
#define LOG_TAG "BlockLauncher/ModScript"
#define FALSE 0
#define TRUE 1
//This is true on the ARM/Dalvik/bionic platform

//(0x23021C - 0x2301e8) / 4

JavaVM* bl_JavaVM;

jclass bl_scriptmanager_class;

static void (*bl_GameMode_useItemOn_real)(void*, Player*, Level*, ItemInstance*, int, int, int, int, void*);
static void (*bl_Minecraft_setLevel_real)(Minecraft*, Level*, cppstr*, LocalPlayer*);
static void (*bl_Minecraft_leaveGame_real)(Minecraft*, int);
static void (*bl_Level_setTileAndData) (Level*, int, int, int, int, int, int);
static void (*bl_GameMode_attack_real)(void*, Player*, Entity*);
static int (*bl_Level_getTile_real)(int, int, int)
static ItemInstance* (*bl_Player_getCarriedItem)(Player*);

static Level* bl_level;
static Minecraft* bl_minecraft;
static LocalPlayer* bl_localplayer;
static int bl_hasinit_script = 0;
static int preventDefaultStatus = 0;

void bl_GameMode_useItemOn_hook(void* gamemode, Player* player, Level* level, ItemInstance* itemStack, int x, int y, int z, int side, void* vec3) {
	JNIEnv *env;
	bl_level = level;
	bl_localplayer = (LocalPlayer*) player;
	preventDefaultStatus = FALSE;

	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "useItemOnCallback", "(IIIII)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, x, y, z, itemStack->id, 0); //TODO block ID

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

	if (!preventDefaultStatus) bl_GameMode_useItemOn_real(gamemode, player, level, itemStack, x, y, z, side, vec3);
}

void bl_Minecraft_setLevel_hook(Minecraft* minecraft, Level* level, cppstr* levelName, LocalPlayer* player) {
	JNIEnv *env;

	bl_localplayer = player;
	bl_minecraft = minecraft;
	bl_level = level;
	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "setLevelCallback", "(Z)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, (int) (level != NULL));

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

	bl_Minecraft_setLevel_real(minecraft, level, levelName, player);
}

void bl_Minecraft_leaveGame_hook(Minecraft* minecraft, int thatboolean) {
	JNIEnv *env;
	bl_Minecraft_leaveGame_real(minecraft, thatboolean);

	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "leaveGameCallback", "(Z)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, thatboolean);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

}

void bl_GameMode_attack_hook(void* gamemode, Player* player, Entity* entity) {
	JNIEnv *env;
	bl_localplayer = (LocalPlayer*) player;
	preventDefaultStatus = FALSE;
	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "attackCallback", "(JJ)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, (jlong) player, (jlong) entity);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

	if (!preventDefaultStatus) bl_GameMode_attack_real(gamemode, player, entity);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetCarriedItem
  (JNIEnv *env, jclass clazz) {
	ItemInstance* instance = bl_Player_getCarriedItem(bl_localplayer);
	if (instance == NULL) return 0;
	return instance->id;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTile
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint id, jint damage) {
	bl_Level_setTileAndData(bl_level, x, y, z, id, damage, 3); //3 = full block update
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTile
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	return bl_Level_getTile_real(x, y, z)
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePreventDefault
  (JNIEnv *env, jclass clazz) {
	preventDefaultStatus = TRUE;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetupHooks
  (JNIEnv *env, jclass clazz) {
	if (bl_hasinit_script) return;
	//edit the vtables of the GameMode implementations
	bl_GameMode_useItemOn_real = dlsym(RTLD_DEFAULT, "_ZN8GameMode9useItemOnEP6PlayerP5LevelP12ItemInstanceiiiiRK4Vec3");
	int *creativeVtable = (int*) dlsym(RTLD_DEFAULT, "_ZTV12CreativeMode");
	creativeVtable[GAMEMODE_VTABLE_OFFSET_USE_ITEM_ON] = (int) &bl_GameMode_useItemOn_hook;
	int *survivalVtable = (int*) dlsym(RTLD_DEFAULT, "_ZTV12SurvivalMode");
	survivalVtable[GAMEMODE_VTABLE_OFFSET_USE_ITEM_ON] = (int) &bl_GameMode_useItemOn_hook;

	bl_GameMode_attack_real = dlsym(RTLD_DEFAULT, "_ZN8GameMode6attackEP6PlayerP6Entity");
	creativeVtable[GAMEMODE_VTABLE_OFFSET_ATTACK] = (int) &bl_GameMode_attack_hook;
	survivalVtable[GAMEMODE_VTABLE_OFFSET_ATTACK] = (int) &bl_GameMode_attack_hook;

	//edit the vtable of NinecraftApp to get a callback when levels are switched
	bl_Minecraft_setLevel_real = dlsym(RTLD_DEFAULT, "_ZN9Minecraft8setLevelEP5LevelRKSsP11LocalPlayer");
	int *minecraftVtable = (int*) dlsym(RTLD_DEFAULT, "_ZTV12NinecraftApp");
	minecraftVtable[20] = (int) &bl_Minecraft_setLevel_hook;
	//get a callback when the level is exited
	void* leaveGame = dlsym(RTLD_DEFAULT, "_ZN9Minecraft9leaveGameEb");
	mcpelauncher_hook(leaveGame, &bl_Minecraft_leaveGame_hook, (void**) &bl_Minecraft_leaveGame_real);
	//get the level set block method. In future versions this might link against libminecraftpe itself
	bl_Level_setTileAndData = dlsym(RTLD_DEFAULT, "_ZN5Level14setTileAndDataEiiiiii");
	if (bl_Level_setTileAndData == NULL) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to get setTileAndData: %s\n", dlerror());
	}

	bl_Level_getTile_real = dlsym(RTLD_DEFAULT, "_ZN5Level7getTileEiii");
	if{bl_Level_getTile_real == NULL}
	{
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to get getTile: %s\n", dlerror());
	}

	bl_Player_getCarriedItem = dlsym(RTLD_DEFAULT, "_ZN6Player14getCarriedItemEv");

	jclass clz = (*env)->FindClass(env, "net/zhuoweizhang/mcpelauncher/ScriptManager");

	bl_scriptmanager_class = (*env)->NewGlobalRef(env, clz);

	bl_hasinit_script = 1;

}
