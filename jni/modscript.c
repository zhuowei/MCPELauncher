#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>

#include "mcpelauncher.h"
#include "modscript.h"

typedef uint8_t cppbool;

typedef struct {
  char name[128];
  const void* phdr;
  int phnum;
  unsigned entry;
  unsigned base;
  unsigned size;
} soinfo2;

typedef struct {
	void** vtable; //0
	char filler[13];
	cppbool isRemote;
} Level;
typedef struct {
	void** vtable; //0
	float x; //4
	float y; //8
	float z; //12
	char filler[12];//16
	int entityId; //28
	char filler2[20];//32
	float motionX; //52
	float motionY; //56
	float motionZ; //60
	float yaw; //64
	float pitch; //68
	float prevYaw; //72
	float prevPitch; //76
} Entity;
typedef Entity Player;
typedef struct {
	int count;
	int id;
	int damage;
} ItemInstance;

typedef void Minecraft;

typedef struct {
	//I have no idea what this struct looks like. TODO
	//void** vtable; //0
	//char filler[16];//4
	//char* pointer;//20
} cppstr;

typedef Player LocalPlayer;

#define GAMEMODE_VTABLE_OFFSET_USE_ITEM_ON 13
#define GAMEMODE_VTABLE_OFFSET_ATTACK 19
#define GAMEMODE_VTABLE_OFFSET_TICK 9
#define GAMEMODE_VTABLE_OFFSET_INIT_PLAYER 15
#define ENTITY_VTABLE_OFFSET_GET_CARRIED_ITEM 114
#define LOG_TAG "BlockLauncher/ModScript"
#define FALSE 0
#define TRUE 1

#define AXIS_X 0
#define AXIS_Y 1
#define AXIS_Z 2

//This is true on the ARM/Dalvik/bionic platform

//(0x23021C - 0x2301e8) / 4

JavaVM* bl_JavaVM;

jclass bl_scriptmanager_class;

static void (*bl_GameMode_useItemOn_real)(void*, Player*, Level*, ItemInstance*, int, int, int, int, void*);
static void (*bl_Minecraft_setLevel_real)(Minecraft*, Level*, cppstr*, LocalPlayer*);
static void (*bl_Minecraft_leaveGame_real)(Minecraft*, int);
static void (*bl_Level_setTileAndData) (Level*, int, int, int, int, int, int);
static void (*bl_GameMode_attack_real)(void*, Player*, Entity*);
static ItemInstance* (*bl_Player_getCarriedItem)(Player*);
static void (*bl_Player_ride)(Player*, Entity*);
static void (*bl_Entity_setPos)(Entity*, float, float, float);
static void (*bl_Level_explode)(Level*, Entity*, float, float, float, float, int);
static int (*bl_Inventory_add)(void*, ItemInstance*);
static void (*bl_Level_addEntity)(Level*, Entity*);
static Entity* (*bl_MobFactory_createMob)(int, Level*);
static int (*bl_Level_getTile)(Level*, int, int, int);
static void (*bl_Level_setNightMode)(Level*, int);
static void (*bl_Entity_setRot)(Entity*, float, float);
static void (*bl_GameMode_tick_real)(void*);
static Entity* (*bl_Level_getEntity)(Level*, int);
static void (*bl_GameMode_initPlayer_real)(void*, Player*);
static void (*bl_ChatScreen_sendChatMessage_real)(void*);
static float (*bl_GameRenderer_getFov_real)(void*, float, int);

static Level* bl_level;
static Minecraft* bl_minecraft;
static Player* bl_localplayer;
static int bl_hasinit_script = 0;
static int preventDefaultStatus = 0;
static float bl_newfov = -1.0f;

void bl_GameMode_useItemOn_hook(void* gamemode, Player* player, Level* level, ItemInstance* itemStack, int x, int y, int z, int side, void* vec3) {
	JNIEnv *env;
	bl_level = level;
	bl_localplayer = player;
	preventDefaultStatus = FALSE;
	int itemId = 0;
	if (itemStack != NULL) itemId = itemStack->id;

	int blockId = bl_Level_getTile(level, x, y, z);

#ifdef EXTREME_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "use item on: JavaVM = %p\n", bl_JavaVM);
#endif

	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

#ifdef EXTREME_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "use item on: env = %p\n", env);
#endif

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "useItemOnCallback", "(IIIIII)V");

#ifdef EXTREME_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "use item on: mid = %i, class = %p\n", mid, bl_scriptmanager_class);
#endif

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, x, y, z, itemId, blockId, side); //TODO block ID

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
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "setLevelCallback", "(ZZ)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, (int) (level != NULL), (jboolean) level->isRemote);

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
	bl_localplayer = player;
	preventDefaultStatus = FALSE;
	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "attackCallback", "(II)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, player->entityId, entity->entityId);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

	if (!preventDefaultStatus) bl_GameMode_attack_real(gamemode, player, entity);
}

void bl_GameMode_tick_hook(void* gamemode) {
	JNIEnv *env;
	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "tickCallback", "()V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	bl_GameMode_tick_real(gamemode);
}

void bl_GameMode_initPlayer_hook(void* gamemode, Player* player) {
	bl_GameMode_initPlayer_real(gamemode, player);
	bl_localplayer = player;
}

void bl_ChatScreen_sendChatMessage_hook(void* chatScreen) {
	int chatMessagePtr = *((int*) ((int) chatScreen + 84));
	//__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Chat message ptr: %#x\n", chatMessagePtr);
	char* chatMessageChars = (char*) chatMessagePtr;
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Chat message: %p\n", chatMessageChars);
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Chat message: %s\n", chatMessageChars);
	/*int chatMessagePtr = *(*((int**) ((int) chatScreen + 84))) - 12; 
	char* chatMessageChars = *((char**) chatMessagePtr);*/

	JNIEnv *env;
	preventDefaultStatus = FALSE;
	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	jstring chatMessageJString = (*env)->NewStringUTF(env, chatMessageChars);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "chatCallback", "(Ljava/lang/String;)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, chatMessageJString);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	if (!preventDefaultStatus) bl_ChatScreen_sendChatMessage_real(chatScreen);
}

float bl_GameRenderer_getFov_hook(void* gameRenderer, float datFloat, int datBoolean) {
	/*if (bl_newfov < 0)*/ return bl_GameRenderer_getFov_real(gameRenderer, datFloat, datBoolean);
	//return bl_newfov;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetCarriedItem
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return 0;
	ItemInstance* instance = bl_Player_getCarriedItem(bl_localplayer);
	if (instance == NULL) return 0;
	return instance->id;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTile
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint id, jint damage) {
	bl_Level_setTileAndData(bl_level, x, y, z, id, damage, 3); //3 = full block update
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePreventDefault
  (JNIEnv *env, jclass clazz) {
	preventDefaultStatus = TRUE;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPlayerEnt
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return 0;
	return bl_localplayer->entityId;
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetLevel
  (JNIEnv *env, jclass clazz) {
	return (jlong) (intptr_t) bl_level;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPlayerLoc
  (JNIEnv *env, jclass clazz, jint axis) {
	if (bl_localplayer == NULL) return 0;
	switch (axis) {
		case AXIS_X:
			return bl_localplayer->x;
		case AXIS_Y:
			return bl_localplayer->y;
		case AXIS_Z:
			return bl_localplayer->z;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetPosition
  (JNIEnv *env, jclass clazz, jint entityId, jfloat x, jfloat y, jfloat z) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setPos(entity, x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetVel
  (JNIEnv *env, jclass clazz, jint entityId, jfloat vel, jint axis) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return;
	//the iOS version probably uses Entity::lerpMotion; that's too mainstream so we set velocity directly
	switch (axis) {
		case AXIS_X:
			entity->motionX = vel;
			break;
		case AXIS_Y:
			entity->motionY = vel;
			break;
		case AXIS_Z:
			entity->motionZ = vel;
			break;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRideAnimal
  (JNIEnv *env, jclass clazz, jint riderId, jint mountId) {
	//use vtable so the rider doesn't have to be a player (useful?)
	Entity* rider = bl_Level_getEntity(bl_level, riderId);
	Entity* mount = bl_Level_getEntity(bl_level, mountId);
	if (rider == NULL) return;
	void* vtable = rider->vtable[19];
	void (*fn)(Entity*, Entity*) = (void (*) (Entity*, Entity*)) vtable;
	fn(rider, mount);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeExplode
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jfloat power) {
	bl_Level_explode(bl_level, NULL, x, y, z, power, FALSE);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemInventory
  (JNIEnv *env, jclass clazz, jint id, jint amount) {
	if (bl_localplayer == NULL) return;
	ItemInstance* instance = (ItemInstance*) malloc(sizeof(ItemInstance));
	instance->id = id;
	instance->damage = 0;
	instance->count = amount;
	//we grab the inventory instance from the player
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + 3120)); //TODO fix this for 0.7.2
	bl_Inventory_add(invPtr, instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSpawnEntity
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jint type) {
	//TODO: spawn entities, not just mobs
	Entity* entity = bl_MobFactory_createMob(type, bl_level);
	if (entity == NULL) {
		//WTF?
		return -1;
	}
	bl_Entity_setPos(entity, x, y, z);
	bl_Level_addEntity(bl_level, entity);
	return entity->entityId;
	
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetNightMode
  (JNIEnv *env, jclass clazz, jboolean nightMode) {
	bl_Level_setNightMode(bl_level, nightMode);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTile
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	return bl_Level_getTile(bl_level, x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetPositionRelative
  (JNIEnv *env, jclass clazz, jint entityId, jfloat deltax, jfloat deltay, jfloat deltaz) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return;
	//again, the iOS implement probably uses Entity::move, but too mainstream
	bl_Entity_setPos(entity, entity->x + deltax, entity->y + deltay, entity->z + deltaz);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetRot
  (JNIEnv *env, jclass clazz, jint entityId, jfloat yaw, jfloat pitch) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setRot(entity, yaw, pitch);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPitch
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return 0.0f;
	return entity->pitch;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetYaw
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return 0.0f;
	return entity->yaw;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCarriedItem
  (JNIEnv *env, jclass clazz, jint entityId, jint itemId, jint itemCount, jint itemDamage) {
	Entity* entity = bl_Level_getEntity(bl_level, entityId);
	if (entity == NULL) return;
	void* vtableEntry = entity->vtable[ENTITY_VTABLE_OFFSET_GET_CARRIED_ITEM];
	ItemInstance* (*fn)(Entity*) = (ItemInstance* (*) (Entity*)) vtableEntry;
	ItemInstance* item = fn(entity);
	if (item == NULL) return;
	item->count = itemCount;
	item->id = itemId;
	item->damage = itemDamage;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetFov
  (JNIEnv *env, jclass clazz, jfloat newfov) {
	bl_newfov = newfov;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetupHooks
  (JNIEnv *env, jclass clazz, jint versionCode) {
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
	bl_GameMode_tick_real = dlsym(RTLD_DEFAULT, "_ZN8GameMode4tickEv");
	creativeVtable[GAMEMODE_VTABLE_OFFSET_TICK] = (int) &bl_GameMode_tick_hook;
	survivalVtable[GAMEMODE_VTABLE_OFFSET_TICK] = (int) &bl_GameMode_tick_hook;

	bl_GameMode_initPlayer_real = dlsym(RTLD_DEFAULT, "_ZN8GameMode10initPlayerEP6Player");
	creativeVtable[GAMEMODE_VTABLE_OFFSET_INIT_PLAYER] = (int) &bl_GameMode_initPlayer_hook;
	survivalVtable[GAMEMODE_VTABLE_OFFSET_INIT_PLAYER] = (int) &bl_GameMode_initPlayer_hook;

	//edit the vtable of NinecraftApp to get a callback when levels are switched
	bl_Minecraft_setLevel_real = dlsym(RTLD_DEFAULT, "_ZN9Minecraft8setLevelEP5LevelRKSsP11LocalPlayer");
	int *minecraftVtable = (int*) dlsym(RTLD_DEFAULT, "_ZTV12NinecraftApp");
	minecraftVtable[20] = (int) &bl_Minecraft_setLevel_hook;
	//get a callback when the level is exited
	void* leaveGame = dlsym(RTLD_DEFAULT, "_ZN9Minecraft9leaveGameEb");
	mcpelauncher_hook(leaveGame, &bl_Minecraft_leaveGame_hook, (void**) &bl_Minecraft_leaveGame_real);

	void* sendChatMessage = dlsym(RTLD_DEFAULT, "_ZN10ChatScreen15sendChatMessageEv");
	mcpelauncher_hook(sendChatMessage, &bl_ChatScreen_sendChatMessage_hook, (void**) &bl_ChatScreen_sendChatMessage_real);

	void* getFov = dlsym(RTLD_DEFAULT, "_ZN12GameRenderer6getFovEfb");
	//mcpelauncher_hook(getFov, &bl_GameRenderer_getFov_hook, (void**) &bl_GameRenderer_getFov_real);

	//get the level set block method. In future versions this might link against libminecraftpe itself
	bl_Level_setTileAndData = dlsym(RTLD_DEFAULT, "_ZN5Level14setTileAndDataEiiiiii");
	if (bl_Level_setTileAndData == NULL) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to get setTileAndData: %s\n", dlerror());
	}

	bl_Player_getCarriedItem = dlsym(RTLD_DEFAULT, "_ZN6Player14getCarriedItemEv");
	bl_Player_ride = dlsym(RTLD_DEFAULT, "_ZN6Player4rideEP6Entity");
	bl_Entity_setPos = dlsym(RTLD_DEFAULT, "_ZN6Entity6setPosEfff");
	bl_Level_explode = dlsym(RTLD_DEFAULT, "_ZN5Level7explodeEP6Entityffffb");
	bl_Inventory_add = dlsym(RTLD_DEFAULT, "_ZN9Inventory3addEP12ItemInstance");
	//bl_MobFactory_getStaticTestMob = dlsym(RTLD_DEFAULT, "_ZN10MobFactory16getStaticTestMobEiP5Level");
	bl_Level_addEntity = dlsym(RTLD_DEFAULT, "_ZN5Level9addEntityEP6Entity");
	bl_Level_getTile = dlsym(RTLD_DEFAULT, "_ZN5Level7getTileEiii");
	bl_Level_setNightMode = dlsym(RTLD_DEFAULT, "_ZN5Level12setNightModeEb");
	bl_Entity_setRot = dlsym(RTLD_DEFAULT, "_ZN6Entity6setRotEff");
	bl_Level_getEntity = dlsym(RTLD_DEFAULT, "_ZN5Level9getEntityEi");

	soinfo2* mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);
	int createMobOffset = 0xe3fe4;
	if (versionCode == 0x30007010) {
		createMobOffset = 0xe130a;
	} else if (versionCode == 0x40007010) {
		createMobOffset = 0xe1322;
	} else if (versionCode == 0x30007020) {
		createMobOffset = 0xee6e6;
	}
	bl_MobFactory_createMob = (Entity* (*)(int, Level*)) (mcpelibhandle->base + createMobOffset + 1);

	jclass clz = (*env)->FindClass(env, "net/zhuoweizhang/mcpelauncher/ScriptManager");

	bl_scriptmanager_class = (*env)->NewGlobalRef(env, clz);

	bl_hasinit_script = 1;

}
