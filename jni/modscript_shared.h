#include <jni.h>
#ifdef __cplusplus
extern "C" {
#endif

typedef void Minecraft;

Minecraft* bl_minecraft;

extern int preventDefaultStatus;

JavaVM* bl_JavaVM;
jclass bl_scriptmanager_class;

void bl_changeEntitySkin(void* entity, const char* newSkin);

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
