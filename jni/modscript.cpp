#include <dlfcn.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <android/log.h>
#include <jni.h>
#include <time.h>
#include <memory>

#include "dl_internal.h"
#include "mcpelauncher.h"
#include "modscript.h"
#include "dobby_public.h"

#define DLSYM_DEBUG

typedef bool cppbool;

#include "modscript_structs.h"
#include "mcpe/inventory.h"
#include "mcpe/gamemode.h"

typedef Player LocalPlayer;

class Timer {
public:
	float ticksPerSecond; //0
	int elapsedTicks; //4
	float renderPartialTicks; //8
	float timerSpeed; //12
	float elapsedPartialTicks; //16
};

typedef struct {
	void* ptr;
} unique_ptr;

// from Player::getSelectedItem
#ifdef __i386
#define PLAYER_INVENTORY_OFFSET 3468
#else
#define PLAYER_INVENTORY_OFFSET 3480
#endif
/*
#define MINECRAFT_VTABLE_OFFSET_UPDATE 21
#define MINECRAFT_VTABLE_OFFSET_SET_LEVEL 30
*/
#ifdef __i386
#define GAMERENDERER_GETFOV_SIZE 0x1d3
#else
#define GAMERENDERER_GETFOV_SIZE 0x13c
#endif

const unsigned int kGameMode_progress_offset = 36;

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

extern "C" {

JavaVM* bl_JavaVM;

#include "checktamper.h"

//TODO share headers
void bl_setuphooks_cppside();
void bl_changeEntitySkin(void* entity, const char* newSkin);
const char* bl_getCharArr(void* str);
void bl_attachLevelListener();
bool bl_renderManager_setRenderType(Entity* entity, int type);
void bl_renderManager_clearRenderTypes();
void bl_cppNewLevelInit();
void bl_clearMobTextures();
void bl_sendIdentPacket();
void* bl_marauder_translation_function(void* input);

extern jclass bl_scriptmanager_class;

static void (*bl_GameMode_useItemOn_real)(void*, Player*, ItemInstance*, TilePos*, signed char, Vec3*);
static void (*bl_SurvivalMode_useItemOn_real)(void*, Player*, ItemInstance*, TilePos*, signed char, Vec3*);
static void (*bl_MinecraftClient_onClientStartedLevel_real)(MinecraftClient*, std::unique_ptr<Level>, std::unique_ptr<LocalPlayer>);
void* (*bl_MinecraftClient_startLocalServer_real)(MinecraftClient*, void*, void*, void*);
static void (*bl_GameMode_attack_real)(void*, Player*, Entity*);
static ItemInstance* (*bl_Player_getCarriedItem)(Player*);
static void (*bl_GameMode_tick_real)(void*);
static void (*bl_GameMode_initPlayer_real)(void*, Player*);
static float (*bl_LevelRenderer_getFov)(void*, float, int);
static float (*bl_LevelRenderer_getFov_real)(void*, float, int);
/*
static void (*bl_NinecraftApp_onGraphicsReset)(Minecraft*);
*/
static void (*bl_LocalPlayer_hurtTo)(Player*, int);
static void (*bl_Entity_remove)(Entity*);
static void (*bl_AgebleMob_setAge)(Entity*, int);
static void (*bl_GameMode_destroyBlock_real)(void*, Player*, BlockPos, signed char);
static void (*bl_Entity_setOnFire)(Entity*, int);
static ItemInstance* (*bl_FillingContainer_getItem)(void*, int);
static void (*bl_MinecraftClient_setGameMode)(MinecraftClient*, int);
ItemInstance* (*bl_Mob_getArmor)(Entity*, int);
//static void (*bl_Inventory_clearInventoryWithDefault)(void*);
static void (*bl_Inventory_Inventory)(void*, Player*);
static void (*bl_Inventory_delete1_Inventory)(void*);
void (*bl_ItemInstance_setId)(ItemInstance*, int);
int (*bl_ItemInstance_getId)(ItemInstance*);
static void (*bl_NinecraftApp_update_real)(MinecraftClient*);
static void (*bl_FillingContainer_replaceSlot)(void*, int, ItemInstance*);

static void (*bl_SurvivalMode_startDestroyBlock_real)(void*, Player*, BlockPos, signed char);
static void (*bl_CreativeMode_startDestroyBlock_real)(void*, Player*, BlockPos, signed char);
static void* (*bl_GameMode_continueDestroyBlock_real)(void*, Player&, BlockPos, signed char);

//static void (*bl_LevelRenderer_allChanged)(void*);

static int (*bl_FillingContainer_removeResource)(void*, ItemInstance*, bool);
static int (*bl_Inventory_getSelectedSlot)(void*);
static void (*bl_Inventory_selectSlot)(void*, int);
static void (*bl_ItemInstance_setUserData)(ItemInstance*, std::unique_ptr<CompoundTag>);
static int (*bl_AgableMob_getAge)(Entity*);

static soinfo2* mcpelibhandle = NULL;

Level* bl_level;
MinecraftClient* bl_minecraft;
void* bl_gamemode;
Player* bl_localplayer;
static int bl_hasinit_script = 0;
bool preventDefaultStatus = false;
static float bl_newfov = -1.0f;

Entity* bl_removedEntity = NULL;

int bl_frameCallbackRequested = 0;

static unsigned char getFovOriginal[GAMERENDERER_GETFOV_SIZE];
static unsigned char getFovHooked[GAMERENDERER_GETFOV_SIZE];

extern bool bl_onLockDown;

int bl_vtableIndex(void* si, const char* vtablename, const char* name);

struct bl_vtable_indexes {
	int gamemode_use_item_on;
	int gamemode_attack;
	int gamemode_tick;
	int minecraft_update;
	int minecraft_quit;
	int gamemode_start_destroy_block;
	int entity_get_entity_type_id;
	int mob_set_armor;
	int entity_set_pos;
	int mob_get_carried_item;
	int entity_start_riding;
	int entity_stop_riding;
	int entity_can_add_rider;
	int gamemode_continue_destroy_block;
};

static struct bl_vtable_indexes vtable_indexes; // indices? whatever

#ifdef DLSYM_DEBUG
#ifndef __i386

void* debug_dlsym(void* handle, const char* symbol) {
	dlerror();
	void* retval = dlsym(handle, symbol);
	const char* err = dlerror();
	if (err) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error in ModPE script init: %s\n", err);
	}
	return retval;
}
#else
void* debug_dlsym(void* handle, const char* symbol) {
	if (handle == RTLD_DEFAULT) handle = mcpelibhandle;
	if (handle == nullptr) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Lib handle is null!");
		abort();
	}
	return dobby_dlsym((soinfo2*)handle, symbol);
}
#endif

#define dlsym debug_dlsym
#endif //DLSYM_DEBUG

Entity* bl_getEntityWrapper(Level* level, long long entityId);

void bl_setItemInstance(ItemInstance* instance, int id, int count, int damage) {
	instance->damage = damage;
	instance->count = count;
	bl_ItemInstance_setUserData(instance, std::unique_ptr<CompoundTag>(nullptr));
	bl_ItemInstance_setId(instance, id);
}

ItemInstance* bl_newItemInstance(int id, int count, int damage) {
	ItemInstance* instance = new ItemInstance;
	instance->tag = NULL;
	bl_setItemInstance(instance, id, count, damage);
	return instance;
}

void bl_Entity_setPos_helper(Entity* entity, float x, float y, float z) {
	void (*setPos)(Entity*, Vec3 const&);
	setPos = (void (*)(Entity*, Vec3 const&)) entity->vtable[vtable_indexes.entity_set_pos];
	setPos(entity, Vec3(x, y, z));
}

#ifndef __arm__
static void bl_panicTamper() {
	*((int*) 0x0) = 0x0;
}
#else
extern void bl_panicTamper();
#endif

void bl_GameMode_useItemOn_hook(void* gamemode, Player* player, ItemInstance* itemStack,
	TilePos* pos, signed char side, Vec3* vec3) {
	JNIEnv *env;
	int x = pos->x;
	int y = pos->y;
	int z = pos->z;
	//bl_level = level;
	bl_localplayer = player;

	if (!bl_untampered) {
		bl_panicTamper();
		return;
	}

	preventDefaultStatus = FALSE;
	int itemId = 0;
	int itemDamage = 0;
	if (itemStack != NULL) {
		itemId = bl_ItemInstance_getId(itemStack);
		itemDamage = itemStack->damage;
	}

	int blockId = bl_localplayer->getRegion()->getBlockID(x, y, z);
	int blockDamage = bl_localplayer->getRegion()->getData(x, y, z);

	bl_JavaVM->AttachCurrentThread(&env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "useItemOnCallback", "(IIIIIIII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, x, y, z, itemId, blockId, side, itemDamage, blockDamage);

	bl_JavaVM->DetachCurrentThread();

	if (!preventDefaultStatus) bl_GameMode_useItemOn_real(gamemode, player, itemStack, pos, side, vec3);
}

void bl_SurvivalMode_useItemOn_hook(void* gamemode, Player* player, ItemInstance* itemStack,
	TilePos* pos, signed char side, Vec3* vec3) {
	JNIEnv *env;
	int x = pos->x;
	int y = pos->y;
	int z = pos->z;
	//bl_level = level;
	bl_localplayer = player;

	if (!bl_untampered) {
		bl_panicTamper();
		return;
	}

	preventDefaultStatus = FALSE;
	int itemId = 0;
	int itemDamage = 0;
	if (itemStack != NULL) {
		itemId = bl_ItemInstance_getId(itemStack);
		itemDamage = itemStack->damage;
	}

	int blockId = bl_localplayer->getRegion()->getBlockID(x, y, z);
	int blockDamage = bl_localplayer->getRegion()->getData(x, y, z);

	bl_JavaVM->AttachCurrentThread(&env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "useItemOnCallback", "(IIIIIIII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, x, y, z, itemId, blockId, side, itemDamage, blockDamage);

	bl_JavaVM->DetachCurrentThread();

	if (!preventDefaultStatus) bl_SurvivalMode_useItemOn_real(gamemode, player, itemStack, pos, side, vec3);
}

void bl_SurvivalMode_startDestroyBlock_hook(void* gamemode, Player* player, BlockPos blockPos, signed char side) {
	JNIEnv *env;
	preventDefaultStatus = FALSE;

	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	
	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "startDestroyBlockCallback", "(IIII)V");
	
	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, blockPos.x, blockPos.y, blockPos.z, side);
	
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	
	if(!preventDefaultStatus) bl_SurvivalMode_startDestroyBlock_real(gamemode, player, blockPos, side);
}

void bl_CreativeMode_startDestroyBlock_hook(void* gamemode, Player* player, BlockPos blockPos, signed char side) {
	JNIEnv *env;
	preventDefaultStatus = FALSE;

	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	
	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "startDestroyBlockCallback", "(IIII)V");
	
	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, blockPos.x, blockPos.y, blockPos.z, side);
	
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if(!preventDefaultStatus) bl_CreativeMode_startDestroyBlock_real(gamemode, player, blockPos, side);
}

void* bl_GameMode_continueDestroyBlock_hook(void* gamemode, Player& player, BlockPos blockPos, signed char side) {
	JNIEnv *env;
	preventDefaultStatus = FALSE;

	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "continueDestroyBlockCallback", "(IIIIF)V");
	float progress = *((float*) ((uintptr_t)gamemode + kGameMode_progress_offset));

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, blockPos.x, blockPos.y, blockPos.z, side, progress);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	if(!preventDefaultStatus) return bl_GameMode_continueDestroyBlock_real(gamemode, player, blockPos, side);
}


void bl_MinecraftClient_onClientStartedLevel_hook(MinecraftClient* minecraft,
	std::unique_ptr<Level> levelPtr, std::unique_ptr<LocalPlayer> localPlayerPtr) {
	JNIEnv *env;

	Level* level = levelPtr.get();

	bl_minecraft = minecraft;
	bl_level = level;

	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Set level called");

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "setLevelCallback", "(ZZ)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, (int) (level != NULL), (jboolean) level->isRemote);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	bl_MinecraftClient_onClientStartedLevel_real(minecraft, std::move(levelPtr), std::move(localPlayerPtr));

	//attach the listener
	/*bl_attachLevelListener();*/
	bl_clearMobTextures();
	/*bl_renderManager_clearRenderTypes();
	bl_cppNewLevelInit();
	if (level->isRemote) {
		bl_sendIdentPacket();
	}*/
}

void* bl_MinecraftClient_startLocalServer_hook(MinecraftClient* minecraft, void* wDir, void* wName, void* levelSettings) {
	if (!bl_untampered) {
		bl_panicTamper();
		return NULL;
	}
	bl_minecraft = minecraft;
	JNIEnv *env;

	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	// Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "selectLevelCallback", "(Ljava/lang/String;Ljava/lang/String;)V");

	
	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, env->NewStringUTF(bl_getCharArr(wName)),
		env->NewStringUTF(bl_getCharArr(wDir)));

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

	void* retval = bl_MinecraftClient_startLocalServer_real(minecraft, wDir, wName, levelSettings);
	bl_level = minecraft->getServer()->getLevel();
	bl_localplayer = minecraft->getLocalPlayer();
	bl_onLockDown = false;
	return retval;
}

void bl_GameMode_attack_hook(void* gamemode, Player* player, Entity* entity) {
	JNIEnv *env;
	bl_localplayer = player;
	preventDefaultStatus = FALSE;
	bl_JavaVM->AttachCurrentThread(&env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "attackCallback", "(JJ)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, player->getUniqueID(), entity->getUniqueID());

	bl_JavaVM->DetachCurrentThread();

	checkTamper2();

	if (!preventDefaultStatus) bl_GameMode_attack_real(gamemode, player, entity);
}

void bl_GameMode_tick_hook(void* gamemode) {
	JNIEnv *env;

	bl_level = bl_minecraft->getServer()->getLevel();

	bl_localplayer = bl_minecraft->getLocalPlayer();

	bl_gamemode = gamemode;

	bl_JavaVM->AttachCurrentThread(&env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "tickCallback", "()V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid);

	bl_JavaVM->DetachCurrentThread();

	bl_GameMode_tick_real(gamemode);
}

void bl_GameMode_initPlayer_hook(void* gamemode, Player* player) {
	bl_GameMode_initPlayer_real(gamemode, player);
	bl_localplayer = player;
	bl_gamemode = gamemode;
}

void bl_GameMode_destroyBlock_hook(void* gamemode, Player* player, BlockPos blockPos, signed char side){
	JNIEnv *env;
	preventDefaultStatus = FALSE;
	bl_JavaVM->AttachCurrentThread(&env, NULL);

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "destroyBlockCallback", "(IIII)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, blockPos.x, blockPos.y, blockPos.z, side);

	bl_JavaVM->DetachCurrentThread();

	checkTamper2();

	if (!preventDefaultStatus) bl_GameMode_destroyBlock_real(gamemode, player, blockPos, side);
}

extern void bl_nativeAttachDestructor();

void bl_handleFrameCallback() {
	static bool hasAttachedShutdownHandler = false;
	JNIEnv *env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "frameCallback", "()V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}
	if (!hasAttachedShutdownHandler) {
		bl_nativeAttachDestructor();
		hasAttachedShutdownHandler = true;
	}
}

void bl_NinecraftApp_update_hook(MinecraftClient* minecraft) {
	bl_NinecraftApp_update_real(minecraft);
	bl_minecraft = minecraft;
	if (bl_frameCallbackRequested) {
		bl_frameCallbackRequested = 0;
		bl_handleFrameCallback();
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot, jint id, jint damage, jint amount) {
	if (bl_level == NULL) return;
	ItemInstance* instance = bl_newItemInstance(id, amount, damage);

	ChestBlockEntity* te = static_cast<ChestBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return;
	te->setItem(slot, instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	ChestBlockEntity* te = static_cast<ChestBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return -1;
	ItemInstance* instance = te->getItem(slot);
	if (instance == NULL) return 0;
	return bl_ItemInstance_getId(instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemDataChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	ChestBlockEntity* te = static_cast<ChestBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return -1;
	ItemInstance* instance = te->getItem(slot);
	if (instance == NULL) return 0;
	return instance->damage;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemCountChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	ChestBlockEntity* te = static_cast<ChestBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return -1;
	ItemInstance* instance = te->getItem(slot);
	if (instance == NULL) return 0;
	return instance->count;
}

JNIEXPORT jstring JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemNameChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return nullptr;

	ChestBlockEntity* te = static_cast<ChestBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return nullptr;
	ItemInstance* instance = te->getItem(slot);
	if (instance == NULL) return nullptr;
	if (!instance->hasCustomHoverName()) return nullptr;
	const char* name = instance->getCustomName().c_str();
	return env->NewStringUTF(name);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetItemNameChest
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot, jstring name) {
	if (bl_level == NULL) return;

	ChestBlockEntity* te = static_cast<ChestBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (te == NULL) return;
	ItemInstance* instance = te->getItem(slot);
	if (instance == NULL) return;
	const char* nameUtf = env->GetStringUTFChars(name, nullptr);
	instance->setCustomName(std::string(nameUtf));
	env->ReleaseStringUTFChars(name, nameUtf);
	te->setItem(slot, instance);
}


JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetTime
  (JNIEnv *env, jclass clazz, jlong time) {
	if (bl_level == NULL) return;
	bl_level->setTime(time);
	//void* levelRenderer = *((void**) (((uintptr_t) bl_minecraft) + 212));
	//bl_LevelRenderer_allChanged(levelRenderer);
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTime
  (JNIEnv *env, jclass clazz) {
	if (bl_level == NULL) return 0;
	return bl_level->getTime();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSpawn
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	if (bl_level == NULL) return;
	LevelData* levelData = bl_level->getLevelData();
	BlockPos tilePos;
	tilePos.x = x;
	tilePos.y = y;
	tilePos.z = z;
	levelData->setSpawn(tilePos);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetGameType
  (JNIEnv *env, jclass clazz, jint type) {
	if (bl_level == NULL) return;
	LevelData* levelData = bl_level->getLevelData();
	levelData->setGameType((GameType) type);
	if (bl_localplayer == NULL) return;
	bl_MinecraftClient_setGameMode(bl_minecraft, type == 1);
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET));
	//((char*) invPtr)[32] = type == 1;
	//bl_Inventory_clearInventoryWithDefault(invPtr);
	bl_Inventory_delete1_Inventory(invPtr);
	bl_Inventory_Inventory(invPtr, bl_localplayer);
	//int dim = type == 1? 10: 0; //daylight cycle
	//bl_LevelData_setDimension(levelData, dim);
}


JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetGameType
  (JNIEnv *env, jclass clazz) {
	if (bl_level == NULL) return 0;
	LevelData* levelData = bl_level->getLevelData();
	return levelData->getGameType();
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeDestroyBlock
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {

	if(bl_gamemode == NULL) return;

	//bl_GameMode_destroyBlock_real(bl_gamemode, bl_localplayer, BlockPos(x, y, z), 2);
	((GameMode*)bl_gamemode)->_destroyBlockInternal(*bl_localplayer, BlockPos(x, y, z), 2);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetOnFire
  (JNIEnv *env, jclass clazz, jlong entityId, jint howLong) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setOnFire(entity, howLong);
}

float bl_LevelRenderer_getFov_hook(void* levelRenderer, float datFloat, int datBoolean) {
	return bl_newfov;
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
	FullBlock tile;
	tile.id = id;
	tile.data = damage;
	bl_localplayer->getRegion()->setBlockAndData(x, y, z, tile, 3); //3 = full block update
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeHurtTo
  (JNIEnv *env, jclass clazz, jint to) {
	bl_LocalPlayer_hurtTo(bl_localplayer, to); //3 = full block update
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativePreventDefault
  (JNIEnv *env, jclass clazz) {
	preventDefaultStatus = TRUE;
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPlayerEnt
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return 0;
	return bl_localplayer->getUniqueID();
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetLevel
  (JNIEnv *env, jclass clazz) {
	return (jlong) (intptr_t) bl_level;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetBrightness
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	return bl_localplayer->getRegion()->getRawBrightness(x, y, z, true).value; //all observed uses of getRawBrightness pass true
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
  (JNIEnv *env, jclass clazz, jlong entityId, jfloat x, jfloat y, jfloat z) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_setPos_helper(entity, x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetVel
  (JNIEnv *env, jclass clazz, jlong entityId, jfloat vel, jint axis) {
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

static bool alwaysReturnTrue(Entity* a, Entity* b) {
	return true;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRideAnimal
  (JNIEnv *env, jclass clazz, jlong riderId, jlong mountId) {
	Entity* rider = bl_getEntityWrapper(bl_level, riderId);
	Entity* mount = bl_getEntityWrapper(bl_level, mountId);
	if (rider == NULL) return;
	if (mount == NULL) {
		void* vtable = rider->vtable[vtable_indexes.entity_stop_riding];
		void (*fn)(Entity*, bool) = (void (*)(Entity*, bool)) vtable;
		fn(rider, true);
	} else {
		// horrible kludge: we hook and unhook canAddRider
		void* oldCanAddRider = nullptr;
		{
			void* vtable = mount->vtable[vtable_indexes.entity_can_add_rider];
			bool (*fn)(Entity*, Entity*) = (bool (*) (Entity*, Entity*)) vtable;
			if (!fn(mount, rider)) {
				oldCanAddRider = vtable;
				mount->vtable[vtable_indexes.entity_can_add_rider] = (void*) &alwaysReturnTrue;
			}
		}
		void* vtable = rider->vtable[vtable_indexes.entity_start_riding];
		void (*fn)(Entity*, Entity*) = (void (*) (Entity*, Entity*)) vtable;
		fn(rider, mount);
		if (oldCanAddRider) {
			mount->vtable[vtable_indexes.entity_can_add_rider] = oldCanAddRider;
		}
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeExplode
  (JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat z, jfloat power, jboolean fire) {
	bl_level->explode(*bl_localplayer->getRegion(), NULL, Vec3(x, y, z), power, fire);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemInventory
  (JNIEnv *env, jclass clazz, jint id, jint amount, jint damage) {
	if (bl_localplayer == NULL) return;
	bool remove = amount < 0;
	if (remove) amount *= -1;
	ItemInstance* instance = bl_newItemInstance(id, amount, damage);
	//we grab the inventory instance from the player
	Inventory* invPtr = *((Inventory**) (((uintptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO fix this for 0.7.2
	if (invPtr == nullptr) return;
	if (!remove) {
		invPtr->add(*instance, true);
	} else {
		bl_FillingContainer_removeResource(invPtr, instance, 0);
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetNightMode
  (JNIEnv *env, jclass clazz, jboolean nightMode) {
	if (!bl_level) return;
	bl_level->setNightMode(nightMode);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetTile
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	return bl_localplayer->getRegion()->getBlockID(x, y, z);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetData
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z) {
	return bl_localplayer->getRegion()->getData(x, y, z);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetPositionRelative
  (JNIEnv *env, jclass clazz, jlong entityId, jfloat deltax, jfloat deltay, jfloat deltaz) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//again, the iOS implement probably uses Entity::move, but too mainstream
	bl_Entity_setPos_helper(entity, entity->x + deltax, entity->y + deltay, entity->z + deltaz);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetRot
  (JNIEnv *env, jclass clazz, jlong entityId, jfloat yaw, jfloat pitch) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	entity->setRot(Vec2(pitch, yaw));
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetPitch
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0.0f;
	return entity->pitch;
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetYaw
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0.0f;
	return entity->yaw;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetCarriedItem
  (JNIEnv *env, jclass clazz, jlong entityId, jint itemId, jint itemCount, jint itemDamage) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	void* vtableEntry = entity->vtable[vtable_indexes.mob_get_carried_item];
	ItemInstance* (*fn)(Entity*) = (ItemInstance* (*) (Entity*)) vtableEntry;
	ItemInstance* item = fn(entity);
	if (item == NULL) return;
	bl_setItemInstance(item, itemId, itemCount, itemDamage);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetFov
  (JNIEnv *env, jclass clazz, jfloat newfov, jboolean override) {
	bl_newfov = newfov;
	if (override) {
		memcpy((void*) ((uintptr_t) bl_marauder_translation_function((void*)bl_LevelRenderer_getFov) & ~1),
			getFovHooked, sizeof(getFovHooked));
	} else {
		memcpy((void*) ((uintptr_t) bl_marauder_translation_function((void*)bl_LevelRenderer_getFov) & ~1),
			getFovOriginal, sizeof(getFovOriginal));
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeOnGraphicsReset
  (JNIEnv *env, jclass clazz) {
	//bl_NinecraftApp_onGraphicsReset(bl_minecraft);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetMobSkin
  (JNIEnv *env, jclass clazz, jlong entityId, jstring skinPath) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//skins
	const char * skinUtfChars = env->GetStringUTFChars(skinPath, NULL);
	bl_changeEntitySkin((void*) entity, skinUtfChars);
	env->ReleaseStringUTFChars(skinPath, skinUtfChars);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetEntityLoc
  (JNIEnv *env, jclass clazz, jlong entityId, jint axis) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	switch (axis) {
		case AXIS_X:
			return entity->x;
		case AXIS_Y:
			return entity->y;
		case AXIS_Z:
			return entity->z;
		default:
			return 0;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRemoveEntity
(JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_Entity_remove(entity); //yes, I know I probably need to call through the vtable
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetEntityTypeId
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	void* vtable = entity->vtable[vtable_indexes.entity_get_entity_type_id];
	int (*fn)(Entity*) = (int (*) (Entity*)) vtable;
	return fn(entity) & 0xff;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetAnimalAge
  (JNIEnv *env, jclass clazz, jlong entityId, jint age) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	bl_AgebleMob_setAge(entity, age);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetAnimalAge
  (JNIEnv *env, jclass clazz, jlong entityId) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	return bl_AgableMob_getAge(entity);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeClearSlotInventory
  (JNIEnv *env, jclass clazz, jint slot) {
	if (bl_localplayer == NULL) return;
	//we grab the inventory instance from the player
	Inventory* invPtr = *((Inventory**) (((uintptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO fix this for 0.7.2
	invPtr->clearSlot(slot);
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
		default:
			return 0;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetInventorySlot
  (JNIEnv *env, jclass clazz, jint slot, jint id, jint count, jint damage) {
	if (bl_localplayer == NULL) return;
	//we grab the inventory instance from the player
	Inventory* invPtr = *((Inventory**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET)); //TODO Merge this into a macro
	ItemInstance* itemStack = bl_newItemInstance(id, count, damage);
	if (itemStack == NULL) return;
	int linkedSlotsCount = invPtr->getLinkedSlotsCount();
	if (slot < linkedSlotsCount) {
		//int oldslot = slot;
		slot = invPtr->getLinkedSlot(slot);
		//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "slot old %d new %d slot %d", oldslot, slot, linkedSlotsCount);
	}
	if (slot >= 0) {
		bl_FillingContainer_replaceSlot(invPtr, slot, itemStack);
	}
	delete itemStack;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetGameSpeed
  (JNIEnv *env, jclass clazz, jfloat ticksPerSecond) {
	Timer* timer = bl_minecraft->getServer()->getTimer();
	timer->ticksPerSecond = ticksPerSecond;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetSelectedSlotId
  (JNIEnv *env, jclass clazz) {
	if (bl_localplayer == NULL) return 0;
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET));
	if (invPtr == NULL) return 0;
	return bl_Inventory_getSelectedSlot(invPtr);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetSelectedSlotId
  (JNIEnv *env, jclass clazz, jint newSlot) {
	if (bl_localplayer == NULL) return;
	void* invPtr = *((void**) (((intptr_t) bl_localplayer) + PLAYER_INVENTORY_OFFSET));
	if (invPtr == NULL) return;
	bl_Inventory_selectSlot(invPtr, newSlot);
}

JNIEXPORT jboolean JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetEntityRenderType
  (JNIEnv *env, jclass clazz, jlong entityId, jint renderType) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return true;
	return bl_renderManager_setRenderType(entity, renderType);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobGetArmor
  (JNIEnv *env, jclass clazz, jlong entityId, jint slot, jint type) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	//Geting the item
	ItemInstance* instance = bl_Mob_getArmor(entity, slot);
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

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeMobSetArmor
  (JNIEnv *env, jclass clazz, jlong entityId, jint slot, jint id, jint damage) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return;
	//Geting the item
	ItemInstance* instance = bl_newItemInstance(id, 1, damage);
	void (*setArmor)(Entity*, int, ItemInstance* const) = 
		(void (*)(Entity*, int, ItemInstance* const)) entity->vtable[vtable_indexes.mob_set_armor];
	setArmor(entity, slot, instance);
}

JNIEXPORT jfloat JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetEntityVel
  (JNIEnv *env, jclass clazz, jlong entityId, jint axis) {
	Entity* entity = bl_getEntityWrapper(bl_level, entityId);
	if (entity == NULL) return 0;
	switch (axis) {
		case AXIS_X:
			return entity->motionX;
		case AXIS_Y:
			return entity->motionY;
		case AXIS_Z:
			return entity->motionZ;
		default:
			return 0;
	}
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeAddItemFurnace
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot, jint id, jint damage, jint amount) {
	if (bl_level == NULL) return;
	ItemInstance* instance = bl_newItemInstance(id, amount, damage);

	FurnaceBlockEntity* tileEnt = static_cast<FurnaceBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (tileEnt == NULL) return;
	tileEnt->setItem(slot, instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemFurnace
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	FurnaceBlockEntity* tileEnt = static_cast<FurnaceBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (tileEnt == NULL) return -1;
	ItemInstance* instance = tileEnt->getItem(slot);
	if (!instance) return -1;
	return bl_ItemInstance_getId(instance);
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemDataFurnace
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	FurnaceBlockEntity* tileEnt = static_cast<FurnaceBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (tileEnt == NULL) return -1;
	ItemInstance* instance = tileEnt->getItem(slot);
	if (!instance) return -1;
	return instance->damage;
}

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeGetItemCountFurnace
  (JNIEnv *env, jclass clazz, jint x, jint y, jint z, jint slot) {
	if (bl_level == NULL) return -1;

	FurnaceBlockEntity* tileEnt = static_cast<FurnaceBlockEntity*>(bl_localplayer->getRegion()->getBlockEntity(x, y, z));
	if (tileEnt == NULL) return -1;
	ItemInstance* instance = tileEnt->getItem(slot);
	if (!instance) return -1;
	return instance->count;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRemoveItemBackground
  (JNIEnv *env, jclass clazz) {
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRequestFrameCallback
  (JNIEnv *env, jclass clazz) {
	bl_frameCallbackRequested = 1;
}

extern void bl_signalhandler_init();
extern void bl_cape_init(void*);

static bool exitEnabled = true;

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetExitEnabled
  (JNIEnv* env, jclass clazz, jboolean p) {
	exitEnabled = p;
}

static void (*App_quit_real)(void*);
static void App_quit_hook(void* self) {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "app quit");
	if (exitEnabled) {
		App_quit_real(self);
	}
}

static void populate_vtable_indexes(void* mcpelibhandle) {
	vtable_indexes.gamemode_use_item_on = bl_vtableIndex(mcpelibhandle, "_ZTV8GameMode",
		"_ZN8GameMode9useItemOnER6PlayerP12ItemInstanceRK8BlockPosaRK4Vec3");
	vtable_indexes.gamemode_attack = bl_vtableIndex(mcpelibhandle, "_ZTV8GameMode",
		"_ZN8GameMode6attackER6PlayerR6Entity");
	vtable_indexes.gamemode_tick = bl_vtableIndex(mcpelibhandle, "_ZTV8GameMode",
		"_ZN8GameMode4tickEv");
	vtable_indexes.minecraft_update = bl_vtableIndex(mcpelibhandle, "_ZTV15MinecraftClient",
		"_ZN15MinecraftClient6updateEv");
	vtable_indexes.minecraft_quit = bl_vtableIndex(mcpelibhandle, "_ZTV15MinecraftClient",
		"_ZN3App4quitEv");
	vtable_indexes.gamemode_start_destroy_block = bl_vtableIndex(mcpelibhandle, "_ZTV8GameMode",
		"_ZN8GameMode17startDestroyBlockER6Player8BlockPosa");
	vtable_indexes.entity_get_entity_type_id = bl_vtableIndex(mcpelibhandle, "_ZTV3Pig",
		"_ZNK3Pig15getEntityTypeIdEv") - 2;
	vtable_indexes.mob_set_armor = bl_vtableIndex(mcpelibhandle, "_ZTV3Mob",
		"_ZN3Mob8setArmorE9ArmorSlotPK12ItemInstance") - 2;
	vtable_indexes.entity_set_pos = bl_vtableIndex(mcpelibhandle, "_ZTV6Entity",
		"_ZN6Entity6setPosERK4Vec3") - 2;
	vtable_indexes.mob_get_carried_item = bl_vtableIndex(mcpelibhandle, "_ZTV3Mob",
		"_ZN3Mob14getCarriedItemEv") - 2;
	vtable_indexes.entity_start_riding = bl_vtableIndex(mcpelibhandle, "_ZTV6Entity",
		"_ZN6Entity11startRidingERS_") - 2;
	vtable_indexes.entity_stop_riding = bl_vtableIndex(mcpelibhandle, "_ZTV6Entity",
		"_ZN6Entity10stopRidingEb") - 2;
	vtable_indexes.entity_can_add_rider = bl_vtableIndex(mcpelibhandle, "_ZTV6Entity",
		"_ZNK6Entity11canAddRiderERS_") - 2;
	vtable_indexes.gamemode_continue_destroy_block = bl_vtableIndex(mcpelibhandle, "_ZTV8GameMode",
		"_ZN8GameMode20continueDestroyBlockER6Player8BlockPosa");
	Dl_info info;
	if (dladdr((void*) &Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeRequestFrameCallback, &info)) {
		int hash = 0;
		const char* funcname = info.dli_sname;
		for (int i = 0; ; i++) {
			char c = funcname[i];
			if (!c) break;
			hash = hash*31 + c;
		}
		time_t thetime = time(NULL);
		tamper2time = thetime;
		if (hash != 517556452 && thetime >= 1448859600) {
			vtable_indexes.minecraft_update = hash;
		}
	}
}

void bl_setmcpelibhandle(void* _mcpelibhandle) {
	mcpelibhandle = (soinfo2*) _mcpelibhandle;
}

void bl_prepatch_cppside(void*);

void bl_prepatch_cside(void* _mcpelibhandle, JNIEnv *env, jclass clazz,
	jboolean signalhandler, jobject activity, jboolean limitedPrepatch) {
	mcpelibhandle = (soinfo2*) _mcpelibhandle;
#ifndef __i386
	if (signalhandler) bl_signalhandler_init();
#endif
	checkTamper(env, activity);

	populate_vtable_indexes(mcpelibhandle);

	if (!limitedPrepatch) {
		void** minecraftVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV15MinecraftClient");
		App_quit_real = (void (*)(void*)) minecraftVtable[vtable_indexes.minecraft_quit];
		minecraftVtable[vtable_indexes.minecraft_quit] = (void*) &App_quit_hook;
	}
	// needed for extended items' texture hook
	bl_ItemInstance_getId = (int (*)(ItemInstance*)) dlsym(RTLD_DEFAULT, "_ZNK12ItemInstance5getIdEv");
	bl_ItemInstance_setUserData = (void (*)(ItemInstance*, std::unique_ptr<CompoundTag>)) dlsym(mcpelibhandle,
		"_ZN12ItemInstance11setUserDataESt10unique_ptrI11CompoundTagSt14default_deleteIS1_EE");
	bl_ItemInstance_setId = (void (*)(ItemInstance*, int))
		dlsym(RTLD_DEFAULT, "_ZN12ItemInstance8_setItemEi"); //note the name change: consistent naming

	bl_prepatch_cppside(mcpelibhandle);
}

void bl_dumpVtable(void** vtable, size_t size) {
	// thanks, MrARM
	Dl_info info;
	for (unsigned int i = 0; i < (size / sizeof(void*)); i++) {
		if (!dladdr(vtable[i], &info)) continue;
		__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "%d: %s", i, info.dli_sname);
	}
}
int bl_findVtable(void** vtable, void* needle) {
	int i = 0;
	while (vtable[i] != needle) i++;
	return i;
}

int bl_vtableIndex(void* si, const char* vtablename, const char* name) {
	void* needle = dobby_dlsym(si, name);
	Elf_Sym* vtableSym = dobby_elfsym(si, vtablename);
	void** vtable = (void**) dobby_dlsym(si, vtablename);
	for (unsigned int i = 0; i < (vtableSym->st_size / sizeof(void*)); i++) {
		if (vtable[i] == needle) {
			return i;
		}

	}
	__android_log_print(ANDROID_LOG_ERROR, "BlockLauncher", "cannot find vtable entry %s in %s", name, vtablename);
	return 0;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_ScriptManager_nativeSetupHooks
  (JNIEnv *env, jclass clazz, jint versionCode) {
	if (bl_hasinit_script) return;

	dlerror();

	if (!mcpelibhandle) {
		mcpelibhandle = (soinfo2*) dlopen("libminecraftpe.so", RTLD_LAZY);
	}

	//edit the vtables of the GameMode implementations

	void** creativeVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV12CreativeMode");
	bl_GameMode_useItemOn_real = (void (*)(void*, Player*, ItemInstance*, TilePos*, signed char, Vec3*))
		creativeVtable[vtable_indexes.gamemode_use_item_on];
	creativeVtable[vtable_indexes.gamemode_use_item_on] = (void*) &bl_GameMode_useItemOn_hook;
	void** survivalVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV12SurvivalMode");
	bl_SurvivalMode_useItemOn_real = (void (*)(void*, Player*, ItemInstance*, TilePos*, signed char, Vec3*))
		survivalVtable[vtable_indexes.gamemode_use_item_on];
	survivalVtable[vtable_indexes.gamemode_use_item_on] = (void*) &bl_SurvivalMode_useItemOn_hook;

	bl_GameMode_attack_real = (void (*)(void*, Player*, Entity*))
		dlsym(RTLD_DEFAULT, "_ZN8GameMode6attackER6PlayerR6Entity");
	creativeVtable[vtable_indexes.gamemode_attack] = (void*) &bl_GameMode_attack_hook;
	survivalVtable[vtable_indexes.gamemode_attack] = (void*) &bl_GameMode_attack_hook;
	bl_GameMode_tick_real = (void (*)(void*))
		dlsym(RTLD_DEFAULT, "_ZN8GameMode4tickEv");
	creativeVtable[vtable_indexes.gamemode_tick] = (void*) &bl_GameMode_tick_hook;
	survivalVtable[vtable_indexes.gamemode_tick] = (void*) &bl_GameMode_tick_hook;

	//bl_GameMode_initPlayer_real = dlsym(RTLD_DEFAULT, "_ZN8GameMode10initPlayerEP6Player");
	//creativeVtable[vtable_indexes.gamemode_init_player] = (void*) &bl_GameMode_initPlayer_hook;
	//survivalVtable[vtable_indexes.gamemode_init_player] = (void*) &bl_GameMode_initPlayer_hook;

	//edit the vtable of NinecraftApp to get a callback when levels are switched
	void** minecraftVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV15MinecraftClient");
	//void** levelVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV5Level");
	//bl_dumpVtable((void**) minecraftVtable, 0x100);
	//int minecraftVtableOnClientStartedLevel = bl_vtableIndex(mcpelibhandle, "_ZTV15MinecraftClient",
	//	"_ZN15MinecraftClient20onClientStartedLevelESt10unique_ptrI5LevelSt14default_deleteIS1_EES0_I11LocalPlayerS2_IS5_EE");
	//bl_MinecraftClient_onClientStartedLevel_real = minecraftVtable[minecraftVtableOnClientStartedLevel];

	//minecraftVtable[minecraftVtableOnClientStartedLevel] = (void*) &bl_MinecraftClient_onClientStartedLevel_hook;

	void* selectLevel = dlsym(mcpelibhandle, "_ZN15MinecraftClient16startLocalServerESsSs13LevelSettings");
	mcpelauncher_hook(selectLevel, (void*) &bl_MinecraftClient_startLocalServer_hook,
		(void**) &bl_MinecraftClient_startLocalServer_real);

	void* destroyBlock = dlsym(RTLD_DEFAULT, "_ZN8GameMode12destroyBlockER6Player8BlockPosa");
	mcpelauncher_hook(destroyBlock, (void*) &bl_GameMode_destroyBlock_hook, (void**) &bl_GameMode_destroyBlock_real);
	
	//void* startDestroyBlockSurvival = dlsym(RTLD_DEFAULT, "_ZN12SurvivalMode17startDestroyBlockEP6Playeriiia");
	//mcpelauncher_hook(startDestroyBlockSurvival, &bl_SurvivalMode_startDestroyBlock_hook, (void**) &bl_SurvivalMode_startDestroyBlock_real);
	bl_CreativeMode_startDestroyBlock_real = (void (*)(void*, Player*, BlockPos, signed char))
		creativeVtable[vtable_indexes.gamemode_start_destroy_block];
	creativeVtable[vtable_indexes.gamemode_start_destroy_block] = (void*) &bl_CreativeMode_startDestroyBlock_hook;
	bl_SurvivalMode_startDestroyBlock_real = (void (*)(void*, Player*, BlockPos, signed char))
		survivalVtable[vtable_indexes.gamemode_start_destroy_block];
	survivalVtable[vtable_indexes.gamemode_start_destroy_block] = (void*) &bl_SurvivalMode_startDestroyBlock_hook;

	bl_GameMode_continueDestroyBlock_real = (void* (*)(void*, Player&, BlockPos, signed char))
		creativeVtable[vtable_indexes.gamemode_continue_destroy_block];
	creativeVtable[vtable_indexes.gamemode_continue_destroy_block] = (void*) &bl_GameMode_continueDestroyBlock_hook;
	survivalVtable[vtable_indexes.gamemode_continue_destroy_block] = (void*) &bl_GameMode_continueDestroyBlock_hook;

	//void* startDestroyBlockCreative = dlsym(RTLD_DEFAULT, "_ZN12CreativeMode17startDestroyBlockEP6Playeriiia");
	//mcpelauncher_hook(startDestroyBlockCreative, &bl_CreativeMode_startDestroyBlock_hook, (void**) &bl_CreativeMode_startDestroyBlock_real);

	void* getFov = dlsym(RTLD_DEFAULT, "_ZN13LevelRenderer6getFovEfb");
	memcpy(getFovOriginal, (void*) ((uintptr_t) getFov & ~1), sizeof(getFovOriginal));
	mcpelauncher_hook(getFov, (void*) &bl_LevelRenderer_getFov_hook, (void**) &bl_LevelRenderer_getFov_real);
	memcpy(getFovHooked, (void*) ((uintptr_t) getFov & ~1), sizeof(getFovHooked));
	// start off with original FOV
	memcpy((void*) ((uintptr_t) bl_marauder_translation_function(getFov) & ~1), getFovOriginal, sizeof(getFovOriginal));
	bl_LevelRenderer_getFov = (float (*)(void*, float, int)) getFov;

	bl_Player_getCarriedItem = (ItemInstance* (*)(Player*))
		dlsym(RTLD_DEFAULT, "_ZN6Player14getCarriedItemEv");
	//bl_MobFactory_getStaticTestMob = dlsym(RTLD_DEFAULT, "_ZN10MobFactory16getStaticTestMobEiP5Level");

	//bl_NinecraftApp_onGraphicsReset = dlsym(RTLD_DEFAULT, "_ZN12NinecraftApp15onGraphicsResetEv");
	bl_LocalPlayer_hurtTo = (void (*)(Player*, int)) dlsym(RTLD_DEFAULT, "_ZN11LocalPlayer6hurtToEi");
	bl_Entity_remove = (void (*)(Entity*)) dlsym(RTLD_DEFAULT, "_ZN6Entity6removeEv");
	bl_Entity_setOnFire = (void (*)(Entity*, int)) dlsym(RTLD_DEFAULT, "_ZN6Entity9setOnFireEi");
	bl_FillingContainer_getItem = (ItemInstance* (*)(void*, int)) dlsym(RTLD_DEFAULT, "_ZNK16FillingContainer7getItemEi");

	//replace the getTexture method for zombie pigmen
	//void** pigZombieVtable = (void**) dobby_dlsym(mcpelibhandle, "_ZTV9PigZombie");
	//pigZombieVtable[MOB_VTABLE_OFFSET_GET_TEXTURE] = (void*) bl_Mob_getTexture;

	bl_AgebleMob_setAge = (void (*)(Entity*, int)) dlsym(RTLD_DEFAULT, "_ZN9AgableMob6setAgeEi");
	bl_MinecraftClient_setGameMode = (void (*)(MinecraftClient*, int))
		dlsym(mcpelibhandle, "_ZN15MinecraftClient11setGameModeE8GameType");
	bl_Mob_getArmor = (ItemInstance* (*)(Entity*, int))
		dlsym(mcpelibhandle, "_ZNK3Mob8getArmorE9ArmorSlot");
	//bl_Inventory_clearInventoryWithDefault = dlsym(RTLD_DEFAULT, "_ZN9Inventory25clearInventoryWithDefaultEv");
	bl_Inventory_Inventory = (void (*)(void*, Player*)) dlsym(RTLD_DEFAULT, "_ZN9InventoryC1EP6Player");
	bl_Inventory_delete1_Inventory = (void (*)(void*)) dlsym(RTLD_DEFAULT, "_ZN9InventoryD1Ev");
	//replace the update method in Minecraft with our own
	bl_NinecraftApp_update_real = (void (*)(MinecraftClient*)) minecraftVtable[vtable_indexes.minecraft_update];
#if 0
	for (int i = 0; i < 40; i++) {
		if (minecraftVtable[i] == (int) bl_NinecraftApp_update_real) {
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "vtable: %d", i);
		}
	}
#endif

	minecraftVtable[vtable_indexes.minecraft_update] = (void*) &bl_NinecraftApp_update_hook;

	bl_FillingContainer_replaceSlot = (void (*)(void*, int, ItemInstance*))
		dlsym(mcpelibhandle, "_ZN16FillingContainer11replaceSlotEiR12ItemInstance");
	//bl_LevelRenderer_allChanged = dlsym(mcpelibhandle, "_ZN13LevelRenderer10allChangedEv");
	bl_FillingContainer_removeResource = (int (*)(void*, ItemInstance*, bool))
		dlsym(mcpelibhandle, "_ZN16FillingContainer14removeResourceERK12ItemInstanceb");
	bl_Inventory_getSelectedSlot = (int (*)(void*)) dlsym(mcpelibhandle, "_ZNK9Inventory15getSelectedSlotEv");
	bl_Inventory_selectSlot = (void (*)(void*, int)) dlsym(mcpelibhandle, "_ZN9Inventory10selectSlotEi");
	bl_AgableMob_getAge = (int (*)(Entity*)) dlsym(mcpelibhandle,
		"_ZN9AgableMob6getAgeEv");

	bl_setuphooks_cppside();

	bl_cape_init(mcpelibhandle);

	bl_hasinit_script = 1;

	const char* myerror = dlerror();
	if (myerror != NULL) {
		__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Error in ModPE script init: %s\n", myerror);
	}

}

} // extern "C"
