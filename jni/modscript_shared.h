#include <jni.h>
#ifdef __cplusplus
extern "C" {
#endif

typedef void Minecraft;

Minecraft* bl_minecraft;

void bl_changeEntitySkin(void* entity, const char* newSkin);

#ifdef __cplusplus
}
#endif
