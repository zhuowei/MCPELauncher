#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"
#include "dobby_public.h"

typedef uint8_t cppbool;

#include "modscript_structs.h"

typedef void Minecraft;

typedef Player LocalPlayer;

typedef struct {
	float ticksPerSecond; //0
	int elapsedTicks; //4
	float renderPartialTicks; //8
	float timerSpeed; //12
	float elapsedPartialTicks; //16
	
} MCPETimer;

#define GAMEMODE_VTABLE_OFFSET_USE_ITEM_ON 13
#define GAMEMODE_VTABLE_OFFSET_ATTACK 19
#define GAMEMODE_VTABLE_OFFSET_TICK 9
#define GAMEMODE_VTABLE_OFFSET_INIT_PLAYER 15
#define ENTITY_VTABLE_OFFSET_GET_CARRIED_ITEM 118
#define MOB_VTABLE_OFFSET_GET_TEXTURE 88
#define ENTITY_VTABLE_OFFSET_GET_ENTITY_TYPE_ID 62
#define PLAYER_INVENTORY_OFFSET 3252
#define MINECRAFT_VTABLE_OFFSET_UPDATE 18
#define MINECRAFT_VTABLE_OFFSET_SET_LEVEL 27
//this is / 4 bytes already
#define MOB_HEALTH_OFFSET 80
#define ENTITY_RENDER_TYPE_OFFSET 59

#define LOG_TAG "BlockLauncher/ModScript"
#define FALSE 0
#define TRUE 1

#define AXIS_X 0
#define AXIS_Y 1
#define AXIS_Z 2

#define ITEMID 0
#define DAMAGE 1
#define AMOUNT 2

//This is true on the ARM/Dalvik/bionic platform

//(0x23021C - 0x2301e8) / 4

JavaVM* bl_JavaVM;

//TODO share headers
void bl_setuphooks_cppside();
void bl_changeEntitySkin(void* entity, const char* newSkin);
const char* bl_getCharArr(void* str);
void bl_attachLevelListener();

jclass bl_scriptmanager_class;

static void (*bl_GameMode_useItemOn_real)(void*, Player*, Level*, ItemInstance*, int, int, int, int, void*);
static void (*bl_Minecraft_setLevel_real)(Minecraft*, Level*, cppstr*, LocalPlayer*);
void (*bl_Minecraft_selectLevel_real)(Minecraft*, void*, void*, void*);
static void (*bl_Minecraft_leaveGame_real)(Minecraft*, int, int);
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
int (*bl_Level_getData) (Level*, int, int, int);
static void (*bl_Level_setNightMode)(Level*, int);
static void (*bl_Entity_setRot)(Entity*, float, float);
static void (*bl_GameMode_tick_real)(void*);
static Entity* (*bl_Level_getEntity)(Level*, int);
static void (*bl_GameMode_initPlayer_real)(void*, Player*);
static float (*bl_GameRenderer_getFov_real)(void*, float, int);
static void (*bl_NinecraftApp_onGraphicsReset)(Minecraft*);
static void* (*bl_Mob_getTexture)(Entity*);
static void (*bl_LocalPlayer_hurtTo)(Player*, int);
static void (*bl_Level_removeEntity)(Level*, Entity*);
static void (*bl_AgebleMob_setAge)(Entity*, int);
static void (*bl_GameMode_destroyBlock_real)(void*, int, int, int, int);
static Entity* (*bl_EntityFactory_CreateEntity)(int, Level*);
static void (*bl_Entity_spawnAtLocation)(void*, ItemInstance*, float);
static long (*bl_Level_getTime)(Level*);
static void (*bl_Level_setTime)(Level*, float);
static void* (*bl_Level_getLevelData)(Level*);
static void (*bl_LevelData_setSpawn)(void*, int, int, int);
static void (*bl_LevelData_setGameType)(void*, int);
static int (*bl_LevelData_getGameType)(void*);
static void (*bl_Entity_setOnFire)(Entity*, int);
void* (*bl_Level_getTileEntity)(Level*, int, int, int);
static void (*bl_ChestTileEntity_setItem)(void*, int, ItemInstance*);
static ItemInstance* (*bl_ChestTileEntity_getItem)(void*, int);
static int (*bl_FillingContainer_clearSlot)(void*, int);
static ItemInstance* (*bl_FillingContainer_getItem)(void*, int);
static void (*bl_Mob_die_real)(void*, Entity*);
static void (*bl_Minecraft_setIsCreativeMode)(Minecraft*, int);
static void (*bl_Level_ExtinguishFire)(Level*, int, int, int, int);
static ItemInstance* (*bl_Player_getArmor)(Player*, int);
static void  (*bl_Player_setArmor)(Player*, int, ItemInstance*);
static void (*bl_Inventory_clearInventoryWithDefault)(void*);
static void (*bl_Inventory_Inventory)(void*, Player*, cppbool);
static void (*bl_Inventory_delete1_Inventory)(void*);
void (*bl_ItemInstance_setId)(ItemInstance*, int);
int (*bl_ItemInstance_getId)(ItemInstance*);
static void (*bl_NinecraftApp_update_real)(Minecraft*);
static void (*bl_FillingContainer_replaceSlot)(void*, int, ItemInstance*);

Level* bl_level;
Minecraft* bl_minecraft;
void* bl_gamemode;
static Player* bl_localplayer;
static int bl_hasinit_script = 0;
int preventDefaultStatus = 0;
static float bl_newfov = -1.0f;

Entity* bl_removedEntity = NULL;

int bl_frameCallbackRequested = 0;

Entity* bl_getEntityWrapper(Level* level, int entityId) {
	if (bl_removedEntity != NULL && bl_removedEntity->entityId == entityId) {
		return bl_removedEntity;
	}
	return bl_Level_getEntity(level, entityId);
}

ItemInstance* bl_newItemInstance(int id, int count, int damage) {
	ItemInstance* instance = (ItemInstance*) malloc(sizeof(ItemInstance));
	instance->damage = damage;
	instance->count = count;
	bl_ItemInstance_setId(instance, id);
	return instance;
}

void bl_GameMode_useItemOn_hook(void* gamemode, Player* player, Level* level, ItemInstance* itemStack, int x, int y, int z, int side, void* vec3) {
	JNIEnv *env;
	bl_level = level;
	bl_localplayer = player;
	preventDefaultStatus = FALSE;
	int itemId = 0;
	int itemDamage = 0;
	if (itemStack != NULL) {
		itemId = bl_ItemInstance_getId(itemStack);
		itemDamage = itemStack->damage;
	}

	int blockId = bl_Level_getTile(level, x, y, z);
	int blockDamage = bl_Level_getData(level, x, y, z);

#ifdef EXTREME_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "use item on: JavaVM = %p\n", bl_JavaVM);
#endif

	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

#ifdef EXTREME_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "use item on: env = %p\n", env);
#endif

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "useItemOnCallback", "(IIIIIIII)V");

#ifdef EXTREME_LOGGING
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "use item on: mid = %i, class = %p\n", mid, bl_scriptmanager_class);
#endif

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, x, y, z, itemId, blockId, side, itemDamage, blockDamage);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

	if (!preventDefaultStatus) bl_GameMode_useItemOn_real(gamemode, player, level, itemStack, x, y, z, side, vec3);
}

void bl_Minecraft_setLevel_hook(Minecraft* minecraft, Level* level, cppstr* levelName, LocalPlayer* player) {
	JNIEnv *env;

	bl_localplayer = player;
	bl_minecraft = minecraft;
	bl_level = level;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = (*bl_JavaVM)->GetEnv(bl_JavaVM, (void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "setLevelCallback", "(ZZ)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, (int) (level != NULL), (jboolean) level->isRemote);

	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	}

	bl_Minecraft_setLevel_real(minecraft, level, levelName, player);

	//attach the listener
	bl_attachLevelListener();
}

void bl_Minecraft_selectLevel_hook(Minecraft* minecraft, void* wDir, void* wName, void* levelSettings) {
	JNIEnv *env;

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = (*bl_JavaVM)->GetEnv(bl_JavaVM, (void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);
	}

	// Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "selectLevelCallback", "(Ljava/lang/String;Ljava/lang/String;)V");

	
	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, (*env)->NewStringUTF(env, bl_getCharArr(wName)), (*env)->NewStringUTF(env, bl_getCharArr(wDir)));

	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	}

	bl_Minecraft_selectLevel_real(minecraft, wDir, wName, levelSettings);
}

void bl_Minecraft_leaveGame_hook(Minecraft* minecraft, int saveLevel, int thatotherboolean) {
	JNIEnv *env;
	bl_Minecraft_leaveGame_real(minecraft, saveLevel, thatotherboolean);

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = (*bl_JavaVM)->GetEnv(bl_JavaVM, (void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "leaveGameCallback", "(Z)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, saveLevel);

	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	}

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
	bl_gamemode = gamemode;
}

void bl_GameMode_destroyBlock_hook(void* gamemode, int x, int y, int z, int side){
	JNIEnv *env;
	preventDefaultStatus = FALSE;
	(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "destroyBlockCallback", "(IIII)V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, x, y, z, side);

	(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);

	if (!preventDefaultStatus) bl_GameMode_destroyBlock_real(gamemode, x, y, z, side);
}

void bl_Mob_die_hook(Entity* entity1, Entity* entity2) {
	JNIEnv *env;
	preventDefaultStatus = FALSE;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = (*bl_JavaVM)->GetEnv(bl_JavaVM, (void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "mobDieCallback", "(II)V");
	int victimId = entity1->entityId;
	int attackerId = -1;
	if (entity2 != NULL) {
		attackerId = entity2->entityId;
	}

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid, attackerId, victimId);

	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	}

	bl_Mob_die_real(entity1, entity2);
}

void bl_handleFrameCallback() {
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = (*bl_JavaVM)->GetEnv(bl_JavaVM, (void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->AttachCurrentThread(bl_JavaVM, &env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = (*env)->GetStaticMethodID(env, bl_scriptmanager_class, "frameCallback", "()V");

	(*env)->CallStaticVoidMethod(env, bl_scriptmanager_class, mid);

	if (attachStatus == JNI_EDETACHED) {
		(*bl_JavaVM)->DetachCurrentThread(bl_JavaVM);
	}
}

void bl_NinecraftApp_update_hook(Minecraft* minecraft) {
	bl_NinecraftApp_update_real(minecraft);
	if (bl_frameCallbackRequested) {
		bl_handleFrameCallback();
		bl_frameCallbackRequested = 0;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot, jint id, jint damage, jint amount) {
	if (bl_level == NULL) return;
	ItemInstance* instance = bl_newItemInstance(id, amount, damage);

	void* te = bl_Level_getTileEntity(bl_level, x, y, z);
	if (te == NULL) return;
	bl_ChestTileEntity_setItem(te, slot, instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	void* te = bl_Level_getTileEntity(bl_level, x, y, z);
	if (te == NULL) return -1;
	ItemInstance* instance = bl_ChestTileEntity_getItem(te, slot);
	if (instance == NULL) return 0;
	return bl_ItemInstance_getId(instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemDataChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	void* te = bl_Level_getTileEntity(bl_level, x, y, z);
	if (te == NULL) return -1;
	ItemInstance* instance = bl_ChestTileEntity_getItem(te, slot);
	if (instance == NULL) return 0;
	return instance->damage;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemCountChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	void* te = bl_Level_getTileEntity(bl_level, x, y, z);
	if (te == NULL) return -1;
	ItemInstance* instance = bl_ChestTileEntity_getItem(te, slot);
	if (instance == NULL) return 0;
	return instance->count;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTime
  (JNIEnv *env, jclass clazz, jlong time) {
	if (bl_level == NULL) return;
	bl_Level_setTime(bl_level, time);
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTime
  (JNIEnv *env, jclass clazz) {
	if (bl_level == NULL) return 0;
	return bl_Level_getTime(bl_level);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDropItem
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jfloat range, jint id, jint count, jint damage) {
	ItemInstance* instance = bl_newItemInstance(id, count, damage);

	Entity* entity = bl_EntityFactory_CreateEntity(64, bl_level);
	if (entity == NULL) {
		//WTF?
		return;
	}
	bl_Entity_setPos(entity, x, y, z);
	bl_Entity_spawnAtLocation(entity, instance, range);
	//TODO: WTF, MrARM: why spawn an entity, use its spawn at location to make it drop another entity,
	//and then never use the original?!
	//(Potential memory leak?)
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSpawn
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	if (bl_level == NULL) return;
	void* levelData = bl_Level_getLevelData(bl_level);
	bl_LevelData_setSpawn(levelData, x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetGameType
  (JNIEnv *env, jclass clazz, jint type) {
	if (bl_level == NULL) return;
	void* levelData = bl_Level_getLevelData(bl_level);
	bl_LevelData_setGameType(levelData, type);
	if (bl_localplayer == NULL) return;
	bl_Minecraft_setIsCreativeMode(bl_minecraft, type == 1);
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO fix this for 0.7.2
	//((char*) invPtr)[32] = type == 1;
	//bl_Inventory_clearInventoryWithDefault(invPtr);
	bl_Inventory_delete1_Inventory(invPtr);
	bl_Inventory_Inventory(invPtr, bl_localplayer, type == 1);
	//int dim = type == 1? 10: 0; //daylight cycle
	//bl_LevelData_setDimension(levelData, dim);
}


JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetGameType
  (JNIEnv *env, jclass clazz) {
	if (bl_level == NULL) return 0;
	void* levelData = bl_Level_getLevelData(bl_level);
	return bl_LevelData_getGameType(levelData);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDestroyBlock
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {

	if(bl_gamemode == NULL) return;

	bl_GameMode_destroyBlock_real(bl_gamemode, x, y, z, 2);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetOnFire
  (JNIEnv *env, jclass clazz, jint entityId, jint howLong) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setOnFire(entity, howLong);
}

float bl_GameRenderer_getFov_hook(void* gameRenderer, float datFloat, int datBoolean) {
	/*if (bl_newfov < 0)*/ return bl_GameRenderer_getFov_real(gameRenderer, datFloat, datBoolean);
	//return bl_newfov;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetCarriedItem
  (JNIEnv *env, jclass clazz, jint type) {
	if (bl_localplayer == NULL) return 0;
	ItemInstance* instance = bl_Player_getCarriedItem(bl_localplayer);
	if (instance == NULL) return 0;

	switch (type) {
		case ITEMID:
			return bl_ItemInstance_getId(instance);
		case DAMAGE:
			return instance->damage;
		case AMOUNT:
			return instance->count;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTile
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint id, jint damage) {
	bl_Level_setTileAndData(bl_level, x, y, z, id, damage, 3); //3 = full block update
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeHurtTo
  (JNIEnv *env, jclass clazz, jint to) {
	bl_LocalPlayer_hurtTo(bl_localplayer, to); //3 = full block update
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
	return 0;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetPosition
  (JNIEnv *env, jclass clazz, jint entityId, jfloat x, jfloat y, jfloat z) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setPos(entity, x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetVel
  (JNIEnv *env, jclass clazz, jint entityId, jfloat vel, jint axis) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
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
	Entity* rider = bl_getEntityWrapper(bl_level, riderId);
	Entity* mount = bl_getEntityWrapper(bl_level, mountId);
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
  (JNIEnv *env, jclass clazz, jint id, jint amount, jint damage) {
	if (bl_localplayer == NULL) return;
	ItemInstance* instance = bl_newItemInstance(id, amount, damage);
	//we grab the inventory instance from the player
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO fix this for 0.7.2
	bl_Inventory_add(invPtr, instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSpawnEntity
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jint type, jstring skinPath) {
	//TODO: spawn entities, not just mobs
	Entity* entity;
	if (type < 64) {
		entity = bl_MobFactory_createMob(type, bl_level);
	} else {
		entity = bl_EntityFactory_CreateEntity(type, bl_level);
	}

	if (entity == NULL) {
		//WTF?
		return -1;
	}
	bl_Entity_setPos(entity, x, y, z);
	bl_Level_addEntity(bl_level, entity);

	//skins
	if (skinPath != NULL && type < 64) {
		const char * skinUtfChars = (*env)->GetStringUTFChars(env, skinPath, NULL);
		bl_changeEntitySkin((void*) entity, skinUtfChars);
		(*env)->ReleaseStringUTFChars(env, skinPath, skinUtfChars);
	}

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

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetData
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	return bl_Level_getData(bl_level, x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetPositionRelative
  (JNIEnv *env, jclass clazz, jint entityId, jfloat deltax, jfloat deltay, jfloat deltaz) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//again, the iOS implement probably uses Entity::move, but too mainstream
	bl_Entity_setPos(entity, entity->x + deltax, entity->y + deltay, entity->z + deltaz);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetRot
  (JNIEnv *env, jclass clazz, jint entityId, jfloat yaw, jfloat pitch) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setRot(entity, yaw, pitch);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPitch
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0.0f;
	return entity->pitch;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetYaw
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0.0f;
	return entity->yaw;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCarriedItem
  (JNIEnv *env, jclass clazz, jint entityId, jint itemId, jint itemCount, jint itemDamage) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	void* vtableEntry = entity->vtable[ENTITY_VTABLE_OFFSET_GET_CARRIED_ITEM];
	ItemInstance* (*fn)(Entity*) = (ItemInstance* (*) (Entity*)) vtableEntry;
	ItemInstance* item = fn(entity);
	if (item == NULL) return;
	item->count = itemCount;
	item->damage = itemDamage;
	bl_ItemInstance_setId(item, itemId);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetFov
  (JNIEnv *env, jclass clazz, jfloat newfov) {
	bl_newfov = newfov;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeOnGraphicsReset
  (JNIEnv *env, jclass clazz) {
	//bl_NinecraftApp_onGraphicsReset(bl_minecraft);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetMobSkin
  (JNIEnv *env, jclass clazz, jint entityId, jstring skinPath) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//skins
	const char * skinUtfChars = (*env)->GetStringUTFChars(env, skinPath, NULL);
	bl_changeEntitySkin((void*) entity, skinUtfChars);
	(*env)->ReleaseStringUTFChars(env, skinPath, skinUtfChars);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetEntityLoc
  (JNIEnv *env, jclass clazz, jint entityId, jint axis) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	switch (axis) {
		case AXIS_X:
			return entity->x;
		case AXIS_Y:
			return entity->y;
		case AXIS_Z:
			return entity->z;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRemoveEntity
(JNIEnv *env, jclass clazz, jint entityId) {
Entity* entity = bl_getEntityWrapper(bl_level, entityId);
if (entity == NULL) return;
bl_Level_removeEntity(bl_level, entity);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetEntityTypeId
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	void* vtable = entity->vtable[ENTITY_VTABLE_OFFSET_GET_ENTITY_TYPE_ID];
	int (*fn)(Entity*) = (int (*) (Entity*)) vtable;
	return fn(entity);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetAnimalAge
  (JNIEnv *env, jclass clazz, jint entityId, jint age) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_AgebleMob_setAge(entity, age);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetAnimalAge
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	return ((int*) entity)[772];
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClearSlotInventory
  (JNIEnv *env, jclass clazz, jint slot) {
	if (bl_localplayer == NULL) return;
	//we grab the inventory instance from the player
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO fix this for 0.7.2
	bl_FillingContainer_clearSlot(invPtr, slot);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetSlotInventory
  (JNIEnv *env, jclass clazz, jint slot, jint type) {
	if (bl_localplayer == NULL) return 0;
	//we grab the inventory instance from the player
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO fix this for 0.7.2
	ItemInstance* instance = bl_FillingContainer_getItem(invPtr, slot);
	if (instance == NULL) return 0;
	switch (type) {
		case ITEMID:
			return bl_ItemInstance_getId(instance);
		case DAMAGE:
			return instance->damage;
		case AMOUNT:
			return instance->count;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetInventorySlot
  (JNIEnv *env, jclass clazz, jint slot, jint id, jint count, jint damage) {
	if (bl_localplayer == NULL) return 0;
	//we grab the inventory instance from the player
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO Merge this into a macro
	ItemInstance* itemStack = bl_newItemInstance(id, count, damage);
	if (itemStack == NULL) return;
	bl_FillingContainer_replaceSlot(invPtr, slot, itemStack);
	free(itemStack);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetGameSpeed
  (JNIEnv *env, jclass clazz, jfloat ticksPerSecond) {
	MCPETimer* timer = (MCPETimer*) (((int) bl_minecraft) + 3264);
	timer->ticksPerSecond = ticksPerSecond;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetSelectedSlotId
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return 0;
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET));
	if (invPtr == NULL) return 0;
	return ((int*) invPtr)[10];
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetMobHealth
  (JNIEnv *env, jclass clazz, jint entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	return ((int*) entity)[MOB_HEALTH_OFFSET];
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetMobHealth
  (JNIEnv *env, jclass clazz, jint entityId, jint halfhearts) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	((int*) entity)[MOB_HEALTH_OFFSET] = halfhearts;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetEntityRenderType
  (JNIEnv *env, jclass clazz, jint entityId, jint renderType) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	((int*) entity)[ENTITY_RENDER_TYPE_OFFSET] = renderType;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeExtinguishFire
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint side) {
	bl_Level_ExtinguishFire(bl_level, x, y, z, side);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetSlotArmor
  (JNIEnv *env, jclass clazz, jint slot, jint type) {
	if (bl_localplayer == NULL) return 0;
	//Geting the item
	ItemInstance* instance = bl_Player_getArmor(bl_localplayer, slot);
	if(instance == NULL) return 0;
	switch (type)
	{
		case ITEMID:
			return bl_ItemInstance_getId(instance);
		case DAMAGE:
			return instance->damage;
		case AMOUNT:
			return instance->count;
		default:
			return 0;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetArmorSlot
  (JNIEnv *env, jclass clazz, jint slot, jint id, jint damage) {
	if (bl_localplayer == NULL) return;
	//Geting the item
	ItemInstance* instance = bl_newItemInstance(id, 1, damage);
	bl_Player_setArmor(bl_localplayer, slot, instance);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRemoveItemBackground
  (JNIEnv *env, jclass clazz) {
	//void* ItemRenderer_renderGuiItem = dlsym(RTLD_DEFAULT, "_ZN12ItemRenderer13renderGuiItemEP4FontP8TexturesPK12ItemInstanceffffb");
	//int drawRedSquareInstrLoc = ((int) ItemRenderer_renderGuiItem & ~1) + (0x131b38 - 0x131aa8);
	//*((int*) drawRedSquareInstrLoc) = 0xbf00bf00; //NOP
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRequestFrameCallback
  (JNIEnv *env, jclass clazz) {
	bl_frameCallbackRequested = 1;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetupHooks
  (JNIEnv *env, jclass clazz, jint versionCode) {
	if (bl_hasinit_script) return;

	dlerror();

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
	minecraftVtable[MINECRAFT_VTABLE_OFFSET_SET_LEVEL] = (int) &bl_Minecraft_setLevel_hook;

	void* selectLevel = dlsym(RTLD_DEFAULT, "_ZN9Minecraft11selectLevelERKSsS1_RK13LevelSettings");
	mcpelauncher_hook(selectLevel, &bl_Minecraft_selectLevel_hook, (void**) &bl_Minecraft_selectLevel_real);

	void* destroyBlock = dlsym(RTLD_DEFAULT, "_ZN8GameMode12destroyBlockEiiii");
	mcpelauncher_hook(destroyBlock, &bl_GameMode_destroyBlock_hook, (void**) &bl_GameMode_destroyBlock_real);

	void* mobDie = dlsym(RTLD_DEFAULT, "_ZN3Mob3dieEP6Entity");
	mcpelauncher_hook(mobDie, &bl_Mob_die_hook, (void**) &bl_Mob_die_real);

	//get a callback when the level is exited
	void* leaveGame = dlsym(RTLD_DEFAULT, "_ZN9Minecraft9leaveGameEbb");
	mcpelauncher_hook(leaveGame, &bl_Minecraft_leaveGame_hook, (void**) &bl_Minecraft_leaveGame_real);

	void* getFov = dlsym(RTLD_DEFAULT, "_ZN12GameRenderer6getFovEfb");
	//mcpelauncher_hook(getFov, &bl_GameRenderer_getFov_hook, (void**) &bl_GameRenderer_getFov_real);

	//get the level set block method. In future versions this might link against libminecraftpe itself
	bl_Level_setTileAndData = dlsym(RTLD_DEFAULT, "_ZN5Level14setTileAndDataEiiiiii");
	if (bl_Level_setTileAndData == NULL) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to get setTileAndData: %s\n", dlerror());
	}

	bl_Level_getData = dlsym(RTLD_DEFAULT, "_ZN5Level7getDataEiii");

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
	//bl_NinecraftApp_onGraphicsReset = dlsym(RTLD_DEFAULT, "_ZN12NinecraftApp15onGraphicsResetEv");
	bl_Mob_getTexture = dlsym(RTLD_DEFAULT, "_ZN3Mob10getTextureEv");
	bl_LocalPlayer_hurtTo = dlsym(RTLD_DEFAULT, "_ZN11LocalPlayer6hurtToEi");
	bl_Level_removeEntity = dlsym(RTLD_DEFAULT, "_ZN5Level12removeEntityEP6Entity");
	bl_EntityFactory_CreateEntity = dlsym(RTLD_DEFAULT, "_ZN13EntityFactory12CreateEntityEiP5Level");
	bl_Entity_spawnAtLocation = dlsym(RTLD_DEFAULT, "_ZN6Entity15spawnAtLocationERK12ItemInstancef");
	bl_Level_setTime = dlsym(RTLD_DEFAULT, "_ZN5Level7setTimeEl");
	bl_Level_getTime = dlsym(RTLD_DEFAULT, "_ZN5Level7getTimeEv");
	bl_Level_getLevelData = dlsym(RTLD_DEFAULT, "_ZN5Level12getLevelDataEv");
	bl_LevelData_setSpawn = dlsym(RTLD_DEFAULT, "_ZN9LevelData8setSpawnEiii");
	bl_LevelData_setGameType = dlsym(RTLD_DEFAULT, "_ZN9LevelData11setGameTypeEi");
	bl_LevelData_getGameType = dlsym(RTLD_DEFAULT, "_ZNK9LevelData11getGameTypeEv");
	bl_Level_getTileEntity = dlsym(RTLD_DEFAULT, "_ZN5Level13getTileEntityEiii");
	bl_ChestTileEntity_setItem = dlsym(RTLD_DEFAULT, "_ZN15ChestTileEntity7setItemEiP12ItemInstance");
	bl_ChestTileEntity_getItem = dlsym(RTLD_DEFAULT, "_ZN15ChestTileEntity7getItemEi");
	bl_Entity_setOnFire = dlsym(RTLD_DEFAULT, "_ZN6Entity9setOnFireEi");
	bl_FillingContainer_clearSlot = dlsym(RTLD_DEFAULT, "_ZN16FillingContainer9clearSlotEi");
	bl_FillingContainer_getItem = dlsym(RTLD_DEFAULT, "_ZN16FillingContainer7getItemEi");
	bl_Level_ExtinguishFire = dlsym(RTLD_DEFAULT, "_ZN5Level14extinguishFireEiiii");
	bl_Player_getArmor = dlsym(RTLD_DEFAULT, "_ZN6Player8getArmorEi");
	bl_Player_setArmor = dlsym(RTLD_DEFAULT, "_ZN6Player8setArmorEiPK12ItemInstance");

	//replace the getTexture method for zombie pigmen
	int *pigZombieVtable = (int*) dlsym(RTLD_DEFAULT, "_ZTV9PigZombie");
	pigZombieVtable[MOB_VTABLE_OFFSET_GET_TEXTURE] = (int) bl_Mob_getTexture;

	bl_AgebleMob_setAge = dlsym(RTLD_DEFAULT, "_ZN9AgableMob6setAgeEi");
	bl_Minecraft_setIsCreativeMode = dlsym(RTLD_DEFAULT, "_ZN9Minecraft17setIsCreativeModeEb");
	bl_Inventory_clearInventoryWithDefault = dlsym(RTLD_DEFAULT, "_ZN9Inventory25clearInventoryWithDefaultEv");
	bl_Inventory_Inventory = dlsym(RTLD_DEFAULT, "_ZN9InventoryC2EP6Playerb");
	bl_Inventory_delete1_Inventory = dlsym(RTLD_DEFAULT, "_ZN9InventoryD1Ev");
	bl_ItemInstance_setId = dlsym(RTLD_DEFAULT, "_ZN12ItemInstance8_setItemEi"); //note the name change: consistent naming
	bl_ItemInstance_getId = dlsym(RTLD_DEFAULT, "_ZNK12ItemInstance5getIdEv");
	//replace the update method in Minecraft with our own
	minecraftVtable[MINECRAFT_VTABLE_OFFSET_UPDATE] = (int) &bl_NinecraftApp_update_hook;
	bl_NinecraftApp_update_real = dlsym(RTLD_DEFAULT, "_ZN12NinecraftApp6updateEv");

	soinfo2* mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);
	bl_MobFactory_createMob = (Entity* (*)(int, Level*)) dobby_dlsym(mcpelibhandle, "_ZN10MobFactory9CreateMobEiP5Level");

	bl_FillingContainer_replaceSlot = dlsym(mcpelibhandle, "_ZN16FillingContainer11replaceSlotEiP12ItemInstance");

	jclass clz = (*env)->FindClass(env, "net/zhuoweizhang/mcpelauncher/ScriptManager");

	bl_scriptmanager_class = (*env)->NewGlobalRef(env, clz);

	bl_setuphooks_cppside();

	bl_hasinit_script = 1;

	const char* myerror = dlerror();
	if (myerror != NULL) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error in ModPE script init: %s\n", myerror);
	}

}
