#include "jni.h"
#include <cstring>
#include <string>
#include <vector>
#include <dlfcn.h>
#include <android/log.h>

#define cppbool bool

#include "modscript_renderer_jni.h"
#include "modscript_shared.h"

extern "C" {

static void* (*bl_EntityRenderDispatcher_getInstance)();

static EntityRenderer* (*bl_EntityRenderDispatcher_getRenderer)(void*, int);

static void (*bl_MeshBuffer_reset)(void*);

ModelPart* bl_renderManager_getModelPart(int rendererId, const char* modelPartName) {
	MobRenderer* renderer = (MobRenderer*) bl_EntityRenderDispatcher_getRenderer(bl_EntityRenderDispatcher_getInstance(), rendererId);
	HumanoidModel* model = (HumanoidModel*) renderer->model; //TODO: make sure that this is indeed a humanoid model
	if (strcmp(modelPartName, "head") == 0) {
		return &model->bipedHead;
	} else if (strcmp(modelPartName, "body") == 0) {
		return &model->bipedBody;
	} else if (strcmp(modelPartName, "rightArm") == 0) {
		return &model->bipedRightArm;
	} else if (strcmp(modelPartName, "leftArm") == 0) {
		return &model->bipedLeftArm;
	} else if (strcmp(modelPartName, "rightLeg") == 0) {
		return &model->bipedRightLeg;
	} else if (strcmp(modelPartName, "leftLeg") == 0) {
		return &model->bipedLeftLeg;
	} else {
		return NULL;
	}
}

void bl_renderManager_invalidateModelPart(ModelPart* part) {
	void* meshBuffer = (void*) ((uintptr_t) part + 84);
	bl_MeshBuffer_reset(meshBuffer);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelAddBox
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName, jfloat xOffset, jfloat yOffset, jfloat zOffset,
    jint width, jint height, jint depth, jfloat scale, jint textureX, jint textureY, jboolean transparent) {
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	part->textureOffsetX = textureX;
	part->textureOffsetY = textureY;
	part->transparent = transparent;
	bl_ModelPart_addBox(part, xOffset, yOffset, zOffset, width, height, depth, scale);
	bl_renderManager_invalidateModelPart(part);
	env->ReleaseStringUTFChars(modelPartName, utfChars);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_RendererManager_nativeModelClear
  (JNIEnv *env, jclass clazz, jint rendererId, jstring modelPartName) {
	const char * utfChars = env->GetStringUTFChars(modelPartName, NULL);
	ModelPart* part = bl_renderManager_getModelPart(rendererId, utfChars);
	std::vector<Cube*>* cubeVector = (std::vector<Cube*>*) ((uintptr_t) part + 32);
	cubeVector->clear();
	bl_renderManager_invalidateModelPart(part);
	env->ReleaseStringUTFChars(modelPartName, utfChars);
}

void bl_renderManager_init(void* mcpelibhandle) {
	bl_EntityRenderDispatcher_getRenderer = (EntityRenderer* (*) (void*, int))
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getRendererE16EntityRendererId");
	bl_EntityRenderDispatcher_getInstance = (void* (*)())
		dlsym(mcpelibhandle, "_ZN22EntityRenderDispatcher11getInstanceEv");
	bl_MeshBuffer_reset = (void (*)(void*))
		dlsym(mcpelibhandle, "_ZN10MeshBuffer5resetEv");
}

} //extern "C"
