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

#ifdef __cplusplus
}
#endif
