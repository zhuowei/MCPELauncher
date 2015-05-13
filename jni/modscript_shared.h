#ifndef _MODSCRIPT_SHARED_H
#define _MODSCRIPT_SHARED_H	1

#include <jni.h>

#include "modscript_structs.h"

#ifdef __cplusplus
#include <array>
extern "C" {
#endif

typedef void Minecraft;

extern Minecraft* bl_minecraft;

extern Level* bl_level;

extern bool preventDefaultStatus;

extern JavaVM* bl_JavaVM;
extern jclass bl_scriptmanager_class;

void bl_changeEntitySkin(void* entity, const char* newSkin);
extern void (*bl_Minecraft_selectLevel_real)(Minecraft*, std::string const&, std::string const&, void*);
void bl_attachLevelListener();
extern Entity* bl_removedEntity;

extern void (*bl_ItemInstance_setId)(ItemInstance*, int);
extern int (*bl_ItemInstance_getId)(ItemInstance*);
ItemInstance* bl_newItemInstance(int id, int count, int damage);
void bl_setItemInstance(ItemInstance* instance, int id, int count, int damage);

extern void* (*bl_TileSource_getTileEntity)(TileSource*, int, int, int);
extern unsigned char bl_TileSource_getTile(TileSource*, int, int, int);
extern int (*bl_TileSource_getData) (TileSource*, int, int, int);
Entity* bl_getEntityWrapper(Level* level, long long entityId);

void bl_clearNameTags();
void bl_sendIdentPacket();

extern Player* bl_localplayer;

void bl_renderManager_init(void* libHandle);
extern void (*bl_ModelPart_addBox)(ModelPart*, float, float, float, int, int, int, float);
void bl_renderManager_setRenderType(Entity* entity, int type);
int bl_renderManager_getRenderType(Entity* entity);
void bl_renderManager_clearRenderTypes();

extern void** bl_EntityRenderDispatcher_instance;
extern EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer)(void*, int);
extern void bl_cape_init(void* mcpelibhandle);
extern ItemInstance* (*bl_Player_getArmor)(Player*, int);

void bl_dumpVtable(void** vtable, size_t size);

#define ITEM_TYPE_STANDARD 0
#define ITEM_TYPE_FOOD 1

#ifdef __cplusplus
extern std::array<std::string, 512> bl_armorRenders;
#endif

void bl_Entity_setPos_helper(Entity*, float, float, float);

#ifdef __cplusplus
}
#endif

#endif
