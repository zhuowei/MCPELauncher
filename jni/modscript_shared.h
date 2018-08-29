#ifndef _MODSCRIPT_SHARED_H
#define _MODSCRIPT_SHARED_H	1

#include <jni.h>

#include "modscript_structs.h"
#include "dl_internal.h"

#ifdef __cplusplus
#include <array>
extern "C" {
#endif

class Minecraft;
class MinecraftClient;

extern MinecraftClient* bl_minecraft;

extern Level* bl_level;

extern bool preventDefaultStatus;

extern JavaVM* bl_JavaVM;
extern jclass bl_scriptmanager_class;
class Actor;
void bl_changeEntitySkin(Actor* entity, const char* newSkin);
extern void (*bl_Minecraft_selectLevel_real)(Minecraft*, std::string const&, std::string const&, void*);
void bl_attachLevelListener();
extern Actor* bl_removedEntity;

extern void (*bl_ItemInstance_setId)(ItemInstance*, int);
extern int (*bl_ItemInstance_getId)(ItemInstance*);
ItemInstance* bl_newItemInstance(int id, int count, int damage);
void bl_setItemInstance(ItemInstance* instance, int id, int count, int damage);

Actor* bl_getEntityWrapper(Level* level, long long entityId);

void bl_clearNameTags();
void bl_sendIdentPacket();

extern Player* bl_localplayer;

void bl_renderManager_init(void* libHandle);
bool bl_renderManager_setRenderType(Actor* entity, int type);
int bl_renderManager_getRenderType(Actor* entity);
void bl_renderManager_clearRenderTypes();

extern void** bl_EntityRenderDispatcher_instance;
extern EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer)(void*, int);
extern void bl_cape_init(void* mcpelibhandle);

void bl_dumpVtable(void** vtable, size_t size);

#define ITEM_TYPE_STANDARD 0
#define ITEM_TYPE_FOOD 1

#define BL_ITEMS_EXPANDED_COUNT 4096

#ifdef __cplusplus

namespace mce {
	class TexturePtr;
};

extern std::array<mce::TexturePtr*, BL_ITEMS_EXPANDED_COUNT> bl_armorRenders;
#endif

void bl_Entity_setPos_helper(Actor*, float, float, float);

#ifndef MCPELAUNCHER_LITE
#define DLSYM_DEBUG
#endif

#ifdef DLSYM_DEBUG

void* debug_dlsym(void* handle, const char* symbol);

#define dlsym debug_dlsym
#endif //DLSYM_DEBUG

int bl_vtableIndex(void* si, const char* vtablename, const char* name);

extern cppstr* (*bl_Mob_getTexture)(Entity*);

bool bl_patch_got(soinfo2* mcpelibhandle, void* original, void* newptr);

extern int bl_item_id_count;

bool bl_isActiveLevel(Level* level);


#ifdef __cplusplus
}
#endif

#endif
