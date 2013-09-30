#ifndef _MODSCRIPT_SHARED_H
#define _MODSCRIPT_SHARED_H	1

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "modscript_structs.h"

typedef void Minecraft;

extern Minecraft* bl_minecraft;

extern Level* bl_level;

extern int preventDefaultStatus;

extern JavaVM* bl_JavaVM;
extern jclass bl_scriptmanager_class;

void bl_changeEntitySkin(void* entity, const char* newSkin);
extern void (*bl_Minecraft_selectLevel_real)(Minecraft*, std::string const&, std::string const&, void*);
void bl_attachLevelListener();
extern Entity* bl_removedEntity;

typedef struct {
	void** vtable; //0
	int itemId; //4
	int idunno; //8
	int icon; //12
	int idunno2; //16
	int idunno3; //20
	int idunno4; //24
	int idunno5; //28
	std::string* description; //31
} Item;

#define ITEM_TYPE_STANDARD 0
#define ITEM_TYPE_FOOD 1

#ifdef __cplusplus
}
#endif

#endif
